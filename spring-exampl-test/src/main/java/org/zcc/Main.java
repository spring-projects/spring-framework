package org.zcc;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.zcc.configs.BeanConfig;

import java.util.Arrays;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
	public static void main(String[] args) {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(factory);
		int i = xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource("myBeanFactory.xml"));

		System.out.println(Arrays.toString(factory.getBeanDefinitionNames()));

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(BeanConfig.class);
		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		System.out.println(Arrays.toString(beanDefinitionNames));

		applicationContext.start();


	}
}