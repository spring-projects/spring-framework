package com.cn.mayf.mapper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @Author mayf
 * @Date 2021/3/9 22:45
 */
@Component
public class DemoBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//		System.out.println("this is "+DemoBeanFactoryPostProcessor.class.getSimpleName());
//		System.out.println(beanFactory.getBean("aa"));
		/*String[] names = beanFactory.getBeanDefinitionNames();
		for (String name : names) {
			System.out.println(name);
			if(beanFactory.getBeanDefinition(name).isAbstract()){
				System.out.println(name+" is abstract!");
				System.out.println("~~~~~~~~~~~~~~~~~~");
				continue;
			}
			System.out.println(beanFactory.getBean(name));
			System.out.println("+++++++++++++++++");
		}*/
//		System.out.println("----------------------");
//		for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
//			System.out.println(beanDefinitionName+" type:"
//					+beanFactory.getBean(beanDefinitionName).getClass());
//		}
	}
}
