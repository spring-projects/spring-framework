package com.atlwj.demo.ioc.ext.extfirst;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;


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
