package com.atlwj.demo.ioc.lifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 测试bean的生命周期
 *
 * @author LengWJ
 */
public class LifeCycleBean implements BeanNameAware, BeanFactoryAware, BeanClassLoaderAware, BeanPostProcessor,

		InitializingBean, DisposableBean {

	@Autowired
	LifeCycleBean02 lifeCycleBean02;

	public LifeCycleBean() {
		System.out.println("LifeCycleBean...constructor...构造函数调用...");
	}

	public void display() {
		System.out.println("display....方法调用..." + lifeCycleBean02);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.println("BeanClassLoaderAware 被调用...");
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.println("BeanFactoryAware 被调用...");
	}

	@Override
	public void setBeanName(String name) {
		System.out.println("BeanNameAware 被调用...");
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("DisposableBean destroy 被调动...");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitializingBean afterPropertiesSet 被调动...");
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("BeanPostProcessor postProcessBeforeInitialization 被调用...");
		return bean;

	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("BeanPostProcessor postProcessAfterInitialization 被调用...");
		return bean;
	}

	public void initMethod() {
		System.out.println("init-method 被调用...");
	}

	public void destroyMethod() {
		System.out.println("destroy-method 被调用...");
	}
}
