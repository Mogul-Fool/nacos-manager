package com.wwj.service;

import com.wwj.bean.NacosConfigYamlFile;
import com.wwj.result.Result;

import java.util.List;

public interface NacosConfigManagerService {

    Result updateNacosConfig(String content);

    Result updateAllNacosConfig(String content);

}
