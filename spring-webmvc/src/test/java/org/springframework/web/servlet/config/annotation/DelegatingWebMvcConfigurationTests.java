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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.PathMatcher;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * A test fixture for {@link DelegatingWebMvcConfiguration} tests.
 *
 * @author Rossen Stoyanchev
 */
public class DelegatingWebMvcConfigurationTests {

	private DelegatingWebMvcConfiguration delegatingConfig;

	@Mock
	private WebMvcConfigurer webMvcConfigurer;

	@Captor
	private ArgumentCaptor<List<HttpMessageConverter<?>>> converters;

	@Captor
	private ArgumentCaptor<ContentNegotiationConfigurer> contentNegotiationConfigurer;

	@Captor
	private ArgumentCaptor<FormattingConversionService> conversionService;

	@Captor
	private ArgumentCaptor<List<HandlerMethodArgumentResolver>> resolvers;

	@Captor
	private ArgumentCaptor<List<HandlerMethodReturnValueHandler>> handlers;

	@Captor
	private ArgumentCaptor<AsyncSupportConfigurer> asyncConfigurer;

	@Captor
	private ArgumentCaptor<List<HandlerExceptionResolver>> exceptionResolvers;


	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		delegatingConfig = new DelegatingWebMvcConfiguration();
	}

	@Test
	public void requestMappingHandlerAdapter() throws Exception {

		delegatingConfig.setConfigurers(Arrays.asList(webMvcConfigurer));
		RequestMappingHandlerAdapter adapter = delegatingConfig.requestMappingHandlerAdapter();

		ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();
		ConversionService initializerConversionService = initializer.getConversionService();
		assertTrue(initializer.getValidator() instanceof LocalValidatorFactoryBean);

		verify(webMvcConfigurer).configureMessageConverters(converters.capture());
		verify(webMvcConfigurer).configureContentNegotiation(contentNegotiationConfigurer.capture());
		verify(webMvcConfigurer).addFormatters(conversionService.capture());
		verify(webMvcConfigurer).addArgumentResolvers(resolvers.capture());
		verify(webMvcConfigurer).addReturnValueHandlers(handlers.capture());
		verify(webMvcConfigurer).configureAsyncSupport(asyncConfigurer.capture());

		assertSame(conversionService.getValue(), initializerConversionService);
		assertEquals(0, resolvers.getValue().size());
		assertEquals(0, handlers.getValue().size());
		assertEquals(converters.getValue(), adapter.getMessageConverters());
		assertNotNull(asyncConfigurer);
	}

	@Test
	public void configureMessageConverters() {
		List<WebMvcConfigurer> configurers = new ArrayList<WebMvcConfigurer>();
		configurers.add(new WebMvcConfigurerAdapter() {
			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				converters.add(new StringHttpMessageConverter());
			}
		});
		delegatingConfig = new DelegatingWebMvcConfiguration();
		delegatingConfig.setConfigurers(configurers);

		RequestMappingHandlerAdapter adapter = delegatingConfig.requestMappingHandlerAdapter();
		assertEquals("Only one custom converter should be registered", 1, adapter.getMessageConverters().size());
	}

	@Test
	public void getCustomValidator() {
		given(webMvcConfigurer.getValidator()).willReturn(new LocalValidatorFactoryBean());

		delegatingConfig.setConfigurers(Arrays.asList(webMvcConfigurer));
		delegatingConfig.mvcValidator();

		verify(webMvcConfigurer).getValidator();
	}

	@Test
	public void getCustomMessageCodesResolver() {
		given(webMvcConfigurer.getMessageCodesResolver()).willReturn(new DefaultMessageCodesResolver());

		delegatingConfig.setConfigurers(Arrays.asList(webMvcConfigurer));
		delegatingConfig.getMessageCodesResolver();

		verify(webMvcConfigurer).getMessageCodesResolver();
	}

	@Test
	public void handlerExceptionResolver() throws Exception {

		delegatingConfig.setConfigurers(Arrays.asList(webMvcConfigurer));
		delegatingConfig.handlerExceptionResolver();

		verify(webMvcConfigurer).configureMessageConverters(converters.capture());
		verify(webMvcConfigurer).configureContentNegotiation(contentNegotiationConfigurer.capture());
		verify(webMvcConfigurer).configureHandlerExceptionResolvers(exceptionResolvers.capture());

		assertEquals(3, exceptionResolvers.getValue().size());
		assertTrue(exceptionResolvers.getValue().get(0) instanceof ExceptionHandlerExceptionResolver);
		assertTrue(exceptionResolvers.getValue().get(1) instanceof ResponseStatusExceptionResolver);
		assertTrue(exceptionResolvers.getValue().get(2) instanceof DefaultHandlerExceptionResolver);
		assertTrue(converters.getValue().size() > 0);
	}

	@Test
	public void configureExceptionResolvers() throws Exception {
		List<WebMvcConfigurer> configurers = new ArrayList<WebMvcConfigurer>();
		configurers.add(new WebMvcConfigurerAdapter() {
			@Override
			public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
				exceptionResolvers.add(new DefaultHandlerExceptionResolver());
			}
		});
		delegatingConfig.setConfigurers(configurers);

		HandlerExceptionResolverComposite composite =
			(HandlerExceptionResolverComposite) delegatingConfig.handlerExceptionResolver();
		assertEquals("Only one custom converter is expected", 1, composite.getExceptionResolvers().size());
	}

	@Test
	public void configurePathMatch() throws Exception {
		final PathMatcher pathMatcher = mock(PathMatcher.class);
		final UrlPathHelper pathHelper = mock(UrlPathHelper.class);

		List<WebMvcConfigurer> configurers = new ArrayList<WebMvcConfigurer>();
		configurers.add(new WebMvcConfigurerAdapter() {
			@Override
			public void configurePathMatch(PathMatchConfigurer configurer) {
				configurer.setUseRegisteredSuffixPatternMatch(true)
						.setUseTrailingSlashMatch(false)
						.setUrlPathHelper(pathHelper)
						.setPathMatcher(pathMatcher);
			}
		});
		delegatingConfig.setConfigurers(configurers);

		RequestMappingHandlerMapping handlerMapping = delegatingConfig.requestMappingHandlerMapping();
		assertNotNull(handlerMapping);
		assertTrue(handlerMapping.useRegisteredSuffixPatternMatch());
		assertTrue(handlerMapping.useSuffixPatternMatch());
		assertFalse(handlerMapping.useTrailingSlashMatch());
		assertSame(pathHelper, handlerMapping.getUrlPathHelper());
		assertSame(pathMatcher, handlerMapping.getPathMatcher());
	}

}
