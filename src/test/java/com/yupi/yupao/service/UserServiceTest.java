package com.yupi.yupao.service;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


import com.yupi.yupao.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.Assert;

import javax.annotation.Resource;

@SpringBootTest
public class UserServiceTest {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;
    @Test
    public void test01()
    {
        User user = new User();
        user.setId(0L);
        user.setUsername("yangzihe");
        user.setAvatarUrl("1111");
        user.setUserAccount("1234");
        user.setGender(0);
        user.setUserPassword("1111");
        user.setPhone("12333");
        user.setEmail("1111111");
        user.setUserStatus(0);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);
        userService.save(user);



    }

    @Test
    public void userRegisterTest()
    {
        String userAccount = "yupi111";
        String userPassword = "1111111111";
        String checkPassword = "1111111111";
        String planetCode = "11111111";
        long result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        System.out.println(result);
        /*
        * 用户名重复不能再注册
        * */

        userAccount = "yupi111";
        result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);

        /*
        * 用户名长度不够
        * */
        userAccount = "111";
        result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);

        /*
        * 用户名称里面带特殊符号

        * */
        userAccount = "111??";
        result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);

        /*
        * 验证密码和密码相同
        * */
        userAccount = "12345";
        userPassword = "123445556";
        checkPassword = "1234455561";
        result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);
        /*
        * 验证密码的长度必须都>8才可以
        *
        * */
        userAccount = "12345";
        userPassword = "12344";
        checkPassword = "12344";
        result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        Assertions.assertEquals(-1,result);
    }
    @Test
    public void userRegisterTest01()
    {
        String userAccount = "123456789";
        String userPassword = "123456789";
        String checkPassword = "123456789";


    }
    
    @Test
    public void testSearchUsersByTags()
    {
        List<String> tagNameList = Arrays.asList("c++");
        List<User> usersList = userService.searchUsersByTags(tagNameList);
        System.out.println(usersList);
    }
    @Test
    public void insertUsers()
    {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("yang","bo");
        valueOperations.set("111","bo");
        /*
        * 这一步是没问题的
        * */
        User user = new User();
        user.setUserAccount("972849883");
        valueOperations.set("yagebi",user);
        System.out.println(valueOperations.get("yagebi"));


    }

}