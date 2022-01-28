package com.ysj.bean;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.stereotype.Component;

@Component("firstBean")
public class FirstBean {

	public void test(){
		System.out.println("xxxxxx");
	}

	FirstBean(){
		System.out.println("firstBean构造");
	}
}
