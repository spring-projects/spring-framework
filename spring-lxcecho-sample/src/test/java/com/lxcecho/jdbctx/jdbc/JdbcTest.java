package com.lxcecho.jdbctx.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2024/3/26
 */
public class JdbcTest {

	@Test
	public void testJdbc() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;

		try {
			// 加载驱动类
			Class.forName("com.mysql.jdbc.Driver");
			// 数据库连接
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/spring", "root", "root");
			// 关闭事务自动提交
			connection.setAutoCommit(false);
			// 定义sql
			String sql = "update goods_stock set stock = stock - ? where id = ?";
			// 获取sql执行对象
			preparedStatement = connection.prepareStatement(sql);
			// 设置参数
			preparedStatement.setInt(1, 10);
			preparedStatement.setInt(2, 1);
			// 执行sql
			preparedStatement.executeUpdate();
			// 提交事务
			connection.commit();
		} catch (Exception e) {
			// 有异常则回滚事务
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			// 释放资源
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
