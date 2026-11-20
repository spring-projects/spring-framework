/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.config;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
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
 * A subclass of {@code WebFluxConfigurationSupport} that detects and delegates
 * to all beans of type {@link WebFluxConfigurer} allowing them to customize the
 * configuration provided by {@code WebFluxConfigurationSupport}. This is the
 * class actually imported by {@link EnableWebFlux @EnableWebFlux}.
 *
 * @author Brian Clozel
 * @since 5.0
 */
@Configuration(proxyBeanMethods = false)
public class DelegatingWebFluxConfiguration extends WebFluxConfigurationSupport {

	private final WebFluxConfigurerComposite configurers = new WebFluxConfigurerComposite();


	@Autowired(required = false)
	public void setConfigurers(List<WebFluxConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addWebFluxConfigurers(configurers);
		}
	}


	@Override
	protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		this.configurers.configureHttpMessageCodecs(configurer);
	}

	@Override
	protected void addFormatters(FormatterRegistry registry) {
		this.configurers.addFormatters(registry);
	}

	@Override
	protected @Nullable Validator getValidator() {
		Validator validator = this.configurers.getValidator();
		return (validator != null ? validator : super.getValidator());
	}

	@Override
	protected @Nullable MessageCodesResolver getMessageCodesResolver() {
		MessageCodesResolver messageCodesResolver = this.configurers.getMessageCodesResolver();
		return (messageCodesResolver != null ? messageCodesResolver : super.getMessageCodesResolver());
	}

	@Override
	protected void addCorsMappings(CorsRegistry registry) {
		this.configurers.addCorsMappings(registry);
	}

	@Override
	protected void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
		this.configurers.configureBlockingExecution(configurer);
	}

	@Override
	protected void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		this.configurers.configureContentTypeResolver(builder);
	}

	@Override
	protected void configureApiVersioning(ApiVersionConfigurer configurer) {
		this.configurers.configureApiVersioning(configurer);
	}

	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.configurers.configurePathMatching(configurer);
	}

	@Override
	protected void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		this.configurers.configureArgumentResolvers(configurer);
	}

	@Override
	protected void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
		this.configurers.addErrorResponseInterceptors(interceptors);
	}


	@Override
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.configurers.addResourceHandlers(registry);
	}

	@Override
	protected void configureViewResolvers(ViewResolverRegistry registry) {
		this.configurers.configureViewResolvers(registry);
	}

	@Override
	protected @Nullable WebSocketService getWebSocketService() {
		WebSocketService service = this.configurers.getWebSocketService();
		return (service != null ? service : super.getWebSocketService());
	}

}
