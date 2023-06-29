package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Joe
 * @date 2023/6/29
 */
@Data
public class RedisData {

    private LocalDateTime expireTime;

    private Object data;
}
