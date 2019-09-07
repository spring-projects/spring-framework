package com.atlwj.demo.ioc.xml.ext.test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ComponentScans(
		@ComponentScan("com.atlwj.demo.ioc.xml.ext.test")
)
public class PgiConfig {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(Pgi.class);
		Pgi sss = (Pgi) ioc.getBean("sss");
		sss.getName();
		ioc.close();
	}
}
