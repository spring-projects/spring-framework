package com.atlwj.demo.ioc.cyclereference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype")
public class A {

//	@Autowired
	private B b;

//	public A(B b){
//		this.b = b;
//	}

	public void showA(){
		System.out.println("aaaaaa");
	}

	public void setB(B b){
		this.b = b;
	}
}
