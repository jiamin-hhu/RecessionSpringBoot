package com.hhu.recession.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

@Data
public class ApiResp<T> implements Serializable {
    private static final long serialVersionUID = 1900200612748443408L;

    //正常响应码
    private static final int SUCCESS_CODE = 200;
    //正常响应消息
    private static final String SUCCESS_MSG = "SUCCESS";

    //错误码
    private int code = SUCCESS_CODE;
    //错误信息
    private String msg = SUCCESS_MSG;
    //响应内容，默认为null
    private T data = null;

    /**
     * 是否正常响应 true=正常；false=异常
     * @return
     */
    @JsonIgnore
    public boolean isOK() {
        return code == SUCCESS_CODE;
    }

    /**
     *
     * @return 无data的正常返回
     */
    public static ApiResp retOK() {
        return new ApiResp();
    }

    /**
     * 有data的正常返回
     * @param data data内容
     * @param <T> data类型
     */
    public static <T> ApiResp<T> retOK(T data) {
        ApiResp<T> response = new ApiResp<>();
        response.setData(data);
        return response;
    }

    /**
     * 无data的失败返回
     *
     * @param msg 错误信息
     */
    public static <T> ApiResp<T> retFail(String msg) {
        ApiResp<T> response = new ApiResp<>();
        response.setCode(3001);
        response.setMsg(msg);
        return response;
    }


    /**
     * 无data的失败返回
     *
     * @param code 错误码
     * @param msg 错误信息
     */
    public static <T> ApiResp<T> retFail(int code, String msg) {
        ApiResp<T> response = new ApiResp<>();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }

    /**
     * 有data的失败返回
     * 失败返回的场景不多，所以没有严格要求T泛型
     * @param code 错误码
     * @param msg 错误信息
     */
    public static <T> ApiResp<T> retFail(int code, String msg, T data) {
        ApiResp<T> response = new ApiResp<>();
        response.setCode(code);
        response.setMsg(msg);
        response.setData(data);
        return response;
    }

}
