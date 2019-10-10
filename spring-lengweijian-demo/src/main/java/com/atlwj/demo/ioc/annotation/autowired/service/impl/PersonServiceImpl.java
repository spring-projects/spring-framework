package com.atlwj.demo.ioc.annotation.autowired.service.impl;

import com.atlwj.demo.ioc.annotation.autowired.dao.PersonDao;
import com.atlwj.demo.ioc.annotation.autowired.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class PersonServiceImpl implements PersonService {

	@Autowired
	PersonDao personDao;

	@Override
	public int add() {
		System.out.println("PersonServiceImpl...add...." + personDao);
		return 0;
	}
}
