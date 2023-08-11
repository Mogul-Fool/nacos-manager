package com.wwj.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.utils.StringUtils;
import com.wwj.bean.NacosConfigFileManger;
import com.wwj.bean.NacosConfigYamlFile;
import com.wwj.controller.ReceiveController;
import com.wwj.service.GogsConfigFileService;
import com.wwj.service.NacosConfigManagerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@RefreshScope
public class NacosConfigManagerServiceImpl implements NacosConfigManagerService {

    @Resource
    GogsConfigFileService gogsConfigFileService;

    @Value("${nacos.server.wuxiSand}")
    private String serverAddr_wuxiSand;

    @Value("${nacos.server.nanjingSand}")
    private String serverAddr_nanjingSand;

    @Value("${nacos.server.wuxi}")
    private String serverAddr_wuxi;

    @Value("${nacos.server.nanjing}")
    private String serverAddr_nanjing;

    private static final Logger logger = LogManager.getLogger(ReceiveController.class);

    @Resource
    NacosConfigFileManger nacosConfigFileManger;

    List<NacosConfigYamlFile> ans = new ArrayList<>();
    HashMap<NacosConfigYamlFile, String> ansModified = new HashMap<>();
    HashMap<NacosConfigYamlFile, String> ansRemoved = new HashMap<>();

    @Override
    public List<NacosConfigYamlFile> updateNacosConfig(String content) {

        JSONObject pushContent = JSON.parseObject(content);
        //获取提交文件的分支，仓库和组织
        String[] refSplit = pushContent.getString("ref").split("/");
        String branch = refSplit[refSplit.length - 1];
        String full_name = pushContent.getJSONObject("repository").getString("full_name");
        logger.info("\n" + "更新分支为：" + branch + "\n" + "更新组织和仓库为：" + full_name);
        //限制非指定分支更新
        List<String> branchSum = nacosConfigFileManger.getBranches();
        if (null == branchSum || !branchSum.contains(branch)) {
            return null;
        }
        JSONArray added = pushContent.getJSONArray("commits").getJSONObject(0).getJSONArray("added");
        JSONArray removed = pushContent.getJSONArray("commits").getJSONObject(0).getJSONArray("removed");
        JSONArray modified = pushContent.getJSONArray("commits").getJSONObject(0).getJSONArray("modified");
        logger.info("\n" + "新增文件有：" + added +
                "\n" + "删除文件有：" + removed +
                "\n" + "修改文件有：" + modified);
        boolean flagok;
        flagok = safeAuth(added, "added");
        flagok = flagok && safeAuth(modified, "modified");
        flagok = flagok && safeAuth(removed, "removed");
        if(flagok) {
            updateNacosConfigFile(ansModified, false, full_name, branch);
//            logger.info(ansModified);
            updateNacosConfigFile(ansRemoved, true, full_name, branch);
//            logger.info(ansRemoved);
        } else {
            throw new RuntimeException("文件信息有误");
        }
        return ans;
    }

    public boolean safeAuth(JSONArray fileList, String str) {
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
                    file = nacosConfigYamlFile;
                }
            }
            if(StringUtils.isEmpty(file.getDataId()) || StringUtils.isEmpty(file.getGroup())){
                return false;
            }
            if(str.equals("added") || str.equals("modified")) {
                ansModified.put(file, filePath);
            } else if(str.equals("removed")){
                ansRemoved.put(file,filePath);
            }
            ans.add(file);
        }
        return true;
    }

    private void updateNacosConfigFile(HashMap<NacosConfigYamlFile, String> fileList, boolean isDelete, String full_name, String branch) {
        for (HashMap.Entry<NacosConfigYamlFile, String> entry : fileList.entrySet()) {
            if(!isDelete) {
                String fileContent = gogsConfigFileService.getConfigFromGogs(full_name, branch, entry.getValue(), nacosConfigFileManger.getGogsToken());
                handleNacosConfig(entry.getKey().getDataId(), entry.getKey().getGroup(), fileContent, false);
                if(!StringUtils.isEmpty(fileContent)){
                    logger.info("\n从gogs中获取"+entry.getKey().getDataId()+"成功");
                }
            } else {
                handleNacosConfig(entry.getKey().getDataId(), entry.getKey().getGroup(), "", true);
            }
        }
    }

    public void handleNacosConfig(String dataId, String group, String content, boolean isDeleted) {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr_wuxiSand);
        boolean isPublishOk;
        try {
            ConfigService configService = NacosFactory.createConfigService(properties);
            if(isDeleted){
                isPublishOk = configService.removeConfig(dataId, group);
            }else {
                isPublishOk = configService.publishConfig(dataId, group, content, "yaml");
            }
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        if(isPublishOk){
            if(isDeleted){
                logger.info("\n" + dataId + "已删除");
            }else {
                logger.info("\n" + dataId+"已更新");
            }
        }else {
            logger.info("\n" + dataId+"发布失败");
        }
    }
}
