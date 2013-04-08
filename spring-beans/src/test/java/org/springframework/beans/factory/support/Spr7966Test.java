package org.springframework.beans.factory.support;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

public class Spr7966Test {

	@Test(expected=BeanCreationException.class)
	public void test() {
		try {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource("Spr7966Tests.xml", getClass()));
			
			bf.getBean("bean1");
		}
		catch (BeanCreationException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
}

class TestService {
	
}

class TestService1 {
	
}

class TestService2 {
	
}