/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	public @Nullable Validator getValidator() {
		return createSingleBean(WebFluxConfigurer::getValidator, Validator.class);
	}

	@Override
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
