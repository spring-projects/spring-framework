/*
 * Copyright 2002-2011 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;

/**
 * Test fixture for the configuration in mvc-config-annotation-driven.xml.
 * @author Rossen Stoyanchev
 */
public class AnnotationDrivenBeanDefinitionParserTests {

	private GenericWebApplicationContext appContext;

	@Before
	public void setup() {
		appContext = new GenericWebApplicationContext();
	}

	@Test
	public void testMessageCodesResolver() {
		loadBeanDefinitions("mvc-config-message-codes-resolver.xml");
		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);
		Object initializer = adapter.getWebBindingInitializer();
		assertNotNull(initializer);
		MessageCodesResolver resolver = ((ConfigurableWebBindingInitializer) initializer).getMessageCodesResolver();
		assertNotNull(resolver);
		assertEquals(TestMessageCodesResolver.class, resolver.getClass());
		assertEquals(false, new DirectFieldAccessor(adapter).getPropertyValue("ignoreDefaultModelOnRedirect"));
	}

	@Test
	public void testMessageConverters() {
		loadBeanDefinitions("mvc-config-message-converters.xml");
		verifyMessageConverters(appContext.getBean(RequestMappingHandlerAdapter.class), true);
		verifyMessageConverters(appContext.getBean(ExceptionHandlerExceptionResolver.class), true);
	}

	@Test
	public void testMessageConvertersWithoutDefaultRegistrations() {
		loadBeanDefinitions("mvc-config-message-converters-defaults-off.xml");
		verifyMessageConverters(appContext.getBean(RequestMappingHandlerAdapter.class), false);
		verifyMessageConverters(appContext.getBean(ExceptionHandlerExceptionResolver.class), false);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testArgumentResolvers() {
		loadBeanDefinitions("mvc-config-argument-resolvers.xml");
		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);
		Object value = new DirectFieldAccessor(adapter).getPropertyValue("customArgumentResolvers");
		assertNotNull(value);
		assertTrue(value instanceof List);
		List<HandlerMethodArgumentResolver> resolvers = (List<HandlerMethodArgumentResolver>) value;
		assertEquals(2, resolvers.size());
		assertTrue(resolvers.get(0) instanceof ServletWebArgumentResolverAdapter);
		assertTrue(resolvers.get(1) instanceof TestHandlerMethodArgumentResolver);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReturnValueHandlers() {
		loadBeanDefinitions("mvc-config-return-value-handlers.xml");
		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);
		Object value = new DirectFieldAccessor(adapter).getPropertyValue("customReturnValueHandlers");
		assertNotNull(value);
		assertTrue(value instanceof List);
		List<HandlerMethodReturnValueHandler> handlers = (List<HandlerMethodReturnValueHandler>) value;
		assertEquals(1, handlers.size());
		assertEquals(TestHandlerMethodReturnValueHandler.class, handlers.get(0).getClass());
	}

	@Test
	public void beanNameUrlHandlerMapping() {
		loadBeanDefinitions("mvc-config.xml");
		BeanNameUrlHandlerMapping mapping = appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(2, mapping.getOrder());
	}

	private void loadBeanDefinitions(String fileName) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		ClassPathResource resource = new ClassPathResource(fileName, AnnotationDrivenBeanDefinitionParserTests.class);
		reader.loadBeanDefinitions(resource);
		appContext.refresh();
	}

	@SuppressWarnings("unchecked")
	private void verifyMessageConverters(Object bean, boolean hasDefaultRegistrations) {
		assertNotNull(bean);
		Object value = new DirectFieldAccessor(bean).getPropertyValue("messageConverters");
		assertNotNull(value);
		assertTrue(value instanceof List);
		List<HttpMessageConverter<?>> converters = (List<HttpMessageConverter<?>>) value;
		if (hasDefaultRegistrations) {
			assertTrue("Default converters are registered in addition to custom ones", converters.size() > 2);
		} else {
			assertTrue("Default converters should not be registered", converters.size() == 2);
		}
		assertTrue(converters.get(0) instanceof StringHttpMessageConverter);
		assertTrue(converters.get(1) instanceof ResourceHttpMessageConverter);
	}

}

class TestWebArgumentResolver implements WebArgumentResolver {

	public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception {
		return null;
	}

}

class TestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		return false;
	}

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		return null;
	}
}

class TestHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	public boolean supportsReturnType(MethodParameter returnType) {
		return false;
	}

	public void handleReturnValue(Object returnValue,
			MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
	}

}

class TestMessageCodesResolver implements MessageCodesResolver {

	public String[] resolveMessageCodes(String errorCode, String objectName) {
		return new String[] { "test.foo.bar" };
	}

	@SuppressWarnings("rawtypes")
	public String[] resolveMessageCodes(String errorCode, String objectName, String field, Class fieldType) {
		return new String[] { "test.foo.bar" };
	}

}
