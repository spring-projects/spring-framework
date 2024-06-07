package org.springframework.test1;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.model.User;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author sushuaiqiang
 * @date 2024/5/15 - 17:10
 */
public class Main {
	public static void main(String[] args) throws IOException {
//		ClassPathResource classPathResource = new ClassPathResource("bean.xml");
//		InputStream inputStream = classPathResource.getInputStream();
//		BeanFactory xmlBeanFactory = new XmlBeanFactory(new ClassPathResource("bean.xml"));
//		User user = (User) xmlBeanFactory.getBean("user");

		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("bean.xml");
		User user = (User) applicationContext.getBean("user");
		System.out.println(user);
	}
}
