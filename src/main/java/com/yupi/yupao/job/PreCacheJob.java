package com.yupi.yupao.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupao.domain.User;
import com.yupi.yupao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;
     private List<Long> mainUserList = Arrays.asList(1L);
    @Scheduled(cron = "0 59 23 * * ?")
    public void doCacheRecommendUsers ()
    {
        for (Long userid : mainUserList) {
            String redisKeys = String.format("yupao:user:recommned:%s",userid);
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();

            Page<User> userPage = userService.page(new Page<>(1, 20), userQueryWrapper);

            ValueOperations valueOperations = redisTemplate.opsForValue();
            valueOperations.set(redisKeys,userPage,300000, TimeUnit.MILLISECONDS);

            try {
                valueOperations.set(redisKeys,userPage,100000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.info("redis key set error" + e);
            }


        }

    }

}
