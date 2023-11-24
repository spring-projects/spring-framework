/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.PathMatcher;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A test fixture for {@link DelegatingWebMvcConfiguration} tests.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(MockitoExtension.class)
public class DelegatingWebMvcConfigurationTests {

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

	private final DelegatingWebMvcConfiguration webMvcConfig = new DelegatingWebMvcConfiguration();


	@Test
	public void requestMappingHandlerAdapter() {
		webMvcConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		RequestMappingHandlerAdapter adapter = this.webMvcConfig.requestMappingHandlerAdapter(
				this.webMvcConfig.mvcContentNegotiationManager(),
				this.webMvcConfig.mvcConversionService(),
				this.webMvcConfig.mvcValidator());

		ConfigurableWebBindingInitializer initializer =
				(ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();

		verify(webMvcConfigurer).configureMessageConverters(converters.capture());
		verify(webMvcConfigurer).configureContentNegotiation(contentNegotiationConfigurer.capture());
		verify(webMvcConfigurer).addFormatters(conversionService.capture());
		verify(webMvcConfigurer).addArgumentResolvers(resolvers.capture());
		verify(webMvcConfigurer).addReturnValueHandlers(handlers.capture());
		verify(webMvcConfigurer).configureAsyncSupport(asyncConfigurer.capture());

		assertThat(initializer).isNotNull();
		assertThat(initializer.getConversionService()).isSameAs(conversionService.getValue());
		boolean condition = initializer.getValidator() instanceof LocalValidatorFactoryBean;
		assertThat(condition).isTrue();
		assertThat(resolvers.getValue()).isEmpty();
		assertThat(handlers.getValue()).isEmpty();
		assertThat(adapter.getMessageConverters()).isEqualTo(converters.getValue());
		assertThat(asyncConfigurer).isNotNull();
	}

	@Test
	public void configureMessageConverters() {
		HttpMessageConverter<?> customConverter = mock();
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		WebMvcConfigurer configurer = new WebMvcConfigurer() {
			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				converters.add(stringConverter);
			}

			@Override
			public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
				converters.add(0, customConverter);
			}
		};
		webMvcConfig.setConfigurers(Collections.singletonList(configurer));

		RequestMappingHandlerAdapter adapter = webMvcConfig.requestMappingHandlerAdapter(
				this.webMvcConfig.mvcContentNegotiationManager(),
				this.webMvcConfig.mvcConversionService(),
				this.webMvcConfig.mvcValidator());

