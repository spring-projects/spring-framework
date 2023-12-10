package com.lxcecho.service.impl;

import com.lxcecho.anno.Bean;
import com.lxcecho.anno.Di;
import com.lxcecho.dao.UserDao;
import com.lxcecho.service.UserService;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Bean
public class UserServiceImpl  implements UserService {

    @Di
    private UserDao userDao;

    public void add() {
        System.out.println("service.......");
        //调用dao的方法
        userDao.add();
    }
}
