package com.atlwj.demo.ioc.xml.ext.extfirst;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class MyComponent{

	public MyComponent() {
		System.out.println("MyComponent.....constructor....");
	}

	@PostConstruct
	public void init(){
		System.out.println("init...");
	}

	@PreDestroy
	public void destroy(){
		System.out.println("destroy.....");
	}
}
