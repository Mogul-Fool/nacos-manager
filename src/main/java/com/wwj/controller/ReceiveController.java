package com.wwj.controller;

import com.wwj.bean.NacosConfigYamlFile;
import com.wwj.result.Result;
import com.wwj.service.NacosConfigManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Api(value="webhooks")
public class ReceiveController {

    @Resource
    NacosConfigManagerService nacosConfigManagerService;

    @ApiOperation("Nacos配置文件（新增，删除，修改）")
    @PostMapping("hook")
    public Result receiveHookRequest(@RequestBody String content) {
        return nacosConfigManagerService.updateNacosConfig(content);
    }

    @ApiOperation("Nacos配置文件（全部更新）")
    @PostMapping("hookall")
    public Result receiveHookALLRequest(@RequestBody String content) {
        return nacosConfigManagerService.updateAllNacosConfig(content);
    }

}
