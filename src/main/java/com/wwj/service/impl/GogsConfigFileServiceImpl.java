package com.wwj.service.impl;

import com.wwj.service.GogsConfigFileService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

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
        HttpUrl.Builder urlBuilder = HttpUrl
                .parse("http://" + gogsAddr + "/" + fullName + "/src/" + branchName + "/" + filePath).newBuilder();
        urlBuilder.addQueryParameter("token", token);
        //http://127.0.0.1:3000/web/test/src/wwj/src/main/resources/config
        String url = urlBuilder.build().toString();
//        System.out.println(url);
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            resBody = response.body().string();
            tempString = resBody.split("<ol class=\"linenums\">")[1];
            tempString = tempString.split("</ol></code></pre></td>")[0];
            String[] yamlStr = tempString.split("</li>");
            String[] yamlStr2 = new String[yamlStr.length];
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < yamlStr.length - 1; i++) {
                yamlStr2[i] = yamlStr[i].split("\">")[1];
                sb.append(yamlStr2[i]);
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

}
