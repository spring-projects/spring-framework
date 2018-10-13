/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import javax.servlet.ServletContext;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.util.UrlPathHelper;

/**
 * This is the main class providing the configuration behind the MVC Java config.
 * It is typically imported by adding {@link EnableWebMvc @EnableWebMvc} to an
 * application {@link Configuration @Configuration} class. An alternative more
 * advanced option is to extend directly from this class and override methods as
 * necessary, remembering to add {@link Configuration @Configuration} to the
 * subclass and {@link Bean @Bean} to overridden {@link Bean @Bean} methods.
 * For more details see the javadoc of {@link EnableWebMvc @EnableWebMvc}.
 *
 * <p>This class registers the following {@link HandlerMapping HandlerMappings}:</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}
 * ordered at 0 for mapping requests to annotated controller methods.
 * <li>{@link HandlerMapping}
 * ordered at 1 to map URL paths directly to view names.
 * <li>{@link BeanNameUrlHandlerMapping}
 * ordered at 2 to map URL paths to controller bean names.
 * <li>{@link HandlerMapping}
 * ordered at {@code Integer.MAX_VALUE-1} to serve static resource requests.
 * <li>{@link HandlerMapping}
 * ordered at {@code Integer.MAX_VALUE} to forward requests to the default servlet.
 * </ul>
 *
 * <p>Registers these {@link HandlerAdapter HandlerAdapters}:
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}
 * for processing requests with annotated controller methods.
 * <li>{@link HttpRequestHandlerAdapter}
 * for processing requests with {@link HttpRequestHandler HttpRequestHandlers}.
 * <li>{@link SimpleControllerHandlerAdapter}
 * for processing requests with interface-based {@link Controller Controllers}.
 * </ul>
 *
 * <p>Registers a {@link HandlerExceptionResolverComposite} with this chain of
 * exception resolvers:
 * <ul>
 * <li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through
 * {@link org.springframework.web.bind.annotation.ExceptionHandler} methods.
 * <li>{@link ResponseStatusExceptionResolver} for exceptions annotated with
 * {@link org.springframework.web.bind.annotation.ResponseStatus}.
 * <li>{@link DefaultHandlerExceptionResolver} for resolving known Spring
 * exception types
 * </ul>
 *
 * <p>Registers an {@link AntPathMatcher} and a {@link UrlPathHelper}
 * to be used by:
 * <ul>
 * <li>the {@link RequestMappingHandlerMapping},
 * <li>the {@link HandlerMapping} for ViewControllers
 * <li>and the {@link HandlerMapping} for serving resources
 * </ul>
 * Note that those beans can be configured with a {@link PathMatchConfigurer}.
 *
 * <p>Both the {@link RequestMappingHandlerAdapter} and the
 * {@link ExceptionHandlerExceptionResolver} are configured with default
 * instances of the following by default:
 * <ul>
 * <li>a {@link ContentNegotiationManager}
 * <li>a {@link DefaultFormattingConversionService}
 * <li>an {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}
 * if a JSR-303 implementation is available on the classpath
 * <li>a range of {@link HttpMessageConverter HttpMessageConverters} depending on the third-party
 * libraries available on the classpath.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 3.1
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 */
public class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {

	private static final boolean romePresent;

	private static final boolean jaxb2Present;

	private static final boolean jackson2Present;

	private static final boolean jackson2XmlPresent;

	private static final boolean jackson2SmilePresent;

	private static final boolean jackson2CborPresent;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	static {
		ClassLoader classLoader = WebMvcConfigurationSupport.class.getClassLoader();
		romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
	}


	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ServletContext servletContext;

	@Nullable
	private List<Object> interceptors;

	@Nullable
	private PathMatchConfigurer pathMatchConfigurer;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private List<HandlerMethodArgumentResolver> argumentResolvers;

	@Nullable
	private List<HandlerMethodReturnValueHandler> returnValueHandlers;

	@Nullable
	private List<HttpMessageConverter<?>> messageConverters;

	@Nullable
	private Map<String, CorsConfiguration> corsConfigurations;


