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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

/**
 * Factory for creating a client proxy given an RSocket service interface with
 * {@link RSocketExchange @RSocketExchange} methods.
 *
 * <p>This class is intended to be declared as a bean in Spring configuration.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public final class RSocketServiceProxyFactory implements InitializingBean, EmbeddedValueResolverAware {

	@Nullable
	private final BuilderInitializedFactory builderInitializedFactory;

	@Nullable
	private final BeanStyleFactory beanStyleFactory;


	/**
	 * Create an instance with the underlying RSocketRequester to perform requests with.
	 * @param rsocketRequester the requester to use
	 * @deprecated in favor of using the Builder to initialize the
	 * RSocketServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC2", forRemoval = true)
	public RSocketServiceProxyFactory(RSocketRequester rsocketRequester) {
		this.beanStyleFactory = new BeanStyleFactory(rsocketRequester);
		this.builderInitializedFactory = null;
	}

	private RSocketServiceProxyFactory(
			RSocketRequester rsocketRequester, List<RSocketServiceArgumentResolver> argumentResolvers,
			@Nullable StringValueResolver embeddedValueResolver,
			ReactiveAdapterRegistry reactiveAdapterRegistry, Duration blockTimeout) {

		this.beanStyleFactory = null;
		this.builderInitializedFactory = new BuilderInitializedFactory(
				rsocketRequester, argumentResolvers, embeddedValueResolver, reactiveAdapterRegistry, blockTimeout);
	}


	/**
	 * Register a custom argument resolver, invoked ahead of default resolvers.
	 * @param resolver the resolver to add
	 * @deprecated in favor of using the Builder to initialize the
	 * RSocketServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC2", forRemoval = true)
	public void addCustomArgumentResolver(RSocketServiceArgumentResolver resolver) {
		Assert.state(this.beanStyleFactory != null, "RSocketServiceProxyFactory was created through the builder");
		this.beanStyleFactory.addCustomArgumentResolver(resolver);
	}

	/**
	 * Set the custom argument resolvers to use, ahead of default resolvers.
	 * @param resolvers the resolvers to use
	 * @deprecated in favor of using the Builder to initialize the
	 * RSocketServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC2", forRemoval = true)
	public void setCustomArgumentResolvers(List<RSocketServiceArgumentResolver> resolvers) {
		Assert.state(this.beanStyleFactory != null, "RSocketServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setCustomArgumentResolvers(resolvers);
	}

	/**
	 * Set the StringValueResolver to use for resolving placeholders and
	 * expressions in {@link RSocketExchange#value()}.
	 * @param resolver the resolver to use
	 * @deprecated in favor of using the Builder to initialize the
	 * RSocketServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC2", forRemoval = true)
	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		if (this.beanStyleFactory != null) {
			this.beanStyleFactory.setEmbeddedValueResolver(resolver);
		}
	}

	/**
	 * Set the {@link ReactiveAdapterRegistry} to use to support different
	 * asynchronous types for RSocket service method return values.
	 * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
	 * @deprecated in favor of using the Builder to initialize the
	 * RSocketServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC2", forRemoval = true)
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		Assert.state(this.beanStyleFactory != null, "RSocketServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setReactiveAdapterRegistry(registry);
	}

	/**
	 * Configure how long to wait for a response for an RSocket service method
	 * with a synchronous (blocking) method signature.
	 * <p>By default this is 5 seconds.
	 * @param blockTimeout the timeout value
	 * @deprecated in favor of using the Builder to initialize the
	 * RSocketServiceProxyFactory instance.
	 */
	@Deprecated(since = "6.0.0-RC2", forRemoval = true)
	public void setBlockTimeout(Duration blockTimeout) {
		Assert.state(this.beanStyleFactory != null, "RSocketServiceProxyFactory was created through the builder");
		this.beanStyleFactory.setBlockTimeout(blockTimeout);
	}


	@Override
	@Deprecated
	public void afterPropertiesSet() throws Exception {
		if (this.beanStyleFactory != null) {
			this.beanStyleFactory.afterPropertiesSet();
		}
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
		if (this.builderInitializedFactory != null) {
			return this.builderInitializedFactory.createClient(serviceType);
		}
		else if (this.beanStyleFactory != null) {
			return this.beanStyleFactory.createClient(serviceType);
		}
		else {
			throw new IllegalStateException("Expected Builder initialized or Bean-style delegate");
		}
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
		private Duration blockTimeout = Duration.ofSeconds(5);

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
		 * Configure how long to wait for a response for an HTTP service method
		 * with a synchronous (blocking) method signature.
		 * <p>By default this is 5 seconds.
		 * @param blockTimeout the timeout value
		 * @return this same builder instance
		 */
		public Builder blockTimeout(Duration blockTimeout) {
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
					this.embeddedValueResolver, this.reactiveAdapterRegistry,
					(this.blockTimeout != null ? this.blockTimeout : Duration.ofSeconds(5)));
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


	/**
	 * Temporary class until bean-style initialization is removed.
	 */
	private static final class BuilderInitializedFactory {

		private final RSocketRequester rsocketRequester;

		private final List<RSocketServiceArgumentResolver> argumentResolvers;

		@Nullable
		private final StringValueResolver embeddedValueResolver;

		private final ReactiveAdapterRegistry reactiveAdapterRegistry;

		private final Duration blockTimeout;


		public BuilderInitializedFactory(
				RSocketRequester rsocketRequester, List<RSocketServiceArgumentResolver> argumentResolvers,
				@Nullable StringValueResolver embeddedValueResolver,
				ReactiveAdapterRegistry reactiveAdapterRegistry, Duration blockTimeout) {

			this.rsocketRequester = rsocketRequester;
			this.argumentResolvers = argumentResolvers;
			this.embeddedValueResolver = embeddedValueResolver;
			this.reactiveAdapterRegistry = reactiveAdapterRegistry;
			this.blockTimeout = blockTimeout;
		}


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
	}


	/**
	 * Temporary class to support bean-style initialization during deprecation period.
	 */
	private static final class BeanStyleFactory implements InitializingBean, EmbeddedValueResolverAware {

		private final RSocketRequester rsocketRequester;

		@Nullable
		private List<RSocketServiceArgumentResolver> customArgumentResolvers;

		@Nullable
		private List<RSocketServiceArgumentResolver> argumentResolvers;

		@Nullable
		private StringValueResolver embeddedValueResolver;

		private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

		private Duration blockTimeout = Duration.ofSeconds(5);


		public BeanStyleFactory(RSocketRequester rsocketRequester) {
			Assert.notNull(rsocketRequester, "RSocketRequester is required");
			this.rsocketRequester = rsocketRequester;
		}


		public void addCustomArgumentResolver(RSocketServiceArgumentResolver resolver) {
			if (this.customArgumentResolvers == null) {
				this.customArgumentResolvers = new ArrayList<>();
			}
			this.customArgumentResolvers.add(resolver);
		}

		public void setCustomArgumentResolvers(List<RSocketServiceArgumentResolver> resolvers) {
			this.customArgumentResolvers = new ArrayList<>(resolvers);
		}

		@Override
		public void setEmbeddedValueResolver(StringValueResolver resolver) {
			this.embeddedValueResolver = resolver;
		}

		public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
			this.reactiveAdapterRegistry = registry;
		}

		public void setBlockTimeout(Duration blockTimeout) {
			this.blockTimeout = blockTimeout;
		}


		@Override
		public void afterPropertiesSet() {
			this.argumentResolvers = initArgumentResolvers();
		}

		private List<RSocketServiceArgumentResolver> initArgumentResolvers() {
			List<RSocketServiceArgumentResolver> resolvers = new ArrayList<>();

			// Custom
			if (this.customArgumentResolvers != null) {
				resolvers.addAll(this.customArgumentResolvers);
			}

			// Annotation-based
			resolvers.add(new PayloadArgumentResolver(this.reactiveAdapterRegistry, false));
			resolvers.add(new DestinationVariableArgumentResolver());

			// Type-based
			resolvers.add(new MetadataArgumentResolver());

			// Fallback
			resolvers.add(new PayloadArgumentResolver(this.reactiveAdapterRegistry, true));

			return resolvers;
		}


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
	}

}