		assertThat(adapter.getMessageConverters()).as("One custom converter expected").hasSize(2);
		assertThat(adapter.getMessageConverters().get(0)).isSameAs(customConverter);
		assertThat(adapter.getMessageConverters().get(1)).isSameAs(stringConverter);
	}

	@Test
	public void getCustomValidator() {
		given(webMvcConfigurer.getValidator()).willReturn(new LocalValidatorFactoryBean());

		webMvcConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		webMvcConfig.mvcValidator();

		verify(webMvcConfigurer).getValidator();
	}

	@Test
	public void getCustomMessageCodesResolver() {
		given(webMvcConfigurer.getMessageCodesResolver()).willReturn(new DefaultMessageCodesResolver());

		webMvcConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		webMvcConfig.getMessageCodesResolver();

		verify(webMvcConfigurer).getMessageCodesResolver();
	}

	@Test
	public void handlerExceptionResolver() {
		webMvcConfig.setConfigurers(Collections.singletonList(webMvcConfigurer));
		webMvcConfig.handlerExceptionResolver(webMvcConfig.mvcContentNegotiationManager());

		verify(webMvcConfigurer).configureMessageConverters(converters.capture());
		verify(webMvcConfigurer).configureContentNegotiation(contentNegotiationConfigurer.capture());
		verify(webMvcConfigurer).configureHandlerExceptionResolvers(exceptionResolvers.capture());

		assertThat(exceptionResolvers.getValue()).hasSize(3);
		boolean condition2 = exceptionResolvers.getValue().get(0) instanceof ExceptionHandlerExceptionResolver;
		assertThat(condition2).isTrue();
		boolean condition1 = exceptionResolvers.getValue().get(1) instanceof ResponseStatusExceptionResolver;
		assertThat(condition1).isTrue();
		boolean condition = exceptionResolvers.getValue().get(2) instanceof DefaultHandlerExceptionResolver;
		assertThat(condition).isTrue();
		assertThat(converters.getValue()).isNotEmpty();
	}

	@Test
	public void configureExceptionResolvers() {
		WebMvcConfigurer configurer = new WebMvcConfigurer() {
			@Override
			public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
				resolvers.add(new DefaultHandlerExceptionResolver());
			}
		};
		webMvcConfig.setConfigurers(Collections.singletonList(configurer));

		HandlerExceptionResolverComposite composite =
				(HandlerExceptionResolverComposite) webMvcConfig
						.handlerExceptionResolver(webMvcConfig.mvcContentNegotiationManager());

		assertThat(composite.getExceptionResolvers())
				.as("Only one custom converter is expected").hasSize(1);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void configurePathMatcher() {
		PathMatcher pathMatcher = mock();
		UrlPathHelper pathHelper = mock();

		WebMvcConfigurer configurer = new WebMvcConfigurer() {
			@Override
			@SuppressWarnings("deprecation")
			public void configurePathMatch(PathMatchConfigurer configurer) {
				configurer.setUseRegisteredSuffixPatternMatch(true)
						.setUseTrailingSlashMatch(false)
						.setUrlPathHelper(pathHelper)
						.setPathMatcher(pathMatcher);
			}
			@Override
			public void addViewControllers(ViewControllerRegistry registry) {
				registry.addViewController("/").setViewName("home");
			}
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				registry.addResourceHandler("/resources/**").addResourceLocations("/");
			}
		};

		MockServletContext servletContext = new MockServletContext();
		webMvcConfig.setConfigurers(Collections.singletonList(configurer));
		webMvcConfig.setServletContext(servletContext);
		webMvcConfig.setApplicationContext(new GenericWebApplicationContext(servletContext));


		BiConsumer<UrlPathHelper, PathMatcher> configAssertion = (helper, matcher) -> {
			assertThat(helper).isSameAs(pathHelper);
			assertThat(matcher).isSameAs(pathMatcher);
		};

		RequestMappingHandlerMapping annotationsMapping = webMvcConfig.requestMappingHandlerMapping(
				webMvcConfig.mvcContentNegotiationManager(),
				webMvcConfig.mvcConversionService(),
				webMvcConfig.mvcResourceUrlProvider());

		assertThat(annotationsMapping).isNotNull();
		assertThat(annotationsMapping.useRegisteredSuffixPatternMatch()).isTrue();
		assertThat(annotationsMapping.useSuffixPatternMatch()).isTrue();
		assertThat(annotationsMapping.useTrailingSlashMatch()).isFalse();
		configAssertion.accept(annotationsMapping.getUrlPathHelper(), annotationsMapping.getPathMatcher());

		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) webMvcConfig.viewControllerHandlerMapping(
				webMvcConfig.mvcConversionService(),
				webMvcConfig.mvcResourceUrlProvider());

		assertThat(mapping).isNotNull();
		configAssertion.accept(mapping.getUrlPathHelper(), mapping.getPathMatcher());

		mapping = (SimpleUrlHandlerMapping) webMvcConfig.resourceHandlerMapping(
				webMvcConfig.mvcContentNegotiationManager(),
				webMvcConfig.mvcConversionService(),
				webMvcConfig.mvcResourceUrlProvider());

		assertThat(mapping).isNotNull();
		configAssertion.accept(mapping.getUrlPathHelper(), mapping.getPathMatcher());

		configAssertion.accept(
				webMvcConfig.mvcResourceUrlProvider().getUrlPathHelper(),
				webMvcConfig.mvcResourceUrlProvider().getPathMatcher());

		configAssertion.accept(webMvcConfig.mvcUrlPathHelper(), webMvcConfig.mvcPathMatcher());
	}

	@Test
	public void configurePathPatternParser() {
		PathPatternParser patternParser = new PathPatternParser();
		PathMatcher pathMatcher = mock();
		UrlPathHelper pathHelper = mock();

		WebMvcConfigurer configurer = new WebMvcConfigurer() {
			@Override
			public void configurePathMatch(PathMatchConfigurer configurer) {
				configurer.setPatternParser(patternParser)
						.setUrlPathHelper(pathHelper)
						.setPathMatcher(pathMatcher);
			}
			@Override
			public void addViewControllers(ViewControllerRegistry registry) {
				registry.addViewController("/").setViewName("home");
			}
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				registry.addResourceHandler("/resources/**").addResourceLocations("/");
			}
		};

		MockServletContext servletContext = new MockServletContext();
		webMvcConfig.setConfigurers(Collections.singletonList(configurer));
		webMvcConfig.setServletContext(servletContext);
		webMvcConfig.setApplicationContext(new GenericWebApplicationContext(servletContext));


		BiConsumer<UrlPathHelper, PathMatcher> configAssertion = (helper, matcher) -> {
			assertThat(helper).isNotSameAs(pathHelper);
			assertThat(matcher).isNotSameAs(pathMatcher);
		};

		RequestMappingHandlerMapping annotationsMapping = webMvcConfig.requestMappingHandlerMapping(
				webMvcConfig.mvcContentNegotiationManager(),
				webMvcConfig.mvcConversionService(),
				webMvcConfig.mvcResourceUrlProvider());

		assertThat(annotationsMapping).isNotNull();
		assertThat(annotationsMapping.getPatternParser())
				.isSameAs(patternParser)
				.isSameAs(webMvcConfig.mvcPatternParser());
		configAssertion.accept(annotationsMapping.getUrlPathHelper(), annotationsMapping.getPathMatcher());

		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) webMvcConfig.viewControllerHandlerMapping(
				webMvcConfig.mvcConversionService(),
				webMvcConfig.mvcResourceUrlProvider());

		assertThat(mapping).isNotNull();
		assertThat(mapping.getPatternParser()).isSameAs(patternParser);
		configAssertion.accept(mapping.getUrlPathHelper(), mapping.getPathMatcher());

		mapping = (SimpleUrlHandlerMapping) webMvcConfig.resourceHandlerMapping(
				webMvcConfig.mvcContentNegotiationManager(),
				webMvcConfig.mvcConversionService(),
				webMvcConfig.mvcResourceUrlProvider());

		assertThat(mapping).isNotNull();
		assertThat(mapping.getPatternParser()).isSameAs(patternParser);
		configAssertion.accept(mapping.getUrlPathHelper(), mapping.getPathMatcher());

		BeanNameUrlHandlerMapping beanNameMapping = webMvcConfig.beanNameHandlerMapping(
				webMvcConfig.mvcConversionService(),
				webMvcConfig.mvcResourceUrlProvider());

		assertThat(beanNameMapping).isNotNull();
		assertThat(beanNameMapping.getPatternParser()).isSameAs(patternParser);
		configAssertion.accept(beanNameMapping.getUrlPathHelper(), beanNameMapping.getPathMatcher());

		assertThat(webMvcConfig.mvcResourceUrlProvider().getUrlPathHelper()).isSameAs(pathHelper);
		assertThat(webMvcConfig.mvcResourceUrlProvider().getPathMatcher()).isSameAs(pathMatcher);
	}

}
