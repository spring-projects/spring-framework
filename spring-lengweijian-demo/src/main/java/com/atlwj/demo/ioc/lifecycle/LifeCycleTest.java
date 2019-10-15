package com.atlwj.demo.ioc.lifecycle;

import org.springframework.context.annotation.*;

//@EnableAspectJAutoProxy
//@ComponentScan("com.atlwj.demo.ioc.lifecycle")
@Configuration
public class LifeCycleTest {

	@Bean(value = "lifeCycleBean01",initMethod = "initMethod",destroyMethod = "destroyMethod")
	public LifeCycleBean lifeCycleBean01(){
		return new LifeCycleBean();
	}
	@Bean(value = "lifeCycleBean02")
	public LifeCycleBean02 lifeCycleBean02(){
		return new LifeCycleBean02();
	}

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(LifeCycleTest.class);
		LifeCycleBean lifeCycleBean = (LifeCycleBean) ioc.getBean("lifeCycleBean01");
		LifeCycleBean02 lifeCycleBean02 = (LifeCycleBean02) ioc.getBean("lifeCycleBean02");
		lifeCycleBean.display();
		lifeCycleBean02.say02();
	}
}
