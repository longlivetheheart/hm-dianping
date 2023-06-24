package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Joe
 * @date 2023/6/24
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取Session中的用户信息
        HttpSession session = request.getSession();
        Object user = session.getAttribute(SystemConstants.USER);
        // 2. 判断用户是否存在
        if (user == null) {
            // 3. 不存在进行拦截,返回状态码401-Unauthorized
            response.setStatus(401);
            return false;
        }

        // 4. 保存用户到ThreadLocal
        UserHolder.saveUser(BeanUtil.copyProperties(user, UserDTO.class));
        // 5. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户，避免内存泄露
        UserHolder.removeUser();
    }
}
