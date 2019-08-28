package com.atlwj.demo.ioc.annotation.resource.controller;

import com.atlwj.demo.ioc.annotation.resource.entity.Person;
import com.atlwj.demo.ioc.annotation.resource.service.PersonService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;

@Controller
public class PersonController implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Resource
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
