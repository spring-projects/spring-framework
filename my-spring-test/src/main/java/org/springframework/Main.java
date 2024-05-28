package org.springframework;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.model.User;

public class Main {

	public static void main(String[] args) {
		// 1 启动 SpringIOC 容器
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("bean.xml");
		//2 从 IOC 容器中获取 bean
		User person = (User) applicationContext.getBean("user");
		//2 使用 bean
		System.out.println(person);
		System.out.println("Hello world!");
	}

}