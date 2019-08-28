package com.atlwj.demo.ioc.annotation.autowired.config;

import com.atlwj.demo.ioc.annotation.autowired.controller.PersonController;
import org.springframework.context.annotation.*;

/**
 *
 * 使用@Autowired注解可以完成对象的依赖注入。接下来是重点：
 * 		特性：   @Autowired默认是按照bean的配型装配，什么意思？就是注入的属性名称可以和你要注入的bean的id不同。
 * 				但是当同一类型的bean有两个以上的时候，如果随意起属性名称会报【NoUniqueBeanDefinitionException】异常。
 * 			  	但是如果你将你声明的属性的名称修改为同一类型的bean的多个实现的其中之一的bean的id名称时，spring可以为你注入这个bean的实例。
 * 			    个人认为这是spring自动装配最牛X的地方！！！）。
 * 			    当然你也可以不这么做，你可以结合@Qualifier（value="beanid"）注解，在属性里面指定你要注入的bean的id。
 * 			    当然你还可以不这么做，你可以在你想要注入的bean上边标上@Primary注解。
 * 			    包括@Autowired提供了required（true/false）属性，就是容器在启动的时候，如果当前bean为null就不会报错。否则会报【No qualifying bean】异常。
 * 			    spring 5还支持使用@Nullable注解实现。
 * 				@Autowired 可以用在CONSTRUCTOR, METHOD,PARAMETER, FIELD。用法一样。
 *
 *
 */
@Configuration
@ComponentScan(value = "com.atlwj.demo.ioc.annotation")
public class Config {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		for (String bdNamne : context.getBeanDefinitionNames()) {
			System.out.println(bdNamne);
		}
		context.getBean(PersonController.class).add();
	}
}
