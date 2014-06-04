package source.source.spring.ch4;

import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class CustomBeanTest {

	@Test
	public void test() {
		ClassPathResource classPathResource = new ClassPathResource("customBean.xml");
		XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(classPathResource);
		User f = (User) xmlBeanFactory.getBean("mj");
		System.out.println(f);
	}

}
