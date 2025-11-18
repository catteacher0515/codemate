package com.pingyu.codematebackend.common;

/**
 * 错误码枚举 (Error Code Enum)
 * * 约定：
 * 1. 0 - 代表成功
 * 2. 400xx - 客户端/请求错误 (如参数错误)
 * 3. 401xx - 认证错误 (如未登录、无权限)
 * 4. 404xx - 资源未找到
 * 5. 500xx - 服务器内部错误
 */

public enum ErrorCode {

    // --- 0: 成功 ---
    SUCCESS(0, "ok"),

    // --- 400xx: 客户端错误 ---
    PARAMS_ERROR(40000, "请求参数错误"),
    USERNAME_TAKEN(40001,"用户名已被占用"), // <-- 我们新加的

    // --- 401xx: 认证错误 ---
    NOT_LOGGED_IN(40100, "用户未登录，请重新登录"), // <-- 我们新加的
    NO_AUTH(40101, "无权限访问"),

    // --- 404xx: 资源未找到 ---
    NOT_FOUND_ERROR(40400, "请求的资源不存在"), // <-- 我们新加的

    // --- 500xx: 服务器错误 ---
    SYSTEM_ERROR(50000, "服务器开小差了，请稍后再试"),

    NO_AUTH_ERROR(40000,"无权查看"),
    FORBIDDEN(40000,"加入队伍错误" ),
    NULL_ERROR(40000,"队伍已满,无法邀请" ); // <-- 必须有这个兜底


    /**
     * 业务状态码
     */
    private final int code;

    /**
     * 业务消息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}