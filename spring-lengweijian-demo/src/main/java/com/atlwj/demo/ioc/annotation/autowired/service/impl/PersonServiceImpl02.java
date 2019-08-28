package com.atlwj.demo.ioc.annotation.autowired.service.impl;

import com.atlwj.demo.ioc.annotation.autowired.dao.PersonDao;
import com.atlwj.demo.ioc.annotation.autowired.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
@Primary
public class PersonServiceImpl02 implements PersonService {

	@Autowired
	@Nullable
	PersonDao personDao;

	@Override
	public int add() {
		System.out.println("PersonServiceImpl02...add02...." + personDao);
		return 0;
	}
}
