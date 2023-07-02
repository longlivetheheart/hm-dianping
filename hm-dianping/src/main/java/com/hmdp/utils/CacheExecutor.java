package com.hmdp.utils;

import java.util.concurrent.*;

/**
 * @author Joe
 * @date 2023/7/2
 */
public class CacheExecutor {
    public static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            10,
            10,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
}
