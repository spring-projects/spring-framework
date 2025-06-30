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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.socket.server.WebSocketService;

/**
 * A {@link WebFluxConfigurer} that delegates to one or more others.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebFluxConfigurerComposite implements WebFluxConfigurer {

	private final List<WebFluxConfigurer> delegates = new ArrayList<>();


	public void addWebFluxConfigurers(List<WebFluxConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.delegates.addAll(configurers);
		}
	}


	@Override
	public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configureHttpMessageCodecs(configurer));
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addFormatters(registry));
	}

	@Override
	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1128
	public @Nullable Validator getValidator() {
		return createSingleBean(WebFluxConfigurer::getValidator, Validator.class);
	}

	@Override
	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1128
	public @Nullable MessageCodesResolver getMessageCodesResolver() {
		return createSingleBean(WebFluxConfigurer::getMessageCodesResolver, MessageCodesResolver.class);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addCorsMappings(registry));
	}

	@Override
	public void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configureBlockingExecution(configurer));
	}

	@Override
	public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		this.delegates.forEach(delegate -> delegate.configureContentTypeResolver(builder));
	}

	@Override
	public void configureApiVersioning(ApiVersionConfigurer configurer) {
		for (WebFluxConfigurer delegate : this.delegates) {
			delegate.configureApiVersioning(configurer);
		}
	}

	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configurePathMatching(configurer));
	}

	@Override
	public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configureArgumentResolvers(configurer));
	}

	@Override
	public void addErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
		for (WebFluxConfigurer delegate : this.delegates) {
			delegate.addErrorResponseInterceptors(interceptors);
		}
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		this.delegates.forEach(delegate -> delegate.configureViewResolvers(registry));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addResourceHandlers(registry));
	}

	@Override
	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1128
	public @Nullable WebSocketService getWebSocketService() {
		return createSingleBean(WebFluxConfigurer::getWebSocketService, WebSocketService.class);
	}

	private <T> @Nullable T createSingleBean(Function<WebFluxConfigurer, @Nullable T> factory, Class<T> beanType) {
		List<T> result = this.delegates.stream().map(factory).filter(Objects::nonNull).toList();
		if (result.isEmpty()) {
			return null;
		}
		else if (result.size() == 1) {
			return result.get(0);
		}
		else {
			throw new IllegalStateException("More than one WebFluxConfigurer implements " +
					beanType.getSimpleName() + " factory method.");
		}
	}

}
