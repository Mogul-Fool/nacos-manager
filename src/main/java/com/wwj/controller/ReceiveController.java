package com.wwj.controller;

import com.wwj.bean.NacosConfigYamlFile;
import com.wwj.service.NacosConfigManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Api(value="webhook")
public class ReceiveController {

    @Resource
    NacosConfigManagerService nacosConfigManagerService;

    @ApiOperation("Nacos更新")
    @PostMapping("hook")
    public List<NacosConfigYamlFile> receiveHookRequest(@RequestBody String content) {
        return nacosConfigManagerService.updateNacosConfig(content);
    }
}
