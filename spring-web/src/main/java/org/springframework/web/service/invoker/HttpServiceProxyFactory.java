/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Factory for creating a client proxy given an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public final class HttpServiceProxyFactory {

	private final HttpClientAdapter clientAdapter;

	private final List<HttpServiceArgumentResolver> argumentResolvers;

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;

	private final Duration blockTimeout;


	private HttpServiceProxyFactory(
			HttpClientAdapter clientAdapter, List<HttpServiceArgumentResolver> argumentResolvers,
			ReactiveAdapterRegistry reactiveAdapterRegistry, Duration blockTimeout) {

		this.clientAdapter = clientAdapter;
		this.argumentResolvers = argumentResolvers;
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

		List<HttpServiceMethod> methods =
				MethodIntrospector.selectMethods(serviceType, this::isExchangeMethod)
						.stream()
						.map(method ->
								new HttpServiceMethod(
										method, serviceType, this.argumentResolvers,
										this.clientAdapter, this.reactiveAdapterRegistry, this.blockTimeout))
						.toList();

		return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(methods));
	}

	private boolean isExchangeMethod(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
	}


	/**
	 * Return a builder for an {@link HttpServiceProxyFactory}.
	 * @param adapter an adapter for the underlying HTTP client
	 * @return the builder
	 */
	public static Builder builder(HttpClientAdapter adapter) {
		return new Builder(adapter);
	}


	/**
	 * Builder for {@link HttpServiceProxyFactory}.
	 */
	public final static class Builder {

		private final HttpClientAdapter clientAdapter;

		private final List<HttpServiceArgumentResolver> customResolvers = new ArrayList<>();

		@Nullable
		private ConversionService conversionService;

		private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		private Duration blockTimeout = Duration.ofSeconds(5);

		private Builder(HttpClientAdapter clientAdapter) {
			Assert.notNull(clientAdapter, "HttpClientAdapter is required");
			this.clientAdapter = clientAdapter;
		}

		/**
		 * Register a custom argument resolver. This will be inserted ahead of
		 * default resolvers.
		 * @return the same builder instance
		 */
		public Builder addCustomResolver(HttpServiceArgumentResolver resolver) {
			this.customResolvers.add(resolver);
			return this;
		}

		/**
		 * Set the {@link ConversionService} to use where input values need to
		 * be formatted as Strings.
		 * <p>By default this is {@link DefaultFormattingConversionService}.
		 * @return the same builder instance
		 */
		public Builder setConversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
			return this;
		}

		/**
		 * Set the {@link ReactiveAdapterRegistry} to use to support different
		 * asynchronous types for HTTP service method return values.
		 * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
		 * @return the same builder instance
		 */
		public Builder setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
			this.reactiveAdapterRegistry = registry;
			return this;
		}

		/**
		 * Configure how long to wait for a response for an HTTP service method
		 * with a synchronous (blocking) method signature.
		 * <p>By default this is 5 seconds.
		 * @param blockTimeout the timeout value
		 * @return the same builder instance
		 */
		public Builder setBlockTimeout(Duration blockTimeout) {
			this.blockTimeout = blockTimeout;
			return this;
		}

		/**
		 * Build and return the {@link HttpServiceProxyFactory} instance.
		 */
		public HttpServiceProxyFactory build() {

			ConversionService conversionService = initConversionService();
			List<HttpServiceArgumentResolver> resolvers = initArgumentResolvers(conversionService);

			return new HttpServiceProxyFactory(
					this.clientAdapter, resolvers, this.reactiveAdapterRegistry, this.blockTimeout);
		}

		private ConversionService initConversionService() {
			return (this.conversionService != null ?
					this.conversionService : new DefaultFormattingConversionService());
		}

		private List<HttpServiceArgumentResolver> initArgumentResolvers(ConversionService conversionService) {
			List<HttpServiceArgumentResolver> resolvers = new ArrayList<>(this.customResolvers);
			resolvers.add(new RequestHeaderArgumentResolver(conversionService));
			resolvers.add(new RequestBodyArgumentResolver(this.reactiveAdapterRegistry));
			resolvers.add(new PathVariableArgumentResolver(conversionService));
			resolvers.add(new RequestParamArgumentResolver(conversionService));
			resolvers.add(new CookieValueArgumentResolver(conversionService));
			resolvers.add(new UrlArgumentResolver());
			resolvers.add(new HttpMethodArgumentResolver());
			return resolvers;
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
		public Object invoke(MethodInvocation invocation) {
			Method method = invocation.getMethod();
			HttpServiceMethod httpServiceMethod = this.httpServiceMethods.get(method);
			return httpServiceMethod.invoke(invocation.getArguments());
		}
	}

}
