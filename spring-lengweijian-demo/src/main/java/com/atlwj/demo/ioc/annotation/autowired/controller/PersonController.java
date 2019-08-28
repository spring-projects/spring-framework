package com.atlwj.demo.ioc.annotation.autowired.controller;

import com.atlwj.demo.ioc.annotation.autowired.entity.Person;
import com.atlwj.demo.ioc.annotation.autowired.service.PersonService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;

@Controller
public class PersonController implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Autowired
	//@Qualifier(value = "personServiceImpl02")
	PersonService personService;

	public int add(){
		// 获取person bean
		String personInfo = applicationContext.getBean(Person.class).toString();
		System.out.println("PersonController...add..." + personService.add() + personInfo);
		return 0;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
