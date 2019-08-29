package com.atlwj.demo.ioc.annotation.inject.service.impl;

import com.atlwj.demo.ioc.annotation.inject.dao.PersonDao;
import com.atlwj.demo.ioc.annotation.inject.service.PersonService;

import javax.inject.Inject;
import javax.inject.Named;

//@Named
public class PersonServiceImpl implements PersonService {

	@Inject
	PersonDao personDao;

	@Override
	public int add() {
		System.out.println("PersonServiceImpl...add...." + personDao);
		return 0;
	}
}
