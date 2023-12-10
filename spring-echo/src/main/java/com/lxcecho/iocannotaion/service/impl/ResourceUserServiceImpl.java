package com.lxcecho.iocannotaion.service.impl;

import com.lxcecho.iocannotaion.dao.UserDao;
import com.lxcecho.iocannotaion.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service("myUserService")
public class ResourceUserServiceImpl implements UserService {

	/**
	 * 不 指定名称，根据属性名称进行注入
	 */
	@Resource
	private UserDao myUserDao;

	@Override
	public void add() {
		System.out.println("service.....");
		myUserDao.add();
	}
}
