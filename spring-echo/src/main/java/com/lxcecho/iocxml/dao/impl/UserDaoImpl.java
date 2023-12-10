package com.lxcecho.iocxml.dao.impl;

import com.lxcecho.iocxml.dao.UserDao;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class UserDaoImpl implements UserDao {
	@Override
	public void run() {
		System.out.println("run.....");
	}

	@Override
	public void addUserDao() {
		System.out.println("userDao方法执行了...");
	}
}
