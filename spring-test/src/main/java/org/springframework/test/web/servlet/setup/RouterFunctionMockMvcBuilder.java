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

package org.springframework.test.web.servlet.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A {@code MockMvcBuilder} that accepts {@link RouterFunction} registrations
 * thus allowing full control over the instantiation and initialization of
 * router functions and their dependencies similar to plain unit tests, and also
 * making it possible to test one function at a time.
 *
 * <p>This builder creates the minimum infrastructure required by the
 * {@link DispatcherServlet} to serve requests with router functions and
 * also provides methods for customization. The resulting configuration and
 * customization options are equivalent to using MVC Java config except
 * using builder style methods.
 *
 * <p>To configure view resolution, either select a "fixed" view to use for every
 * request performed (see {@link #setSingleView(View)}) or provide a list of
 * {@code ViewResolver}s (see {@link #setViewResolvers(ViewResolver...)}).
 *
 * @author Arjen Poutsma
 * @since 6.2
 */
public class RouterFunctionMockMvcBuilder extends AbstractMockMvcBuilder<RouterFunctionMockMvcBuilder> {

	private final RouterFunction<?> routerFunction;

	private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

	private final List<MappedInterceptor> mappedInterceptors = new ArrayList<>();

	@Nullable
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	@Nullable
	private Long asyncRequestTimeout;

	@Nullable
	private List<ViewResolver> viewResolvers;

	@Nullable
	private PathPatternParser patternParser;

	private Supplier<RouterFunctionMapping> handlerMappingFactory = RouterFunctionMapping::new;


	protected RouterFunctionMockMvcBuilder(RouterFunction<?>... routerFunctions) {
		Assert.notEmpty(routerFunctions, "RouterFunctions must not be empty");

		this.routerFunction = Arrays.stream(routerFunctions).reduce(RouterFunction::andOther).orElseThrow();
	}


	/**
	 * Set the message converters to use in argument resolvers and in return value
	 * handlers, which support reading and/or writing to the body of the request
	 * and response. If no message converters are added to the list, a default
	 * list of converters is added instead.
	 */
	public RouterFunctionMockMvcBuilder setMessageConverters(HttpMessageConverter<?>...messageConverters) {
		this.messageConverters = Arrays.asList(messageConverters);
		return this;
	}

	/**
	 * Add interceptors mapped to all incoming requests.
	 */
	public RouterFunctionMockMvcBuilder addInterceptors(HandlerInterceptor... interceptors) {
		addMappedInterceptors(null, interceptors);
		return this;
	}

	/**
	 * Add interceptors mapped to a set of path patterns.
	 */
	public RouterFunctionMockMvcBuilder addMappedInterceptors(@Nullable String[] pathPatterns,
			HandlerInterceptor... interceptors) {

		for (HandlerInterceptor interceptor : interceptors) {
			this.mappedInterceptors.add(new MappedInterceptor(pathPatterns, null, interceptor));
		}
		return this;
	}

	/**
	 * Set the HandlerExceptionResolver types to use as a list.
	 */
	public RouterFunctionMockMvcBuilder setHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.handlerExceptionResolvers = exceptionResolvers;
		return this;
	}

	/**
	 * Set the HandlerExceptionResolver types to use as an array.
	 */
	public RouterFunctionMockMvcBuilder setHandlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
		this.handlerExceptionResolvers = Arrays.asList(exceptionResolvers);
		return this;
	}

	/**
	 * Configure the factory to create a custom {@link RequestMappingHandlerMapping}.
	 * @param factory the factory
	 */
	public RouterFunctionMockMvcBuilder setCustomHandlerMapping(Supplier<RouterFunctionMapping> factory) {
		this.handlerMappingFactory = factory;
		return this;
	}

	/**
	 * Set up view resolution with the given {@link ViewResolver ViewResolvers}.
	 * <p>If not set, an {@link InternalResourceViewResolver} is used by default.
	 */
	public RouterFunctionMockMvcBuilder setViewResolvers(ViewResolver...resolvers) {
		this.viewResolvers = Arrays.asList(resolvers);
		return this;
	}

	/**
	 * Set up a single {@link ViewResolver} that always returns the provided
	 * view instance.
	 * <p>This is a convenient shortcut if you need to use one {@link View}
	 * instance only &mdash; for example, rendering generated content (JSON, XML,
	 * Atom).
	 */
	public RouterFunctionMockMvcBuilder setSingleView(View view) {
		this.viewResolvers = Collections.singletonList(new StaticViewResolver(view));
		return this;
	}

	/**
	 * Specify the timeout value for async execution.
	 * <p>In Spring MVC Test, this value is used to determine how long to wait
	 * for async execution to complete so that a test can verify the results
	 * synchronously.
	 * @param timeout the timeout value in milliseconds
	 */
	public RouterFunctionMockMvcBuilder setAsyncRequestTimeout(long timeout) {
		this.asyncRequestTimeout = timeout;
		return this;
	}

	/**
	 * Enable URL path matching with parsed
	 * {@link org.springframework.web.util.pattern.PathPattern PathPatterns}
	 * instead of String pattern matching with a {@link org.springframework.util.PathMatcher}.
	 * @param parser the parser to use
	 */
	public RouterFunctionMockMvcBuilder setPatternParser(@Nullable PathPatternParser parser) {
		this.patternParser = parser;
		return this;
	}


	@Override
	protected WebApplicationContext initWebAppContext() {
		MockServletContext servletContext = new MockServletContext();
		StubWebApplicationContext wac = new StubWebApplicationContext(servletContext);
		registerRouterFunction(wac);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		return wac;
	}

	private void registerRouterFunction(StubWebApplicationContext wac) {
		HandlerFunctionConfiguration config = new HandlerFunctionConfiguration();
		config.setApplicationContext(wac);
		ServletContext sc = wac.getServletContext();

		wac.addBean("routerFunction", this.routerFunction);

		FormattingConversionService mvcConversionService = config.mvcConversionService();
		wac.addBean("mvcConversionService", mvcConversionService);
		ResourceUrlProvider resourceUrlProvider = config.mvcResourceUrlProvider();
		wac.addBean("mvcResourceUrlProvider", resourceUrlProvider);
		ContentNegotiationManager mvcContentNegotiationManager = config.mvcContentNegotiationManager();
		wac.addBean("mvcContentNegotiationManager", mvcContentNegotiationManager);

		RouterFunctionMapping hm = config.getHandlerMapping(mvcConversionService, resourceUrlProvider);
		if (sc != null) {
			hm.setServletContext(sc);
		}
		hm.setApplicationContext(wac);
		hm.afterPropertiesSet();
		wac.addBean("routerFunctionMapping", hm);

		HandlerFunctionAdapter ha = config.handlerFunctionAdapter();
		wac.addBean("handlerFunctionAdapter", ha);

		wac.addBean("handlerExceptionResolver", config.handlerExceptionResolver(mvcContentNegotiationManager));

		wac.addBeans(initViewResolvers(wac));
	}

	private List<ViewResolver> initViewResolvers(WebApplicationContext wac) {
		this.viewResolvers = (this.viewResolvers != null ? this.viewResolvers :
				Collections.singletonList(new InternalResourceViewResolver()));
		for (Object viewResolver : this.viewResolvers) {
			if (viewResolver instanceof WebApplicationObjectSupport support) {
				support.setApplicationContext(wac);
			}
		}
		return this.viewResolvers;
	}


	/** Using the MVC Java configuration as the starting point for the "standalone" setup. */
	private class HandlerFunctionConfiguration extends WebMvcConfigurationSupport {

		public RouterFunctionMapping getHandlerMapping(
				FormattingConversionService mvcConversionService,
				ResourceUrlProvider mvcResourceUrlProvider) {

			RouterFunctionMapping handlerMapping = handlerMappingFactory.get();
			handlerMapping.setOrder(0);
			handlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
			handlerMapping.setMessageConverters(getMessageConverters());
			if (patternParser != null) {
				handlerMapping.setPatternParser(patternParser);
			}
			return handlerMapping;
		}

		@Override
		protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.addAll(messageConverters);
		}

		@Override
		protected void addInterceptors(InterceptorRegistry registry) {
			for (MappedInterceptor interceptor : mappedInterceptors) {
				InterceptorRegistration registration = registry.addInterceptor(interceptor.getInterceptor());
				if (interceptor.getIncludePathPatterns() != null) {
					registration.addPathPatterns(interceptor.getIncludePathPatterns());
				}
			}
		}

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			if (asyncRequestTimeout != null) {
				configurer.setDefaultTimeout(asyncRequestTimeout);
			}
		}

		@Override
		protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			if (handlerExceptionResolvers == null) {
				return;
			}
			for (HandlerExceptionResolver resolver : handlerExceptionResolvers) {
				if (resolver instanceof ApplicationContextAware applicationContextAware) {
					ApplicationContext applicationContext = getApplicationContext();
					if (applicationContext != null) {
						applicationContextAware.setApplicationContext(applicationContext);
					}
				}
				if (resolver instanceof InitializingBean initializingBean) {
					try {
						initializingBean.afterPropertiesSet();
					}
					catch (Exception ex) {
						throw new IllegalStateException("Failure from afterPropertiesSet", ex);
					}
				}
				exceptionResolvers.add(resolver);
			}
		}
	}

}
