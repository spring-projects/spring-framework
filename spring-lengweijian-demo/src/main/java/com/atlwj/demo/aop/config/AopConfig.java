package com.atlwj.demo.aop.config;

import com.atlwj.demo.aop.service.Calc;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@ComponentScan("com.atlwj.demo.aop")
@Configuration
@EnableAspectJAutoProxy
public class AopConfig{

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(AopConfig.class);
//		for (String beanDefinitionName : ioc.getBeanDefinitionNames()) {
//			System.out.println(beanDefinitionName);
//		}
		Calc bean = ioc.getBean(Calc.class);
		bean.div(8,2);

	}

}
