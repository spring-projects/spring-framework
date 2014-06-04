package source.source.spring;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class Demo {

	public static void main(String[] args) {
		ClassPathResource classPathResource = new ClassPathResource("bean.xml");
		XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(classPathResource);
		TestBean bean = (TestBean) xmlBeanFactory.getBean("testBean");
		System.out.println(123);
	}

}
