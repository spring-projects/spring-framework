package com.atlwj.demo.ioc.xml;

import com.atlwj.demo.ioc.annotation.entity.Person;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

public class AppXml {
	public static void main(String[] args) {
		// 1.获取资源
		ClassPathResource resource = new ClassPathResource("bean1.xml");
		// 2.获取 BeanFactory
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		// 3.根据新建的 BeanFactory 创建一个 BeanDefinitionReader 对象，该 Reader 对象为资源的解析器
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		// 4.装载资源
		reader.loadBeanDefinitions(resource);

		Person person = (Person) factory.getBean("person");
		System.out.println(person);

	}
}
