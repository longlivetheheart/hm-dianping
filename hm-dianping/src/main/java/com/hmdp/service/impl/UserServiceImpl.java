package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. 校验失败，直接返回
            return Result.fail(SystemConstants.PHONE_ERROR_CODE);
        }
        // 3. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4. 保存验证码到Session/Redis
//        session.setAttribute(SystemConstants.MESSAGE_CODE, code);
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code,
                RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
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

        // 3. 校验验证码,从Redis中获取
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
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

        // 8. 保存用户信息到redis中
        // 8.1 生成随机token，作为登录令牌
        String token = UUID.randomUUID().toString();
        // 8.2 将用户信息User对象转换为Map进行存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) ->
                        fieldValue.toString()));
        // 8.3 存储
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        // note: 由于使用的是StringRedisSerializer，在进行存储到redis序列化时，无法将long类型转化为String类型
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 8.4 设置token有效期
        stringRedisTemplate.expire(tokenKey, 30, TimeUnit.MINUTES);

        // 9. 不要忘记返回token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));

        save(user);
        return user;
    }
}
