package com.atlwj.demo.ioc.ext;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 扩展原理：
 * BeanPostProcessor:
 * BeanFactoryPostProcessor:所有的bean定义已经保存加载，但是还没有创建bean实例。
 * ServletContainerInitializer
 */
@Configuration
@ComponentScan(value = "com.atlwj.demo.ioc.ext")
public class ExtConfig {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(ExtConfig.class);
	}
}
