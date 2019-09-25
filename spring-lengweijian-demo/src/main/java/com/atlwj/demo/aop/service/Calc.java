package com.atlwj.demo.aop.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * implements InitializingBean, DisposableBean, BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, BeanPostProcessor
 */
@Component(value = "calcService")
public class Calc {

//	public Calc(){
//		System.out.println("Calc...constructor.....");
//	}

	public int div(int i,int j){
		System.out.println("div.....");
		return i/j;
	}
//
//	@Override
//	public void setBeanClassLoader(ClassLoader classLoader) {
//		System.out.printf("Calc....setBeanClassLoader....%s\n",classLoader);
//	}
//
//	@Override
//	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
//		System.out.printf("Calc....setBeanFactory....%s\n",beanFactory);
//	}
//
//	@Override
//	public void setBeanName(String name) {
//		System.out.printf("Calc....setBeanName....%s\n",name);
//	}
//
//	@Override
//	public void destroy() throws Exception {
//		System.out.println("Calc....destroy....");
//	}
//
//	@Override
//	public void afterPropertiesSet() throws Exception {
//		System.out.println("Calc....afterPropertiesSet....");
//	}
//
//	@Override
//	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
//		System.out.printf("Calc....postProcessBeforeInitialization......%s\n",beanName);
//		return bean;
//	}
//
//	@Override
//	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//		System.out.printf("Calc....postProcessAfterInitialization......%s\n",beanName);
//		return bean;
//	}
}
