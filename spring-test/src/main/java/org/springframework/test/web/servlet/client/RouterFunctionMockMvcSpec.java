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

package org.springframework.test.web.servlet.client;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.RouterFunctionMockMvcBuilder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Simple wrapper around a {@link RouterFunctionMockMvcBuilder} that implements
 * {@link MockMvcWebTestClient.RouterFunctionSpec}.
 *
 * @author Arjen Poutsma
 * @since 6.2
 */
class RouterFunctionMockMvcSpec extends AbstractMockMvcServerSpec<MockMvcWebTestClient.RouterFunctionSpec>
		implements MockMvcWebTestClient.RouterFunctionSpec {

	private final RouterFunctionMockMvcBuilder mockMvcBuilder;


	RouterFunctionMockMvcSpec(RouterFunction<?>... routerFunctions) {
		this.mockMvcBuilder = MockMvcBuilders.routerFunctions(routerFunctions);
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
		this.mockMvcBuilder.setMessageConverters(messageConverters);
		return this;
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec interceptors(HandlerInterceptor... interceptors) {
		mappedInterceptors(null, interceptors);
		return this;
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec mappedInterceptors(@Nullable String[] pathPatterns, HandlerInterceptor... interceptors) {
		this.mockMvcBuilder.addMappedInterceptors(pathPatterns, interceptors);
		return this;
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec asyncRequestTimeout(long timeout) {
		this.mockMvcBuilder.setAsyncRequestTimeout(timeout);
		return this;
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
		this.mockMvcBuilder.setHandlerExceptionResolvers(exceptionResolvers);
		return this;
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec viewResolvers(ViewResolver... resolvers) {
		this.mockMvcBuilder.setViewResolvers(resolvers);
		return this;
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec singleView(View view) {
		this.mockMvcBuilder.setSingleView(view);
		return this;
	}

	@Override
	public MockMvcWebTestClient.RouterFunctionSpec patternParser(PathPatternParser parser) {
		this.mockMvcBuilder.setPatternParser(parser);
		return this;
	}

	@Override
	protected ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
		return this.mockMvcBuilder;
	}
}
