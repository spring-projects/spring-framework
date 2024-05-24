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

package org.springframework.web.reactive.result.method.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.DispatchExceptionHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.DisconnectedClientHelper;

/**
 * Supports the invocation of
 * {@link org.springframework.web.bind.annotation.RequestMapping @RequestMapping}
 * handler methods.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class RequestMappingHandlerAdapter
		implements HandlerAdapter, DispatchExceptionHandler, ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(RequestMappingHandlerAdapter.class);

	/**
	 * Log category to use for network failure after a client has gone away.
	 * @see DisconnectedClientHelper
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.reactive.result.method.annotation.DisconnectedClient";

	private static final DisconnectedClientHelper disconnectedClientHelper =
			new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);


	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	@Nullable
	private WebBindingInitializer webBindingInitializer;

	@Nullable
	private ArgumentResolverConfigurer argumentResolverConfigurer;

	private RequestedContentTypeResolver contentTypeResolver = new RequestedContentTypeResolverBuilder().build();

	@Nullable
	private Scheduler scheduler;

	@Nullable
	private Predicate<HandlerMethod> blockingMethodPredicate;

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
	 * Set the {@link RequestedContentTypeResolver} to use to determine requested
	 * media types. If not set, the default constructor is used.
	 * @since 6.2
	 */
	public void setContentTypeResolver(RequestedContentTypeResolver contentTypeResolver) {
		Assert.notNull(contentTypeResolver, "'contentTypeResolver' must not be null");
		this.contentTypeResolver = contentTypeResolver;
	}

	/**
	 * Return the configured {@link RequestedContentTypeResolver}.
	 * @since 6.2
	 */
	public RequestedContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	/**
	 * Configure an executor to invoke blocking controller methods with.
	 * <p>By default, this is not set in which case controller methods are
	 * invoked without the use of an Executor.
	 * @param executor the task executor to use
	 * @since 6.1
	 */
	public void setBlockingExecutor(@Nullable Executor executor) {
		this.scheduler = (executor != null ? Schedulers.fromExecutor(executor) : null);
	}

	/**
	 * Provide a predicate to decide which controller methods to invoke through
	 * the configured {@link #setBlockingExecutor blockingExecutor}.
	 * <p>If an executor is configured, the default predicate matches controller
	 * methods whose return type is not recognized by the configured
	 * {@link org.springframework.core.ReactiveAdapterRegistry}.
	 * @param predicate the predicate to use
	 * @since 6.1
	 */
	public void setBlockingMethodPredicate(Predicate<HandlerMethod> predicate) {
		this.blockingMethodPredicate = predicate;
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
		if (applicationContext instanceof ConfigurableApplicationContext cac) {
			this.applicationContext = cac;
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

		if (this.scheduler != null && this.blockingMethodPredicate == null) {
			this.blockingMethodPredicate = new NonReactiveHandlerMethodPredicate(this.reactiveAdapterRegistry);
		}

		this.methodResolver = new ControllerMethodResolver(
				this.argumentResolverConfigurer, this.reactiveAdapterRegistry, this.applicationContext,
				this.contentTypeResolver, this.messageReaders, this.webBindingInitializer,
				this.scheduler, this.blockingMethodPredicate);

		this.modelInitializer = new ModelInitializer(this.methodResolver, this.reactiveAdapterRegistry);
	}


	@Override
	public boolean supports(Object handler) {
		return handler instanceof HandlerMethod;
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {

		Assert.state(this.methodResolver != null &&
				this.modelInitializer != null && this.reactiveAdapterRegistry != null, "Not initialized");

		HandlerMethod handlerMethod = (HandlerMethod) handler;

		InitBinderBindingContext bindingContext = new InitBinderBindingContext(
				this.webBindingInitializer, this.methodResolver.getInitBinderMethods(handlerMethod),
				this.methodResolver.hasMethodValidator() && handlerMethod.shouldValidateArguments(),
				this.reactiveAdapterRegistry);

		InvocableHandlerMethod invocableMethod = this.methodResolver.getRequestMappingMethod(handlerMethod);

		DispatchExceptionHandler exceptionHandler =
				(exchange2, ex) -> handleException(exchange, ex, handlerMethod, bindingContext);

		Mono<HandlerResult> resultMono = this.modelInitializer
				.initModel(handlerMethod, bindingContext, exchange)
				.then(Mono.defer(() -> invocableMethod.invoke(exchange, bindingContext)))
				.doOnNext(result -> result.setExceptionHandler(exceptionHandler))
				.onErrorResume(ex -> exceptionHandler.handleError(exchange, ex));

		Scheduler optionalScheduler = this.methodResolver.getSchedulerFor(handlerMethod);
		if (optionalScheduler != null) {
			return resultMono.subscribeOn(optionalScheduler);
		}

		return resultMono;
	}

	private Mono<HandlerResult> handleException(
			ServerWebExchange exchange, Throwable exception,
			@Nullable HandlerMethod handlerMethod, @Nullable BindingContext bindingContext) {

		Assert.state(this.methodResolver != null, "Not initialized");

		// Success and error responses may use different content types
		exchange.getAttributes().remove(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		exchange.getResponse().getHeaders().clearContentHeaders();

		InvocableHandlerMethod invocable =
				this.methodResolver.getExceptionHandlerMethod(exception, exchange, handlerMethod);

		if (invocable != null) {
			ArrayList<Throwable> exceptions = new ArrayList<>();
			try {
				if (logger.isDebugEnabled()) {
					logger.debug(exchange.getLogPrefix() + "Using @ExceptionHandler " + invocable);
				}
				if (bindingContext != null) {
					bindingContext.getModel().asMap().clear();
				}
				else {
					bindingContext = new BindingContext();
				}

				// Expose causes as provided arguments as well
				Throwable exToExpose = exception;
				while (exToExpose != null) {
					exceptions.add(exToExpose);
					Throwable cause = exToExpose.getCause();
					exToExpose = (cause != exToExpose ? cause : null);
				}
				Object[] arguments = new Object[exceptions.size() + 1];
				exceptions.toArray(arguments);  // efficient arraycopy call in ArrayList
				arguments[arguments.length - 1] = handlerMethod;

				return invocable.invoke(exchange, bindingContext, arguments)
						.onErrorResume(invocationEx ->
								handleExceptionHandlerFailure(exchange, exception, invocationEx, exceptions, invocable));
			}
			catch (Throwable invocationEx) {
				return handleExceptionHandlerFailure(exchange, exception, invocationEx, exceptions, invocable);
			}
		}
		return Mono.error(exception);
	}

	private static Mono<HandlerResult> handleExceptionHandlerFailure(
			ServerWebExchange exchange, Throwable exception, Throwable invocationEx,
			ArrayList<Throwable> exceptions, InvocableHandlerMethod invocable) {

		if (disconnectedClientHelper.checkAndLogClientDisconnectedException(invocationEx)) {
			return Mono.empty();
		}

		// Any other than the original exception (or a cause) is unintended here,
		// probably an accident (e.g. failed assertion or the like).
		if (!exceptions.contains(invocationEx) && logger.isWarnEnabled()) {
			logger.warn(exchange.getLogPrefix() + "Failure in @ExceptionHandler " + invocable, invocationEx);
		}

		return Mono.error(exception);
	}

	@Override
	public Mono<HandlerResult> handleError(ServerWebExchange exchange, Throwable ex) {
		return handleException(exchange, ex, null, null);
	}


	/**
	 * Match methods with a return type without an adapter in {@link ReactiveAdapterRegistry}
	 * which are not suspending functions.
	 */
	private record NonReactiveHandlerMethodPredicate(ReactiveAdapterRegistry adapterRegistry)
			implements Predicate<HandlerMethod> {

		@Override
		public boolean test(HandlerMethod handlerMethod) {
			Class<?> returnType = handlerMethod.getReturnType().getParameterType();
			return (this.adapterRegistry.getAdapter(returnType) == null
					&& !KotlinDetector.isSuspendingFunction(handlerMethod.getMethod()));
		}
	}

}
