package io.codegitz;

import io.codegitz.controller.WelcomeController;
import io.codegitz.entity.Example;
import io.codegitz.entity.User;
import io.codegitz.entity.bd;
import io.codegitz.entity.factory.UserFactoryBean;
import io.codegitz.service.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.Locale;

/**
 * @author 张观权
 * @date 2020/8/6 19:43
 **/

@Configuration
@ComponentScan("io.codegitz")
public class Entrance {
	public static void main1(String[] args) {
		String path = "classpath:spring/spring-config.xml";
		ApplicationContext applicationContext = new FileSystemXmlApplicationContext(path);
		HelloService helloService = (HelloService) applicationContext.getBean("helloService");
		helloService.sayHello("Codegitz");
	}

	/**
	 * 注解配置
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void main5(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(Entrance.class);
		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		for (String beanName:beanDefinitionNames
			 ) {
			System.out.println(beanName);
		}
		WelcomeController welcomeController = (WelcomeController) applicationContext.getBean("welcomeController");
		welcomeController.handleRequest();
		User user = (User) applicationContext.getBean("user5");
		System.out.println("postprocessor create bean: " + user);
	}

	public static void main6(String[] args) {
		MessageSource resources = new ClassPathXmlApplicationContext("beans.xml");
		String message = resources.getMessage("message", null, "Default", Locale.ENGLISH);
		ApplicationContext applicationContext = (ApplicationContext) resources;
		Example example = (Example) applicationContext.getBean("example");
		example.execute();
		System.out.println(message);
	}

	public static void main7(String[] args) {
		bd bd = new bd();
		add(bd);
		System.out.println(bd.getList().toString());
	}

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("tags.xml");
		io.codegitz.customtag.User user = (io.codegitz.customtag.User) applicationContext.getBean("testBean");
		System.out.println(user.getUserName()+", "+user.getEmail());
	}

	private static void add(bd bd) {
		bd.getList().add(1);
	}

	/**
	 * xml配置多个bean
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void main2(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		String path = "classpath:spring/spring-config.xml";
		ApplicationContext applicationContext = new FileSystemXmlApplicationContext(path);
		User user1a = (User) applicationContext.getBean("user1");
		User user1b = (User) applicationContext.getBean("user1");
		User user2a = (User) applicationContext.getBean("user2");
		User user2b = (User) applicationContext.getBean("user2");
		User user3a = (User) applicationContext.getBean("user3");
		User user3b = (User) applicationContext.getBean("user3");
		User user4a = (User) applicationContext.getBean("userFactoryBean");
		User user4b = (User) applicationContext.getBean("userFactoryBean");
		UserFactoryBean userFactoryBean = (UserFactoryBean) applicationContext.getBean("&userFactoryBean");
		System.out.println(user1a+":"+user1b);
		System.out.println(user2a+":"+user2b);
		System.out.println(user3a+":"+user3b);
		System.out.println(user4a+":"+user4b);
		System.out.println("userFactoryBean: "+userFactoryBean);


	}
}
