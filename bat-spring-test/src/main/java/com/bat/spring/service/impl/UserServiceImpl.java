package com.bat.spring.service.impl;

import com.bat.spring.entity.User;
import com.bat.spring.service.UserService;

/**
 * @program: ESAT
 * @author: zhq
 * @description:
 * @create: 2021/9/2 16:15
 **/
public class UserServiceImpl implements UserService {
	@Override
	public User getUserById(Integer id) {
		User user = new User();
		user.setAge(24);
		user.setName("attack bat!");
		return user;
	}
}
