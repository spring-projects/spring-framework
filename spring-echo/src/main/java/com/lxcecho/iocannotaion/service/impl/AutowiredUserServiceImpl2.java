package com.lxcecho.iocannotaion.service.impl;

import com.lxcecho.iocannotaion.dao.UserDao;
import com.lxcecho.iocannotaion.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AutowiredUserServiceImpl2 implements UserService {

	// 注 入dao

	/**
	 * 第一种方式  属性注入
	 */
//    @Autowired  //根据类型找到对应对象，完成注入
//    private UserDao userDao;

	/**
	 * 第二种方式 set方法注入
	 */
//    private UserDao userDao;
//
//    @Autowired
//    public void setUserDao(UserDao userDao) {
//        this.userDao = userDao;
//    }

	/**
	 * 第三种方式  构造方法注入
	 */
//    private UserDao userDao;
//
//    @Autowired
//    public UserServiceImpl(UserDao userDao) {
//        this.userDao = userDao;
//    }

	/**
	 * 第四种方式 形参上注入
	 */
//    private UserDao userDao;
//
//    public UserServiceImpl(@Autowired UserDao userDao) {
//        this.userDao = userDao;
//    }

	/**
	 * 最后方式： 两个注解，根据名称注入
	 */
	@Autowired
	@Qualifier(value = "userRedisDaoImpl")
	private UserDao userDao;

	@Override
	public void add() {
		System.out.println("service.....");
		userDao.add();
	}
}
