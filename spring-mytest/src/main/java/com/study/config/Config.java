package com.study.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * @author zhutongtong
 * @date 2022/6/23 18:47
 */
@Configuration
@ComponentScan("com.study")
public class Config {

	private static String url = "jdbc:mysql://localhost:3306/king?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=utf8&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai";
	private static String username = "root";
	private static String password = "root";
	private static String driver = "com.mysql.cj.jdbc.Driver";


	@Bean
	DataSource dataSource() {
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setUrl(url);
		driverManagerDataSource.setUsername(username);
		driverManagerDataSource.setPassword(password);
		driverManagerDataSource.setDriverClassName(driver);
		return driverManagerDataSource;
	}
}
