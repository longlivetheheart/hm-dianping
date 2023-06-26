package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
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
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 先去Redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在，存在直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
        }
        // 3. 不存在去数据库查询
        Shop shop = getById(id);
        // 4. 数据库也不存在返回错误信息
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 5. 数据库存在，放入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 6. 返回查询信息
        return Result.ok(shop);
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
}
