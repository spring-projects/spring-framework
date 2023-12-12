package com.lxcecho.resources.di;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class TestBean {

	public static void main(String[] args) {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("beans.xml");
		ResourceBean resourceBean = context.getBean(ResourceBean.class);
		resourceBean.parse();
	}
}
