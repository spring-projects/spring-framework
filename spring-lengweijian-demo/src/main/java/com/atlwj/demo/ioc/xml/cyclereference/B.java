package com.atlwj.demo.ioc.xml.cyclereference;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype")
public class B {

	//@Autowired
	private C c;

//	public B(C c){
//		this.c = c;
//	}

	public void showB(){
		System.out.println("bbbbbb");
	}

	public void setC(C c){
		this.c = c;
	}
}
