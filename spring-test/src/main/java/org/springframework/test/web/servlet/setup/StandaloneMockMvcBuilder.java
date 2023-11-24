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

package org.springframework.test.web.servlet.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.ServletContext;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;
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
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A {@code MockMvcBuilder} that accepts {@code @Controller} registrations
 * thus allowing full control over the instantiation and initialization of
 * controllers and their dependencies similar to plain unit tests, and also
 * making it possible to test one controller at a time.
 *
 * <p>This builder creates the minimum infrastructure required by the
 * {@link DispatcherServlet} to serve requests with annotated controllers and
 * also provides methods for customization. The resulting configuration and
 * customization options are equivalent to using MVC Java config except
 * using builder style methods.
 *
 * <p>To configure view resolution, either select a "fixed" view to use for every
 * request performed (see {@link #setSingleView(View)}) or provide a list of
 * {@code ViewResolver}s (see {@link #setViewResolvers(ViewResolver...)}).
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class StandaloneMockMvcBuilder extends AbstractMockMvcBuilder<StandaloneMockMvcBuilder> {

	private final List<Object> controllers;

	@Nullable
	private List<Object> controllerAdvice;

	private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

	private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<>();

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<>();

	private final List<MappedInterceptor> mappedInterceptors = new ArrayList<>();

	@Nullable
	private Validator validator;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private FormattingConversionService conversionService;

	@Nullable
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	@Nullable
	private Long asyncRequestTimeout;

	@Nullable
	private List<ViewResolver> viewResolvers;

	private LocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

	@Nullable
	private FlashMapManager flashMapManager;

	private boolean preferPathMatcher = false;

	@Nullable
	private PathPatternParser patternParser;

	private boolean useSuffixPatternMatch = false;

	private boolean useTrailingSlashPatternMatch = true;

	@Nullable
	private Boolean removeSemicolonContent;

	private final Map<String, String> placeholderValues = new HashMap<>();

	private Supplier<RequestMappingHandlerMapping> handlerMappingFactory = RequestMappingHandlerMapping::new;


	/**
	 * Protected constructor. Not intended for direct instantiation.
	 * @see MockMvcBuilders#standaloneSetup(Object...)
	 */
	protected StandaloneMockMvcBuilder(Object... controllers) {
		this.controllers = instantiateIfNecessary(controllers);
	}

	private static List<Object> instantiateIfNecessary(Object[] specified) {
		List<Object> instances = new ArrayList<>(specified.length);
		for (Object obj : specified) {
			instances.add(obj instanceof Class<?> clazz ? BeanUtils.instantiateClass(clazz) : obj);
		}
		return instances;
	}


	/**
	 * Register one or more {@link org.springframework.web.bind.annotation.ControllerAdvice}
	 * instances to be used in tests (specified {@code Class} will be turned into instance).
	 * <p>Normally {@code @ControllerAdvice} are auto-detected as long as they're declared
	 * as Spring beans. However since the standalone setup does not load any Spring config,
	 * they need to be registered explicitly here instead much like controllers.
	 * @since 4.2
	 */
	public StandaloneMockMvcBuilder setControllerAdvice(Object... controllerAdvice) {
		this.controllerAdvice = instantiateIfNecessary(controllerAdvice);
		return this;
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
	public StandaloneMockMvcBuilder addMappedInterceptors(
			@Nullable String[] pathPatterns, HandlerInterceptor... interceptors) {

		for (HandlerInterceptor interceptor : interceptors) {
			this.mappedInterceptors.add(new MappedInterceptor(pathPatterns, null, interceptor));
		}
		return this;
	}

	/**
	 * Set a ContentNegotiationManager.
	 */
	public StandaloneMockMvcBuilder setContentNegotiationManager(ContentNegotiationManager manager) {
		this.contentNegotiationManager = manager;
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
	 * Set the HandlerExceptionResolver types to use as a list.
	 */
	public StandaloneMockMvcBuilder setHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.handlerExceptionResolvers = exceptionResolvers;
		return this;
	}

	/**
	 * Set the HandlerExceptionResolver types to use as an array.
	 */
	public StandaloneMockMvcBuilder setHandlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
		this.handlerExceptionResolvers = Arrays.asList(exceptionResolvers);
		return this;
	}

	/**
	 * Set up view resolution with the given {@link ViewResolver ViewResolvers}.
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
	 * Enable URL path matching with parsed
	 * {@link org.springframework.web.util.pattern.PathPattern PathPatterns}
	 * instead of String pattern matching with a {@link org.springframework.util.PathMatcher}.
	 * @param parser the parser to use
	 * @since 5.3
	 */
	public StandaloneMockMvcBuilder setPatternParser(@Nullable PathPatternParser parser) {
		this.patternParser = parser;
		this.preferPathMatcher = (this.patternParser == null);
		return this;
	}

	/**
	 * Whether to use suffix pattern match (".*") when matching patterns to
	 * requests. If enabled a method mapped to "/users" also matches to "/users.*".
	 * <p>The default value is {@code false}.
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link RequestMappingHandlerMapping} on the deprecation of path extension
	 * config options.
	 */
	@Deprecated
	public StandaloneMockMvcBuilder setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		this.preferPathMatcher |= useSuffixPatternMatch;
		return this;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a method mapped to "/users" also matches to "/users/".
	 * @deprecated as of 6.0, see
	 * {@link PathPatternParser#setMatchOptionalTrailingSeparator(boolean)}
	 */
	@Deprecated(since = "6.0")
	public StandaloneMockMvcBuilder setUseTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
		this.useTrailingSlashPatternMatch = useTrailingSlashPatternMatch;
		return this;
	}

	/**
	 * Set if ";" (semicolon) content should be stripped from the request URI. The value,
	 * if provided, is in turn set on
	 * {@link org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)}.
	 */
	public StandaloneMockMvcBuilder setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.removeSemicolonContent = removeSemicolonContent;
		return this;
	}

	/**
	 * In a standalone setup there is no support for placeholder values embedded in
	 * request mappings. This method allows manually provided placeholder values so they
	 * can be resolved. Alternatively consider creating a test that initializes a
	 * {@link WebApplicationContext}.
	 * @since 4.2.8
	 */
	public StandaloneMockMvcBuilder addPlaceholderValue(String name, String value) {
		this.placeholderValues.put(name, value);
		return this;
	}

	/**
	 * Configure factory to create a custom {@link RequestMappingHandlerMapping}.
	 * @param factory the factory
	 * @since 5.0
	 */
	public StandaloneMockMvcBuilder setCustomHandlerMapping(Supplier<RequestMappingHandlerMapping> factory) {
		this.handlerMappingFactory = factory;
		return this;
	}


	@Override
	protected WebApplicationContext initWebAppContext() {
		MockServletContext servletContext = new MockServletContext();
		StubWebApplicationContext wac = new StubWebApplicationContext(servletContext);
		registerMvcSingletons(wac);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		return wac;
	}

	@SuppressWarnings("deprecation")
	private void registerMvcSingletons(StubWebApplicationContext wac) {
		StandaloneConfiguration config = new StandaloneConfiguration();
		config.setApplicationContext(wac);
		ServletContext sc = wac.getServletContext();

		wac.addBeans(this.controllers);
		wac.addBeans(this.controllerAdvice);

		FormattingConversionService mvcConversionService = config.mvcConversionService();
		wac.addBean("mvcConversionService", mvcConversionService);
		ResourceUrlProvider resourceUrlProvider = config.mvcResourceUrlProvider();
		wac.addBean("mvcResourceUrlProvider", resourceUrlProvider);
		ContentNegotiationManager mvcContentNegotiationManager = config.mvcContentNegotiationManager();
		wac.addBean("mvcContentNegotiationManager", mvcContentNegotiationManager);
		Validator mvcValidator = config.mvcValidator();
		wac.addBean("mvcValidator", mvcValidator);

		RequestMappingHandlerMapping hm = config.getHandlerMapping(mvcConversionService, resourceUrlProvider);
		if (sc != null) {
			hm.setServletContext(sc);
		}
		hm.setApplicationContext(wac);
		hm.afterPropertiesSet();
		wac.addBean("requestMappingHandlerMapping", hm);

		RequestMappingHandlerAdapter ha = config.requestMappingHandlerAdapter(mvcContentNegotiationManager,
				mvcConversionService, mvcValidator);
		if (sc != null) {
			ha.setServletContext(sc);
		}
		ha.setApplicationContext(wac);
		ha.afterPropertiesSet();
		wac.addBean("requestMappingHandlerAdapter", ha);

		wac.addBean("handlerExceptionResolver", config.handlerExceptionResolver(mvcContentNegotiationManager));

		wac.addBeans(initViewResolvers(wac));
		wac.addBean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, this.localeResolver);
		wac.addBean(DispatcherServlet.THEME_RESOLVER_BEAN_NAME,
				new org.springframework.web.servlet.theme.FixedThemeResolver());
		wac.addBean(DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME,
				new DefaultRequestToViewNameTranslator());

		this.flashMapManager = new SessionFlashMapManager();
		wac.addBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, this.flashMapManager);

		extendMvcSingletons(sc).forEach(wac::addBean);
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

	/**
	 * This method could be used from a subclass to register additional Spring
	 * MVC infrastructure such as additional {@code HandlerMapping},
	 * {@code HandlerAdapter}, and others.
	 * @param servletContext the ServletContext
	 * @return a map with additional MVC infrastructure object instances
	 * @since 5.1.4
	 */
	protected Map<String, Object> extendMvcSingletons(@Nullable ServletContext servletContext) {
		return Collections.emptyMap();
	}


	/** Using the MVC Java configuration as the starting point for the "standalone" setup. */
	private class StandaloneConfiguration extends WebMvcConfigurationSupport {

		@SuppressWarnings("deprecation")
		public RequestMappingHandlerMapping getHandlerMapping(
				FormattingConversionService mvcConversionService,
				ResourceUrlProvider mvcResourceUrlProvider) {

			RequestMappingHandlerMapping handlerMapping = handlerMappingFactory.get();
			handlerMapping.setEmbeddedValueResolver(new StaticStringValueResolver(placeholderValues));
			if (patternParser == null && preferPathMatcher) {
				handlerMapping.setPatternParser(null);
				handlerMapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
				if (removeSemicolonContent != null) {
					UrlPathHelper pathHelper = new UrlPathHelper();
					pathHelper.setRemoveSemicolonContent(removeSemicolonContent);
					handlerMapping.setUrlPathHelper(pathHelper);
				}
			}
			else if (patternParser != null) {
				handlerMapping.setPatternParser(patternParser);
			}
			handlerMapping.setUseTrailingSlashMatch(useTrailingSlashPatternMatch);
			handlerMapping.setOrder(0);
			handlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
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
				if (interceptor.getIncludePathPatterns() != null) {
					registration.addPathPatterns(interceptor.getIncludePathPatterns());
				}
			}
		}

		@Override
		public ContentNegotiationManager mvcContentNegotiationManager() {
			return (contentNegotiationManager != null) ? contentNegotiationManager : super.mvcContentNegotiationManager();
		}

		@Override
		public FormattingConversionService mvcConversionService() {
			return (conversionService != null ? conversionService : super.mvcConversionService());
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
			if (mvcValidator instanceof InitializingBean initializingBean) {
				try {
					initializingBean.afterPropertiesSet();
				}
				catch (Exception ex) {
					throw new BeanInitializationException("Failed to initialize Validator", ex);
				}
			}
			return mvcValidator;
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

	/**
	 * A static resolver placeholder for values embedded in request mappings.
	 */
	private static class StaticStringValueResolver implements StringValueResolver {

		private final PropertyPlaceholderHelper helper;

		private final PlaceholderResolver resolver;

		public StaticStringValueResolver(Map<String, String> values) {
			this.helper = new PropertyPlaceholderHelper("${", "}", ":", false);
			this.resolver = values::get;
		}

		@Override
		public String resolveStringValue(String strVal) throws BeansException {
			return this.helper.replacePlaceholders(strVal, this.resolver);
		}
	}


	/**
	 * A {@link ViewResolver} that always returns same View.
	 */
	private static class StaticViewResolver implements ViewResolver {

		private final View view;

		public StaticViewResolver(View view) {
			this.view = view;
		}

		@Override
		@Nullable
		public View resolveViewName(String viewName, Locale locale) {
			return this.view;
		}
	}

}
