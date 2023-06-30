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

package org.springframework.web.server.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import io.micrometer.observation.ObservationRegistry;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHandlerDecoratorFactory;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * This builder has two purposes:
 *
 * <p>One is to assemble a processing chain that consists of a target {@link WebHandler},
 * then decorated with a set of {@link WebFilter WebFilters}, then further decorated with
 * a set of {@link WebExceptionHandler WebExceptionHandlers}.
 *
 * <p>The second purpose is to adapt the resulting processing chain to an {@link HttpHandler}:
 * the lowest-level reactive HTTP handling abstraction which can then be used with any of the
 * supported runtimes. The adaptation is done with the help of {@link HttpWebHandlerAdapter}.
 *
 * <p>The processing chain can be assembled manually via builder methods, or detected from
 * a Spring {@link ApplicationContext} via {@link #applicationContext}, or a mix of both.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 * @see HttpWebHandlerAdapter
 */
public final class WebHttpHandlerBuilder {

	/** Well-known name for the target WebHandler in the bean factory. */
	public static final String WEB_HANDLER_BEAN_NAME = "webHandler";

	/** Well-known name for the WebSessionManager in the bean factory. */
	public static final String WEB_SESSION_MANAGER_BEAN_NAME = "webSessionManager";

	/** Well-known name for the ServerCodecConfigurer in the bean factory. */
	public static final String SERVER_CODEC_CONFIGURER_BEAN_NAME = "serverCodecConfigurer";

	/** Well-known name for the LocaleContextResolver in the bean factory. */
	public static final String LOCALE_CONTEXT_RESOLVER_BEAN_NAME = "localeContextResolver";

	/** Well-known name for the ForwardedHeaderTransformer in the bean factory. */
	public static final String FORWARDED_HEADER_TRANSFORMER_BEAN_NAME = "forwardedHeaderTransformer";

	private final WebHandler webHandler;

	@Nullable
	private final ApplicationContext applicationContext;

	private final List<WebFilter> filters = new ArrayList<>();

	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	@Nullable
	private Function<HttpHandler, HttpHandler> httpHandlerDecorator;

	@Nullable
	private WebSessionManager sessionManager;

	@Nullable
	private ServerCodecConfigurer codecConfigurer;

	@Nullable
	private LocaleContextResolver localeContextResolver;

	@Nullable
	private ForwardedHeaderTransformer forwardedHeaderTransformer;

	@Nullable
	private ObservationRegistry observationRegistry;

	@Nullable
	private ServerRequestObservationConvention observationConvention;


	/**
	 * Private constructor to use when initialized from an ApplicationContext.
	 */
	private WebHttpHandlerBuilder(WebHandler webHandler, @Nullable ApplicationContext applicationContext) {
		Assert.notNull(webHandler, "WebHandler must not be null");
		this.webHandler = webHandler;
		this.applicationContext = applicationContext;
	}

	/**
	 * Copy constructor.
	 */
	private WebHttpHandlerBuilder(WebHttpHandlerBuilder other) {
		this.webHandler = other.webHandler;
		this.applicationContext = other.applicationContext;
		this.filters.addAll(other.filters);
		this.exceptionHandlers.addAll(other.exceptionHandlers);
		this.sessionManager = other.sessionManager;
		this.codecConfigurer = other.codecConfigurer;
		this.localeContextResolver = other.localeContextResolver;
		this.forwardedHeaderTransformer = other.forwardedHeaderTransformer;
		this.observationRegistry = other.observationRegistry;
		this.observationConvention = other.observationConvention;
		this.httpHandlerDecorator = other.httpHandlerDecorator;
	}


	/**
	 * Static factory method to create a new builder instance.
	 * @param webHandler the target handler for the request
	 * @return the prepared builder
	 */
	public static WebHttpHandlerBuilder webHandler(WebHandler webHandler) {
		return new WebHttpHandlerBuilder(webHandler, null);
	}

	/**
	 * Static factory method to create a new builder instance by detecting beans
	 * in an {@link ApplicationContext}. The following are detected:
	 * <ul>
	 * <li>{@link WebHandler} [1] -- looked up by the name
	 * {@link #WEB_HANDLER_BEAN_NAME}.
	 * <li>{@link WebFilter} [0..N] -- detected by type and ordered,
	 * see {@link AnnotationAwareOrderComparator}.
	 * <li>{@link WebExceptionHandler} [0..N] -- detected by type and
	 * ordered.
	 * <li>{@link HttpHandlerDecoratorFactory} [0..N] -- detected by type and
	 * ordered.
	 * <li>{@link WebSessionManager} [0..1] -- looked up by the name
	 * {@link #WEB_SESSION_MANAGER_BEAN_NAME}.
	 * <li>{@link ServerCodecConfigurer} [0..1] -- looked up by the name
	 * {@link #SERVER_CODEC_CONFIGURER_BEAN_NAME}.
	 * <li>{@link LocaleContextResolver} [0..1] -- looked up by the name
	 * {@link #LOCALE_CONTEXT_RESOLVER_BEAN_NAME}.
	 * </ul>
	 * @param context the application context to use for the lookup
	 * @return the prepared builder
	 */
	public static WebHttpHandlerBuilder applicationContext(ApplicationContext context) {

		WebHttpHandlerBuilder builder = new WebHttpHandlerBuilder(
				context.getBean(WEB_HANDLER_BEAN_NAME, WebHandler.class), context);

		List<WebFilter> webFilters = context
				.getBeanProvider(WebFilter.class)
				.orderedStream()
				.toList();
		builder.filters(filters -> filters.addAll(webFilters));

		List<WebExceptionHandler> exceptionHandlers = context
				.getBeanProvider(WebExceptionHandler.class)
				.orderedStream()
				.toList();
		builder.exceptionHandlers(handlers -> handlers.addAll(exceptionHandlers));

		context.getBeanProvider(HttpHandlerDecoratorFactory.class)
				.orderedStream()
				.forEach(builder::httpHandlerDecorator);

		try {
			builder.sessionManager(
					context.getBean(WEB_SESSION_MANAGER_BEAN_NAME, WebSessionManager.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		try {
			builder.codecConfigurer(
					context.getBean(SERVER_CODEC_CONFIGURER_BEAN_NAME, ServerCodecConfigurer.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		try {
			builder.localeContextResolver(
					context.getBean(LOCALE_CONTEXT_RESOLVER_BEAN_NAME, LocaleContextResolver.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		try {
			builder.forwardedHeaderTransformer(
					context.getBean(FORWARDED_HEADER_TRANSFORMER_BEAN_NAME, ForwardedHeaderTransformer.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		return builder;
	}


	/**
	 * Add the given filter(s).
	 * @param filters the filter(s) to add that's
	 */
	public WebHttpHandlerBuilder filter(WebFilter... filters) {
		if (!ObjectUtils.isEmpty(filters)) {
			this.filters.addAll(Arrays.asList(filters));
			updateFilters();
		}
		return this;
	}

	/**
	 * Manipulate the "live" list of currently configured filters.
	 * @param consumer the consumer to use
	 */
	public WebHttpHandlerBuilder filters(Consumer<List<WebFilter>> consumer) {
		consumer.accept(this.filters);
		updateFilters();
		return this;
	}

	private void updateFilters() {
		if (this.filters.isEmpty()) {
			return;
		}

		List<WebFilter> filtersToUse = this.filters.stream()
				.peek(filter -> {
					if (filter instanceof ForwardedHeaderTransformer forwardedHeaderTransformerFilter
							&& this.forwardedHeaderTransformer == null) {
						this.forwardedHeaderTransformer = forwardedHeaderTransformerFilter;
					}
				})
				.filter(filter -> !(filter instanceof ForwardedHeaderTransformer))
				.toList();

		this.filters.clear();
		this.filters.addAll(filtersToUse);
	}

	/**
	 * Add the given exception handler(s).
	 * @param handlers the exception handler(s)
	 */
	public WebHttpHandlerBuilder exceptionHandler(WebExceptionHandler... handlers) {
		if (!ObjectUtils.isEmpty(handlers)) {
			this.exceptionHandlers.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	/**
	 * Manipulate the "live" list of currently configured exception handlers.
	 * @param consumer the consumer to use
	 */
	public WebHttpHandlerBuilder exceptionHandlers(Consumer<List<WebExceptionHandler>> consumer) {
		consumer.accept(this.exceptionHandlers);
		return this;
	}

	/**
	 * Configure the {@link WebSessionManager} to set on the
	 * {@link ServerWebExchange WebServerExchange}.
	 * <p>By default {@link DefaultWebSessionManager} is used.
	 * @param manager the session manager
	 * @see HttpWebHandlerAdapter#setSessionManager(WebSessionManager)
	 */
	public WebHttpHandlerBuilder sessionManager(WebSessionManager manager) {
		this.sessionManager = manager;
		return this;
	}

	/**
	 * Whether a {@code WebSessionManager} is configured or not, either detected from an
	 * {@code ApplicationContext} or explicitly configured via {@link #sessionManager}.
	 * @since 5.0.9
	 */
	public boolean hasSessionManager() {
		return (this.sessionManager != null);
	}

	/**
	 * Configure the {@link ServerCodecConfigurer} to set on the {@code WebServerExchange}.
	 * @param codecConfigurer the codec configurer
	 */
	public WebHttpHandlerBuilder codecConfigurer(ServerCodecConfigurer codecConfigurer) {
		this.codecConfigurer = codecConfigurer;
		return this;
	}


	/**
	 * Whether a {@code ServerCodecConfigurer} is configured or not, either detected from an
	 * {@code ApplicationContext} or explicitly configured via {@link #codecConfigurer}.
	 * @since 5.0.9
	 */
	public boolean hasCodecConfigurer() {
		return (this.codecConfigurer != null);
	}

	/**
	 * Configure the {@link LocaleContextResolver} to set on the
	 * {@link ServerWebExchange WebServerExchange}.
	 * @param localeContextResolver the locale context resolver
	 */
	public WebHttpHandlerBuilder localeContextResolver(LocaleContextResolver localeContextResolver) {
		this.localeContextResolver = localeContextResolver;
		return this;
	}

	/**
	 * Whether a {@code LocaleContextResolver} is configured or not, either detected from an
	 * {@code ApplicationContext} or explicitly configured via {@link #localeContextResolver}.
	 * @since 5.0.9
	 */
	public boolean hasLocaleContextResolver() {
		return (this.localeContextResolver != null);
	}

	/**
	 * Configure the {@link ForwardedHeaderTransformer} for extracting and/or
	 * removing forwarded headers.
	 * @param transformer the transformer
	 * @since 5.1
	 */
	public WebHttpHandlerBuilder forwardedHeaderTransformer(ForwardedHeaderTransformer transformer) {
		this.forwardedHeaderTransformer = transformer;
		return this;
	}

	/**
	 * Whether a {@code ForwardedHeaderTransformer} is configured or not, either
	 * detected from an {@code ApplicationContext} or explicitly configured via
	 * {@link #forwardedHeaderTransformer(ForwardedHeaderTransformer)}.
	 * @since 5.1
	 */
	public boolean hasForwardedHeaderTransformer() {
		return (this.forwardedHeaderTransformer != null);
	}

	/**
	 * Configure an {@link ObservationRegistry} for recording server exchange observations.
	 * By default, a {@link ObservationRegistry#NOOP no-op} registry will be configured.
	 * @param observationRegistry the observation registry
	 * @since 6.1
	 */
	public WebHttpHandlerBuilder observationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
		return this;
	}

	/**
	 * Configure a {@link ServerRequestObservationConvention} to use for server observations.
	 * By default, a {@link DefaultServerRequestObservationConvention} will be used.
	 * @param observationConvention the convention to use for all recorded observations
	 * @since 6.1
	 */
	public WebHttpHandlerBuilder observationConvention(ServerRequestObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
		return this;
	}

	/**
	 * Configure a {@link Function} to decorate the {@link HttpHandler} returned
	 * by this builder which effectively wraps the entire
	 * {@link WebExceptionHandler} - {@link WebFilter} - {@link WebHandler}
	 * processing chain. This provides access to the request and response before
	 * the entire chain and likewise the ability to observe the result of
	 * the entire chain.
	 * @param handlerDecorator the decorator to apply
	 * @since 5.3
	 */
	public WebHttpHandlerBuilder httpHandlerDecorator(Function<HttpHandler, HttpHandler> handlerDecorator) {
		this.httpHandlerDecorator = (this.httpHandlerDecorator != null ?
				handlerDecorator.andThen(this.httpHandlerDecorator) : handlerDecorator);
		return this;
	}

	/**
	 * Whether a decorator for {@link HttpHandler} is configured or not via
	 * {@link #httpHandlerDecorator(Function)}.
	 * @since 5.3
	 */
	public boolean hasHttpHandlerDecorator() {
		return (this.httpHandlerDecorator != null);
	}

	/**
	 * Build the {@link HttpHandler}.
	 */
	public HttpHandler build() {
		WebHandler decorated = new FilteringWebHandler(this.webHandler, this.filters);
		decorated = new ExceptionHandlingWebHandler(decorated,  this.exceptionHandlers);

		HttpWebHandlerAdapter adapted = new HttpWebHandlerAdapter(decorated);
		if (this.sessionManager != null) {
			adapted.setSessionManager(this.sessionManager);
		}
		if (this.codecConfigurer != null) {
			adapted.setCodecConfigurer(this.codecConfigurer);
		}
		if (this.localeContextResolver != null) {
			adapted.setLocaleContextResolver(this.localeContextResolver);
		}
		if (this.forwardedHeaderTransformer != null) {
			adapted.setForwardedHeaderTransformer(this.forwardedHeaderTransformer);
		}
		if (this.observationRegistry != null) {
			adapted.setObservationRegistry(this.observationRegistry);
		}
		if (this.observationConvention != null) {
			adapted.setObservationConvention(this.observationConvention);
		}
		if (this.applicationContext != null) {
			adapted.setApplicationContext(this.applicationContext);
		}
		adapted.afterPropertiesSet();

		return (this.httpHandlerDecorator != null ? this.httpHandlerDecorator.apply(adapted) : adapted);
	}

	/**
	 * Clone this {@link WebHttpHandlerBuilder}.
	 * @return the cloned builder instance
	 */
	@Override
	public WebHttpHandlerBuilder clone() {
		return new WebHttpHandlerBuilder(this);
	}


	/**
	 * {@code BlockHoundIntegration} for spring-web classes.
	 * @since 5.3.6
	 */
	public static class SpringWebBlockHoundIntegration implements BlockHoundIntegration {

		@Override
		public void applyTo(BlockHound.Builder builder) {

			// Avoid hard references potentially anywhere in spring-web (no need for structural dependency)

			builder.allowBlockingCallsInside("org.springframework.http.MediaTypeFactory", "<clinit>");
			builder.allowBlockingCallsInside("org.springframework.web.util.HtmlUtils", "<clinit>");
		}
	}

}
