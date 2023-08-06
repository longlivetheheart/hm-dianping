package com.hmdp.exception;

import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class WebExceptionAdvice {
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Result defaultExceptionHandler(HttpServletRequest request, Exception e) {
        log.error(e.getMessage(), e);
        return Result.fail("System error");
    }

    @ExceptionHandler(value = BusinessException.class)
    @ResponseBody
    public Result businessExceptionHandler(HttpServletRequest request, Exception e) {
        log.error(e.getMessage(), e);
        return Result.fail("Business error");
    }
}
