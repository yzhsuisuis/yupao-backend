package com.yupi.yupao.exception;

import com.yupi.yupao.common.ErrorCode;

public class BussinessException extends RuntimeException{
    /*
    * message是父类的
    * */
    private int code;
    private String description;
    public BussinessException(String message,int code, String description)
    {
        super(message);
        this.code = code;
        this.description = description;
    }
    public BussinessException(ErrorCode errorCode)
    {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();

    }
    public BussinessException(ErrorCode errorCode,String description)
    {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