	/**
	 * Set the Spring {@link ApplicationContext}, e.g. for resource loading.
	 */
	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Return the associated Spring {@link ApplicationContext}.
	 * @since 4.2
	 */
	@Nullable
	public final ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * Set the {@link javax.servlet.ServletContext}, e.g. for resource handling,
	 * looking up file extensions, etc.
	 */
	@Override
	public void setServletContext(@Nullable ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Return the associated {@link javax.servlet.ServletContext}.
	 * @since 4.2
	 */
	@Nullable
	public final ServletContext getServletContext() {
		return this.servletContext;
	}


	/**
	 * Return a {@link RequestMappingHandlerMapping} ordered at 0 for mapping
	 * requests to annotated controllers.
	 */
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
		mapping.setOrder(0);
		mapping.setInterceptors(getInterceptors());
		mapping.setContentNegotiationManager(mvcContentNegotiationManager());
		mapping.setCorsConfigurations(getCorsConfigurations());

		PathMatchConfigurer configurer = getPathMatchConfigurer();

		Boolean useSuffixPatternMatch = configurer.isUseSuffixPatternMatch();
		if (useSuffixPatternMatch != null) {
			mapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
		}
		Boolean useRegisteredSuffixPatternMatch = configurer.isUseRegisteredSuffixPatternMatch();
		if (useRegisteredSuffixPatternMatch != null) {
			mapping.setUseRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch);
		}
		Boolean useTrailingSlashMatch = configurer.isUseTrailingSlashMatch();
		if (useTrailingSlashMatch != null) {
			mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
		}

		UrlPathHelper pathHelper = configurer.getUrlPathHelper();
		if (pathHelper != null) {
			mapping.setUrlPathHelper(pathHelper);
		}
		PathMatcher pathMatcher = configurer.getPathMatcher();
		if (pathMatcher != null) {
			mapping.setPathMatcher(pathMatcher);
		}
		Map<String, Predicate<Class<?>>> pathPrefixes = configurer.getPathPrefixes();
		if (pathPrefixes != null) {
			mapping.setPathPrefixes(pathPrefixes);
		}

