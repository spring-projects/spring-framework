package com.lxcecho.ioc.iocanno.service.impl;

import com.lxcecho.ioc.iocanno.dao.UserDao;
import com.lxcecho.ioc.iocanno.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Service("myUserService")
public class ResourceUserServiceImpl implements UserService {

	/**
	 * 不 指定名称，根据属性名称进行注入
	 */
	@Resource
	private UserDao myUserDao;

	@Override
	public void add() {
		System.out.println("ResourceUserServiceImpl.....");
		myUserDao.add();
	}
}
