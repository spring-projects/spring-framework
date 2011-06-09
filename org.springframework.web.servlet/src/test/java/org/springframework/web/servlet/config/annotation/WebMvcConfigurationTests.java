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

package org.springframework.web.servlet.config.annotation;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * A test fixture for WebMvcConfiguration tests.
 *
 * @author Rossen Stoyanchev
 */
public class WebMvcConfigurationTests {

	private WebMvcConfiguration mvcConfiguration;

	private WebMvcConfigurer configurer;

	@Before
	public void setUp() {
		configurer = EasyMock.createMock(WebMvcConfigurer.class);
		mvcConfiguration = new WebMvcConfiguration();
	}

	@Test
	public void annotationHandlerAdapter() throws Exception {
		Capture<List<HttpMessageConverter<?>>> converters = new Capture<List<HttpMessageConverter<?>>>();
		Capture<FormattingConversionService> conversionService = new Capture<FormattingConversionService>();
		Capture<List<HandlerMethodArgumentResolver>> resolvers = new Capture<List<HandlerMethodArgumentResolver>>();
		Capture<List<HandlerMethodReturnValueHandler>> handlers = new Capture<List<HandlerMethodReturnValueHandler>>();

		configurer.configureMessageConverters(capture(converters));
		expect(configurer.getValidator()).andReturn(null);
		configurer.addFormatters(capture(conversionService));
		configurer.addArgumentResolvers(capture(resolvers));
		configurer.addReturnValueHandlers(capture(handlers));
		replay(configurer);

		mvcConfiguration.setConfigurers(Arrays.asList(configurer));
		RequestMappingHandlerAdapter adapter = mvcConfiguration.requestMappingHandlerAdapter();

		ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();
		assertSame(conversionService.getValue(), initializer.getConversionService());
		assertTrue(initializer.getValidator() instanceof LocalValidatorFactoryBean);

		assertEquals(0, resolvers.getValue().size());
		assertEquals(0, handlers.getValue().size());
		assertTrue(converters.getValue().size() > 0);
		assertEquals(converters.getValue(), adapter.getMessageConverters());

		verify(configurer);
	}

	@Test 
	public void configureMessageConverters() {
		RequestMappingHandlerAdapter adapter = mvcConfiguration.requestMappingHandlerAdapter();
		assertTrue("There should be at least two default converters ", adapter.getMessageConverters().size() > 1);

		List<WebMvcConfigurer> configurers = new ArrayList<WebMvcConfigurer>();
		configurers.add(new WebMvcConfigurerAdapter() {
			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				converters.add(new StringHttpMessageConverter());
			}
		});
		mvcConfiguration = new WebMvcConfiguration();
		mvcConfiguration.setConfigurers(configurers);
		
		adapter = mvcConfiguration.requestMappingHandlerAdapter();
		assertEquals("Only one custom converter should be registered", 1, adapter.getMessageConverters().size());
	}
	
	@Test
	public void getCustomValidator() {
		expect(configurer.getValidator()).andReturn(new LocalValidatorFactoryBean());
		replay(configurer);

		mvcConfiguration.setConfigurers(Arrays.asList(configurer));
		mvcConfiguration.mvcValidator();

		verify(configurer);
	}

	@Test
	public void configureValidator() {
		expect(configurer.getValidator()).andReturn(null);
		replay(configurer);

		mvcConfiguration.setConfigurers(Arrays.asList(configurer));
		mvcConfiguration.mvcValidator();

		verify(configurer);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void handlerExceptionResolver() throws Exception {
		configurer.configureMessageConverters(isA(List.class));
		configurer.configureHandlerExceptionResolvers(isA(List.class));
		replay(configurer);

		mvcConfiguration.setConfigurers(Arrays.asList(configurer));
		List<HandlerExceptionResolver> actual = mvcConfiguration.handlerExceptionResolver().getExceptionResolvers();

		assertEquals(3, actual.size());
		assertTrue(actual.get(0) instanceof ExceptionHandlerExceptionResolver);
		assertTrue(actual.get(1) instanceof ResponseStatusExceptionResolver);
		assertTrue(actual.get(2) instanceof DefaultHandlerExceptionResolver);

		verify(configurer);
	}

	@Test 
	public void configureExceptionResolvers() throws Exception {
		HandlerExceptionResolverComposite composite;
		
		composite = (HandlerExceptionResolverComposite) mvcConfiguration.handlerExceptionResolver();
		assertTrue("Expected more than one exception resolver by default", composite.getExceptionResolvers().size() > 1);
		
		List<WebMvcConfigurer> configurers = new ArrayList<WebMvcConfigurer>();
		configurers.add(new WebMvcConfigurerAdapter() {
			@Override
			public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
				exceptionResolvers.add(new DefaultHandlerExceptionResolver());
			}
		});
		mvcConfiguration.setConfigurers(configurers);
		
		composite = (HandlerExceptionResolverComposite) mvcConfiguration.handlerExceptionResolver();
		assertEquals("Only one custom converter is expected", 1, composite.getExceptionResolvers().size());
	}
	
	@Test
	public void configureInterceptors() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/");
		
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("controller", TestHandler.class);
		
		RequestMappingHandlerMapping hm = mvcConfiguration.requestMappingHandlerMapping();
		hm.setApplicationContext(context);
		HandlerExecutionChain chain = hm.getHandler(request);
		assertNotNull("No chain returned", chain);
		assertNotNull("Expected at least one default converter", chain.getInterceptors());
	}
	
	@Controller
	private static class TestHandler {
		
		@SuppressWarnings("unused")
		@RequestMapping("/")
		public void handle() {
		}
		
	}
	
}
