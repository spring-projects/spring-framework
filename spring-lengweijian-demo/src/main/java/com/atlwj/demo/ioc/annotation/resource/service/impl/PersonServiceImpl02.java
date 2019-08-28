package com.atlwj.demo.ioc.annotation.resource.service.impl;

import com.atlwj.demo.ioc.annotation.resource.dao.PersonDao;
import com.atlwj.demo.ioc.annotation.resource.service.PersonService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PersonServiceImpl02 implements PersonService {

	@Resource
	PersonDao personDao;

	@Override
	public int add() {
		System.out.println("PersonServiceImpl02...add02...." + personDao);
		return 0;
	}
}
