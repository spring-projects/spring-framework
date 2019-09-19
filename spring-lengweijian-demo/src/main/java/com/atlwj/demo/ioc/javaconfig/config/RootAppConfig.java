package com.atlwj.demo.ioc.javaconfig.config;

import com.atlwj.demo.ioc.javaconfig.domain.Son;
import com.atlwj.demo.ioc.javaconfig.domain.User;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;


@Configuration
//@ComponentScans(
//		@ComponentScan(value = "com.atlwj.demo.ioc.javaconfig",includeFilters={
//				@ComponentScan.Filter(value = {Repository.class, Service.class})
//		},useDefaultFilters = false)
//)
public class RootAppConfig {

	@Bean
	@Description("这是一个User实体类")
	public User user(){
		return new User("lengwj","温和");
	}

//	@Bean
//	@Lookup
//	@Scope("prototype")
//	public Son son(){
//		return new Son("tom","爱学习");
//	}



	public static void main(String[] args) {
		GenericApplicationContext context = new AnnotationConfigApplicationContext(RootAppConfig.class);
//		BeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(Son.class);
//		beanDefinition.setBeanClassName("myson");
//		beanDefinition.setDependsOn("爱学习","内向");
//		context.registerBeanDefinition("son",beanDefinition);
		//User user = (User) context.getBean("user");
		for (String bdName : context.getBeanDefinitionNames()) {
			System.out.println(bdName);
		}

	}
}
