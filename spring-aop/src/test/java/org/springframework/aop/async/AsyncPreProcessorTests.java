package org.springframework.aop.async;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.tests.aop.async.TestService;

import static org.springframework.tests.TestResourceUtils.qualifiedResource;

/**
 * @author huqichao
 * @date 2018-10-16 18:23
 */
public class AsyncPreProcessorTests {

	private static final Resource RESOURCE =
			qualifiedResource(AsyncPreProcessorTests.class, "context.xml");

	@Test
	public void testAsyncCall(){
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(RESOURCE);

		TestService testService = (TestService) bf.getBean("testService");
		testService.sayHello();
	}
}