		return mapping;
	}

	/**
	 * Protected method for plugging in a custom subclass of
	 * {@link RequestMappingHandlerMapping}.
	 * @since 4.0
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	/**
	 * Provide access to the shared handler interceptors used to configure
	 * {@link HandlerMapping} instances with.
	 * <p>This method cannot be overridden; use {@link #addInterceptors} instead.
	 */
	protected final Object[] getInterceptors() {
		if (this.interceptors == null) {
			InterceptorRegistry registry = new InterceptorRegistry();
			addInterceptors(registry);
			registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService()));
			registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider()));
			this.interceptors = registry.getInterceptors();
		}
		return this.interceptors.toArray();
	}

	/**
	 * Override this method to add Spring MVC interceptors for
	 * pre- and post-processing of controller invocation.
	 * @see InterceptorRegistry
	 */
	protected void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * Callback for building the {@link PathMatchConfigurer}.
	 * Delegates to {@link #configurePathMatch}.
	 * @since 4.1
	 */
	protected PathMatchConfigurer getPathMatchConfigurer() {
		if (this.pathMatchConfigurer == null) {
			this.pathMatchConfigurer = new PathMatchConfigurer();
			configurePathMatch(this.pathMatchConfigurer);
		}
		return this.pathMatchConfigurer;
	}

	/**
	 * Override this method to configure path matching options.
	 * @since 4.0.3
	 * @see PathMatchConfigurer
	 */
	protected void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * Return a global {@link PathMatcher} instance for path matching
	 * patterns in {@link HandlerMapping HandlerMappings}.
	 * This instance can be configured using the {@link PathMatchConfigurer}
	 * in {@link #configurePathMatch(PathMatchConfigurer)}.
	 * @since 4.1
	 */
	@Bean
	public PathMatcher mvcPathMatcher() {
		PathMatcher pathMatcher = getPathMatchConfigurer().getPathMatcher();
		return (pathMatcher != null ? pathMatcher : new AntPathMatcher());
	}

	/**
	 * Return a global {@link UrlPathHelper} instance for path matching
	 * patterns in {@link HandlerMapping HandlerMappings}.
	 * This instance can be configured using the {@link PathMatchConfigurer}
	 * in {@link #configurePathMatch(PathMatchConfigurer)}.
	 * @since 4.1
	 */
	@Bean
	public UrlPathHelper mvcUrlPathHelper() {
		UrlPathHelper pathHelper = getPathMatchConfigurer().getUrlPathHelper();
		return (pathHelper != null ? pathHelper : new UrlPathHelper());
	}

	/**
	 * Return a {@link ContentNegotiationManager} instance to use to determine
	 * requested {@linkplain MediaType media types} in a given request.
	 */
	@Bean
	public ContentNegotiationManager mvcContentNegotiationManager() {
		if (this.contentNegotiationManager == null) {
			ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer(this.servletContext);
			configurer.mediaTypes(getDefaultMediaTypes());
			configureContentNegotiation(configurer);
			this.contentNegotiationManager = configurer.buildContentNegotiationManager();
		}
		return this.contentNegotiationManager;
	}

	protected Map<String, MediaType> getDefaultMediaTypes() {
		Map<String, MediaType> map = new HashMap<>(4);
		if (romePresent) {
			map.put("atom", MediaType.APPLICATION_ATOM_XML);
			map.put("rss", MediaType.APPLICATION_RSS_XML);
		}
		if (jaxb2Present || jackson2XmlPresent) {
			map.put("xml", MediaType.APPLICATION_XML);
		}
		if (jackson2Present || gsonPresent || jsonbPresent) {
			map.put("json", MediaType.APPLICATION_JSON);
		}
		if (jackson2SmilePresent) {
			map.put("smile", MediaType.valueOf("application/x-jackson-smile"));
		}
		if (jackson2CborPresent) {
			map.put("cbor", MediaType.valueOf("application/cbor"));
		}
		return map;
	}

	/**
	 * Override this method to configure content negotiation.
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * Return a handler mapping ordered at 1 to map URL paths directly to
	 * view names. To configure view controllers, override
	 * {@link #addViewControllers}.
	 */
	@Bean
	@Nullable
	public HandlerMapping viewControllerHandlerMapping() {
		ViewControllerRegistry registry = new ViewControllerRegistry(this.applicationContext);
		addViewControllers(registry);

		AbstractHandlerMapping handlerMapping = registry.buildHandlerMapping();
		if (handlerMapping == null) {
			return null;
		}
		handlerMapping.setPathMatcher(mvcPathMatcher());
		handlerMapping.setUrlPathHelper(mvcUrlPathHelper());
		handlerMapping.setInterceptors(getInterceptors());
		handlerMapping.setCorsConfigurations(getCorsConfigurations());
		return handlerMapping;
	}

	/**
	 * Override this method to add view controllers.
	 * @see ViewControllerRegistry
	 */
	protected void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * Return a {@link BeanNameUrlHandlerMapping} ordered at 2 to map URL
	 * paths to controller bean names.
	 */
	@Bean
	public BeanNameUrlHandlerMapping beanNameHandlerMapping() {
		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
		mapping.setOrder(2);
		mapping.setInterceptors(getInterceptors());
		mapping.setCorsConfigurations(getCorsConfigurations());
		return mapping;
	}

	/**
	 * Return a handler mapping ordered at Integer.MAX_VALUE-1 with mapped
	 * resource handlers. To configure resource handling, override
	 * {@link #addResourceHandlers}.
	 */
	@Bean
	@Nullable
	public HandlerMapping resourceHandlerMapping() {
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		Assert.state(this.servletContext != null, "No ServletContext set");

		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(this.applicationContext,
				this.servletContext, mvcContentNegotiationManager(), mvcUrlPathHelper());
		addResourceHandlers(registry);

		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		if (handlerMapping == null) {
			return null;
		}
		handlerMapping.setPathMatcher(mvcPathMatcher());
		handlerMapping.setUrlPathHelper(mvcUrlPathHelper());
		handlerMapping.setInterceptors(getInterceptors());
		handlerMapping.setCorsConfigurations(getCorsConfigurations());
		return handlerMapping;
	}

	/**
	 * Override this method to add resource handlers for serving static resources.
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * A {@link ResourceUrlProvider} bean for use with the MVC dispatcher.
	 * @since 4.1
	 */
	@Bean
	public ResourceUrlProvider mvcResourceUrlProvider() {
		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		UrlPathHelper pathHelper = getPathMatchConfigurer().getUrlPathHelper();
		if (pathHelper != null) {
			urlProvider.setUrlPathHelper(pathHelper);
		}
		PathMatcher pathMatcher = getPathMatchConfigurer().getPathMatcher();
		if (pathMatcher != null) {
			urlProvider.setPathMatcher(pathMatcher);
		}
		return urlProvider;
	}

	/**
	 * Return a handler mapping ordered at Integer.MAX_VALUE with a mapped
	 * default servlet handler. To configure "default" Servlet handling,
	 * override {@link #configureDefaultServletHandling}.
	 */
	@Bean
	@Nullable
	public HandlerMapping defaultServletHandlerMapping() {
		Assert.state(this.servletContext != null, "No ServletContext set");
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(this.servletContext);
		configureDefaultServletHandling(configurer);
		return configurer.buildHandlerMapping();
	}

	/**
	 * Override this method to configure "default" Servlet handling.
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * Returns a {@link RequestMappingHandlerAdapter} for processing requests
	 * through annotated controller methods. Consider overriding one of these
	 * other more fine-grained methods:
	 * <ul>
	 * <li>{@link #addArgumentResolvers} for adding custom argument resolvers.
	 * <li>{@link #addReturnValueHandlers} for adding custom return value handlers.
	 * <li>{@link #configureMessageConverters} for adding custom message converters.
	 * </ul>
	 */
	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
		adapter.setContentNegotiationManager(mvcContentNegotiationManager());
		adapter.setMessageConverters(getMessageConverters());
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer());
		adapter.setCustomArgumentResolvers(getArgumentResolvers());
		adapter.setCustomReturnValueHandlers(getReturnValueHandlers());

		if (jackson2Present) {
			adapter.setRequestBodyAdvice(Collections.singletonList(new JsonViewRequestBodyAdvice()));
			adapter.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));
		}

		AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
		configureAsyncSupport(configurer);
		if (configurer.getTaskExecutor() != null) {
			adapter.setTaskExecutor(configurer.getTaskExecutor());
		}
		if (configurer.getTimeout() != null) {
			adapter.setAsyncRequestTimeout(configurer.getTimeout());
		}
		adapter.setCallableInterceptors(configurer.getCallableInterceptors());
		adapter.setDeferredResultInterceptors(configurer.getDeferredResultInterceptors());

		return adapter;
	}

	/**
	 * Protected method for plugging in a custom subclass of
	 * {@link RequestMappingHandlerAdapter}.
	 * @since 4.3
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * Return the {@link ConfigurableWebBindingInitializer} to use for
	 * initializing all {@link WebDataBinder} instances.
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer() {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(mvcConversionService());
		initializer.setValidator(mvcValidator());
		MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
		if (messageCodesResolver != null) {
			initializer.setMessageCodesResolver(messageCodesResolver);
		}
		return initializer;
	}

	/**
	 * Override this method to provide a custom {@link MessageCodesResolver}.
	 */
	@Nullable
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * Override this method to configure asynchronous request processing options.
	 * @see AsyncSupportConfigurer
	 */
	protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * Return a {@link FormattingConversionService} for use with annotated controllers.
	 * <p>See {@link #addFormatters} as an alternative to overriding this method.
	 */
	@Bean
	public FormattingConversionService mvcConversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		addFormatters(conversionService);
		return conversionService;
	}

	/**
	 * Override this method to add custom {@link Converter} and/or {@link Formatter}
	 * delegates to the common {@link FormattingConversionService}.
	 * @see #mvcConversionService()
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Return a global {@link Validator} instance for example for validating
	 * {@code @ModelAttribute} and {@code @RequestBody} method arguments.
	 * Delegates to {@link #getValidator()} first and if that returns {@code null}
	 * checks the classpath for the presence of a JSR-303 implementations
	 * before creating a {@code OptionalValidatorFactoryBean}.If a JSR-303
	 * implementation is not available, a no-op {@link Validator} is returned.
	 */
	@Bean
	public Validator mvcValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(className, WebMvcConfigurationSupport.class.getClassLoader());
				}
				catch (ClassNotFoundException | LinkageError ex) {
					throw new BeanInitializationException("Failed to resolve default validator class", ex);
				}
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			}
			else {
				validator = new NoOpValidator();
			}
		}
		return validator;
	}

	/**
	 * Override this method to provide a custom {@link Validator}.
	 */
	@Nullable
	protected Validator getValidator() {
		return null;
	}

	/**
	 * Provide access to the shared custom argument resolvers used by the
	 * {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}.
	 * <p>This method cannot be overridden; use {@link #addArgumentResolvers} instead.
	 * @since 4.3
	 */
	protected final List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		if (this.argumentResolvers == null) {
			this.argumentResolvers = new ArrayList<>();
			addArgumentResolvers(this.argumentResolvers);
		}
		return this.argumentResolvers;
	}

	/**
	 * Add custom {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
	 * to use in addition to the ones registered by default.
	 * <p>Custom argument resolvers are invoked before built-in resolvers except for
	 * those that rely on the presence of annotations (e.g. {@code @RequestParameter},
	 * {@code @PathVariable}, etc). The latter can be customized by configuring the
	 * {@link RequestMappingHandlerAdapter} directly.
	 * @param argumentResolvers the list of custom converters (initially an empty list)
	 */
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * Provide access to the shared return value handlers used by the
	 * {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}.
	 * <p>This method cannot be overridden; use {@link #addReturnValueHandlers} instead.
	 * @since 4.3
	 */
	protected final List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		if (this.returnValueHandlers == null) {
			this.returnValueHandlers = new ArrayList<>();
			addReturnValueHandlers(this.returnValueHandlers);
		}
		return this.returnValueHandlers;
	}

	/**
	 * Add custom {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}
	 * in addition to the ones registered by default.
	 * <p>Custom return value handlers are invoked before built-in ones except for
	 * those that rely on the presence of annotations (e.g. {@code @ResponseBody},
	 * {@code @ModelAttribute}, etc). The latter can be customized by configuring the
	 * {@link RequestMappingHandlerAdapter} directly.
	 * @param returnValueHandlers the list of custom handlers (initially an empty list)
	 */
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	/**
	 * Provides access to the shared {@link HttpMessageConverter HttpMessageConverters}
	 * used by the {@link RequestMappingHandlerAdapter} and the
	 * {@link ExceptionHandlerExceptionResolver}.
	 * <p>This method cannot be overridden; use {@link #configureMessageConverters} instead.
	 * Also see {@link #addDefaultHttpMessageConverters} for adding default message converters.
	 */
	protected final List<HttpMessageConverter<?>> getMessageConverters() {
		if (this.messageConverters == null) {
			this.messageConverters = new ArrayList<>();
			configureMessageConverters(this.messageConverters);
			if (this.messageConverters.isEmpty()) {
				addDefaultHttpMessageConverters(this.messageConverters);
			}
			extendMessageConverters(this.messageConverters);
		}
		return this.messageConverters;
	}

	/**
	 * Override this method to add custom {@link HttpMessageConverter HttpMessageConverters}
	 * to use with the {@link RequestMappingHandlerAdapter} and the
	 * {@link ExceptionHandlerExceptionResolver}.
	 * <p>Adding converters to the list turns off the default converters that would
	 * otherwise be registered by default. Also see {@link #addDefaultHttpMessageConverters}
	 * for adding default message converters.
	 * @param converters a list to add message converters to (initially an empty list)
	 */
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * Override this method to extend or modify the list of converters after it has
	 * been configured. This may be useful for example to allow default converters
	 * to be registered and then insert a custom converter through this method.
	 * @param converters the list of configured converters to extend
	 * @since 4.1.3
	 */
	protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * Adds a set of default HttpMessageConverter instances to the given list.
	 * Subclasses can call this method from {@link #configureMessageConverters}.
	 * @param messageConverters the list to add the default message converters to
	 */
	protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);  // see SPR-7316

		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(stringHttpMessageConverter);
		messageConverters.add(new ResourceHttpMessageConverter());
		messageConverters.add(new ResourceRegionHttpMessageConverter());
		try {
			messageConverters.add(new SourceHttpMessageConverter<>());
		}
		catch (Throwable ex) {
			// Ignore when no TransformerFactory implementation is available...
		}
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		if (romePresent) {
			messageConverters.add(new AtomFeedHttpMessageConverter());
			messageConverters.add(new RssChannelHttpMessageConverter());
		}

		if (jackson2XmlPresent) {
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.xml();
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			messageConverters.add(new MappingJackson2XmlHttpMessageConverter(builder.build()));
		}
		else if (jaxb2Present) {
			messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
		}

		if (jackson2Present) {
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			messageConverters.add(new MappingJackson2HttpMessageConverter(builder.build()));
		}
		else if (gsonPresent) {
			messageConverters.add(new GsonHttpMessageConverter());
		}
		else if (jsonbPresent) {
			messageConverters.add(new JsonbHttpMessageConverter());
		}

		if (jackson2SmilePresent) {
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.smile();
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			messageConverters.add(new MappingJackson2SmileHttpMessageConverter(builder.build()));
		}
		if (jackson2CborPresent) {
			Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.cbor();
			if (this.applicationContext != null) {
				builder.applicationContext(this.applicationContext);
			}
			messageConverters.add(new MappingJackson2CborHttpMessageConverter(builder.build()));
		}
	}

	/**
	 * Return an instance of {@link CompositeUriComponentsContributor} for use with
	 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder}.
	 * @since 4.0
	 */
	@Bean
	public CompositeUriComponentsContributor mvcUriComponentsContributor() {
		return new CompositeUriComponentsContributor(
				requestMappingHandlerAdapter().getArgumentResolvers(), mvcConversionService());
	}

	/**
	 * Returns a {@link HttpRequestHandlerAdapter} for processing requests
	 * with {@link HttpRequestHandler HttpRequestHandlers}.
	 */
	@Bean
	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	/**
	 * Returns a {@link SimpleControllerHandlerAdapter} for processing requests
	 * with interface-based controllers.
	 */
	@Bean
	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	/**
	 * Returns a {@link HandlerExceptionResolverComposite} containing a list of exception
	 * resolvers obtained either through {@link #configureHandlerExceptionResolvers} or
	 * through {@link #addDefaultHandlerExceptionResolvers}.
	 * <p><strong>Note:</strong> This method cannot be made final due to CGLIB constraints.
	 * Rather than overriding it, consider overriding {@link #configureHandlerExceptionResolvers}
	 * which allows for providing a list of resolvers.
	 */
	@Bean
	public HandlerExceptionResolver handlerExceptionResolver() {
		List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<>();
		configureHandlerExceptionResolvers(exceptionResolvers);
		if (exceptionResolvers.isEmpty()) {
			addDefaultHandlerExceptionResolvers(exceptionResolvers);
		}
		extendHandlerExceptionResolvers(exceptionResolvers);
		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		composite.setOrder(0);
		composite.setExceptionResolvers(exceptionResolvers);
		return composite;
	}

	/**
	 * Override this method to configure the list of
	 * {@link HandlerExceptionResolver HandlerExceptionResolvers} to use.
	 * <p>Adding resolvers to the list turns off the default resolvers that would otherwise
	 * be registered by default. Also see {@link #addDefaultHandlerExceptionResolvers}
	 * that can be used to add the default exception resolvers.
	 * @param exceptionResolvers a list to add exception resolvers to (initially an empty list)
	 */
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * Override this method to extend or modify the list of
	 * {@link HandlerExceptionResolver HandlerExceptionResolvers} after it has been configured.
	 * <p>This may be useful for example to allow default resolvers to be registered
	 * and then insert a custom one through this method.
	 * @param exceptionResolvers the list of configured resolvers to extend.
	 * @since 4.3
	 */
	protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * A method available to subclasses for adding default
	 * {@link HandlerExceptionResolver HandlerExceptionResolvers}.
	 * <p>Adds the following exception resolvers:
	 * <ul>
	 * <li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through
	 * {@link org.springframework.web.bind.annotation.ExceptionHandler} methods.
	 * <li>{@link ResponseStatusExceptionResolver} for exceptions annotated with
	 * {@link org.springframework.web.bind.annotation.ResponseStatus}.
	 * <li>{@link DefaultHandlerExceptionResolver} for resolving known Spring exception types
	 * </ul>
	 */
	protected final void addDefaultHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		ExceptionHandlerExceptionResolver exceptionHandlerResolver = createExceptionHandlerExceptionResolver();
		exceptionHandlerResolver.setContentNegotiationManager(mvcContentNegotiationManager());
		exceptionHandlerResolver.setMessageConverters(getMessageConverters());
		exceptionHandlerResolver.setCustomArgumentResolvers(getArgumentResolvers());
		exceptionHandlerResolver.setCustomReturnValueHandlers(getReturnValueHandlers());
		if (jackson2Present) {
			exceptionHandlerResolver.setResponseBodyAdvice(
					Collections.singletonList(new JsonViewResponseBodyAdvice()));
		}
		if (this.applicationContext != null) {
			exceptionHandlerResolver.setApplicationContext(this.applicationContext);
		}
		exceptionHandlerResolver.afterPropertiesSet();
		exceptionResolvers.add(exceptionHandlerResolver);

		ResponseStatusExceptionResolver responseStatusResolver = new ResponseStatusExceptionResolver();
		responseStatusResolver.setMessageSource(this.applicationContext);
		exceptionResolvers.add(responseStatusResolver);

		exceptionResolvers.add(new DefaultHandlerExceptionResolver());
	}

	/**
	 * Protected method for plugging in a custom subclass of
	 * {@link ExceptionHandlerExceptionResolver}.
	 * @since 4.3
	 */
	protected ExceptionHandlerExceptionResolver createExceptionHandlerExceptionResolver() {
		return new ExceptionHandlerExceptionResolver();
	}

	/**
	 * Register a {@link ViewResolverComposite} that contains a chain of view resolvers
	 * to use for view resolution.
	 * By default this resolver is ordered at 0 unless content negotiation view
	 * resolution is used in which case the order is raised to
	 * {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE
	 * Ordered.HIGHEST_PRECEDENCE}.
	 * <p>If no other resolvers are configured,
	 * {@link ViewResolverComposite#resolveViewName(String, Locale)} returns null in order
	 * to allow other potential {@link ViewResolver} beans to resolve views.
	 * @since 4.1
	 */
	@Bean
	public ViewResolver mvcViewResolver() {
		ViewResolverRegistry registry = new ViewResolverRegistry(
				mvcContentNegotiationManager(), this.applicationContext);
		configureViewResolvers(registry);

		if (registry.getViewResolvers().isEmpty() && this.applicationContext != null) {
			String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					this.applicationContext, ViewResolver.class, true, false);
			if (names.length == 1) {
				registry.getViewResolvers().add(new InternalResourceViewResolver());
			}
		}

		ViewResolverComposite composite = new ViewResolverComposite();
		composite.setOrder(registry.getOrder());
		composite.setViewResolvers(registry.getViewResolvers());
		if (this.applicationContext != null) {
			composite.setApplicationContext(this.applicationContext);
		}
		if (this.servletContext != null) {
			composite.setServletContext(this.servletContext);
		}
		return composite;
	}

	/**
	 * Override this method to configure view resolution.
	 * @see ViewResolverRegistry
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * Return the registered {@link CorsConfiguration} objects,
	 * keyed by path pattern.
	 * @since 4.2
	 */
	protected final Map<String, CorsConfiguration> getCorsConfigurations() {
		if (this.corsConfigurations == null) {
			CorsRegistry registry = new CorsRegistry();
			addCorsMappings(registry);
			this.corsConfigurations = registry.getCorsConfigurations();
		}
		return this.corsConfigurations;
	}

	/**
	 * Override this method to configure cross origin requests processing.
	 * @since 4.2
	 * @see CorsRegistry
	 */
	protected void addCorsMappings(CorsRegistry registry) {
	}

	@Bean
	@Lazy
	public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
		return new HandlerMappingIntrospector();
	}


	private static final class NoOpValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
		}
	}

}
