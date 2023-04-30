package org.springframework.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.example.config.AppConfig;
import org.springframework.example.service.UserService;

public class Test {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(AppConfig.class);
		context.refresh();

		UserService userService = (UserService) context.getBean("userService");
		userService.sayHello();
	}
}
