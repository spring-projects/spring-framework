package com.lxcecho.ioc.iocxml.controller;

import com.lxcecho.ioc.iocxml.service.UserService;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class UserController {

    private UserService userService;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void addUser() {
        System.out.println("controller 方法执行了...");
        // 调用 service 的方法
        userService.addUserService();
//        UserService userService = new UserServiceImpl();
//        userService.addUserService();
    }
}
