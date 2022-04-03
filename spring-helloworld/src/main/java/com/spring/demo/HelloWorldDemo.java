package com.spring.demo;

import com.spring.demo.config.HelloWorldConfig;
import com.spring.demo.service.HelloWorldService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class HelloWorldDemo {
	public static void main(String[] args) {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(HelloWorldConfig.class);

		HelloWorldService helloWorldService = (HelloWorldService) applicationContext.getBean("helloWorldService");

		helloWorldService.printHelloWorld();
	}
}
