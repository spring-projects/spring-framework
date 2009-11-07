package org.springframework.web.servlet.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.Style;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping;

public class MvcNamespaceTests {

	private GenericWebApplicationContext container;
	
	@Before
	public void setUp() {
		container = new GenericWebApplicationContext();
		container.setServletContext(new MockServletContext());
		
		LocaleContextHolder.setLocale(Locale.US);
	}
	
	@Test
	public void testDefaultConfig() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(container);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config.xml", getClass()));
		assertEquals(2, container.getBeanDefinitionCount());
		DefaultAnnotationHandlerMapping mapping = container.getBean("defaultAnnotationHandlerMapping", DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());
		AnnotationMethodHandlerAdapter adapter = container.getBean("annotationMethodHandlerAdapter", AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);

		TestController handler = new TestController();

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "Oct 31, 2009");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, handler);
	}
	
	@Controller
	public static class TestController {
		
		@RequestMapping
		public void testBind(@RequestParam @DateTimeFormat(dateStyle=Style.MEDIUM) Date date) {
			System.out.println(date);
		}
	}
}
