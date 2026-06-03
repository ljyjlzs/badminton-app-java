package com.badminton.common;

import lombok.Data;@Data
public class Result<T> {

    private int code;

    private String message;
    private T data;

    private Result() {}

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(400, message);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> unauthorized() {
        return error(401, "未登录或登录已过期");
    }

    public static <T> Result<T> forbidden() {
        return error(403, "无权限执行此操作");
    }

    public static <T> Result<T> notFound() {
        return error(404, "资源不存在");
    }
}
