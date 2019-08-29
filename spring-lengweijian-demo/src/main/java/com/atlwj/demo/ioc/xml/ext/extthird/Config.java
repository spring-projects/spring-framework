package com.atlwj.demo.ioc.xml.ext.extthird;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = "com.atlwj.demo.ioc.xml.ext.extthird")
public class Config {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(Config.class);
		System.out.println(ioc.getBean("dogFactoryBean").toString());
	}
}
