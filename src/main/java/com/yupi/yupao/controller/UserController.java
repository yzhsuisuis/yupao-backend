package com.yupi.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupao.common.BaseResponse;
import com.yupi.yupao.exception.BussinessException;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.common.ResultUtils;
import com.yupi.yupao.domain.User;
import com.yupi.yupao.domain.request.UserLoginRequest;
import com.yupi.yupao.domain.request.UserRegisetRequest;
import com.yupi.yupao.mapper.UserMapper;
import com.yupi.yupao.service.UserService;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yupi.yupao.constant.UserConstant.ADMIN_ROLE;
import static com.yupi.yupao.constant.UserConstant.User_LOGIN_STATE;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate redisTemplate;
    /**
     * 用户注册
     * @param userRegisetRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisetRequest userRegisetRequest)
    {
        if(userRegisetRequest==null)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisetRequest.getUserAccount();
        String userPassword = userRegisetRequest.getUserPassword();
        String checkPassword = userRegisetRequest.getCheckPassword();
        String planetCode = userRegisetRequest.getPlanetCode();
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode))
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);

        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request)
    {
        if(userLoginRequest == null)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        if(StringUtils.isAllBlank(userAccount,userPassword))
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);


        return ResultUtils.success(user);


    }
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username,HttpServletRequest request)
    {
        if(!userService.isAdmin(request))
        {
            throw new BussinessException(ErrorCode.NO_AUTH);

        }

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        /*
        * 注意这里如果不加user特判的话可能会报空指针异常,因为他要传一个对象
        * */
        if(StringUtils.isNotBlank(username))
        {
            userQueryWrapper.like("username",username);
        }

        List<User> list = userService.list(userQueryWrapper);
        /*
        这里要返回脱敏过后的结果
        * */
        list = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);

    }
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(Integer id,HttpServletRequest request)
    {
        if(!userService.isAdmin(request))
        {
            throw new BussinessException(ErrorCode.NO_AUTH);
        }
        if(id <= 0)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"删除失败");
        }
        boolean b = userService.removeById(id);

        return ResultUtils.success(b);



    }
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize,long pageNum,HttpServletRequest request)
    {
        User loginUser = userService.getLoginUser(request);
        long userid = loginUser.getId();
        String redisKeys = String.format("yupao:user:recommned:%s",userid);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Page<User> userPage = (Page<User>) valueOperations.get(redisKeys);
        if(userPage != null)
        {
            return ResultUtils.success(userPage);
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), userQueryWrapper);


        try {
            valueOperations.set(redisKeys,userPage,100000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.info("redis key set error" + e);
        }
        return ResultUtils.success(userPage);

    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request)
    {
        Object attribute = request.getSession().getAttribute(User_LOGIN_STATE);
        User user=  (User) attribute;
        if(user == null)
        {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        Long id = user.getId();
        User currentUser = userService.getById(id);
        currentUser = userService.getSafetyUser(currentUser);
        return ResultUtils.success(currentUser);
    }
    @GetMapping("/search/tages")
    public BaseResponse<List<User>> searchUserByTags(@RequestParam(required = false) List<String> tagsNameList)
    {
        if(CollectionUtils.isEmpty(tagsNameList))
        {
            /*
            * 你在调用别人的接口的时候是不确定他是否判断了参数是否为空,所以这里要加上一个判断
            * */
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> users = userService.searchUsersByTags(tagsNameList);
        return ResultUtils.success(users);
    }


    /**
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request)
    {
        if(request==null)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        int i = userService.userLogout(request);
        return ResultUtils.success(i);

    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(User user, HttpServletRequest request)
    {
        if(request == null)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);

    }

    /**
     *
     * @param request
     * @return
     */



}
