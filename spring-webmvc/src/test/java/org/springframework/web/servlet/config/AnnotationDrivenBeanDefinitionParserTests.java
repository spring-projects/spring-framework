/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for the configuration in mvc-config-annotation-driven.xml.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Agim Emruli
 */
class AnnotationDrivenBeanDefinitionParserTests {

	private final GenericWebApplicationContext appContext = new GenericWebApplicationContext();

	@Test
	void testMessageCodesResolver() {
		loadBeanDefinitions("mvc-config-message-codes-resolver.xml");
		RequestMappingHandlerAdapter adapter = this.appContext.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		Object initializer = adapter.getWebBindingInitializer();
		assertThat(initializer).isNotNull();
		MessageCodesResolver resolver =
				((ConfigurableWebBindingInitializer) initializer).getMessageCodesResolver();
		assertThat(resolver).isNotNull();
		assertThat(resolver.getClass()).isEqualTo(TestMessageCodesResolver.class);
	}

	@SuppressWarnings("removal")
	@Test
	public void testPathMatchingConfiguration() {
		loadBeanDefinitions("mvc-config-path-matching.xml");
		RequestMappingHandlerMapping hm = this.appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(hm).isNotNull();
		assertThat(hm.getUrlPathHelper()).isInstanceOf(TestPathHelper.class);
		assertThat(hm.getPathMatcher()).isInstanceOf(TestPathMatcher.class);
		assertThat(hm.getPatternParser()).isNull();
	}

	@Test
	void testMessageConverters() {
		loadBeanDefinitions("mvc-config-message-converters.xml");
		verifyMessageConverters(this.appContext.getBean(RequestMappingHandlerAdapter.class), true);
		verifyMessageConverters(this.appContext.getBean(ExceptionHandlerExceptionResolver.class), true);
		verifyRequestResponseBodyAdvice(this.appContext.getBean(RequestMappingHandlerAdapter.class));
		verifyResponseBodyAdvice(this.appContext.getBean(ExceptionHandlerExceptionResolver.class));
	}

	@Test
	void testMessageConvertersWithoutDefaultRegistrations() {
		loadBeanDefinitions("mvc-config-message-converters-defaults-off.xml");
		verifyMessageConverters(this.appContext.getBean(RequestMappingHandlerAdapter.class), false);
		verifyMessageConverters(this.appContext.getBean(ExceptionHandlerExceptionResolver.class), false);
	}

	@Test
	void testArgumentResolvers() {
		loadBeanDefinitions("mvc-config-argument-resolvers.xml");
		testArgumentResolvers(this.appContext.getBean(RequestMappingHandlerAdapter.class));
		testArgumentResolvers(this.appContext.getBean(ExceptionHandlerExceptionResolver.class));
	}

	private void testArgumentResolvers(Object bean) {
		assertThat(bean).isNotNull();
		Object value = new DirectFieldAccessor(bean).getPropertyValue("customArgumentResolvers");
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<HandlerMethodArgumentResolver> resolvers = (List<HandlerMethodArgumentResolver>) value;
		assertThat(resolvers).hasSize(3);
		assertThat(resolvers).element(0).isInstanceOf(ServletWebArgumentResolverAdapter.class);
		assertThat(resolvers).element(1).isInstanceOf(TestHandlerMethodArgumentResolver.class);
		assertThat(resolvers).element(2).isInstanceOf(TestHandlerMethodArgumentResolver.class);
		assertThat(resolvers).element(2).isNotSameAs(resolvers.get(1));
	}

	@Test
	void testReturnValueHandlers() {
		loadBeanDefinitions("mvc-config-return-value-handlers.xml");
		testReturnValueHandlers(this.appContext.getBean(RequestMappingHandlerAdapter.class));
		testReturnValueHandlers(this.appContext.getBean(ExceptionHandlerExceptionResolver.class));
	}

	private void testReturnValueHandlers(Object bean) {
		assertThat(bean).isNotNull();
		Object value = new DirectFieldAccessor(bean).getPropertyValue("customReturnValueHandlers");
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<HandlerMethodReturnValueHandler> handlers = (List<HandlerMethodReturnValueHandler>) value;
		assertThat(handlers).hasSize(2);
		assertThat(handlers.get(0).getClass()).isEqualTo(TestHandlerMethodReturnValueHandler.class);
		assertThat(handlers.get(1).getClass()).isEqualTo(TestHandlerMethodReturnValueHandler.class);
		assertThat(handlers).element(1).isNotSameAs(handlers.get(0));
	}

	@Test
	void beanNameUrlHandlerMapping() {
		loadBeanDefinitions("mvc-config.xml");
		BeanNameUrlHandlerMapping mapping = this.appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(2);
	}

	private void loadBeanDefinitions(String fileName) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.appContext);
		Resource resource = new ClassPathResource(fileName, AnnotationDrivenBeanDefinitionParserTests.class);
		reader.loadBeanDefinitions(resource);
		this.appContext.refresh();
	}

	@SuppressWarnings("unchecked")
	private void verifyMessageConverters(Object bean, boolean hasDefaultRegistrations) {
		assertThat(bean).isNotNull();
		Object value = new DirectFieldAccessor(bean).getPropertyValue("messageConverters");
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(List.class);
		List<HttpMessageConverter<?>> converters = (List<HttpMessageConverter<?>>) value;
		if (hasDefaultRegistrations) {
			assertThat(converters.size()).as("Default and custom converter expected").isGreaterThan(2);
		}
		else {
			assertThat(converters.size()).as("Only custom converters expected").isEqualTo(2);
		}
		assertThat(converters).element(0).isInstanceOf(StringHttpMessageConverter.class);
		assertThat(converters).element(1).isInstanceOf(ResourceHttpMessageConverter.class);
	}

	@SuppressWarnings("unchecked")
	private void verifyResponseBodyAdvice(Object bean) {
		assertThat(bean).isNotNull();
		Object value = new DirectFieldAccessor(bean).getPropertyValue("responseBodyAdvice");
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(List.class);
		List<ResponseBodyAdvice<?>> converters = (List<ResponseBodyAdvice<?>>) value;
		assertThat(converters).element(0).isInstanceOf(JsonViewResponseBodyAdvice.class);
	}

	@SuppressWarnings("unchecked")
	private void verifyRequestResponseBodyAdvice(Object bean) {
		assertThat(bean).isNotNull();
		Object value = new DirectFieldAccessor(bean).getPropertyValue("requestResponseBodyAdvice");
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(List.class);
		List<ResponseBodyAdvice<?>> converters = (List<ResponseBodyAdvice<?>>) value;
		assertThat(converters).element(0).isInstanceOf(JsonViewRequestBodyAdvice.class);
		assertThat(converters).element(1).isInstanceOf(JsonViewResponseBodyAdvice.class);
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
