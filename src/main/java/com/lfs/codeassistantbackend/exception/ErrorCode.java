package com.lfs.codeassistantbackend.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统异常"),
    NOT_FOUND(404, "请求地址不存在"),
    PARAMETER_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "没有权限"),
    OPERATION_ERROR(500, "操作失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
