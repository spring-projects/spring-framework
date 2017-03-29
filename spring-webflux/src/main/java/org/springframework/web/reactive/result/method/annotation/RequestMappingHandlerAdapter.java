/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.ArrayList;
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
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * Supports the invocation of {@code @RequestMapping} methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(RequestMappingHandlerAdapter.class);


	private final List<HttpMessageReader<?>> messageReaders = new ArrayList<>(32);

	private WebBindingInitializer webBindingInitializer;

	private ArgumentResolverConfigurer argumentResolverConfigurer;

	private ReactiveAdapterRegistry reactiveAdapterRegistry;

	private ConfigurableApplicationContext applicationContext;

	private ControllerMethodResolver methodResolver;

	private ModelInitializer modelInitializer;


	public RequestMappingHandlerAdapter() {
		this.messageReaders.add(new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		this.messageReaders.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		this.messageReaders.add(new DecoderHttpMessageReader<>(new DataBufferDecoder()));
		this.messageReaders.add(new DecoderHttpMessageReader<>(new ResourceDecoder()));
		this.messageReaders.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(true)));
	}


	/**
	 * Configure HTTP message readers to de-serialize the request body with.
	 * <p>By default only basic data types such as bytes and text are registered.
	 * Consider using {@link ServerCodecConfigurer} to configure a richer list
	 * including JSON encoding .
	 * @see ServerCodecConfigurer
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.messageReaders.clear();
		this.messageReaders.addAll(messageReaders);
	}

	/**
	 * Return the configured HTTP message readers.
	 */
	public List<HttpMessageReader<?>> getMessageReaders() {
		return this.messageReaders;
	}

	/**
	 * Provide a WebBindingInitializer with "global" initialization to apply
	 * to every DataBinder instance.
	 */
	public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * Return the configured WebBindingInitializer, or {@code null} if none.
	 */
	public WebBindingInitializer getWebBindingInitializer() {
		return this.webBindingInitializer;
	}

	/**
	 * Configure resolvers for controller method arguments.
	 */
	public void setArgumentResolverConfigurer(ArgumentResolverConfigurer configurer) {
		Assert.notNull(configurer, "ArgumentResolverConfigurer is required");
		this.argumentResolverConfigurer = configurer;
	}

	/**
	 * Return the configured resolvers for controller method arguments.
	 */
	public ArgumentResolverConfigurer getArgumentResolverConfigurer() {
		return this.argumentResolverConfigurer;
	}

	/**
	 * Configure the registry for adapting various reactive types.
	 * <p>By default this is an instance of {@link ReactiveAdapterRegistry} with
	 * default settings.
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}

	/**
	 * Return the configured registry for adapting reactive types.
	 */
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

	public ConfigurableApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() throws Exception {

		if (this.argumentResolverConfigurer == null) {
			this.argumentResolverConfigurer = new ArgumentResolverConfigurer();
		}

		if (this.reactiveAdapterRegistry == null) {
			this.reactiveAdapterRegistry = new ReactiveAdapterRegistry();
		}

		this.methodResolver = new ControllerMethodResolver(this.argumentResolverConfigurer,
				this.messageReaders, this.reactiveAdapterRegistry, this.applicationContext);

		this.modelInitializer = new ModelInitializer(this.reactiveAdapterRegistry);
	}


	@Override
	public boolean supports(Object handler) {
		return HandlerMethod.class.equals(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {

		Assert.notNull(handler, "Expected handler");
		HandlerMethod handlerMethod = (HandlerMethod) handler;

		BindingContext bindingContext = new InitBinderBindingContext(
				getWebBindingInitializer(), this.methodResolver.getInitBinderMethods(handlerMethod));

		List<InvocableHandlerMethod> modelAttributeMethods =
				this.methodResolver.getModelAttributeMethods(handlerMethod);

		Function<Throwable, Mono<HandlerResult>> exceptionHandler =
				ex -> handleException(ex, handlerMethod, bindingContext, exchange);

		return this.modelInitializer
				.initModel(bindingContext, modelAttributeMethods, exchange)
				.then(() -> this.methodResolver.getRequestMappingMethod(handlerMethod)
						.invoke(exchange, bindingContext)
						.doOnNext(result -> result.setExceptionHandler(exceptionHandler))
						.otherwise(exceptionHandler));
	}

	private Mono<HandlerResult> handleException(Throwable ex, HandlerMethod handlerMethod,
			BindingContext bindingContext, ServerWebExchange exchange) {

		return this.methodResolver.getExceptionHandlerMethod(ex, handlerMethod)
				.map(invocable -> {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("Invoking @ExceptionHandler method: " + invocable.getMethod());
						}
						bindingContext.getModel().asMap().clear();
						Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
						return invocable.invoke(exchange, bindingContext, cause, handlerMethod);
					}
					catch (Throwable invocationEx) {
						if (logger.isWarnEnabled()) {
							logger.warn("Failed to invoke: " + invocable.getMethod(), invocationEx);
						}
						return null;
					}
				})
				.orElseGet(() -> Mono.error(ex));
	}

}
