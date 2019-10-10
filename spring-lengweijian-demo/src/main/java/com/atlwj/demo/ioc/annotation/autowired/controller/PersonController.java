package com.atlwj.demo.ioc.annotation.autowired.controller;

import com.atlwj.demo.ioc.annotation.autowired.entity.Person;
import com.atlwj.demo.ioc.annotation.autowired.service.PersonService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;

@Controller
public class PersonController{


	@Autowired
	PersonService personServiceImpl02;

	public int add(){
		// 获取person bean
		System.out.println("PersonController...add..." + personServiceImpl02.add());
		return 0;
	}
}
