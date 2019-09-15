package com.atlwj.demo.ioc.xml.aware;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

import java.util.Properties;

@ComponentScan("com.atlwj.demo.ioc.xml.aware")
@Configurable
public class AwareApp {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(AwareApp.class);
		MyApplicationAware bean = ioc.getBean(MyApplicationAware.class);
		bean.display();
	}
}
