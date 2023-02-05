package org.springframework.debug.spring.cache;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Author: zhoudong
 * @Description:
 * @Date: 2022/7/20 19:11
 * @Version:
 **/
public class Test {
	public static void main(String[] args) {

		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring.xml");
		AService aService = applicationContext.getBean(AService.class);
		System.out.println(aService.getbService());
		BService bService = applicationContext.getBean(BService.class);
		System.out.println(bService.getaService());

		Class<?>[] classes = AService.class.getClasses();
		Class<AService> aServiceClass = AService.class;
//		Class<?> aClass = Class.forName("");
		new Thread(()-> System.out.println("abc")).start();
		ThreadLocal threadLocal = new ThreadLocal<>();
		new ThreadLocal<>();
	}
}
