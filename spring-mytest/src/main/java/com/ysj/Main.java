package com.ysj;

import com.ysj.bean.FirstBean;
import com.ysj.config.AppConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	public static void main(String[] args) {
		//ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:application.xml");
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		FirstBean bean = applicationContext.getBean(FirstBean.class);
		bean.test();
	}
}
