package com.study.dao;

import com.study.entity.User;

/**
 * @author zhutongtong
 * @date 2022/6/23 19:23
 */
public interface UserDao {

	User getById(Integer id);
}
