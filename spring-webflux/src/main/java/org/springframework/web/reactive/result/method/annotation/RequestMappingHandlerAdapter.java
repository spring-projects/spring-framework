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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * Supports the invocation of {@code @RequestMapping} methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(RequestMappingHandlerAdapter.class);


	private final List<HttpMessageReader<?>> messageReaders = new ArrayList<>(10);

	private WebBindingInitializer webBindingInitializer;

	private ReactiveAdapterRegistry reactiveAdapterRegistry = new ReactiveAdapterRegistry();

	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private List<HandlerMethodArgumentResolver> argumentResolvers;

	private List<SyncHandlerMethodArgumentResolver> customInitBinderArgumentResolvers;

	private List<SyncHandlerMethodArgumentResolver> initBinderArgumentResolvers;

	private ConfigurableApplicationContext applicationContext;

	private ControllerMethodResolver controllerMethodResolver;

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
	 * Configure custom argument resolvers without overriding the built-in ones.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.customArgumentResolvers = resolvers;
	}

	/**
	 * Return the custom argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * Configure the complete list of supported argument types thus overriding
	 * the resolvers that would otherwise be configured by default.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.argumentResolvers = new ArrayList<>(resolvers);
	}

	/**
	 * Return the configured argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers;
	}

	/**
	 * Configure custom argument resolvers for {@code @InitBinder} methods.
	 */
	public void setCustomInitBinderArgumentResolvers(List<SyncHandlerMethodArgumentResolver> resolvers) {
		this.customInitBinderArgumentResolvers = resolvers;
	}

	/**
	 * Return the custom {@code @InitBinder} argument resolvers.
	 */
	public List<SyncHandlerMethodArgumentResolver> getCustomInitBinderArgumentResolvers() {
		return this.customInitBinderArgumentResolvers;
	}

	/**
	 * Configure the supported argument types in {@code @InitBinder} methods.
	 */
	public void setInitBinderArgumentResolvers(List<SyncHandlerMethodArgumentResolver> resolvers) {
		this.initBinderArgumentResolvers = null;
		if (resolvers != null) {
			this.initBinderArgumentResolvers = new ArrayList<>();
			this.initBinderArgumentResolvers.addAll(resolvers);
		}
	}

	/**
	 * Return the configured argument resolvers for {@code @InitBinder} methods.
	 */
	public List<SyncHandlerMethodArgumentResolver> getInitBinderArgumentResolvers() {
		return this.initBinderArgumentResolvers;
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

	public ConfigurableBeanFactory getBeanFactory() {
		return this.applicationContext.getBeanFactory();
	}


	@Override
	public void afterPropertiesSet() throws Exception {

		if (this.argumentResolvers == null) {
			this.argumentResolvers = getDefaultArgumentResolvers();
		}
		if (this.initBinderArgumentResolvers == null) {
			this.initBinderArgumentResolvers = getDefaultInitBinderArgumentResolvers();
		}

		this.controllerMethodResolver = new ControllerMethodResolver(
				getArgumentResolvers(), getInitBinderArgumentResolvers(), getApplicationContext());

		this.modelInitializer = new ModelInitializer(getReactiveAdapterRegistry());
	}

	protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Annotation-based argument resolution
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry(), false));
		resolvers.add(new RequestParamMapMethodArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new PathVariableMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new PathVariableMapMethodArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new RequestBodyArgumentResolver(getMessageReaders(), getReactiveAdapterRegistry()));
		resolvers.add(new ModelAttributeMethodArgumentResolver(getReactiveAdapterRegistry(), false));
		resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new RequestHeaderMapMethodArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new CookieValueMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new SessionAttributeMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new RequestAttributeMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));

		// Type-based argument resolution
		resolvers.add(new HttpEntityArgumentResolver(getMessageReaders(), getReactiveAdapterRegistry()));
		resolvers.add(new ModelArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new ErrorsMethodArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new ServerWebExchangeArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new PrincipalArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new WebSessionArgumentResolver(getReactiveAdapterRegistry()));

		// Custom resolvers
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// Catch-all
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry(), true));
		resolvers.add(new ModelAttributeMethodArgumentResolver(getReactiveAdapterRegistry(), true));
		return resolvers;
	}

	protected List<SyncHandlerMethodArgumentResolver> getDefaultInitBinderArgumentResolvers() {
		List<SyncHandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Annotation-based argument resolution
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry(), false));
		resolvers.add(new RequestParamMapMethodArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new PathVariableMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new PathVariableMapMethodArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new RequestHeaderMapMethodArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new CookieValueMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));
		resolvers.add(new RequestAttributeMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry()));

		// Type-based argument resolution
		resolvers.add(new ModelArgumentResolver(getReactiveAdapterRegistry()));
		resolvers.add(new ServerWebExchangeArgumentResolver(getReactiveAdapterRegistry()));

		// Custom resolvers
		if (getCustomInitBinderArgumentResolvers() != null) {
			resolvers.addAll(getCustomInitBinderArgumentResolvers());
		}

		// Catch-all
		resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), getReactiveAdapterRegistry(), true));
		return resolvers;
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
				getWebBindingInitializer(), getInitBinderMethods(handlerMethod));

		return this.modelInitializer
				.initModel(bindingContext, getModelAttributeMethods(handlerMethod), exchange)
				.then(() -> {
					Function<Throwable, Mono<HandlerResult>> exceptionHandler =
							ex -> handleException(exchange, handlerMethod, bindingContext, ex);

					InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
					invocable.setArgumentResolvers(getArgumentResolvers());

					return invocable.invoke(exchange, bindingContext)
							.doOnNext(result -> result.setExceptionHandler(exceptionHandler))
							.otherwise(exceptionHandler);
				});
	}

	private List<SyncInvocableHandlerMethod> getInitBinderMethods(HandlerMethod handlerMethod) {
		return this.controllerMethodResolver.resolveInitBinderMethods(handlerMethod);
	}

	private List<InvocableHandlerMethod> getModelAttributeMethods(HandlerMethod handlerMethod) {
		return this.controllerMethodResolver.resolveModelAttributeMethods(handlerMethod);
	}

	private Mono<HandlerResult> handleException(ServerWebExchange exchange, HandlerMethod handlerMethod,
			BindingContext bindingContext, Throwable ex) {

		InvocableHandlerMethod invocable =
				this.controllerMethodResolver.resolveExceptionHandlerMethod(ex, handlerMethod);

		if (invocable != null) {
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
			}
		}
		return Mono.error(ex);
	}

}
