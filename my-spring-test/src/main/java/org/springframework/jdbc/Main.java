package org.springframework.jdbc;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.model.User;
import org.springframework.jdbc.service.UserService;

import java.util.List;

/**
 * @author sushuaiqiang
 * @date 2024/12/25 - 16:14
 */
public class Main {

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("jdbc/jdbc.xml");
		UserService userService = applicationContext.getBean(UserService.class);
		userService.save(new User().setName("sushuaiqiang").setAge(18).setSex("男"));
		List<User> users = userService.getUsers();
		for (User user : users) {
			System.out.println(user);
		}
	}
}
