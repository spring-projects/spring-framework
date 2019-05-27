package com.zxc.learn.dao;

import com.zxc.learn.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

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
@Repository
@Slf4j
public class UserDao {

	public User add(User user){
		log.info("add --"+user.toString());
		return user;
	}
}
