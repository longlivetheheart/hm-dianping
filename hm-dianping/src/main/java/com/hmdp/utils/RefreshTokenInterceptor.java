package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Joe
 * @date 2023/6/24
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取token
        String token = request.getHeader(SystemConstants.AUTHORIZATION);
        if (StrUtil.isBlank(token)) {
            // 直接进入下一层拦截器
            return true;
        }
        // 2. 根据token判断用户是否存在
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> user = stringRedisTemplate.opsForHash().entries(tokenKey);
        if (user.isEmpty()) {
            return true;
        }
        // 3. 存储用户信息到ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(user, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        // 4. 对一切操作刷新token有效期
        stringRedisTemplate.expire(tokenKey, 30, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
