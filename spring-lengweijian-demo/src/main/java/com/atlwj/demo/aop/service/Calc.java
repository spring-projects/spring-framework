package com.atlwj.demo.aop.service;

import org.springframework.stereotype.Component;

@Component
public class Calc {

	public Calc(){

	}

	public int div(int i,int j){
		System.out.println("div.....");
		return i/j;
	}
}
