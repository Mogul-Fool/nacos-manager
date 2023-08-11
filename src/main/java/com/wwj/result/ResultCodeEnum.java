package com.wwj.result;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    SUCCESS(200,"发布成功"),
    FAIL(201, "发布失败"),
    FILE_ERROR(202,"文件错误"),
    CONNECT_ERROR(203, "连接失败"),
    CONTEXT_ERROR(204, "文件内容异常");

    private Integer code;

    private String message;

    private ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
