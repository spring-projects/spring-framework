package org.springframework.ioc.test02;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;


public class MyTestBeanTest {

	public static void main(String[] args) {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("test02/bean.xml"));
		MyTestBean bean = (MyTestBean)beanFactory.getBean("myTestBean");
		System.out.println(bean.getTestStr());
	}
}
