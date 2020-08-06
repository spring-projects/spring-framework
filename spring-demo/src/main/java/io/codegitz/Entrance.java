package io.codegitz;

import io.codegitz.service.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * @author 张观权
 * @date 2020/8/6 19:43
 **/
public class Entrance {
	public static void main(String[] args) {
		String path = "classpath:spring/spring-config.xml";
		ApplicationContext applicationContext = new FileSystemXmlApplicationContext(path);
		HelloService helloService = (HelloService) applicationContext.getBean("helloService");
		helloService.sayHello("Codegitz");
	}
}
