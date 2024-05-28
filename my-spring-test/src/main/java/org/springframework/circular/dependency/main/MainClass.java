package org.springframework.circular.dependency.main;

import org.springframework.circular.dependency.config.MainConfig;
import org.springframework.circular.dependency.model.InstanceA;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MainClass {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(MainConfig.class);
		InstanceA instanceA1 = (InstanceA)annotationConfigApplicationContext.getBean("instanceA");
		System.out.println(instanceA1);
		ApplicationContext context 	= new ClassPathXmlApplicationContext("circular/dependncy/bean.xml");
//		InstanceA instanceA = context.getBean("instanceA", InstanceA.class);
//		System.out.println(instanceA);
	}
}
