/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Locale;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;

/**
 * @author Keith Donald
 */
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
		assertEquals(4, container.getBeanDefinitionCount());
		container.refresh();

		DefaultAnnotationHandlerMapping mapping = container.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());

		AnnotationMethodHandlerAdapter adapter = container.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);
		assertNotNull(container.getBean(FormattingConversionServiceFactoryBean.class));
		assertNotNull(container.getBean(ConversionService.class));
		assertNotNull(container.getBean(LocalValidatorFactoryBean.class));
		assertNotNull(container.getBean(Validator.class));

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
		assertEquals(4, container.getBeanDefinitionCount());
		container.refresh();

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
		assertEquals(4, container.getBeanDefinitionCount());
		container.refresh();

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
	
	@Test
	public void testInterceptors() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(container);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-interceptors.xml", getClass()));
		assertEquals(6, container.getBeanDefinitionCount());
		container.refresh();

		DefaultAnnotationHandlerMapping mapping = container.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(new TestController());
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("locale", "en");
		request.addParameter("theme", "green");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(3, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof ThemeChangeInterceptor);
	}

	@Test
	public void testBeanDecoration() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(container);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-bean-decoration.xml", getClass()));
		assertEquals(5, container.getBeanDefinitionCount());
		container.refresh();

		DefaultAnnotationHandlerMapping mapping = container.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(new TestController());		

		MockHttpServletRequest request = new MockHttpServletRequest();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(2, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);
		LocaleChangeInterceptor interceptor = (LocaleChangeInterceptor) chain.getInterceptors()[1];
		assertEquals("lang", interceptor.getParamName());
	}

	@Test
	public void testViewControllers() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(container);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-view-controllers.xml", getClass()));
		assertEquals(8, container.getBeanDefinitionCount());
		container.refresh();

		DefaultAnnotationHandlerMapping mapping = container.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(new TestController());		

		MockHttpServletRequest request = new MockHttpServletRequest();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(3, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);

		SimpleUrlHandlerMapping mapping2 = container.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping2);
		
		SimpleControllerHandlerAdapter adapter = container.getBean(SimpleControllerHandlerAdapter.class);
		assertNotNull(adapter);
		
		request.setRequestURI("/foo");
		request.setMethod("GET");
		chain = mapping2.getHandler(request);
		assertEquals(3, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);
		ModelAndView mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertNull(mv.getViewName());

		request.setRequestURI("/bar");
		chain = mapping2.getHandler(request);
		assertEquals(3, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);
		ModelAndView mv2 = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertEquals("baz", mv2.getViewName());
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
