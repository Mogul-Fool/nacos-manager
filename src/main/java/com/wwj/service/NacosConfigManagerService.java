package com.wwj.service;

import com.wwj.bean.NacosConfigYamlFile;

import java.util.List;

public interface NacosConfigManagerService {

    List<NacosConfigYamlFile> updateNacosConfig(String content);

}
