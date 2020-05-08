package com.gaok;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author : gaokang
 * @date : 2020/5/2
 */
public class SpringBeanTest {

	public static void main(String[] args) {
		//常规的xml读取
		/*ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:test.xml");
		User user = (User)context.getBean("user1");
		System.out.println(user.getName());*/

		//注解读取
		AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		System.out.println(configApplicationContext.getBean(CityService.class));


	}
}
