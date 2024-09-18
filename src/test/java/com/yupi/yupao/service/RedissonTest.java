package com.yupi.yupao.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.yupi.yupao.domain.request.TeamJoinRequest;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
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
    @Test
    public void test03()
    {
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        TeamJoinRequest teamJoinRequest = new TeamJoinRequest();
        teamJoinRequest.setTeamId(12L);
        teamJoinRequest.setPassword("12345");

        Map<String, Object> map = BeanUtil.beanToMap(teamJoinRequest, new HashMap<>()
                , CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor(
                                (name, value) -> value.toString()
                        ));
        Object password = map.get("password");
        System.out.println(map);
        System.out.println(password);
        stringRedisTemplate.opsForHash().putAll("12345",map);
        /*
        *
        *
        * {password=12345, teamId=12}
           12345
        * */
    }
}
