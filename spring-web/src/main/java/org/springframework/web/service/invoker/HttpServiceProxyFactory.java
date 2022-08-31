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
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Factory for creating a client proxy given an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * <p>This class is intended to be declared as a bean in a Spring configuration.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see org.springframework.web.reactive.function.client.support.WebClientAdapter
 */
public final class HttpServiceProxyFactory implements InitializingBean, EmbeddedValueResolverAware {

	private final HttpClientAdapter clientAdapter;

	@Nullable
	private List<HttpServiceArgumentResolver> customArgumentResolvers;

	@Nullable
	private List<HttpServiceArgumentResolver> argumentResolvers;

	@Nullable
	private ConversionService conversionService;

	@Nullable
	private StringValueResolver embeddedValueResolver;

	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

	private Duration blockTimeout = Duration.ofSeconds(5);


	/**
	 * Create an instance with the underlying HTTP client to use.
	 * @param clientAdapter an adapter for the client
	 * @see org.springframework.web.reactive.function.client.support.WebClientAdapter#createHttpServiceProxyFactory(org.springframework.web.reactive.function.client.WebClient)
	 */
	public HttpServiceProxyFactory(HttpClientAdapter clientAdapter) {
		Assert.notNull(clientAdapter, "HttpClientAdapter is required");
		this.clientAdapter = clientAdapter;
	}


	/**
	 * Register a custom argument resolver, invoked ahead of default resolvers.
	 * @param resolver the resolver to add
	 */
	public void addCustomArgumentResolver(HttpServiceArgumentResolver resolver) {
		if (this.customArgumentResolvers == null) {
			this.customArgumentResolvers = new ArrayList<>();
		}
		this.customArgumentResolvers.add(resolver);
	}

	/**
	 * Set the custom argument resolvers to use, ahead of default resolvers.
	 * @param resolvers the resolvers to use
	 */
	public void setCustomArgumentResolvers(List<HttpServiceArgumentResolver> resolvers) {
		this.customArgumentResolvers = new ArrayList<>(resolvers);
	}

	/**
	 * Set the {@link ConversionService} to use where input values need to
	 * be formatted as Strings.
	 * <p>By default this is {@link DefaultFormattingConversionService}.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Set the StringValueResolver to use for resolving placeholders and
	 * expressions in {@link HttpExchange#url()}.
	 * @param resolver the resolver to use
	 */
	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	/**
	 * Set the {@link ReactiveAdapterRegistry} to use to support different
	 * asynchronous types for HTTP service method return values.
	 * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}

	/**
	 * Configure how long to wait for a response for an HTTP service method
	 * with a synchronous (blocking) method signature.
	 * <p>By default this is 5 seconds.
	 * @param blockTimeout the timeout value
	 */
	public void setBlockTimeout(Duration blockTimeout) {
		this.blockTimeout = blockTimeout;
	}


	@Override
	public void afterPropertiesSet() throws Exception {

		this.conversionService = (this.conversionService != null ?
				this.conversionService : new DefaultFormattingConversionService());

		this.argumentResolvers = initArgumentResolvers(this.conversionService);
	}

	private List<HttpServiceArgumentResolver> initArgumentResolvers(ConversionService conversionService) {
		List<HttpServiceArgumentResolver> resolvers = new ArrayList<>();

		// Custom
		if (this.customArgumentResolvers != null) {
			resolvers.addAll(this.customArgumentResolvers);
		}

		// Annotation-based
		resolvers.add(new RequestHeaderArgumentResolver(conversionService));
		resolvers.add(new RequestBodyArgumentResolver(this.reactiveAdapterRegistry));
		resolvers.add(new PathVariableArgumentResolver(conversionService));
		resolvers.add(new RequestParamArgumentResolver(conversionService));
		resolvers.add(new CookieValueArgumentResolver(conversionService));
		resolvers.add(new RequestAttributeArgumentResolver());

		// Specific type
		resolvers.add(new UrlArgumentResolver());
		resolvers.add(new HttpMethodArgumentResolver());

		return resolvers;
	}


	/**
	 * Return a proxy that implements the given HTTP service interface to perform
	 * HTTP requests and retrieve responses through an HTTP client.
	 * @param serviceType the HTTP service to create a proxy for
	 * @param <S> the HTTP service type
	 * @return the created proxy
	 */
	public <S> S createClient(Class<S> serviceType) {

		List<HttpServiceMethod> httpServiceMethods =
				MethodIntrospector.selectMethods(serviceType, this::isExchangeMethod).stream()
						.map(method -> createHttpServiceMethod(serviceType, method))
						.toList();

		return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(httpServiceMethods));
	}

	private boolean isExchangeMethod(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
	}

	private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
		Assert.notNull(this.argumentResolvers,
				"No argument resolvers: afterPropertiesSet was not called");

		return new HttpServiceMethod(
				method, serviceType, this.argumentResolvers, this.clientAdapter,
				this.embeddedValueResolver, this.reactiveAdapterRegistry, this.blockTimeout);
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

}
