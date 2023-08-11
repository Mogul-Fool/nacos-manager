package com.wwj.service;

import com.wwj.bean.NacosConfigYamlFile;

import java.util.List;

public interface GogsConfigFileService {
    String getConfigFromGogs(String fullName, String branchName, String filePath, String token);

    List<NacosConfigYamlFile> getAllConfigFromGogs(String fullName, String branchName, String serverName, String token);

}
