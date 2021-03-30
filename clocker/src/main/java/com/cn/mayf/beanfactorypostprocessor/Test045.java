package com.cn.mayf.beanfactorypostprocessor;

import com.cn.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author mayf
 * @Date 2021/3/16 20:36
 * 这一期的关键点--->策略模式
 */
public class Test045 {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(AppConfig.class);
		context.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
		context.refresh();
		context.close();
	}
}
