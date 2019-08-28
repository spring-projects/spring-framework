package com.atlwj.demo.ioc.annotation.resource.service.impl;

import com.atlwj.demo.ioc.annotation.resource.dao.PersonDao;
import com.atlwj.demo.ioc.annotation.resource.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Primary
public class PersonServiceImpl02 implements PersonService {

	@Resource
	PersonDao personDao;

	@Override
	public int add() {
		System.out.println("PersonServiceImpl02...add02...." + personDao);
		return 0;
	}
}
