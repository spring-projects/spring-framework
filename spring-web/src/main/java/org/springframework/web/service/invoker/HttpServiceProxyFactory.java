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

package org.springframework.web.service.invoker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.reactor.MonoKt;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import reactor.core.publisher.Mono;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Factory to create a client proxy from an HTTP service interface with
 * annotated methods.
 *
 * <p>Add custom {@link HttpRequestValuesResolver} to support another annotations, not limited to {@link HttpExchange}.
 *
 * <p>To create an instance, use static methods to obtain a
 * {@link Builder Builder}.
 *
 * @author Rossen Stoyanchev
 * @author Freeman Lau
 * @since 6.0
 * @see org.springframework.web.reactive.function.client.support.WebClientAdapter
 * @see HttpExchangeRequestValuesResolver
 */
public final class HttpServiceProxyFactory {

	private final HttpClientAdapter clientAdapter;

	private final List<HttpServiceArgumentResolver> argumentResolvers;

	private final List<HttpRequestValuesResolver> requestValuesResolvers;

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;

	@Nullable
	private final Duration blockTimeout;

	private HttpServiceProxyFactory(
			HttpClientAdapter clientAdapter, List<HttpServiceArgumentResolver> argumentResolvers,
			List<HttpRequestValuesResolver> requestValuesResolvers,
			ReactiveAdapterRegistry reactiveAdapterRegistry, @Nullable Duration blockTimeout) {

		this.clientAdapter = clientAdapter;
		this.argumentResolvers = argumentResolvers;
		this.requestValuesResolvers = requestValuesResolvers;
		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
		this.blockTimeout = blockTimeout;
	}


