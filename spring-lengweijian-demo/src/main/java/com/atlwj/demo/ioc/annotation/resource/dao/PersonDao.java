package com.atlwj.demo.ioc.annotation.resource.dao;

import org.springframework.stereotype.Repository;

@Repository
public class PersonDao {

	public void add(){
		System.out.println("PersonDao....add....");
	}
}
