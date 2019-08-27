package com.atlwj.demo.ioc.ext.extsecend;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = "com.atlwj.demo.ioc.ext.extsecend")
public class ExtSecConfig {

	@Bean
	public Cat cat () {
		return new Cat("tomcat","Yellow");
	}

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(ExtSecConfig.class);
		System.out.println(ioc.getBean("cat"));
	}
}
