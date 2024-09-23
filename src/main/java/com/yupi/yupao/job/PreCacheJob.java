package com.yupi.yupao.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupao.domain.User;
import com.yupi.yupao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Slf4j
//@Component
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;
     private List<Long> mainUserList = Arrays.asList(1L);

    @Scheduled(cron = "0 * * * * ?")
    public void doCacheRecommendUsers ()
    {
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");
        try {
            System.out.print("getLock-------"+Thread.currentThread().getId());
            //lock.tryLock()
            if(lock.tryLock(0L,30000L,TimeUnit.MILLISECONDS))
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            //看门狗机制
            if(lock.isHeldByCurrentThread())
            {
                System.out.println("unlock------"+Thread.currentThread().getId());
                lock.unlock();

            }
        }


    }

}
