package com.lxcecho.resources.prefix;

import com.lxcecho.resources.di.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class TestDemo {

	public static void main(String[] args) {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("classpath:bean*.xml");
//        Resource resource = context.getResource("lxcecho.txt");
//        System.out.println(resource.getDescription());

		User user = context.getBean(User.class);
		System.out.println(user);
	}
}
