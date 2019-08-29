package com.atlwj.demo.ioc.xml.cyclereference;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype")
public class C {

	//@Autowired
	private A a;

//	public C(A a){
//		this.a = a;
//	}

	public void showC(){
		System.out.println("ccccc");
	}

	public void setA(A a){
		this.a = a;
	}
}
