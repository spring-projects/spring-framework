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

package org.springframework.messaging.rsocket.service;

import java.lang.reflect.InvocationHandler;
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
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

/**
 * Factory to create a client proxy from an RSocket service interface with
 * {@link RSocketExchange @RSocketExchange} methods.
 *
 * <p>To create an instance, use static methods to obtain a
 * {@link Builder Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public final class RSocketServiceProxyFactory {

	private final RSocketRequester rsocketRequester;

	private final List<RSocketServiceArgumentResolver> argumentResolvers;

	@Nullable
	private final StringValueResolver embeddedValueResolver;

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;

	@Nullable
	private final Duration blockTimeout;


	private RSocketServiceProxyFactory(
			RSocketRequester rsocketRequester, List<RSocketServiceArgumentResolver> argumentResolvers,
			@Nullable StringValueResolver embeddedValueResolver,
			ReactiveAdapterRegistry reactiveAdapterRegistry, @Nullable Duration blockTimeout) {

		this.rsocketRequester = rsocketRequester;
		this.argumentResolvers = argumentResolvers;
		this.embeddedValueResolver = embeddedValueResolver;
		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
		this.blockTimeout = blockTimeout;
	}


	/**
	 * Return a proxy that implements the given RSocket service interface to
	 * perform RSocket requests and retrieve responses through the configured
	 * {@link RSocketRequester}.
	 * @param serviceType the RSocket service to create a proxy for
	 * @param <S> the RSocket service type
	 * @return the created proxy
	 */
	public <S> S createClient(Class<S> serviceType) {

		List<RSocketServiceMethod> serviceMethods =
				MethodIntrospector.selectMethods(serviceType, this::isExchangeMethod).stream()
						.map(method -> createRSocketServiceMethod(serviceType, method))
						.toList();

		return ProxyFactory.getProxy(serviceType, new ServiceMethodInterceptor(serviceMethods));
	}

	private boolean isExchangeMethod(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, RSocketExchange.class);
	}

	private <S> RSocketServiceMethod createRSocketServiceMethod(Class<S> serviceType, Method method) {
		Assert.notNull(this.argumentResolvers,
				"No argument resolvers: afterPropertiesSet was not called");

		return new RSocketServiceMethod(
				method, serviceType, this.argumentResolvers, this.rsocketRequester,
				this.embeddedValueResolver, this.reactiveAdapterRegistry, this.blockTimeout);
	}


	/**
	 * Return an {@link RSocketServiceProxyFactory} builder, initialized with the
	 * given client.
	 */
	public static Builder builder(RSocketRequester requester) {
		return new Builder().rsocketRequester(requester);
	}

	/**
	 * Return an {@link RSocketServiceProxyFactory} builder.
	 */
	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Builder to create an {@link RSocketServiceProxyFactory}.
	 */
	public static final class Builder {

		@Nullable
		private RSocketRequester rsocketRequester;

		private final List<RSocketServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();

		@Nullable
		private StringValueResolver embeddedValueResolver;

		private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		@Nullable
		private Duration blockTimeout;

		private Builder() {
		}

		/**
		 * Provide the requester to perform requests through.
		 * @param requester the requester to use
		 * @return the same builder instance
		 */
		public Builder rsocketRequester(RSocketRequester requester) {
			this.rsocketRequester = requester;
			return this;
		}

		/**
		 * Register a custom argument resolver, invoked ahead of default resolvers.
		 * @param resolver the resolver to add
		 * @return the same builder instance
		 */
		public Builder customArgumentResolver(RSocketServiceArgumentResolver resolver) {
			this.customArgumentResolvers.add(resolver);
			return this;
		}

		/**
		 * Set the {@link StringValueResolver} to use for resolving placeholders
		 * and expressions embedded in {@link RSocketExchange#value()}.
		 * @param resolver the resolver to use
		 * @return this same builder instance
		 */
		public Builder embeddedValueResolver(StringValueResolver resolver) {
			this.embeddedValueResolver = resolver;
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
		 * Configure how long to block for the response of an RSocket service method
		 * with a synchronous (blocking) method signature.
		 * <p>By default this is not set, in which case the behavior depends on
		 * connection and response timeout settings of the underlying RSocket
		 * {@code ClientTransport} as well as RSocket keep-alive settings.
		 * We recommend configuring timeout values at the RSocket level which
		 * provides more control.
		 * @param blockTimeout the timeout value
		 * @return this same builder instance
		 */
		public Builder blockTimeout(@Nullable Duration blockTimeout) {
			this.blockTimeout = blockTimeout;
			return this;
		}

		/**
		 * Build the {@link RSocketServiceProxyFactory} instance.
		 */
		public RSocketServiceProxyFactory build() {
			Assert.notNull(this.rsocketRequester, "RSocketRequester is required");

			return new RSocketServiceProxyFactory(
					this.rsocketRequester, initArgumentResolvers(),
					this.embeddedValueResolver, this.reactiveAdapterRegistry, this.blockTimeout);
		}

		private List<RSocketServiceArgumentResolver> initArgumentResolvers() {

			// Custom
			List<RSocketServiceArgumentResolver> resolvers = new ArrayList<>(this.customArgumentResolvers);

			// Annotation-based
			resolvers.add(new PayloadArgumentResolver(this.reactiveAdapterRegistry, false));
			resolvers.add(new DestinationVariableArgumentResolver());

			// Type-based
			resolvers.add(new MetadataArgumentResolver());

			// Fallback
			resolvers.add(new PayloadArgumentResolver(this.reactiveAdapterRegistry, true));

			return resolvers;
		}
	}


	/**
	 * {@link MethodInterceptor} that invokes an {@link RSocketServiceMethod}.
	 */
	private static final class ServiceMethodInterceptor implements MethodInterceptor {

		private final Map<Method, RSocketServiceMethod> serviceMethods;

		private ServiceMethodInterceptor(List<RSocketServiceMethod> methods) {
			this.serviceMethods = methods.stream()
					.collect(Collectors.toMap(RSocketServiceMethod::getMethod, Function.identity()));
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Method method = invocation.getMethod();
			RSocketServiceMethod serviceMethod = this.serviceMethods.get(method);
			if (serviceMethod != null) {
				return serviceMethod.invoke(invocation.getArguments());
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

}
