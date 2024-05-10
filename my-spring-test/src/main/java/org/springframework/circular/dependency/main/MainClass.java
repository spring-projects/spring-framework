package org.springframework.circular.dependency.main;

import org.springframework.circular.dependency.model.InstanceA;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MainClass {

	public static void main(String[] args) {
		ApplicationContext context 	= new ClassPathXmlApplicationContext("circular/dependncy/bean.xml");
		InstanceA instanceA = context.getBean("instanceA", InstanceA.class);
		System.out.println(instanceA);
	}
}
