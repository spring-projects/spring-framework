package com.atlwj.demo.ioc.annotation.resource.controller;

import com.atlwj.demo.ioc.annotation.resource.entity.Person;
import com.atlwj.demo.ioc.annotation.resource.service.PersonService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;

@Controller
public class PersonController {

	@Resource
	private ApplicationContext applicationContext;

	@Resource(name = "personServiceImpl")
	PersonService personService;

	public int add(){
		// 获取person bean
		String personInfo = applicationContext.getBean(Person.class).toString();
		System.out.println("PersonController...add..." + personService.add() + personInfo);
		return 0;
	}

}
