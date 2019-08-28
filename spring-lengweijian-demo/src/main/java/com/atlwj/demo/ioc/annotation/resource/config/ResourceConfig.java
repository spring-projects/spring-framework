package com.atlwj.demo.ioc.annotation.resource.config;

import com.atlwj.demo.ioc.annotation.resource.controller.PersonController;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @Resource:
 * 			  特性：默认是按照类型自动装配bean。当有多个类型的bean的实现类存在的时候，如果@Resource所在的属性的名称不是现存的bean的id的话，
 * 			  	   跟@Autowired注解一样，会报【NoUniqueBeanDefinitionException: No qualifying bean】，异常。
 * 			  	   解决办法有两个：
 * 			  				1.将@Resource所在属性的名称值修改为现存的多个实现类其中之一的id值。
 *							2.支持@Primary注解。也就是可以在你想要注入的bean的类上边加上@Primary注解。
 *							3.@Resource(name="xxxxx")可以使用name属性指定注入你想要注入的bean的id值。
 *				   不支持JSR-305规范@Nullable注解，而且@Resource里面也没有required属性，所以容器启动如果没有装配成功，容器会报错，启动失败。
 *
 *
 *
 */
@Configuration
@ComponentScan(value = "com.atlwj.demo.ioc.annotation.resource")
public class ResourceConfig {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ResourceConfig.class);
		for (String bdNamne : context.getBeanDefinitionNames()) {
			System.out.println(bdNamne);
		}
		context.getBean(PersonController.class).add();
	}
}
