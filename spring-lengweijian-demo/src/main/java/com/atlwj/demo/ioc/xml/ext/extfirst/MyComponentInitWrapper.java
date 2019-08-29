package com.atlwj.demo.ioc.xml.ext.extfirst;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

//@Component
public class MyComponentInitWrapper implements BeanPostProcessor, Ordered {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("myComponent".equals(beanName)) {
			System.out.println("beanPostProcessor.......Before....");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("myComponent".equals(beanName)) {
			System.out.println("beanPostProcessor.......after....");
		}
		return bean;
	}

	@Override
	public int getOrder() {
		return 3;
	}
}
