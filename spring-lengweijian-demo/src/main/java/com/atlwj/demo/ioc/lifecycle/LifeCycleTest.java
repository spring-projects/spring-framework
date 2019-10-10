package com.atlwj.demo.ioc.lifecycle;

import org.springframework.context.annotation.*;

//@EnableAspectJAutoProxy
//@ComponentScan("com.atlwj.demo.ioc.lifecycle")
@Configuration
public class LifeCycleTest {

	@Bean(name = "lifeCycle",initMethod = "initMethod",destroyMethod = "destroyMethod")
	public LifeCycleBean lifeCycleBean(){
		return new LifeCycleBean();
	}

	@Bean(name = "xxxxxx")
	public LifeCycleBean02 lifeCycleBean02(){
		return new LifeCycleBean02();
	}

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(LifeCycleTest.class);
		LifeCycleBean lifeCycleBean = (LifeCycleBean) ioc.getBean("lifeCycle");
		LifeCycleBean02 lifeCycleBean02 = (LifeCycleBean02) ioc.getBean("xxxxxx");
		lifeCycleBean.display();
		lifeCycleBean02.say02();
		//ioc.close();
	}
}
