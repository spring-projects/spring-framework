/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * A MockMvcBuilder that accepts {@code @Controller} registrations thus allowing
 * full control over the instantiation and the initialization of controllers and
 * their dependencies similar to plain unit tests, and also making it possible
 * to test one controller at a time.
 *
 * <p>This builder creates the minimum infrastructure required by the
 * {@link DispatcherServlet} to serve requests with annotated controllers and
 * also provides methods to customize it. The resulting configuration and
 * customizations possible are equivalent to using the MVC Java config except
 * using builder style methods.
 *
 * <p>To configure view resolution, either select a "fixed" view to use for every
 * performed request (see {@link #setSingleView(View)}) or provide a list of
 * {@code ViewResolver}'s, see {@link #setViewResolvers(ViewResolver...)}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandaloneMockMvcBuilder extends DefaultMockMvcBuilder<StandaloneMockMvcBuilder> {

	private final Object[] controllers;

	private List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();

	private final List<MappedInterceptor> mappedInterceptors = new ArrayList<MappedInterceptor>();

	private Validator validator = null;

	private ContentNegotiationManager contentNegotiationManager;

	private FormattingConversionService conversionService = null;

	private List<HandlerExceptionResolver> handlerExceptionResolvers = new ArrayList<HandlerExceptionResolver>();

	private Long asyncRequestTimeout;

	private List<ViewResolver> viewResolvers;

	private LocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

	private FlashMapManager flashMapManager = null;

	private boolean useSuffixPatternMatch = true;

	private boolean useTrailingSlashPatternMatch = true;


	/**
	 * Protected constructor. Not intended for direct instantiation.
	 * @see MockMvcBuilders#standaloneSetup(Object...)
	 */
	protected StandaloneMockMvcBuilder(Object... controllers) {
		super(new StubWebApplicationContext(new MockServletContext()));
		Assert.isTrue(!ObjectUtils.isEmpty(controllers), "At least one controller is required");
		this.controllers = controllers;
	}

	/**
	 * Set the message converters to use in argument resolvers and in return value
	 * handlers, which support reading and/or writing to the body of the request
	 * and response. If no message converters are added to the list, a default
	 * list of converters is added instead.
	 */
	public StandaloneMockMvcBuilder setMessageConverters(HttpMessageConverter<?>...messageConverters) {
		this.messageConverters = Arrays.asList(messageConverters);
		return this;
	}

	/**
	 * Provide a custom {@link Validator} instead of the one created by default.
	 * The default implementation used, assuming JSR-303 is on the classpath, is
	 * {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}.
	 */
	public StandaloneMockMvcBuilder setValidator(Validator validator) {
		this.validator = validator;
		return this;
	}

	/**
	 * Provide a conversion service with custom formatters and converters.
	 * If not set, a {@link DefaultFormattingConversionService} is used by default.
	 */
	public StandaloneMockMvcBuilder setConversionService(FormattingConversionService conversionService) {
		this.conversionService = conversionService;
		return this;
	}

	/**
	 * Add interceptors mapped to all incoming requests.
	 */
	public StandaloneMockMvcBuilder addInterceptors(HandlerInterceptor... interceptors) {
		addMappedInterceptors(null, interceptors);
		return this;
	}

	/**
	 * Add interceptors mapped to a set of path patterns.
	 */
	public StandaloneMockMvcBuilder addMappedInterceptors(String[] pathPatterns, HandlerInterceptor... interceptors) {
		for (HandlerInterceptor interceptor : interceptors) {
			this.mappedInterceptors.add(new MappedInterceptor(pathPatterns, interceptor));
		}
		return this;
	}

	/**
	 * Set a ContentNegotiationManager.
	 */
	protected StandaloneMockMvcBuilder setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
		return this;
	}

	/**
	 * Specify the timeout value for async execution. In Spring MVC Test, this
	 * value is used to determine how to long to wait for async execution to
	 * complete so that a test can verify the results synchronously.
	 * @param timeout the timeout value in milliseconds
	 */
	public StandaloneMockMvcBuilder setAsyncRequestTimeout(long timeout) {
		this.asyncRequestTimeout = timeout;
		return this;
	}

	/**
	 * Provide custom resolvers for controller method arguments.
	 */
	public StandaloneMockMvcBuilder setCustomArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers) {
		this.customArgumentResolvers = Arrays.asList(argumentResolvers);
		return this;
	}

	/**
	 * Provide custom handlers for controller method return values.
	 */
	public StandaloneMockMvcBuilder setCustomReturnValueHandlers(HandlerMethodReturnValueHandler... handlers) {
		this.customReturnValueHandlers = Arrays.asList(handlers);
		return this;
	}


	/**
	 * Set the HandlerExceptionResolver types to use.
	 */
	public StandaloneMockMvcBuilder setHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.handlerExceptionResolvers = exceptionResolvers;
		return this;
	}

	/**
	 * Set up view resolution with the given {@link ViewResolver}s.
	 * If not set, an {@link InternalResourceViewResolver} is used by default.
	 */
	public StandaloneMockMvcBuilder setViewResolvers(ViewResolver...resolvers) {
		this.viewResolvers = Arrays.asList(resolvers);
		return this;
	}

	/**
	 * Sets up a single {@link ViewResolver} that always returns the provided
	 * view instance. This is a convenient shortcut if you need to use one
	 * View instance only -- e.g. rendering generated content (JSON, XML, Atom).
	 */
	public StandaloneMockMvcBuilder setSingleView(View view) {
		this.viewResolvers = Collections.<ViewResolver>singletonList(new StaticViewResolver(view));
		return this;
	}

	/**
	 * Provide a LocaleResolver instance.
	 * If not provided, the default one used is {@link AcceptHeaderLocaleResolver}.
	 */
	public StandaloneMockMvcBuilder setLocaleResolver(LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
		return this;
	}

	/**
	 * Provide a custom FlashMapManager instance.
	 * If not provided, {@code SessionFlashMapManager} is used by default.
	 */
	public StandaloneMockMvcBuilder setFlashMapManager(FlashMapManager flashMapManager) {
		this.flashMapManager = flashMapManager;
		return this;
	}

	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>The default value is {@code true}.
	 */
	public StandaloneMockMvcBuilder setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		return this;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * <p>The default value is {@code true}.
	 */
	public StandaloneMockMvcBuilder setUseTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
		this.useTrailingSlashPatternMatch = useTrailingSlashPatternMatch;
		return this;
	}

	@Override
	protected void initWebAppContext(WebApplicationContext cxt) {
		StubWebApplicationContext mockCxt = (StubWebApplicationContext) cxt;
		registerMvcSingletons(mockCxt);
		cxt.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCxt);
	}

	private void registerMvcSingletons(StubWebApplicationContext cxt) {

		StandaloneConfiguration configuration = new StandaloneConfiguration();

		RequestMappingHandlerMapping handlerMapping = configuration.requestMappingHandlerMapping();
		handlerMapping.setServletContext(cxt.getServletContext());
		handlerMapping.setApplicationContext(cxt);
		cxt.addBean("requestMappingHandlerMapping", handlerMapping);

		RequestMappingHandlerAdapter handlerAdapter = configuration.requestMappingHandlerAdapter();
		handlerAdapter.setServletContext(cxt.getServletContext());
		handlerAdapter.setApplicationContext(cxt);
		handlerAdapter.afterPropertiesSet();
		cxt.addBean("requestMappingHandlerAdapter", handlerAdapter);

		cxt.addBean("handlerExceptionResolver", configuration.handlerExceptionResolver());

		cxt.addBeans(initViewResolvers(cxt));
		cxt.addBean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, this.localeResolver);
		cxt.addBean(DispatcherServlet.THEME_RESOLVER_BEAN_NAME, new FixedThemeResolver());
		cxt.addBean(DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, new DefaultRequestToViewNameTranslator());

		this.flashMapManager = new SessionFlashMapManager();
		cxt.addBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, this.flashMapManager);
	}

	private List<ViewResolver> initViewResolvers(WebApplicationContext wac) {

		this.viewResolvers = (this.viewResolvers == null) ?
				Arrays.<ViewResolver>asList(new InternalResourceViewResolver()) : this.viewResolvers;

		for (Object viewResolver : this.viewResolvers) {
			if (viewResolver instanceof WebApplicationObjectSupport) {
				((WebApplicationObjectSupport) viewResolver).setApplicationContext(wac);
			}
		}

		return this.viewResolvers;
	}


	/** Using the MVC Java configuration as the starting point for the "standalone" setup */
	private class StandaloneConfiguration extends WebMvcConfigurationSupport {

		@Override
		public RequestMappingHandlerMapping requestMappingHandlerMapping() {

			StaticRequestMappingHandlerMapping handlerMapping = new StaticRequestMappingHandlerMapping();
			handlerMapping.registerHandlers(controllers);

			handlerMapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
			handlerMapping.setUseTrailingSlashMatch(useTrailingSlashPatternMatch);
			handlerMapping.setOrder(0);
			handlerMapping.setInterceptors(getInterceptors());

			return handlerMapping;
		}

		@Override
		protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.addAll(messageConverters);
		}

		@Override
		protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.addAll(customArgumentResolvers);
		}

		@Override
		protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.addAll(customReturnValueHandlers);
		}

		@Override
		protected void addInterceptors(InterceptorRegistry registry) {
			for (MappedInterceptor interceptor : mappedInterceptors) {
				InterceptorRegistration registration = registry.addInterceptor(interceptor.getInterceptor());
				if (interceptor.getPathPatterns() != null) {
					registration.addPathPatterns(interceptor.getPathPatterns());
				}
			}
		}

		@Override
		public ContentNegotiationManager mvcContentNegotiationManager() {
			return (contentNegotiationManager != null) ? contentNegotiationManager : super.mvcContentNegotiationManager();
		}

		@Override
		public FormattingConversionService mvcConversionService() {
			return (conversionService != null) ? conversionService : super.mvcConversionService();
		}

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			if (asyncRequestTimeout != null) {
				configurer.setDefaultTimeout(asyncRequestTimeout);
			}
		}

		@Override
		public Validator mvcValidator() {
			Validator mvcValidator = (validator != null) ? validator : super.mvcValidator();
			if (mvcValidator instanceof InitializingBean) {
				try {
					((InitializingBean) mvcValidator).afterPropertiesSet();
				}
				catch (Exception e) {
					throw new BeanInitializationException("Failed to initialize Validator", e);
				}
			}
			return mvcValidator;
		}

		@Override
		protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			exceptionResolvers.addAll(StandaloneMockMvcBuilder.this.handlerExceptionResolvers);
		}
	}

	/** A {@code RequestMappingHandlerMapping} that allows registration of controllers */
	private static class StaticRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

		public void registerHandlers(Object...handlers) {
			for (Object handler : handlers) {
				super.detectHandlerMethods(handler);
			}
		}
	}

	/** A {@link ViewResolver} that always returns same View */
	private static class StaticViewResolver implements ViewResolver {

		private final View view;

		public StaticViewResolver(View view) {
			this.view = view;
		}

		@Override
		public View resolveViewName(String viewName, Locale locale) throws Exception {
			return this.view;
		}
	}

}