	/**
	 * Return a proxy that implements the given HTTP service interface to perform
	 * HTTP requests and retrieve responses through an HTTP client.
	 * @param serviceType the HTTP service to create a proxy for
	 * @param <S> the HTTP service type
	 * @return the created proxy
	 */
	public <S> S createClient(Class<S> serviceType) {

		List<HttpServiceMethod> httpServiceMethods = createHttpServiceMethods(serviceType);

		return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(httpServiceMethods));
	}

	private List<HttpServiceMethod> createHttpServiceMethods(Class<?> serviceType) {
		List<HttpServiceMethod> result = new ArrayList<>();
		ReflectionUtils.doWithMethods(serviceType, method -> {
			for (HttpRequestValuesResolver processor : this.requestValuesResolvers) {
				if (processor.supports(method)) {
					HttpRequestValuesInitializer valuesInitializer = processor
							.resolve(method, serviceType);
					result.add(createHttpServiceMethod(method, valuesInitializer));
					break;
				}
			}
		});
		return result;
	}

	private HttpServiceMethod createHttpServiceMethod(Method method,
			HttpRequestValuesInitializer httpRequestValuesInitializer) {
		Assert.notNull(this.argumentResolvers,
				"No argument resolvers: afterPropertiesSet was not called");

		return new HttpServiceMethod(
				method, this.argumentResolvers, httpRequestValuesInitializer, this.clientAdapter,
				this.reactiveAdapterRegistry, this.blockTimeout);
	}


	/**
	 * Return a builder that's initialized with the given client.
	 */
	public static Builder builder(HttpClientAdapter clientAdapter) {
		return new Builder().clientAdapter(clientAdapter);
	}

	/**
	 * Return an empty builder, with the client to be provided to builder.
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Builder to create an {@link HttpServiceProxyFactory}.
	 */
	public static final class Builder {

		@Nullable
		private HttpClientAdapter clientAdapter;

		private final List<HttpServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();

		private final List<HttpRequestValuesResolver> customRequestValuesProcessors = new ArrayList<>();

		@Nullable
		private ConversionService conversionService;

		@Nullable
		private StringValueResolver embeddedValueResolver;

		private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		@Nullable
		private Duration blockTimeout;

		private Builder() {
		}

		/**
		 * Provide the HTTP client to perform requests through.
		 * @param clientAdapter a client adapted to {@link HttpClientAdapter}
		 * @return this same builder instance
		 */
		public Builder clientAdapter(HttpClientAdapter clientAdapter) {
			this.clientAdapter = clientAdapter;
			return this;
		}

		/**
		 * Register a custom argument resolver, invoked ahead of default resolvers.
		 * @param resolver the resolver to add
		 * @return this same builder instance
		 */
		public Builder customArgumentResolver(HttpServiceArgumentResolver resolver) {
			this.customArgumentResolvers.add(resolver);
			return this;
		}

		/**
		 * Register a custom {@link HttpRequestValuesResolver}, invoked ahead of
		 * default resolvers.
		 * @param resolver the resolver to add
		 * @return this same builder instance
		 */
		public Builder customRequestValuesResolver(HttpRequestValuesResolver resolver) {
			this.customRequestValuesProcessors.add(resolver);
			return this;
		}

		/**
		 * Set the {@link ConversionService} to use where input values need to
		 * be formatted as Strings.
		 * <p>By default this is {@link DefaultFormattingConversionService}.
		 * @return this same builder instance
		 */
		public Builder conversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
			return this;
		}

		/**
		 * Set the {@link StringValueResolver} to use for resolving placeholders
		 * and expressions embedded in {@link HttpExchange#url()}.
		 * @param embeddedValueResolver the resolver to use
		 * @return this same builder instance
		 */
		public Builder embeddedValueResolver(StringValueResolver embeddedValueResolver) {
			this.embeddedValueResolver = embeddedValueResolver;
			return this;
		}

		/**
		 * Set the {@link ReactiveAdapterRegistry} to use to support different
		 * asynchronous types for HTTP service method return values.
		 * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
		 * @return this same builder instance
		 */
		public Builder reactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
			this.reactiveAdapterRegistry = registry;
			return this;
		}

		/**
		 * Configure how long to block for the response of an HTTP service method
		 * with a synchronous (blocking) method signature.
		 * <p>By default this is not set, in which case the behavior depends on
		 * connection and request timeout settings of the underlying HTTP client.
		 * We recommend configuring timeout values directly on the underlying HTTP
		 * client, which provides more control over such settings.
		 * @param blockTimeout the timeout value
		 * @return this same builder instance
		 */
		public Builder blockTimeout(@Nullable Duration blockTimeout) {
			this.blockTimeout = blockTimeout;
			return this;
		}

		/**
		 * Build the {@link HttpServiceProxyFactory} instance.
		 */
		public HttpServiceProxyFactory build() {
			Assert.notNull(this.clientAdapter, "HttpClientAdapter is required");

			return new HttpServiceProxyFactory(
					this.clientAdapter, initArgumentResolvers(), initRequestValuesResolvers(),
					this.reactiveAdapterRegistry, this.blockTimeout);
		}

		private List<HttpServiceArgumentResolver> initArgumentResolvers() {

			// Custom
			List<HttpServiceArgumentResolver> resolvers = new ArrayList<>(this.customArgumentResolvers);

			ConversionService service = (this.conversionService != null ?
					this.conversionService : new DefaultFormattingConversionService());

			// Annotation-based
			resolvers.add(new RequestHeaderArgumentResolver(service));
			resolvers.add(new RequestBodyArgumentResolver(this.reactiveAdapterRegistry));
			resolvers.add(new PathVariableArgumentResolver(service));
			resolvers.add(new RequestParamArgumentResolver(service));
			resolvers.add(new RequestPartArgumentResolver(this.reactiveAdapterRegistry));
			resolvers.add(new CookieValueArgumentResolver(service));
			resolvers.add(new RequestAttributeArgumentResolver());

			// Specific type
			resolvers.add(new UrlArgumentResolver());
			resolvers.add(new HttpMethodArgumentResolver());
			resolvers.add(new MultipartFileArgumentResolver());

			return resolvers;
		}

		private List<HttpRequestValuesResolver> initRequestValuesResolvers() {

			// Custom
			List<HttpRequestValuesResolver> processors = new ArrayList<>(this.customRequestValuesProcessors);

			// @HttpExchange based annotations
			processors.add(new HttpExchangeRequestValuesResolver(this.embeddedValueResolver));

			return processors;
		}
	}

	/**
	 * {@link MethodInterceptor} that invokes an {@link HttpServiceMethod}.
	 */
	private static final class HttpServiceMethodInterceptor implements MethodInterceptor {

		private final Map<Method, HttpServiceMethod> httpServiceMethods;

		private HttpServiceMethodInterceptor(List<HttpServiceMethod> methods) {
			this.httpServiceMethods = methods.stream()
					.collect(Collectors.toMap(HttpServiceMethod::getMethod, Function.identity()));
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Method method = invocation.getMethod();
			HttpServiceMethod httpServiceMethod = this.httpServiceMethods.get(method);
			if (httpServiceMethod != null) {
				if (KotlinDetector.isSuspendingFunction(method)) {
					return KotlinDelegate.invokeSuspendingFunction(invocation, httpServiceMethod);
				}
				return httpServiceMethod.invoke(invocation.getArguments());
			}
			if (method.isDefault()) {
				if (invocation instanceof ReflectiveMethodInvocation reflectiveMethodInvocation) {
					Object proxy = reflectiveMethodInvocation.getProxy();
					return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
				}
			}
			throw new IllegalStateException("Unexpected method invocation: " + method);
		}
	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	@SuppressWarnings("unchecked")
	private static class KotlinDelegate {

		public static Object invokeSuspendingFunction(MethodInvocation invocation, HttpServiceMethod httpServiceMethod) {
			Object[] rawArguments = invocation.getArguments();
			Object[] arguments = resolveArguments(rawArguments);
			Continuation<Object> continuation = (Continuation<Object>) rawArguments[rawArguments.length - 1];
			Mono<Object> wrapped = (Mono<Object>) httpServiceMethod.invoke(arguments);
			return MonoKt.awaitSingleOrNull(wrapped, continuation);
		}

		private static Object[] resolveArguments(Object[] args) {
			Object[] functionArgs = new Object[args.length - 1];
			System.arraycopy(args, 0, functionArgs, 0, args.length - 1);
			return functionArgs;
		}
	}

}
