package com.yupi.yupao.common;

/*
* 疑惑:这里为什么有的时候可以不加<T>BaseResponse<T>
* */
public class ResultUtils {
    public static <T>BaseResponse<T>success(T data)
    {
        return new BaseResponse<>(0,data,"ok","");

    }
    public static <T>BaseResponse<T>error(ErrorCode errorCode)
    {
        return new BaseResponse<>(errorCode);

    }
    public static BaseResponse error(int code,String message,String description)
    {
        return new BaseResponse(code,message,description);

    }
    public static BaseResponse error(ErrorCode errorCode,String message,String description)
    {
        return new BaseResponse(errorCode.getCode(),message,description);

    }

}
