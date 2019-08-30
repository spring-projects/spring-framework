package com.atlwj.demo.ioc.xml.ext.test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.stereotype.Component;

@Component("sss")
public class Pgi implements InitializingBean, DisposableBean, BeanNameAware, BeanClassLoaderAware, BeanFactoryAware {
	private String name;

	public Pgi() {
		System.out.println("pgi构造器.....");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Pgi{" +
				"name='" + name + '\'' +
				'}';
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("pgi...destroy");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("init....");
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.println("setBeanClassLoader....");
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.println("setBeanFactory...");
	}

	@Override
	public void setBeanName(String name) {
		System.out.println("setBeanName....");
	}
}
