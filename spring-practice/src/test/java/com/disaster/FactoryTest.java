package com.disaster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class FactoryTest {

	@Test
	public void factoryTest(){
		//Resource、BeanFactory、BeanDefinitionReader之间的关系是
		ClassPathResource resource = new ClassPathResource("beans.xml");
		XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(resource);
		System.out.println(xmlBeanFactory.getBean(User.class));
	}

	@Test
	public void factoryTest1(){
		ClassPathResource resource = new ClassPathResource("beans.xml");
		DefaultListableBeanFactory defaultListableBeanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(defaultListableBeanFactory);
		reader.loadBeanDefinitions(resource);
		System.out.println(defaultListableBeanFactory.getBean(User.class));
	}

}
