package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//        Shop shop = queryWithPassThrough(id);
        // 互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 6. 返回查询信息
        return Result.ok(shop);
    }

    private Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        boolean isEmpty = false;
        // 1. 先去Redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在，存在直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断返回的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }
        Shop shop = null;
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            // 3. 实现缓存重建
            // 3.1 获取互斥锁
            boolean isLock = tryLock(lockKey);
            // 3.2 判断是否获取成功
            if (!isLock) {
                // 3.3 失败，休眠并重试
                Thread.sleep(200);
                return queryWithMutex(id);
            }
            // 3.4 成功，根据id查询数据库-Note:这里应该再次校验是否存在
            DoubleCheckResult doubleCheckResult = doubleCheck(key);
            if (!doubleCheckResult.isEmpty()) {
                return shop;
            }
            if (doubleCheckResult.getShop() != null) {
                return doubleCheckResult.getShop();
            }

            shop = getById(id);
            // 4. 数据库也不存在返回错误信息
            if (shop == null) {
                // 将空值写入Redis
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            }
            // 5. 数据库存在，放入缓存
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
                    RedisConstants.CACHE_SHOP_TTL + RandomUtil.randomLong(5), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 6. 释放锁
            unlock(lockKey);
        }
        return shop;
    }

    private DoubleCheckResult doubleCheck(String key) {

        DoubleCheckResult doubleCheckResult = new DoubleCheckResult();
        String shopDoubleCheck = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopDoubleCheck)) {
            doubleCheckResult.setShop(BeanUtil.toBean(shopDoubleCheck, Shop.class));
            doubleCheckResult.setEmpty(false);
        } else doubleCheckResult.setEmpty(shopDoubleCheck == null);
        return doubleCheckResult;
    }
    private Shop queryWithPassThrough(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 先去Redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在，存在直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断返回的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }
        // 3. 不存在去数据库查询
        Shop shop = getById(id);
        // 4. 数据库也不存在返回错误信息
        if (shop == null) {
            // 将空值写入Redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
        }
        // 5. 数据库存在，放入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL + RandomUtil.randomLong(5), TimeUnit.MINUTES);
        return shop;
    }

    // 获取锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        // 1. 先更新数据库
        Long shopId = shop.getId();
        if (shopId == null) {
            return Result.fail("店铺Id不能为空！");
        }
        updateById(shop);
        String key = RedisConstants.CACHE_SHOP_KEY + shopId;
        // 2. 删除缓存
        stringRedisTemplate.delete(key);
        return Result.ok();
    }

    @Data
    static class DoubleCheckResult {
        private boolean empty;

        private Shop shop;
    }
}
