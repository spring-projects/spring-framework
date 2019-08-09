package com.atlwj.demo.aop.config;

import com.atlwj.demo.aop.service.Calc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan("com.atlwj.demo.aop")
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {

	public static void main(String[] args) {
		ApplicationContext ioc = new AnnotationConfigApplicationContext(AopConfig.class);
		Calc bean = ioc.getBean(Calc.class);
		bean.div(4,2);
	}

}
