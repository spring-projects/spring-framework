package com.lxcecho.resources.prefix;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class TestDemo {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:bean-resources.xml");
//        Resource resource = context.getResource("lxcecho.txt");
//        System.out.println(resource.getDescription());

		User user = context.getBean(User.class);
		System.out.println(user);
	}
}
