/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Date;
import java.util.Locale;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static org.junit.Assert.*;
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
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
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
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;

/**
 * @author Keith Donald
 * @author Arjen Poutsma 
 */
public class MvcNamespaceTests {

	private GenericWebApplicationContext appContext;

	@Before
	public void setUp() {
		appContext = new GenericWebApplicationContext();
		appContext.setServletContext(new MockServletContext());
		LocaleContextHolder.setLocale(Locale.US);
	}

	@Test
	public void testDefaultConfig() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config.xml", getClass()));
		assertEquals(5, appContext.getBeanDefinitionCount());
		appContext.refresh();

		DefaultAnnotationHandlerMapping mapping = appContext.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());
		TestController handler = new TestController();
		mapping.setDefaultHandler(handler);
		
		AnnotationMethodHandlerAdapter adapter = appContext.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);
		
		HttpMessageConverter<?>[] messageConverters = adapter.getMessageConverters();
		assertNotNull(messageConverters);
		assertEquals(6, messageConverters.length);
		assertTrue(ByteArrayHttpMessageConverter.class.equals(messageConverters[0].getClass()));
		assertTrue(StringHttpMessageConverter.class.equals(messageConverters[1].getClass()));
		assertTrue(FormHttpMessageConverter.class.equals(messageConverters[2].getClass()));
		assertTrue(SourceHttpMessageConverter.class.equals(messageConverters[3].getClass()));
		assertTrue(Jaxb2RootElementHttpMessageConverter.class.equals(messageConverters[4].getClass()));
		assertTrue(MappingJacksonHttpMessageConverter.class.equals(messageConverters[5].getClass()));

		assertNotNull(appContext.getBean(FormattingConversionServiceFactoryBean.class));
		assertNotNull(appContext.getBean(ConversionService.class));
		assertNotNull(appContext.getBean(LocalValidatorFactoryBean.class));
		assertNotNull(appContext.getBean(Validator.class));

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(2, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		ConversionServiceExposingInterceptor interceptor = (ConversionServiceExposingInterceptor) chain.getInterceptors()[1];
		interceptor.preHandle(request, response, handler);
		assertSame(appContext.getBean(ConversionService.class), request.getAttribute(ConversionService.class.getName()));
		
		adapter.handle(request, response, handler);
		assertTrue(handler.recordedValidationError);
	}

	@Test(expected=ConversionFailedException.class)
	public void testCustomConversionService() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-custom-conversion-service.xml", getClass()));
		assertEquals(5, appContext.getBeanDefinitionCount());
		appContext.refresh();

		DefaultAnnotationHandlerMapping mapping = appContext.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		TestController handler = new TestController();
		mapping.setDefaultHandler(handler);
		
		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/accounts/12345");
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(2, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		ConversionServiceExposingInterceptor interceptor = (ConversionServiceExposingInterceptor) chain.getInterceptors()[1];
		interceptor.preHandle(request, response, handler);
		assertSame(appContext.getBean("conversionService"), request.getAttribute(ConversionService.class.getName()));
	
		AnnotationMethodHandlerAdapter adapter = appContext.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);
		adapter.handle(request, response, handler);
	}

	@Test
	public void testCustomValidator() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-custom-validator.xml", getClass()));
		assertEquals(5, appContext.getBeanDefinitionCount());
		appContext.refresh();

		AnnotationMethodHandlerAdapter adapter = appContext.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);

		TestController handler = new TestController();

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, handler);

		assertTrue(appContext.getBean(TestValidator.class).validatorInvoked);
		assertFalse(handler.recordedValidationError);
	}
	
	@Test
	public void testInterceptors() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-interceptors.xml", getClass()));
		assertEquals(8, appContext.getBeanDefinitionCount());
		appContext.refresh();

		DefaultAnnotationHandlerMapping mapping = appContext.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(new TestController());
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/accounts/12345");
		request.addParameter("locale", "en");
		request.addParameter("theme", "green");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);

		request.setRequestURI("/logged/accounts/12345");
		chain = mapping.getHandler(request);
		assertEquals(5, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[4] instanceof WebRequestHandlerInterceptorAdapter);

		request.setRequestURI("/foo/logged");
		chain = mapping.getHandler(request);
		assertEquals(5, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[4] instanceof WebRequestHandlerInterceptorAdapter);

	}

	@Test
	public void testBeanDecoration() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-bean-decoration.xml", getClass()));
		assertEquals(7, appContext.getBeanDefinitionCount());
		appContext.refresh();

		DefaultAnnotationHandlerMapping mapping = appContext.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(new TestController());		

		MockHttpServletRequest request = new MockHttpServletRequest();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);	
		LocaleChangeInterceptor interceptor = (LocaleChangeInterceptor) chain.getInterceptors()[2];
		assertEquals("lang", interceptor.getParamName());
		ThemeChangeInterceptor interceptor2 = (ThemeChangeInterceptor) chain.getInterceptors()[3];
		assertEquals("style", interceptor2.getParamName());
	}

	@Test
	public void testViewControllers() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-view-controllers.xml", getClass()));
		assertEquals(9, appContext.getBeanDefinitionCount());
		appContext.refresh();

		DefaultAnnotationHandlerMapping mapping = appContext.getBean(DefaultAnnotationHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(new TestController());		

		MockHttpServletRequest request = new MockHttpServletRequest();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);

		SimpleUrlHandlerMapping mapping2 = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping2);
		
		SimpleControllerHandlerAdapter adapter = appContext.getBean(SimpleControllerHandlerAdapter.class);
		assertNotNull(adapter);
		
		request.setRequestURI("/foo");
		request.setMethod("GET");
		chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		ModelAndView mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertNull(mv.getViewName());

		request.setRequestURI("/bar");
		chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
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
