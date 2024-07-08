package org.springframework.ioc.circular.dependency.main;

import org.springframework.ioc.circular.dependency.config.MainConfig;
import org.springframework.ioc.circular.dependency.model.InstanceA;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainClass {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(MainConfig.class);
		InstanceA instanceA1 = (InstanceA)annotationConfigApplicationContext.getBean("instanceA");
		System.out.println(instanceA1);
//		ApplicationContext context 	= new ClassPathXmlApplicationContext("circular/dependncy/bean.xml");
//		InstanceA instanceA = context.getBean("instanceA", InstanceA.class);
//		System.out.println(instanceA);
	}
}
