package com.atlwj.demo.ioc.annotation.inject.dao;

import javax.inject.Named;

@Named
public class PersonDao {

	public void add(){
		System.out.println("PersonDao....add....");
	}
}
