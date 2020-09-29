package io.codegitz.controller;

import io.codegitz.service.HelloService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;

import java.util.Arrays;

/**
 * @author 张观权
 * @date 2020/8/10 13:28
 **/
@Controller
public class WelcomeController implements ApplicationContextAware, BeanNameAware {

	private String myName;

	private ApplicationContext myContainer;

	@Autowired
	private HelloService helloService;

	public void handleRequest(){
		helloService.sayHello("Codegitz");
		System.out.println("who am I ? "+ myName);
		String[] beanDefinitionNames = myContainer.getBeanDefinitionNames();
		Arrays.stream(beanDefinitionNames).forEach((beanName)-> System.out.println("Get container bean: "+beanName));
	}

	@Override
	public void setBeanName(String name) {
		this.myName = name;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.myContainer = applicationContext;
	}
}
