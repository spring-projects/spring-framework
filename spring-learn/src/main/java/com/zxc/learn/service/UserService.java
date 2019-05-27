package com.zxc.learn.service;

import com.zxc.learn.bean.User;
import com.zxc.learn.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 上士闻道，勤而行之；中士闻道，若存若亡；下士闻道，大笑之。
 *
 * @Description:
 * @Author: simon
 * @Create: 2019-05-26 22:52
 * @Version: 1.0.0
 *
 * 上士闻道，勤而行之；中士闻道，若存若亡；下士闻道，大笑之。
 **/
@Service
public class UserService {

	@Autowired
	private UserDao userDao;

	public void add(String name){
		User user = new User();
		user.setId(1);
		userDao.add(user);
	}
}
