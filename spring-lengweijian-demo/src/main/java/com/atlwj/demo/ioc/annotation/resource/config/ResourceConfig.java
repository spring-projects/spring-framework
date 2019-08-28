package com.atlwj.demo.ioc.annotation.resource.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 *
 */
@Configuration
@ComponentScan(value = "com.atlwj.demo.ioc.annotation.resource")
public class ResourceConfig {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ResourceConfig.class);
		for (String bdNamne : context.getBeanDefinitionNames()) {
			System.out.println(bdNamne);
		}
		//context.getBean(PersonController.class).add();
	}
}
