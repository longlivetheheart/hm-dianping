package com.hmdp.exception;

/**
 * @author joe
 * @date 2023/8/6 12:42
 * @description
 */
public class BusinessException extends RuntimeException{
    private static final long serialVersionUID = 1L;
    public BusinessException(String msg) {
        super(msg);
    }
}
