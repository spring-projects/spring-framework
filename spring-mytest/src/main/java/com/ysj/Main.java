package com.ysj;

import com.ysj.bean.FirstBeanAwareTest;
import com.ysj.beanfactory.ABfpp;
import com.ysj.beanfactory.CBfrpp;
import com.ysj.config.AppConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	public static void main(String[] args) {
//		ApplicationContext applicationContext1 = new ClassPathXmlApplicationContext("classpath:application.xml");
//		String[] beanDefinitionNames = applicationContext1.getBeanDefinitionNames();
//		String[] myCbrfpps = applicationContext1.getAliases("myCbrfpp");
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		applicationContext.isAlias("myCfrpp");
//		applicationContext.addBeanFactoryPostProcessor(new CBfrpp());
//		applicationContext.register(AppConfig.class);
//		applicationContext.refresh();
	}

}
