package com.ysj.autowireTest;


import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class A {
	private List<C> myList;

	public A() {
		System.out.println("A初始化了");
	}

	public List<C> getMyList() {
		return myList;
	}

	public void setMyList(List<C> myList) {
		this.myList = myList;
	}
}
