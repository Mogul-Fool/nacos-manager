package com.wwj.service.impl;

import com.wwj.bean.NacosConfigYamlFile;
import com.wwj.service.GogsConfigFileService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GogsConfigFileServiceImpl implements GogsConfigFileService {
    @Value("${gogs.server}")
    String gogsAddr;

    @Override
    public String getConfigFromGogs(String fullName, String branchName, String filePath, String token) {
        //发送一个http请求，获取仓库的指定类型的文件
        String resBody;
        String tempString;
        OkHttpClient okHttpClient = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://" + gogsAddr + "/" + fullName
                + "/blob/" + branchName + "/" + filePath).newBuilder();
        urlBuilder.addQueryParameter("token", token);
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            resBody = response.body().string();
            tempString = resBody.split("rawLines\":\\[")[1];
            tempString = tempString.split("\"],\"stylingDirectives")[0];
            String[] yamlStr = tempString.split(",");
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < yamlStr.length - 1; i++) {
                yamlStr[i] = yamlStr[i].substring(1,yamlStr[i].length()-1);
                sb.append(yamlStr[i]);
                sb.append("\n");
            }
            if (StringUtils.isEmpty(resBody)) {
                throw new RuntimeException("响应为空");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<NacosConfigYamlFile> getAllConfigFromGogs(String fullName, String branchName, String serverName, String token) {
        //发送一个http请求，获取仓库的指定类型的文件
        String resBody;
        String[] tempString;
        OkHttpClient okHttpClient = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://" + gogsAddr + "/" + fullName
                + "/tree/" + branchName + "/" + serverName).newBuilder();
        urlBuilder.addQueryParameter("token", token);
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(request);
        List<NacosConfigYamlFile> yamlFiles = new ArrayList<>();
        try {
            Response response = call.execute();
            resBody = response.body().string();
            tempString = resBody.split("items\":\\[\\{\"name\":\"");
            String tempMedium = tempString[1];
            tempMedium = tempMedium.split("templateDirectorySuggestionUrl")[0];
            tempString = tempMedium.split("name\":\"");
            for (String s : tempString) {
                String temp = s.split(".yml\",\"path")[0];
                NacosConfigYamlFile tempFile = new NacosConfigYamlFile();
                tempFile.setDataId(temp + ".yml");
                tempFile.setGroup("DEFAULT_GROUP");
                yamlFiles.add(tempFile);
            }
            return yamlFiles;
        } catch (IOException e) {
            return null;
        }
    }
}
