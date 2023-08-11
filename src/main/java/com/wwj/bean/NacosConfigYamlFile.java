package com.wwj.bean;

import lombok.Data;

@Data
public class NacosConfigYamlFile {
    //通过dataId和group来唯一确定配置文件
    private String dataId;

    private String group;
}
