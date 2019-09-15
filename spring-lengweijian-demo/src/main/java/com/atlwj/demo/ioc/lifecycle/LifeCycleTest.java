package com.atlwj.demo.ioc.lifecycle;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//@EnableAspectJAutoProxy
@ComponentScan("com.atlwj.demo.ioc.lifecycle")
public class LifeCycleTest {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(LifeCycleTest.class);
		LifeCycleBean lifeCycleBean = (LifeCycleBean) ioc.getBean("lifeCycle");
		lifeCycleBean.display();
		ioc.close();
	}
}
