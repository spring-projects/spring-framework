package com.lxcecho;

import com.lxcecho.bean.AnnotationApplicationContext;
import com.lxcecho.bean.ApplicationContext;
import com.lxcecho.service.UserService;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class Main {
	public static void main(String[] args) {
		ApplicationContext context = new AnnotationApplicationContext("com.lxcecho");
		UserService userService = (UserService) context.getBean(UserService.class);
		System.out.println(userService);
		userService.add();
	}
}