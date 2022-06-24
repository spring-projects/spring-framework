package com.study.dao;

import com.study.entity.User;
import com.study.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * @author zhutongtong
 * @date 2022/6/23 19:24
 */
@Component
public class UserDaoImpl implements UserDao{

	JdbcTemplate jdbcTemplate;

	@Autowired
	public UserDaoImpl(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public User getById(Integer id) {
		return jdbcTemplate.queryForObject("select * from user where id = ?",new Object[] { id },new UserMapper());
	}

}
