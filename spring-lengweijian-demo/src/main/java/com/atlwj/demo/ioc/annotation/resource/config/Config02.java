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
public class Config02 {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config02.class);
		for (String bdNamne : context.getBeanDefinitionNames()) {
			System.out.println(bdNamne);
		}
	}
}
