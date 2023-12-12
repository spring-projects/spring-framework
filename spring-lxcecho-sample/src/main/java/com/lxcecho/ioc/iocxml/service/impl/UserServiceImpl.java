package com.lxcecho.ioc.iocxml.service.impl;

import com.lxcecho.ioc.iocxml.dao.UserDao;
import com.lxcecho.ioc.iocxml.service.UserService;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class UserServiceImpl implements UserService {

	private UserDao userDao;

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	@Override
	public void addUserService() {
		System.out.println("userService方法执行了...");
		userDao.addUserDao();
//        UserDao userDao = new UserDaoImpl();
//        userDao.addUserDao();
	}
}
