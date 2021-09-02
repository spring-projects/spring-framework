package com.bat.spring.test;

import com.bat.spring.entity.User;
import com.bat.spring.service.UserService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @program: spring source code
 * @author: zhq
 * @description: attack bat
 * @create: 2021/9/2 16:08
 **/
public class BatTest {
	public static void main(String[] args) {
		//读取spring配置文件
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-test.xml");

		UserService userService = (UserService) context.getBean("userService");

		User user = userService.getUserById(1);

		System.out.println("User = " + user);

	}
}
