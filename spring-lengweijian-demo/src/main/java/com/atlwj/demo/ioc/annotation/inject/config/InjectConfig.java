package com.atlwj.demo.ioc.annotation.inject.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @Inject:  JSR-330规范提供用来完成依赖注入的功能。可以放在METHOD, CONSTRUCTOR, FIELD上。
 * 			 @Inject注解默认按照bean的类型进行装配，也就是说@Inject标注的属性值可以和你想要注入的bean的id不同，
 * 			 但是注意前提是该类型的bean的实现类在容器中只能存在一个。否则会抛【NoUniqueBeanDefinitionException】
 * 			 解决的办法：
 * 			 		1：将@Inject注解所标注的属性的值修改成你想要注入的bean的id值。
 * 			 		2：@Inject注解支持spring容器的@Primary注解。
 *			 此外：JSR-330规范提供了@Name注解，可以完成将组件注册进IOC容器。
 *
 */
@Configuration
@ComponentScan(value = "com.atlwj.demo.ioc.annotation.inject")
public class InjectConfig {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InjectConfig.class);
		for (String bdNamne : context.getBeanDefinitionNames()) {
			System.out.println(bdNamne);
		}
		//context.getBean(PersonController.class).add();
	}
}
