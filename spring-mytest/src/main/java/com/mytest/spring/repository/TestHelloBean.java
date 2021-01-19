package com.mytest.spring.repository;

import com.mytest.spring.config.MytestConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author liweifan
 */
public class TestHelloBean {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(MytestConfig.class);
		Hello hello = ac.getBean(Hello.class);
		hello.hello();
	}
}
