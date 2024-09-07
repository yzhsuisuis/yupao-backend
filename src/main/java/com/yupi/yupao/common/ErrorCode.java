package com.yupi.yupao.common;

/**
 * 枚举类，定义了应用中使用的错误代码。
 */
public enum ErrorCode {
    /**
     * 返回成功
     */
    SUCCESS(0,"ok",""),
    PARAMS_ERROR(40000,"请求参数错误",""),
    NULL_ERROR(40001,"请求数据为空",""),
    SYSTEM_ERROR(50000,"系统内部异常",""),

    /**
     * 没有权限的错误代码。
     */
    NO_AUTH(40101, "没有权限", ""),

    /**
     * 没有登录的错误代码。
     */
    NOT_LOGIN(40100, "未登录", "");




    /**
     * 错误代码。
     */
    private final int code;

    /**
     * 错误消息。
     */
    private final String message;

    /**
     * 错误描述。
     */
    private final String description;

    /**
     * ErrorCode枚举的构造函数。
     *
     * @param code         错误代码
     * @param message      错误消息
     * @param description  错误描述
     */
    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    /**
     * 获取错误代码。
     *
     * @return 错误代码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取错误描述。
     *
     * @return 错误描述
     */
    public String getDescription() {
        return description;
    }
}