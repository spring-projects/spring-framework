/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link DelegatingWebMvcConfiguration}.
 *
 * @author Stephane Nicoll
 */
class DelegatingWebMvcConfigurationIntegrationTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void requestMappingHandlerMappingUsesMvcInfrastructureByDefault() {
		load(context -> { });
		RequestMappingHandlerMapping handlerMapping = this.context.getBean(RequestMappingHandlerMapping.class);
		assertThat(handlerMapping.getContentNegotiationManager()).isSameAs(this.context.getBean("mvcContentNegotiationManager"));
	}

	@Test
	void requestMappingHandlerMappingWithPrimaryUsesQualifiedContentNegotiationManager() {
		load(registerPrimaryBean("testContentNegotiationManager", ContentNegotiationManager.class));
		RequestMappingHandlerMapping handlerMapping = this.context.getBean(RequestMappingHandlerMapping.class);
		assertThat(handlerMapping.getContentNegotiationManager()).isSameAs(this.context.getBean("mvcContentNegotiationManager"));
		assertThat(this.context.getBeansOfType(ContentNegotiationManager.class)).containsOnlyKeys(
				"mvcContentNegotiationManager", "testContentNegotiationManager");
	}

	@Test
	void viewControllerHandlerMappingUsesMvcInfrastructureByDefault() {
		load(context -> context.registerBean(ViewControllerConfiguration.class));
		AbstractHandlerMapping handlerMapping = this.context.getBean("viewControllerHandlerMapping", AbstractHandlerMapping.class);
		assertThat(handlerMapping.getPathMatcher()).isSameAs(this.context.getBean("mvcPathMatcher"));
		assertThat(handlerMapping.getUrlPathHelper()).isSameAs(this.context.getBean("mvcUrlPathHelper"));
	}

	@Test
	void viewControllerHandlerMappingWithPrimaryUsesQualifiedPathMatcher() {
		load(registerPrimaryBean("testPathMatcher", PathMatcher.class)
				.andThen(context -> context.registerBean(ViewControllerConfiguration.class)));
		AbstractHandlerMapping handlerMapping = this.context.getBean("viewControllerHandlerMapping", AbstractHandlerMapping.class);
		assertThat(handlerMapping.getPathMatcher()).isSameAs(this.context.getBean("mvcPathMatcher"));
		assertThat(this.context.getBeansOfType(PathMatcher.class)).containsOnlyKeys(
				"mvcPathMatcher", "testPathMatcher");
	}

	@Test
	void viewControllerHandlerMappingWithPrimaryUsesQualifiedUrlPathHelper() {
		load(registerPrimaryBean("testUrlPathHelper", UrlPathHelper.class)
				.andThen(context -> context.registerBean(ViewControllerConfiguration.class)));
		AbstractHandlerMapping handlerMapping = this.context.getBean("viewControllerHandlerMapping", AbstractHandlerMapping.class);
		assertThat(handlerMapping.getUrlPathHelper()).isSameAs(this.context.getBean("mvcUrlPathHelper"));
		assertThat(this.context.getBeansOfType(UrlPathHelper.class)).containsOnlyKeys(
				"mvcUrlPathHelper", "testUrlPathHelper");
	}

	@Test
	void resourceHandlerMappingUsesMvcInfrastructureByDefault() {
		load(context -> context.registerBean(ResourceHandlerConfiguration.class));
		AbstractHandlerMapping handlerMapping = this.context.getBean("resourceHandlerMapping", AbstractHandlerMapping.class);
		assertThat(handlerMapping.getPathMatcher()).isSameAs(this.context.getBean("mvcPathMatcher"));
		assertThat(handlerMapping.getUrlPathHelper()).isSameAs(this.context.getBean("mvcUrlPathHelper"));
	}

	@Test
	void resourceHandlerMappingWithPrimaryUsesQualifiedPathMatcher() {
		load(registerPrimaryBean("testPathMatcher", PathMatcher.class)
				.andThen(context -> context.registerBean(ResourceHandlerConfiguration.class)));
		AbstractHandlerMapping handlerMapping = this.context.getBean("resourceHandlerMapping", AbstractHandlerMapping.class);
		assertThat(handlerMapping.getPathMatcher()).isSameAs(this.context.getBean("mvcPathMatcher"));
		assertThat(this.context.getBeansOfType(PathMatcher.class)).containsOnlyKeys(
				"mvcPathMatcher", "testPathMatcher");
	}

	@Test
	void resourceHandlerMappingWithPrimaryUsesQualifiedUrlPathHelper() {
		load(registerPrimaryBean("testUrlPathHelper", UrlPathHelper.class)
				.andThen(context -> context.registerBean(ResourceHandlerConfiguration.class)));
		AbstractHandlerMapping handlerMapping = this.context.getBean("resourceHandlerMapping", AbstractHandlerMapping.class);
		assertThat(handlerMapping.getUrlPathHelper()).isSameAs(this.context.getBean("mvcUrlPathHelper"));
		assertThat(this.context.getBeansOfType(UrlPathHelper.class)).containsOnlyKeys(
				"mvcUrlPathHelper", "testUrlPathHelper");
	}

	@Test
	void requestMappingHandlerAdapterUsesMvcInfrastructureByDefault() {
		load(context -> { });
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter).hasFieldOrPropertyWithValue(
				"contentNegotiationManager", this.context.getBean("mvcContentNegotiationManager"));
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue(
				"conversionService", this.context.getBean("mvcConversionService"));
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue(
				"validator", this.context.getBean("mvcValidator"));
	}

	@Test
	void requestMappingHandlerAdapterWithPrimaryUsesQualifiedContentNegotiationManager() {
		load(registerPrimaryBean("testContentNegotiationManager", ContentNegotiationManager.class));
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter).hasFieldOrPropertyWithValue(
				"contentNegotiationManager", this.context.getBean("mvcContentNegotiationManager"));
		assertThat(this.context.getBeansOfType(ContentNegotiationManager.class)).containsOnlyKeys(
				"mvcContentNegotiationManager", "testContentNegotiationManager");
	}

	@Test
	void requestMappingHandlerAdapterWithPrimaryUsesQualifiedConversionService() {
		load(registerPrimaryBean("testConversionService", ConversionService.class));
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue(
				"conversionService", this.context.getBean("mvcConversionService"));
		assertThat(this.context.getBeansOfType(ConversionService.class)).containsOnlyKeys("mvcConversionService", "testConversionService");
	}

	@Test
	void requestMappingHandlerAdapterWithPrimaryUsesQualifiedValidator() {
		load(registerPrimaryBean("testValidator", Validator.class));
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue(
				"validator", this.context.getBean("mvcValidator"));
		assertThat(this.context.getBeansOfType(Validator.class)).containsOnlyKeys("mvcValidator", "testValidator");
	}

	private <T> Consumer<GenericWebApplicationContext> registerPrimaryBean(String beanName, Class<T> type) {
		return context -> context.registerBean(beanName, type, () -> mock(type), definition -> definition.setPrimary(true));
	}

	private void load(Consumer<GenericWebApplicationContext> context) {
		GenericWebApplicationContext webContext = new GenericWebApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(webContext);
		webContext.setServletContext(new MockServletContext());

		context.accept(webContext);
		webContext.registerBean(DelegatingWebMvcConfiguration.class);
		webContext.refresh();
		this.context = webContext;
	}


	@Configuration
	static class ViewControllerConfiguration implements WebMvcConfigurer {

		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/test");
		}

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			// tests need to check the "mvcPathMatcher" and "mvcUrlPathHelper" instances
			configurer.setPatternParser(null);
		}

	}


	@Configuration
	static class ResourceHandlerConfiguration implements WebMvcConfigurer {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**");
		}

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			// tests need to check the "mvcPathMatcher" and "mvcUrlPathHelper" instances
			configurer.setPatternParser(null);
		}

	}

}
