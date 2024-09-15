package com.yupi.yupao.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.zip.CheckedOutputStream;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissionConfig {

    private String port;

    private String host;

    @Bean
    public RedissonClient redissonClient()
    {
        Config config = new Config();
        String address = String.format("redis://%s:%s", host,port);
        config.useSingleServer().setAddress(address).setDatabase(0);
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}
