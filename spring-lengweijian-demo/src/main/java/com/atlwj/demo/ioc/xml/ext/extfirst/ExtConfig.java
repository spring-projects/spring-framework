package com.atlwj.demo.ioc.xml.ext.extfirst;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@Configurable
@ComponentScan("com.atlwj.demo.ioc.xml.ext.extfirst")
public class ExtConfig {

	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(ExtConfig.class);
		context.getBean("myComponent");
		((AnnotationConfigApplicationContext) context).close();
	}


}
