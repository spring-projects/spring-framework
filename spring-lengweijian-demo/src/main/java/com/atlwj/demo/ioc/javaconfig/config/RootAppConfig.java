package com.atlwj.demo.ioc.javaconfig.config;

import com.atlwj.demo.ioc.javaconfig.domain.Son;
import com.atlwj.demo.ioc.javaconfig.domain.User;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 *
 */
@Configuration
@ComponentScans(
		@ComponentScan(value = "com.atlwj.demo.ioc.javaconfig",includeFilters={
				@ComponentScan.Filter(value = {Repository.class, Service.class})
		},useDefaultFilters = false)
)
public class RootAppConfig {

	@Bean
	@Description("这是一个User实体类")
	public User user(){
		return new User("lengwj","温和",son());
	}

	@Bean
	@Lookup
	@Scope("prototype")
	public Son son(){
		return new Son("tom","爱学习");
	}



	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RootAppConfig.class);
		for (String bdName : context.getBeanDefinitionNames()) {
			System.out.println(bdName);
		}

		User user = (User) context.getBean("user");
		User user2 = (User) context.getBean("user");

		System.out.println(user.hashCode());
		System.out.println(user2.hashCode());

		System.out.println(user.getSon().hashCode());
		System.out.println(user2.getSon().hashCode());
	}
}
