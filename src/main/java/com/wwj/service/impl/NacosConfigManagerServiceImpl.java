package com.wwj.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.wwj.bean.NacosConfigFileManger;
import com.wwj.bean.NacosConfigServer;
import com.wwj.bean.NacosConfigYamlFile;
import com.wwj.result.Result;
import com.wwj.service.GogsConfigFileService;
import com.wwj.service.NacosConfigManagerService;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@RefreshScope
public class NacosConfigManagerServiceImpl implements NacosConfigManagerService {

    @Resource
    GogsConfigFileService gogsConfigFileService;
    @Resource
    NacosConfigFileManger nacosConfigFileManger;
    HashMap<NacosConfigYamlFile, String> ansModified = new HashMap<>();
    HashMap<NacosConfigYamlFile, String> ansRemoved = new HashMap<>();

    @Override
    public Result updateAllNacosConfig(String content) {

        JSONObject pushContent = JSON.parseObject(content);
        String full_name = pushContent.getJSONObject("repository").getString("full_name");
        String branch = pushContent.getJSONObject("pull_request").getJSONObject("head").getString("ref");
        //限制非指定分支更新
        List<String> branchSum = nacosConfigFileManger.getBranches();
        if (null == branchSum || !branchSum.contains(branch)) {
            return Result.fail("分支不对");
        }
        List<NacosConfigServer> servers = nacosConfigFileManger.getNacosServers();
        for(NacosConfigServer server : servers) {
            List<NacosConfigYamlFile> yamlFiles = gogsConfigFileService.getAllConfigFromGogs(full_name, branch, server.getNacosName(), nacosConfigFileManger.getToken());
            if(yamlFiles == null) {
                return Result.fail();
            } else {
                for(NacosConfigYamlFile tempFile : yamlFiles) {
                    String filepath = server.getNacosName() + "/" +tempFile.getDataId();
                    String fileContent = gogsConfigFileService.getConfigFromGogs(full_name, branch, filepath, nacosConfigFileManger.getToken());
                    handleNacosConfig(tempFile.getDataId(), tempFile.getGroup(), fileContent, server.getNacosAddr(), false);
                }
            }
        }
        return Result.ok();
    }

    @Override
    public Result updateNacosConfig(String content) {
        JSONObject pushContent = JSON.parseObject(content);
        //获取提交文件的分支，仓库和组织
        String[] refSplit = pushContent.getString("ref").split("/");
        String branch = refSplit[refSplit.length - 1];
        String full_name = pushContent.getJSONObject("repository").getString("full_name");
        //限制非指定分支更新
        List<String> branchSum = nacosConfigFileManger.getBranches();
        if (null == branchSum || !branchSum.contains(branch)) {
            return Result.fail("分支不对");
        }
        JSONArray added = pushContent.getJSONArray("commits").getJSONObject(0).getJSONArray("added");
        JSONArray removed = pushContent.getJSONArray("commits").getJSONObject(0).getJSONArray("removed");
        JSONArray modified = pushContent.getJSONArray("commits").getJSONObject(0).getJSONArray("modified");
        boolean flagok;
        flagok = safeAuth(added, "added");
        flagok = flagok && safeAuth(modified, "modified");
        flagok = flagok && safeAuth(removed, "removed");
        if(flagok) {
            updateNacosConfigFile(ansModified, false, full_name, branch);
            updateNacosConfigFile(ansRemoved, true, full_name, branch);
        } else {
            return Result.fail("文件不对");
        }
        return Result.ok();
    }

    public boolean safeAuth(JSONArray fileList, String str) {
        boolean flag = false;
        if(fileList == null) {
            return true;
        }
        NacosConfigYamlFile file = new NacosConfigYamlFile();
        for (int i = 0; i < fileList.size(); i++) {
            String filePath = fileList.getString(i);
            String[] filePathSplit = filePath.split("/");
            String fileName = filePathSplit[filePathSplit.length - 1];
            List<NacosConfigYamlFile> yamlFiles = nacosConfigFileManger.getYamlFiles();
            for (NacosConfigYamlFile nacosConfigYamlFile : yamlFiles) {
                if(nacosConfigYamlFile.getDataId().equals(fileName)){
                    if(str.equals("added") || str.equals("modified")) {
                        ansModified.put(nacosConfigYamlFile, filePath);
                    } else if(str.equals("removed")){
                        ansRemoved.put(nacosConfigYamlFile,filePath);
                    }
                    flag = true;
                    break;
                }
                flag = false;
            }
            if(!flag)
                break;
        }
        return flag;
    }

    private void updateNacosConfigFile(HashMap<NacosConfigYamlFile, String> fileList, boolean isDelete, String full_name, String branch) {
        for (HashMap.Entry<NacosConfigYamlFile, String> entry : fileList.entrySet()) {
            //找到对应nacos发布地址
            List<NacosConfigServer> servers = nacosConfigFileManger.getNacosServers();
            String nacosName = entry.getValue().split("/")[0];
            String nacosAddr = "";
            for(NacosConfigServer server : servers) {
                if(server.getNacosName().equals(nacosName)) {
                    nacosAddr = server.getNacosAddr();
                    break;
                }
            }
            if(!isDelete) {
                String fileContent = gogsConfigFileService.getConfigFromGogs(full_name, branch, entry.getValue(), nacosConfigFileManger.getToken());
                handleNacosConfig(entry.getKey().getDataId(), entry.getKey().getGroup(), fileContent, nacosAddr,false);
            } else {
                handleNacosConfig(entry.getKey().getDataId(), entry.getKey().getGroup(), "", nacosAddr,true);
            }
        }
    }

    public Result handleNacosConfig(String dataId, String group, String content, String nacosAddr, boolean isDeleted) {
        Properties properties = new Properties();
        properties.put("serverAddr", nacosAddr);
        try {
            ConfigService configService = NacosFactory.createConfigService(properties);
            if(isDeleted){
                configService.removeConfig(dataId, group);
            }else {
                configService.publishConfig(dataId, group, content, "yaml");
            }
        } catch (NacosException e) {
            return Result.fail("发布失败");
        }
        return Result.ok();
    }
}
