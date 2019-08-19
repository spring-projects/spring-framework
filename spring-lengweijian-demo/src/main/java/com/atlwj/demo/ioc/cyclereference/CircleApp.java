package com.atlwj.demo.ioc.cyclereference;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@Configurable
@ComponentScan(value = "com.atlwj.demo.ioc.cyclereference")
public class CircleApp {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(CircleApp.class);
		A a = ioc.getBean(A.class);
		a.showA();
		B b = ioc.getBean(B.class);
		b.showB();
		C c = ioc.getBean(C.class);
		c.showC();
	}
}
