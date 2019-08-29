package com.atlwj.demo.ioc.xml.ext.extfirst;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;


public class MyComponentInitWrapper4 implements BeanPostProcessor{

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("myComponent".equals(beanName)) {
			System.out.println("MyComponentInitWrapper4.......Before4....");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("myComponent".equals(beanName)) {
			System.out.println("MyComponentInitWrapper4.......after4....");
		}
		return bean;
	}

}
