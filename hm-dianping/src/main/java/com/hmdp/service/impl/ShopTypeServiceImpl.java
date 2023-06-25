package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList() {
        // 1. 从Redis中查询
        String key = RedisConstants.SHOP_LIST_KEY;
        Long size = stringRedisTemplate.opsForList().size(key);
        List<String> shopListJson = stringRedisTemplate.opsForList().range(key, 0, size);
        // 2. 存在直接返回
        if (shopListJson != null && !shopListJson.isEmpty()) {
            return shopListJson.stream().map(s -> JSONUtil.toBean(s, ShopType.class)).collect(Collectors.toList());
        }
        // 3. 不存在去数据库查询
        List<ShopType> sort = query().orderByAsc("sort").list();
        // 4. 数据库存在存入Redis
        if (sort != null && !sort.isEmpty()) {
            List<String> json = sort.stream().map(JSONUtil::toJsonStr).collect(Collectors.toList());
            stringRedisTemplate.opsForList().rightPushAll(key, json);
        }
        // 5. 不存在直接返回空List
        return sort;
    }
}
