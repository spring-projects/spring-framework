package org.bean;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class BeanAnnotationTest {

	public static AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

	static void init(){
		applicationContext.scan("org.bean");
		applicationContext.refresh();
	}
	public static void main(String[] args) {
		init();
		RelationDemo relationDemo = (RelationDemo) applicationContext.getBean("relationDemo");
		System.out.println(relationDemo.getEventDemo());
	}
}
