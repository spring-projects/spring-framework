package org.springframework.web.servlet.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Locale;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
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
		DefaultAnnotationHandlerMapping mapping = container.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());
		AnnotationMethodHandlerAdapter adapter = container.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);

		TestController handler = new TestController();

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, handler);
		assertTrue(handler.recordedValidationError);
	}

	@Test(expected=ConversionFailedException.class)
	public void testCustomConversionService() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(container);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-custom-conversion-service.xml", getClass()));
		assertEquals(3, container.getBeanDefinitionCount());
		DefaultAnnotationHandlerMapping mapping = container.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());
		AnnotationMethodHandlerAdapter adapter = container.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);

		TestController handler = new TestController();

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, handler);
	}

	@Test
	public void testCustomValidator() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(container);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-custom-validator.xml", getClass()));
		assertEquals(3, container.getBeanDefinitionCount());
		DefaultAnnotationHandlerMapping mapping = container.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());
		AnnotationMethodHandlerAdapter adapter = container.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);

		TestController handler = new TestController();

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, handler);

		assertTrue(container.getBean(TestValidator.class).validatorInvoked);
		assertFalse(handler.recordedValidationError);
	}

	@Controller
	public static class TestController {
		
		private boolean recordedValidationError;
		
		@RequestMapping
		public void testBind(@RequestParam @DateTimeFormat(iso=ISO.DATE) Date date, @Valid TestBean bean, BindingResult result) {
			if (result.getErrorCount() == 1) {
				this.recordedValidationError = true;
			} else {
				this.recordedValidationError = false;
			}
		}
	}
	
	public static class TestValidator implements Validator {

		boolean validatorInvoked;
		
		public boolean supports(Class<?> clazz) {
			return true;
		}

		public void validate(Object target, Errors errors) {
			this.validatorInvoked = true;
		}
		
	}
	
	private static class TestBean {
		
		@NotNull
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
		
	}
}
