/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.test.web.servlet.client;

import java.util.function.Supplier;

import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Simple wrapper around a {@link StandaloneMockMvcBuilder} that implements
 * {@link MockMvcWebTestClient.ControllerSpec}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class StandaloneMockMvcSpec extends AbstractMockMvcServerSpec<MockMvcWebTestClient.ControllerSpec>
		implements MockMvcWebTestClient.ControllerSpec {

	private final StandaloneMockMvcBuilder mockMvcBuilder;


	StandaloneMockMvcSpec(Object... controllers) {
		this.mockMvcBuilder = MockMvcBuilders.standaloneSetup(controllers);
	}

	@Override
	public StandaloneMockMvcSpec controllerAdvice(Object... controllerAdvice) {
		this.mockMvcBuilder.setControllerAdvice(controllerAdvice);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
		this.mockMvcBuilder.setMessageConverters(messageConverters);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec validator(Validator validator) {
		this.mockMvcBuilder.setValidator(validator);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec conversionService(FormattingConversionService conversionService) {
		this.mockMvcBuilder.setConversionService(conversionService);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec interceptors(HandlerInterceptor... interceptors) {
		mappedInterceptors(null, interceptors);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec mappedInterceptors(
			@Nullable String[] pathPatterns, HandlerInterceptor... interceptors) {

		this.mockMvcBuilder.addMappedInterceptors(pathPatterns, interceptors);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec contentNegotiationManager(ContentNegotiationManager manager) {
		this.mockMvcBuilder.setContentNegotiationManager(manager);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec asyncRequestTimeout(long timeout) {
		this.mockMvcBuilder.setAsyncRequestTimeout(timeout);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec customArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers) {
		this.mockMvcBuilder.setCustomArgumentResolvers(argumentResolvers);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec customReturnValueHandlers(HandlerMethodReturnValueHandler... handlers) {
		this.mockMvcBuilder.setCustomReturnValueHandlers(handlers);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
		this.mockMvcBuilder.setHandlerExceptionResolvers(exceptionResolvers);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec viewResolvers(ViewResolver... resolvers) {
		this.mockMvcBuilder.setViewResolvers(resolvers);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec singleView(View view) {
		this.mockMvcBuilder.setSingleView(view);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec localeResolver(LocaleResolver localeResolver) {
		this.mockMvcBuilder.setLocaleResolver(localeResolver);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec flashMapManager(FlashMapManager flashMapManager) {
		this.mockMvcBuilder.setFlashMapManager(flashMapManager);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec patternParser(PathPatternParser parser) {
		this.mockMvcBuilder.setPatternParser(parser);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec useTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
		this.mockMvcBuilder.setUseTrailingSlashPatternMatch(useTrailingSlashPatternMatch);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec placeholderValue(String name, String value) {
		this.mockMvcBuilder.addPlaceholderValue(name, value);
		return this;
	}

	@Override
	public StandaloneMockMvcSpec customHandlerMapping(Supplier<RequestMappingHandlerMapping> factory) {
		this.mockMvcBuilder.setCustomHandlerMapping(factory);
		return this;
	}

	@Override
	public ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
		return this.mockMvcBuilder;
	}
}
