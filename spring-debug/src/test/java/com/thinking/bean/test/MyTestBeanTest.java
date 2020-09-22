package com.thinking.bean.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.*;

class MyTestBeanTest {

	@Test
	void testSimpleLoad() {
		BeanFactory bf = new XmlBeanFactory(new ClassPathResource("beanFactoryTest.xml"));
		MyTestBean bean = (MyTestBean) bf.getBean("myTestBean");
		assertEquals("testStr",bean.getTestStr());

	}
}