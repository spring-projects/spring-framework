package com.alex.test;

import com.alex.app.Appconfig;
import com.alex.service.CityService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext =
				new AnnotationConfigApplicationContext(Appconfig.class);

		System.out.println(annotationConfigApplicationContext.getBean(CityService.class));
	}
}
