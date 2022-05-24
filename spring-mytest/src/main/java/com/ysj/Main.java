package com.ysj;

import com.ysj.autowireTest.A;
import com.ysj.bean.FirstBean;
import com.ysj.config.AppConfig;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	public static void main(String[] args) {
//		ApplicationContext applicationContext1 = new ClassPathXmlApplicationContext("classpath:application.xml");
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
	}
}
