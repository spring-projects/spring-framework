/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
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
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;
import org.springframework.web.util.UrlPathHelper;

import static org.junit.Assert.*;

/**
 * Test fixture for the configuration in mvc-config-annotation-driven.xml.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Agim Emruli
 */
public class AnnotationDrivenBeanDefinitionParserTests {

	private final GenericWebApplicationContext appContext = new GenericWebApplicationContext();

	@Test
	public void testMessageCodesResolver() {
		loadBeanDefinitions("mvc-config-message-codes-resolver.xml");
		RequestMappingHandlerAdapter adapter = this.appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);
		Object initializer = adapter.getWebBindingInitializer();
		assertNotNull(initializer);
		MessageCodesResolver resolver =
				((ConfigurableWebBindingInitializer) initializer).getMessageCodesResolver();
		assertNotNull(resolver);
		assertEquals(TestMessageCodesResolver.class, resolver.getClass());
		assertEquals(false, new DirectFieldAccessor(adapter).getPropertyValue("ignoreDefaultModelOnRedirect"));
	}

	@Test
	public void testPathMatchingConfiguration() {
		loadBeanDefinitions("mvc-config-path-matching.xml");
		RequestMappingHandlerMapping hm = this.appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(hm);
		assertTrue(hm.useSuffixPatternMatch());
		assertFalse(hm.useTrailingSlashMatch());
		assertTrue(hm.useRegisteredSuffixPatternMatch());
		assertThat(hm.getUrlPathHelper(), Matchers.instanceOf(TestPathHelper.class));
		assertThat(hm.getPathMatcher(), Matchers.instanceOf(TestPathMatcher.class));
		List<String> fileExtensions = hm.getContentNegotiationManager().getAllFileExtensions();
		assertThat(fileExtensions, Matchers.contains("xml"));
		assertThat(fileExtensions, Matchers.hasSize(1));
	}

	@Test
	public void testMessageConverters() {
		loadBeanDefinitions("mvc-config-message-converters.xml");
		verifyMessageConverters(this.appContext.getBean(RequestMappingHandlerAdapter.class), true);
		verifyMessageConverters(this.appContext.getBean(ExceptionHandlerExceptionResolver.class), true);
		verifyRequestResponseBodyAdvice(this.appContext.getBean(RequestMappingHandlerAdapter.class));
		verifyResponseBodyAdvice(this.appContext.getBean(ExceptionHandlerExceptionResolver.class));
	}

	@Test
	public void testMessageConvertersWithoutDefaultRegistrations() {
		loadBeanDefinitions("mvc-config-message-converters-defaults-off.xml");
		verifyMessageConverters(this.appContext.getBean(RequestMappingHandlerAdapter.class), false);
		verifyMessageConverters(this.appContext.getBean(ExceptionHandlerExceptionResolver.class), false);
	}

	@Test
	public void testArgumentResolvers() {
		loadBeanDefinitions("mvc-config-argument-resolvers.xml");
		testArgumentResolvers(this.appContext.getBean(RequestMappingHandlerAdapter.class));
		testArgumentResolvers(this.appContext.getBean(ExceptionHandlerExceptionResolver.class));
	}

	private void testArgumentResolvers(Object bean) {
		assertNotNull(bean);
		Object value = new DirectFieldAccessor(bean).getPropertyValue("customArgumentResolvers");
		assertNotNull(value);
		assertTrue(value instanceof List);
		@SuppressWarnings("unchecked")
		List<HandlerMethodArgumentResolver> resolvers = (List<HandlerMethodArgumentResolver>) value;
		assertEquals(3, resolvers.size());
		assertTrue(resolvers.get(0) instanceof ServletWebArgumentResolverAdapter);
		assertTrue(resolvers.get(1) instanceof TestHandlerMethodArgumentResolver);
		assertTrue(resolvers.get(2) instanceof TestHandlerMethodArgumentResolver);
		assertNotSame(resolvers.get(1), resolvers.get(2));
	}

	@Test
	public void testReturnValueHandlers() {
		loadBeanDefinitions("mvc-config-return-value-handlers.xml");
		testReturnValueHandlers(this.appContext.getBean(RequestMappingHandlerAdapter.class));
		testReturnValueHandlers(this.appContext.getBean(ExceptionHandlerExceptionResolver.class));
	}

	private void testReturnValueHandlers(Object bean) {
		assertNotNull(bean);
		Object value = new DirectFieldAccessor(bean).getPropertyValue("customReturnValueHandlers");
		assertNotNull(value);
		assertTrue(value instanceof List);
		@SuppressWarnings("unchecked")
		List<HandlerMethodReturnValueHandler> handlers = (List<HandlerMethodReturnValueHandler>) value;
		assertEquals(2, handlers.size());
		assertEquals(TestHandlerMethodReturnValueHandler.class, handlers.get(0).getClass());
		assertEquals(TestHandlerMethodReturnValueHandler.class, handlers.get(1).getClass());
		assertNotSame(handlers.get(0), handlers.get(1));
	}

	@Test
	public void beanNameUrlHandlerMapping() {
		loadBeanDefinitions("mvc-config.xml");
		BeanNameUrlHandlerMapping mapping = this.appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(2, mapping.getOrder());
	}

	private void loadBeanDefinitions(String fileName) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.appContext);
		Resource resource = new ClassPathResource(fileName, AnnotationDrivenBeanDefinitionParserTests.class);
		reader.loadBeanDefinitions(resource);
		this.appContext.refresh();
	}

	@SuppressWarnings("unchecked")
	private void verifyMessageConverters(Object bean, boolean hasDefaultRegistrations) {
		assertNotNull(bean);
		Object value = new DirectFieldAccessor(bean).getPropertyValue("messageConverters");
		assertNotNull(value);
		assertTrue(value instanceof List);
		List<HttpMessageConverter<?>> converters = (List<HttpMessageConverter<?>>) value;
		if (hasDefaultRegistrations) {
			assertTrue("Default and custom converter expected", converters.size() > 2);
		}
		else {
			assertTrue("Only custom converters expected", converters.size() == 2);
		}
		assertTrue(converters.get(0) instanceof StringHttpMessageConverter);
		assertTrue(converters.get(1) instanceof ResourceHttpMessageConverter);
	}

	@SuppressWarnings("unchecked")
	private void verifyResponseBodyAdvice(Object bean) {
		assertNotNull(bean);
		Object value = new DirectFieldAccessor(bean).getPropertyValue("responseBodyAdvice");
		assertNotNull(value);
		assertTrue(value instanceof List);
		List<ResponseBodyAdvice<?>> converters = (List<ResponseBodyAdvice<?>>) value;
		assertTrue(converters.get(0) instanceof JsonViewResponseBodyAdvice);
	}

	@SuppressWarnings("unchecked")
	private void verifyRequestResponseBodyAdvice(Object bean) {
		assertNotNull(bean);
		Object value = new DirectFieldAccessor(bean).getPropertyValue("requestResponseBodyAdvice");
		assertNotNull(value);
		assertTrue(value instanceof List);
		List<ResponseBodyAdvice<?>> converters = (List<ResponseBodyAdvice<?>>) value;
		assertTrue(converters.get(0) instanceof JsonViewRequestBodyAdvice);
		assertTrue(converters.get(1) instanceof JsonViewResponseBodyAdvice);
	}

}

class TestWebArgumentResolver implements WebArgumentResolver {

	@Override
	public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception {
		return null;
	}

}

class TestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return false;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		return null;
	}
}

class TestHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return false;
	}

	@Override
	public void handleReturnValue(Object returnValue,
			MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
	}

}

class TestMessageCodesResolver implements MessageCodesResolver {

	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName) {
		return new String[] { "test.foo.bar" };
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String[] resolveMessageCodes(String errorCode, String objectName, String field, @Nullable Class fieldType) {
		return new String[] { "test.foo.bar" };
	}

}

class TestPathMatcher extends AntPathMatcher { }

class TestPathHelper extends UrlPathHelper { }