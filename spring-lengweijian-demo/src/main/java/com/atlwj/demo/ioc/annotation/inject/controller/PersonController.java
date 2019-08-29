package com.atlwj.demo.ioc.annotation.inject.controller;

import com.atlwj.demo.ioc.annotation.inject.entity.Person;
import com.atlwj.demo.ioc.annotation.inject.service.PersonService;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PersonController {

	@Inject
	private ApplicationContext applicationContext;

	@Inject
	@Nullable
	PersonService personService;

	public int add(){
		// 获取person bean
		String personInfo = applicationContext.getBean(Person.class).toString();
		System.out.println("PersonController...add..." + personService.add() + personInfo);
		return 0;
	}

}
