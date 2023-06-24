package com.hmdp.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. 校验失败，直接返回
            return Result.fail(SystemConstants.PHONE_ERROR_CODE);
        }
        // 3. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4. 保存验证码到Session
        session.setAttribute(SystemConstants.MESSAGE_CODE, code);
        // 5. 发送验证码
        log.info("发送验证码成功：{}", code);
        // 6. 返回成功信息
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. 不符合返回错误信息
            return Result.fail(SystemConstants.PHONE_ERROR_CODE);
        }

        // 3. 校验验证码
        Object cacheCode = session.getAttribute(SystemConstants.MESSAGE_CODE);
        String code = loginForm.getCode();

        if (cacheCode == null || !cacheCode.equals(code)) {
            // 4. 不一致，报错
            return Result.fail(SystemConstants.VERIFY_CODE_ERROR);
        }

        // 5. 一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 6. 判断用户是否存在
        if (user == null) {
            // 7. 不存在，创建用户并入库
            user = createUserWithPhone(phone);
        }

        // 8. 将用户信息存入Session
        session.setAttribute("user", user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));

        save(user);
        return user;
    }
}
