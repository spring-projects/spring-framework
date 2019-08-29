package com.atlwj.demo.ioc.xml.ext.extsecend;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class BeanDefinitionIntercepter implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("******调用BeanFactoryPostProcessor开始");
		//获取到Spring中所有的beanName
		String[] beanStr = beanFactory.getBeanDefinitionNames();
		//循环bean做出自定义的操作
		for (String beanName : beanStr) {
			System.out.println("bean name:"+beanName);
			if ("cat".equals(beanName)) {
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
				System.out.println("修改cat的x属性值");
				beanDefinition.getPropertyValues().add("color", "red");
				beanDefinition.getPropertyValues().add("nick", "tom2");
			}
		}
		System.out.println("******调用BeanFactoryPostProcessor结束");
	}
}
