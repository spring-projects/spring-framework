package org.springframework.web.servlet.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping;

public class MvcNamespaceTests {

	private GenericWebApplicationContext container;
	
	@Before
	public void setUp() {
		container = new GenericWebApplicationContext();
		container.setServletContext(new MockServletContext());
	}
	
	@Test
	public void testDefaultConfig() {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(container);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config.xml", getClass()));
		assertEquals(2, container.getBeanDefinitionCount());
		DefaultAnnotationHandlerMapping mapping = container.getBean("defaultAnnotationHandlerMapping", DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());
		AnnotationMethodHandlerAdapter adapter = container.getBean("annotationMethodHandlerAdapter", AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);
	}
}
