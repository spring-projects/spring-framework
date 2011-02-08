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
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver;

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
		AnnotationMethodHandlerAdapter adapter = appContext.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);
		Object initializer = new DirectFieldAccessor(adapter).getPropertyValue("webBindingInitializer");
		assertNotNull(initializer);
		MessageCodesResolver resolver = ((ConfigurableWebBindingInitializer) initializer).getMessageCodesResolver();
		assertNotNull(resolver);
		assertEquals(TestMessageCodesResolver.class, resolver.getClass());
	}

	@Test
	public void testMessageConverters() {
		loadBeanDefinitions("mvc-config-message-converters.xml");
		verifyMessageConverters(appContext.getBean(AnnotationMethodHandlerAdapter.class), true);
		verifyMessageConverters(appContext.getBean(AnnotationMethodHandlerExceptionResolver.class), true);
	}

	@Test
	public void testMessageConvertersWithoutDefaultRegistrations() {
		loadBeanDefinitions("mvc-config-message-converters-defaults-off.xml");
		verifyMessageConverters(appContext.getBean(AnnotationMethodHandlerAdapter.class), false);
		verifyMessageConverters(appContext.getBean(AnnotationMethodHandlerExceptionResolver.class), false);
	}

	@Test
	public void testArgumentResolvers() {
		loadBeanDefinitions("mvc-config-argument-resolvers.xml");
		AnnotationMethodHandlerAdapter adapter = appContext.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);
		Object resolvers = new DirectFieldAccessor(adapter).getPropertyValue("customArgumentResolvers");
		assertNotNull(resolvers);
		assertTrue(resolvers instanceof WebArgumentResolver[]);
		assertEquals(2, ((WebArgumentResolver[]) resolvers).length);
		assertTrue(((WebArgumentResolver[]) resolvers)[0] instanceof TestWebArgumentResolver);
		assertTrue(((WebArgumentResolver[]) resolvers)[1] instanceof TestWebArgumentResolver);
	}

	private void loadBeanDefinitions(String fileName) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource(fileName,
				AnnotationDrivenBeanDefinitionParserTests.class));
		appContext.refresh();
	}

	private void verifyMessageConverters(Object bean, boolean hasDefaultRegistrations) {
		assertNotNull(bean);
		Object converters = new DirectFieldAccessor(bean).getPropertyValue("messageConverters");
		assertNotNull(converters);
		assertTrue(converters instanceof HttpMessageConverter<?>[]);
		if (hasDefaultRegistrations) {
			assertTrue("Default converters are registered in addition to custom ones",
					((HttpMessageConverter<?>[]) converters).length > 2);
		} else {
			assertTrue("Default converters should not be registered",
					((HttpMessageConverter<?>[]) converters).length == 2);
		}
		assertTrue(((HttpMessageConverter<?>[]) converters)[0] instanceof StringHttpMessageConverter);
		assertTrue(((HttpMessageConverter<?>[]) converters)[1] instanceof ResourceHttpMessageConverter);
	}

}

class TestWebArgumentResolver implements WebArgumentResolver {

	public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception {
		return null;
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
