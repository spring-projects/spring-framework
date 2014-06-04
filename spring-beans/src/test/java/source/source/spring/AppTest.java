package source.source.spring;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit test for simple App.
 */
public class AppTest {
	
	@SuppressWarnings("deprecation")
	@org.junit.Test
	public void testName() throws Exception {
		
		ClassPathResource classPathResource = new ClassPathResource("bean.xml");
		XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(classPathResource);
		TestBean bean = (TestBean) xmlBeanFactory.getBean("testBean");
		assertEquals("bruce name",bean.getName());
		
	}
	
}
