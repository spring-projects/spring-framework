package com.ysj.autowireTest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author ys7
 * 自动装配与ignoreDependencyInterface 和 ignoreDependencyType
 */
public class MainAutowired {
	public static void main(String[] args) {
		ApplicationContext applicationContext1 = new ClassPathXmlApplicationContext("classpath:application.xml");
		All bean = applicationContext1.getBean(All.class);
		System.out.println(bean.getA());
		//AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);FirstBean bean = applicationContext.getBean(FirstBean.class);
	}
}
