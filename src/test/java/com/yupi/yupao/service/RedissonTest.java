package com.yupi.yupao.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;

@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;
    @Test
    public void test01()
    {
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("yupi");
        rList.add("123");
        System.out.println("rlist"+rList);

    }
    @Test
    public void test02()
    {
        Jedis jedis = new Jedis("106.54.235.189", 6379);

        // 进行身份验证，替换 "your_password_here" 为你的 Redis 密码
        jedis.auth("123456");

        // 测试是否连接成功
        String select = jedis.select(1);

        System.out.println(select);

        // 关闭连接
        jedis.close();

    }
}
