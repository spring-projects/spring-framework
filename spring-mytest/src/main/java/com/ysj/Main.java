package com.ysj;

import com.ysj.bean.FirstBeanAwareTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	public static void main(String[] args) {
		ApplicationContext applicationContext1 = new ClassPathXmlApplicationContext("classpath:application.xml");
		//AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		FirstBeanAwareTest bean = applicationContext1.getBean(FirstBeanAwareTest.class);
		System.out.println("sss");
	}
}
