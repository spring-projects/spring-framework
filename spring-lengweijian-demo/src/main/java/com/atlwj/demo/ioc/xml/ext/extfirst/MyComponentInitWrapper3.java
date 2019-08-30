package com.atlwj.demo.ioc.xml.ext.extfirst;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class MyComponentInitWrapper3 implements BeanPostProcessor, Ordered {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("myComponent".equals(beanName)) {
			System.out.println("MyComponentInitWrapper3.......Before3....");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if ("myComponent".equals(beanName)) {
			System.out.println("MyComponentInitWrapper3.......after3....");
		}
		return bean;
	}

	@Override
	public int getOrder() {
		return 1;
	}
}
