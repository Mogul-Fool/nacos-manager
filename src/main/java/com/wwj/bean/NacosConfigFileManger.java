package com.wwj.bean;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.files")
public class NacosConfigFileManger {

    private List<NacosConfigYamlFile> yamlFiles;

    private List<String> branches;

    private String gogsToken;

}
