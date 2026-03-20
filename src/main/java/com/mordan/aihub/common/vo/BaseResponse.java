package com.mordan.aihub.common.vo;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 */
public record BaseResponse<T>(
    /** 状态码 */
    int code,
    /** 返回数据 */
    T data,
    /** 返回消息 */
    String message
) implements Serializable {

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(int code, String message) {
        this(code, null, message);
    }
}
