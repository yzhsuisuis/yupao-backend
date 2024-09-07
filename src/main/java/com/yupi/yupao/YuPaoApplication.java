package com.yupi.yupao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.yupi.yupao.mapper")

public class YuPaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(YuPaoApplication.class,args);
    }
}
