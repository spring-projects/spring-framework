package org.springframework.jdbc.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.mapper.UserRowMapper;
import org.springframework.jdbc.model.User;
import org.springframework.jdbc.service.UserService;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;

/**
 * @author sushuaiqiang
 * @date 2024/12/25 - 15:36
 */
public class UserServiceImpl implements UserService {

	private JdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void save(User user) {
		jdbcTemplate.update("insert into t_user(name,age,sex) values(?,?,?)"
				, new Object[]{user.getName(), user.getAge(), user.getSex()}
				, new int[]{Types.VARCHAR, Types.INTEGER, Types.VARCHAR});
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<User> getUsers() {
		List<User> list = jdbcTemplate.query("select * from t_user", new UserRowMapper());
		return list;
	}

	@Override
	public List<User> getUserByAge() {
		List list = jdbcTemplate.query(
				"select * from t_user where age = ?"
				, new Object[]{18}
				, new int[]{Types.INTEGER}
				, new UserRowMapper()
		);
		return list;
	}
	/*
		基于以上两个查询方法, 一个参 一个无参:
		与之前的query方法最大的不同是少了参数及参数类型的传递，自然也少了PreparedStatementSetter类型的封装。
	既然少了PreparedStatementSetter类型的传入，调用的execute方法自然也会有所改变了。
		这个exexute与之前的execute并无太大差别，都是做一些常规的处理，诸如获取连接、释连接等，但是，
	有一个地方是不一样的，就是statement的创建。这里直接使用connection创建，
	而带有参数的SQL使用的是PreparedStatementCreator类来创建的。
	一个是普通的Statement，另一个是PreparedStatement，两者究竞是何区别呢？
		PreparedStatement接口继承Statement，并与之在两方面有所不同。
		PreparedStatement实例包含已编译的SQL语句。这就是使语句“准备好”。包含于
		PreparedStatement对象中的SQL语句可其有一个或多个IN参数。IN参数的值在SQL
		语句创建时未被指定。相反的，该语句为每个IN参数保留一个问号（"？"）作为占位
		符。每个问号的值必须在旅路句执行之前，通过适当的setXXX方法来提供。
		由于PreparedStatement对象已预编译过，所以其执行速度要快于Statement对象。因
		此，多次执行的SQL语句经常创建为PreparedStatement对象，以提高效率。
		作为Statement的子类，PreparedStatement继承了Statement的所有功能。另外，它还添加
	了一整套方法，用于设置发送给数据库以取代IN参数占位符的值。同时，三种方法execute、
	executeQuery和executeUpdate已被更改以使之不再需要参数。这些方法的Statement形式（接
	受SQL语句参数的形式）不应该用于PreparedStatement对象。
	 */
}
