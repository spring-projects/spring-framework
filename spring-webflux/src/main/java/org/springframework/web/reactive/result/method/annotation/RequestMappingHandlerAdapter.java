/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * Supports the invocation of
 * {@link org.springframework.web.bind.annotation.RequestMapping @RequestMapping}
 * handler methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(RequestMappingHandlerAdapter.class);


	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	@Nullable
	private WebBindingInitializer webBindingInitializer;

	@Nullable
	private ArgumentResolverConfigurer argumentResolverConfigurer;

	@Nullable
	private ReactiveAdapterRegistry reactiveAdapterRegistry;

	@Nullable
	private ConfigurableApplicationContext applicationContext;

	@Nullable
	private ControllerMethodResolver methodResolver;

	@Nullable
	private ModelInitializer modelInitializer;


	/**
	 * Configure HTTP message readers to de-serialize the request body with.
	 * <p>By default this is set to {@link ServerCodecConfigurer}'s readers with defaults.
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		Assert.notNull(messageReaders, "'messageReaders' must not be null");
		this.messageReaders = messageReaders;
	}

	/**
	 * Return the configurer for HTTP message readers.
	 */
	public List<HttpMessageReader<?>> getMessageReaders() {
		return this.messageReaders;
	}

	/**
	 * Provide a WebBindingInitializer with "global" initialization to apply
	 * to every DataBinder instance.
	 */
	public void setWebBindingInitializer(@Nullable WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * Return the configured WebBindingInitializer, or {@code null} if none.
	 */
	@Nullable
	public WebBindingInitializer getWebBindingInitializer() {
		return this.webBindingInitializer;
	}

	/**
	 * Configure resolvers for controller method arguments.
	 */
	public void setArgumentResolverConfigurer(@Nullable ArgumentResolverConfigurer configurer) {
		this.argumentResolverConfigurer = configurer;
	}

	/**
	 * Return the configured resolvers for controller method arguments.
	 */
	@Nullable
	public ArgumentResolverConfigurer getArgumentResolverConfigurer() {
		return this.argumentResolverConfigurer;
	}

	/**
	 * Configure the registry for adapting various reactive types.
	 * <p>By default this is an instance of {@link ReactiveAdapterRegistry} with
	 * default settings.
	 */
	public void setReactiveAdapterRegistry(@Nullable ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}

	/**
	 * Return the configured registry for adapting reactive types.
	 */
	@Nullable
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return this.reactiveAdapterRegistry;
	}

	/**
	 * A {@link ConfigurableApplicationContext} is expected for resolving
	 * expressions in method argument default values as well as for
	 * detecting {@code @ControllerAdvice} beans.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			this.applicationContext = (ConfigurableApplicationContext) applicationContext;
		}
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.applicationContext, "ApplicationContext is required");

		if (CollectionUtils.isEmpty(this.messageReaders)) {
			ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
			this.messageReaders = codecConfigurer.getReaders();
		}
		if (this.argumentResolverConfigurer == null) {
			this.argumentResolverConfigurer = new ArgumentResolverConfigurer();
		}
		if (this.reactiveAdapterRegistry == null) {
			this.reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		}

		this.methodResolver = new ControllerMethodResolver(this.argumentResolverConfigurer,
				this.reactiveAdapterRegistry, this.applicationContext, this.messageReaders);

		this.modelInitializer = new ModelInitializer(this.methodResolver, this.reactiveAdapterRegistry);
	}


	@Override
	public boolean supports(Object handler) {
		return handler instanceof HandlerMethod;
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		Assert.state(this.methodResolver != null && this.modelInitializer != null, "Not initialized");

		InitBinderBindingContext bindingContext = new InitBinderBindingContext(
				getWebBindingInitializer(), this.methodResolver.getInitBinderMethods(handlerMethod));

		InvocableHandlerMethod invocableMethod = this.methodResolver.getRequestMappingMethod(handlerMethod);

		Function<Throwable, Mono<HandlerResult>> exceptionHandler =
				ex -> handleException(ex, handlerMethod, bindingContext, exchange);

		return this.modelInitializer
				.initModel(handlerMethod, bindingContext, exchange)
				.then(Mono.defer(() -> invocableMethod.invoke(exchange, bindingContext)))
				.doOnNext(result -> result.setExceptionHandler(exceptionHandler))
				.doOnNext(result -> bindingContext.saveModel())
				.onErrorResume(exceptionHandler);
	}

	private Mono<HandlerResult> handleException(Throwable exception, HandlerMethod handlerMethod,
			BindingContext bindingContext, ServerWebExchange exchange) {

		Assert.state(this.methodResolver != null, "Not initialized");

		// Success and error responses may use different content types
		exchange.getAttributes().remove(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!exchange.getResponse().isCommitted()) {
			exchange.getResponse().getHeaders().remove(HttpHeaders.CONTENT_TYPE);
		}

		InvocableHandlerMethod invocable = this.methodResolver.getExceptionHandlerMethod(exception, handlerMethod);
		if (invocable != null) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug(exchange.getLogPrefix() + "Using @ExceptionHandler " + invocable);
				}
				bindingContext.getModel().asMap().clear();
				Throwable cause = exception.getCause();
				if (cause != null) {
					return invocable.invoke(exchange, bindingContext, exception, cause, handlerMethod);
				}
				else {
					return invocable.invoke(exchange, bindingContext, exception, handlerMethod);
				}
			}
			catch (Throwable invocationEx) {
				if (logger.isWarnEnabled()) {
					logger.warn(exchange.getLogPrefix() + "Failure in @ExceptionHandler " + invocable, invocationEx);
				}
			}
		}
		return Mono.error(exception);
	}

}
