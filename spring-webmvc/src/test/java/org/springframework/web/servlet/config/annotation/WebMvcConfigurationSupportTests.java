/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

/**
 * A test fixture with an {@link WebMvcConfigurationSupport} instance.
 *
 * @author Rossen Stoyanchev
 */
public class WebMvcConfigurationSupportTests {

	private WebMvcConfigurationSupport mvcConfiguration;

	private StaticWebApplicationContext wac;


	@Before
	public void setUp() {
		this.wac = new StaticWebApplicationContext();
		this.mvcConfiguration = new WebMvcConfigurationSupport();
		this.mvcConfiguration.setApplicationContext(wac);
	}

	@Test
	public void requestMappingHandlerMapping() throws Exception {
		this.wac.registerSingleton("controller", TestController.class);

		RequestMappingHandlerMapping handlerMapping = mvcConfiguration.requestMappingHandlerMapping();
		assertEquals(0, handlerMapping.getOrder());

		handlerMapping.setApplicationContext(this.wac);
		handlerMapping.afterPropertiesSet();
		HandlerExecutionChain chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/"));
		assertNotNull(chain.getInterceptors());
		assertEquals(ConversionServiceExposingInterceptor.class, chain.getInterceptors()[0].getClass());
	}

	@Test
	public void emptyViewControllerHandlerMapping() {
		AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping) mvcConfiguration.viewControllerHandlerMapping();
		assertNotNull(handlerMapping);
		assertEquals(Integer.MAX_VALUE, handlerMapping.getOrder());
		assertTrue(handlerMapping.getClass().getName().endsWith("EmptyHandlerMapping"));
	}

	@Test
	public void beanNameHandlerMapping() throws Exception {
		StaticWebApplicationContext cxt = new StaticWebApplicationContext();
		cxt.registerSingleton("/controller", TestController.class);

		HttpServletRequest request = new MockHttpServletRequest("GET", "/controller");

		BeanNameUrlHandlerMapping handlerMapping = mvcConfiguration.beanNameHandlerMapping();
		assertEquals(2, handlerMapping.getOrder());

		handlerMapping.setApplicationContext(cxt);
		HandlerExecutionChain chain = handlerMapping.getHandler(request);
		assertNotNull(chain.getInterceptors());
		assertEquals(2, chain.getInterceptors().length);
		assertEquals(ConversionServiceExposingInterceptor.class, chain.getInterceptors()[1].getClass());
	}

	@Test
	public void emptyResourceHandlerMapping() {
		mvcConfiguration.setApplicationContext(new StaticWebApplicationContext());
		AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping) mvcConfiguration.resourceHandlerMapping();
		assertNotNull(handlerMapping);
		assertEquals(Integer.MAX_VALUE, handlerMapping.getOrder());
		assertTrue(handlerMapping.getClass().getName().endsWith("EmptyHandlerMapping"));
	}

	@Test
	public void emptyDefaultServletHandlerMapping() {
		mvcConfiguration.setServletContext(new MockServletContext());
		AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping) mvcConfiguration.defaultServletHandlerMapping();
		assertNotNull(handlerMapping);
		assertEquals(Integer.MAX_VALUE, handlerMapping.getOrder());
		assertTrue(handlerMapping.getClass().getName().endsWith("EmptyHandlerMapping"));
	}

	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		RequestMappingHandlerAdapter adapter = mvcConfiguration.requestMappingHandlerAdapter();

		List<HttpMessageConverter<?>> expectedConverters = new ArrayList<HttpMessageConverter<?>>();
		mvcConfiguration.addDefaultHttpMessageConverters(expectedConverters);
		assertEquals(expectedConverters.size(), adapter.getMessageConverters().size());

		ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();
		assertNotNull(initializer);

		ConversionService conversionService = initializer.getConversionService();
		assertNotNull(conversionService);
		assertTrue(conversionService instanceof FormattingConversionService);

		Validator validator = initializer.getValidator();
		assertNotNull(validator);
		assertTrue(validator instanceof LocalValidatorFactoryBean);
	}

	@Test
	public void handlerExceptionResolver() throws Exception {
		HandlerExceptionResolverComposite compositeResolver =
			(HandlerExceptionResolverComposite) mvcConfiguration.handlerExceptionResolver();

		assertEquals(0, compositeResolver.getOrder());

		List<HandlerExceptionResolver> expectedResolvers = compositeResolver.getExceptionResolvers();

		assertEquals(ExceptionHandlerExceptionResolver.class, expectedResolvers.get(0).getClass());
		assertEquals(ResponseStatusExceptionResolver.class, expectedResolvers.get(1).getClass());
		assertEquals(DefaultHandlerExceptionResolver.class, expectedResolvers.get(2).getClass());

		ExceptionHandlerExceptionResolver eher = (ExceptionHandlerExceptionResolver) expectedResolvers.get(0);
		assertNotNull(eher.getApplicationContext());
	}

	@Test
	public void defaultPathMatchConfiguration() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(WebConfig.class);
		context.refresh();

		UrlPathHelper urlPathHelper = context.getBean(UrlPathHelper.class);
		PathMatcher pathMatcher = context.getBean(PathMatcher.class);

		assertNotNull(urlPathHelper);
		assertNotNull(pathMatcher);
		assertEquals(AntPathMatcher.class, pathMatcher.getClass());
	}


	@EnableWebMvc
	@Configuration
	@SuppressWarnings("unused")
	static class WebConfig {

		@Bean
		public TestController testController() {
			return new TestController();
		}
	}

	@Controller
	private static class TestController {

		@RequestMapping("/")
		public void handle() {
		}
	}

}
