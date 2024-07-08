package org.springframework.ioc.test1;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ioc.model.User;

import java.io.IOException;

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
