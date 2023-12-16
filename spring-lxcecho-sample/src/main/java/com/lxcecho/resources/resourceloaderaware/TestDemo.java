package com.lxcecho.resources.resourceloaderaware;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ResourceLoader;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class TestDemo {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-resources.xml");
		TestBean testBean = context.getBean("testBean", TestBean.class);
		ResourceLoader resourceLoader = testBean.getResourceLoader();
		System.out.println(context == resourceLoader);
	}
}
