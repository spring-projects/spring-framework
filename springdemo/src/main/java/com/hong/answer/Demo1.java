package com.hong.answer;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;


/**
 * @author wanghong
 * @date 2022/8/3
 * @apiNote
 */
public class Demo1 {

    @Test
	public void test1(){
		ClassPathResource rs = new ClassPathResource("spring-context.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(rs);

		System.out.println(factory.getBean("human").toString());
	}
}
