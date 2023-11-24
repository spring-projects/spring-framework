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

package org.springframework.web.reactive.config;

import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link DelegatingWebFluxConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class DelegatingWebFluxConfigurationIntegrationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void requestMappingHandlerMappingUsesWebFluxInfrastructureByDefault() {
		load(context -> { });
		RequestMappingHandlerMapping handlerMapping = this.context.getBean(RequestMappingHandlerMapping.class);
		assertThat(handlerMapping.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
	}

	@Test
	void requestMappingHandlerMappingWithPrimaryUsesQualifiedRequestedContentTypeResolver() {
		load(registerPrimaryBean("testContentTypeResolver", RequestedContentTypeResolver.class));
		RequestMappingHandlerMapping handlerMapping = this.context.getBean(RequestMappingHandlerMapping.class);
		assertThat(handlerMapping.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
		assertThat(this.context.getBeansOfType(RequestedContentTypeResolver.class)).containsOnlyKeys(
				"webFluxContentTypeResolver", "testContentTypeResolver");
	}

	@Test
	void requestMappingHandlerAdapterUsesWebFluxInfrastructureByDefault() {
		load(context -> { });
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter.getReactiveAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue("conversionService",
				this.context.getBean("webFluxConversionService"));
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue("validator",
				this.context.getBean("webFluxValidator"));
	}

	@Test
	void requestMappingHandlerAdapterWithPrimaryUsesQualifiedReactiveAdapterRegistry() {
		load(registerPrimaryBean("testReactiveAdapterRegistry", ReactiveAdapterRegistry.class, new ReactiveAdapterRegistry()));
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter.getReactiveAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(this.context.getBeansOfType(ReactiveAdapterRegistry.class)).containsOnlyKeys(
				"webFluxAdapterRegistry", "testReactiveAdapterRegistry");
	}

	@Test
	void requestMappingHandlerAdapterWithPrimaryUsesQualifiedConversionService() {
		load(registerPrimaryBean("testConversionService", FormattingConversionService.class));
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue("conversionService",
				this.context.getBean("webFluxConversionService"));
		assertThat(this.context.getBeansOfType(FormattingConversionService.class)).containsOnlyKeys(
				"webFluxConversionService", "testConversionService");
	}

	@Test
	void requestMappingHandlerAdapterWithPrimaryUsesQualifiedValidator() {
		load(registerPrimaryBean("testValidator", Validator.class));
		RequestMappingHandlerAdapter mappingHandlerAdapter = this.context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(mappingHandlerAdapter.getWebBindingInitializer()).hasFieldOrPropertyWithValue("validator",
				this.context.getBean("webFluxValidator"));
		assertThat(this.context.getBeansOfType(Validator.class)).containsOnlyKeys(
				"webFluxValidator", "testValidator");
	}

	@Test
	void responseEntityResultHandlerUsesWebFluxInfrastructureByDefault() {
		load(context -> { });
		ResponseEntityResultHandler responseEntityResultHandler = this.context.getBean(ResponseEntityResultHandler.class);
		assertThat(responseEntityResultHandler.getAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(responseEntityResultHandler.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
	}

	@Test
	void responseEntityResultHandlerWithPrimaryUsesQualifiedReactiveAdapterRegistry() {
		load(registerPrimaryBean("testReactiveAdapterRegistry", ReactiveAdapterRegistry.class, new ReactiveAdapterRegistry()));
		ResponseEntityResultHandler responseEntityResultHandler = this.context.getBean(ResponseEntityResultHandler.class);
		assertThat(responseEntityResultHandler.getAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(this.context.getBeansOfType(ReactiveAdapterRegistry.class)).containsOnlyKeys(
				"webFluxAdapterRegistry", "testReactiveAdapterRegistry");
	}

	@Test
	void responseEntityResultHandlerWithPrimaryUsesQualifiedRequestedContentTypeResolver() {
		load(registerPrimaryBean("testContentTypeResolver", RequestedContentTypeResolver.class));
		ResponseEntityResultHandler responseEntityResultHandler = this.context.getBean(ResponseEntityResultHandler.class);
		assertThat(responseEntityResultHandler.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
		assertThat(this.context.getBeansOfType(RequestedContentTypeResolver.class)).containsOnlyKeys(
				"webFluxContentTypeResolver", "testContentTypeResolver");
	}

	@Test
	void responseBodyResultHandlerUsesWebFluxInfrastructureByDefault() {
		load(context -> { });
		ResponseBodyResultHandler responseBodyResultHandler = this.context.getBean(ResponseBodyResultHandler.class);
		assertThat(responseBodyResultHandler.getAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(responseBodyResultHandler.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
	}

	@Test
	void responseBodyResultHandlerWithPrimaryUsesQualifiedReactiveAdapterRegistry() {
		load(registerPrimaryBean("testReactiveAdapterRegistry", ReactiveAdapterRegistry.class, new ReactiveAdapterRegistry()));
		ResponseBodyResultHandler responseBodyResultHandler = this.context.getBean(ResponseBodyResultHandler.class);
		assertThat(responseBodyResultHandler.getAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(this.context.getBeansOfType(ReactiveAdapterRegistry.class)).containsOnlyKeys(
				"webFluxAdapterRegistry", "testReactiveAdapterRegistry");
	}

	@Test
	void responseBodyResultHandlerWithPrimaryUsesQualifiedRequestedContentTypeResolver() {
		load(registerPrimaryBean("testContentTypeResolver", RequestedContentTypeResolver.class));
		ResponseBodyResultHandler responseBodyResultHandler = this.context.getBean(ResponseBodyResultHandler.class);
		assertThat(responseBodyResultHandler.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
		assertThat(this.context.getBeansOfType(RequestedContentTypeResolver.class)).containsOnlyKeys(
				"webFluxContentTypeResolver", "testContentTypeResolver");
	}

	@Test
	void viewResolutionResultHandlerUsesWebFluxInfrastructureByDefault() {
		load(context -> { });
		ViewResolutionResultHandler viewResolutionResultHandler = this.context.getBean(ViewResolutionResultHandler.class);
		assertThat(viewResolutionResultHandler.getAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(viewResolutionResultHandler.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
	}

	@Test
	void viewResolutionResultHandlerWithPrimaryUsesQualifiedReactiveAdapterRegistry() {
		load(registerPrimaryBean("testReactiveAdapterRegistry", ReactiveAdapterRegistry.class, new ReactiveAdapterRegistry()));
		ViewResolutionResultHandler viewResolutionResultHandler = this.context.getBean(ViewResolutionResultHandler.class);
		assertThat(viewResolutionResultHandler.getAdapterRegistry()).isSameAs(this.context.getBean("webFluxAdapterRegistry"));
		assertThat(this.context.getBeansOfType(ReactiveAdapterRegistry.class)).containsOnlyKeys(
				"webFluxAdapterRegistry", "testReactiveAdapterRegistry");
	}

	@Test
	void viewResolutionResultHandlerWithPrimaryUsesQualifiedRequestedContentTypeResolver() {
		load(registerPrimaryBean("testContentTypeResolver", RequestedContentTypeResolver.class));
		ViewResolutionResultHandler viewResolutionResultHandler = this.context.getBean(ViewResolutionResultHandler.class);
		assertThat(viewResolutionResultHandler.getContentTypeResolver()).isSameAs(this.context.getBean("webFluxContentTypeResolver"));
		assertThat(this.context.getBeansOfType(RequestedContentTypeResolver.class)).containsOnlyKeys(
				"webFluxContentTypeResolver", "testContentTypeResolver");
	}

	private <T> Consumer<AnnotationConfigApplicationContext> registerPrimaryBean(String beanName, Class<T> type) {
		return context -> context.registerBean(beanName, type, () -> mock(type), definition -> definition.setPrimary(true));
	}

	private <T> Consumer<AnnotationConfigApplicationContext> registerPrimaryBean(String beanName, Class<T> type, T instance) {
		return context -> context.registerBean(beanName, type, () -> instance, definition -> definition.setPrimary(true));
	}

	private void load(Consumer<AnnotationConfigApplicationContext> context) {
		AnnotationConfigApplicationContext testContext = new AnnotationConfigApplicationContext();
		context.accept(testContext);
		testContext.registerBean(DelegatingWebFluxConfiguration.class);
		testContext.refresh();
		this.context = testContext;
	}

}
