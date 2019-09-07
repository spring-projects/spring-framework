package com.atlwj.demo.aop.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan("com.atlwj.demo.aop")
@Configuration
@EnableAspectJAutoProxy
public class AopConfig{

	public static void main(String[] args) {
		ApplicationContext ioc = new AnnotationConfigApplicationContext(AopConfig.class);
		for (String beanDefinitionName : ioc.getBeanDefinitionNames()) {
			System.out.println(beanDefinitionName);
		}
	}

}
