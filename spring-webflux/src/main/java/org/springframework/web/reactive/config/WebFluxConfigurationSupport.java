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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

/**
 * The main class for Spring WebFlux configuration.
 *
 * <p>Import directly or extend and override protected methods to customize.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class WebFluxConfigurationSupport implements ApplicationContextAware {

	private static final boolean jakartaValidatorPresent =
			ClassUtils.isPresent("jakarta.validation.Validator", WebFluxConfigurationSupport.class.getClassLoader());


	@Nullable
	private Map<String, CorsConfiguration> corsConfigurations;

	@Nullable
	private PathMatchConfigurer pathMatchConfigurer;

	@Nullable
	private BlockingExecutionConfigurer blockingExecutionConfigurer;

	@Nullable
	private List<ErrorResponse.Interceptor> errorResponseInterceptors;

	@Nullable
	private ViewResolverRegistry viewResolverRegistry;

	@Nullable
	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (applicationContext != null) {
				Assert.state(!applicationContext.containsBean("mvcContentNegotiationManager"),
						"The Java/XML config for Spring MVC and Spring WebFlux cannot both be enabled, " +
						"e.g. via @EnableWebMvc and @EnableWebFlux, in the same application.");
		}
	}

	@Nullable
	public final ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Bean
	public DispatcherHandler webHandler() {
		return new DispatcherHandler();
	}

	@Bean
	@Order(0)
	public WebExceptionHandler responseStatusExceptionHandler() {
		return new WebFluxResponseStatusExceptionHandler();
	}

	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping(
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
		mapping.setOrder(0);
		mapping.setContentTypeResolver(contentTypeResolver);
		PathMatchConfigurer configurer = getPathMatchConfigurer();
		configureAbstractHandlerMapping(mapping, configurer);
		Map<String, Predicate<Class<?>>> pathPrefixes = configurer.getPathPrefixes();
		if (pathPrefixes != null) {
			mapping.setPathPrefixes(pathPrefixes);
		}

		return mapping;
	}

	@SuppressWarnings("deprecation")
	private void configureAbstractHandlerMapping(AbstractHandlerMapping mapping, PathMatchConfigurer configurer) {
		mapping.setCorsConfigurations(getCorsConfigurations());
		Boolean useTrailingSlashMatch = configurer.isUseTrailingSlashMatch();
		if (useTrailingSlashMatch != null) {
			mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
		}
		Boolean useCaseSensitiveMatch = configurer.isUseCaseSensitiveMatch();
		if (useCaseSensitiveMatch != null) {
			mapping.setUseCaseSensitiveMatch(useCaseSensitiveMatch);
		}
	}

	/**
	 * Override to plug a subclass of {@link RequestMappingHandlerMapping}.
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	@Bean
	public RequestedContentTypeResolver webFluxContentTypeResolver() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		configureContentTypeResolver(builder);
		return builder.build();
	}

	/**
	 * Override to configure how the requested content type is resolved.
	 */
	protected void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * Callback for building the global CORS configuration. This method is final.
	 * Use {@link #addCorsMappings(CorsRegistry)} to customize the CORS config.
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
	 * Override this method to configure cross-origin requests processing.
	 * @see CorsRegistry
	 */
	protected void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * Callback for building the {@link PathMatchConfigurer}. This method is
	 * final, use {@link #configurePathMatching} to customize path matching.
	 */
	protected final PathMatchConfigurer getPathMatchConfigurer() {
		if (this.pathMatchConfigurer == null) {
			this.pathMatchConfigurer = new PathMatchConfigurer();
			configurePathMatching(this.pathMatchConfigurer);
		}
		return this.pathMatchConfigurer;
	}

	/**
	 * Override to configure path matching options.
	 */
	public void configurePathMatching(PathMatchConfigurer configurer) {
	}

	@Bean
	public RouterFunctionMapping routerFunctionMapping(ServerCodecConfigurer serverCodecConfigurer) {
		RouterFunctionMapping mapping = createRouterFunctionMapping();
		mapping.setOrder(-1);  // go before RequestMappingHandlerMapping
		mapping.setMessageReaders(serverCodecConfigurer.getReaders());
		configureAbstractHandlerMapping(mapping, getPathMatchConfigurer());
		return mapping;
	}

	/**
	 * Override to plug a subclass of {@link RouterFunctionMapping}.
	 */
	protected RouterFunctionMapping createRouterFunctionMapping() {
		return new RouterFunctionMapping();
	}

	/**
	 * Return a handler mapping ordered at Integer.MAX_VALUE-1 with mapped
	 * resource handlers. To configure resource handling, override
	 * {@link #addResourceHandlers}.
	 */
	@Bean
	public HandlerMapping resourceHandlerMapping(ResourceUrlProvider resourceUrlProvider) {
		ResourceLoader resourceLoader = this.applicationContext;
		if (resourceLoader == null) {
			resourceLoader = new DefaultResourceLoader();
		}
		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(resourceLoader);
		registry.setResourceUrlProvider(resourceUrlProvider);
		addResourceHandlers(registry);

		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		if (handlerMapping != null) {
			configureAbstractHandlerMapping(handlerMapping, getPathMatchConfigurer());
		}
		else {
			handlerMapping = new EmptyHandlerMapping();
		}
		return handlerMapping;
	}

	@Bean
	public ResourceUrlProvider resourceUrlProvider() {
		return new ResourceUrlProvider();
	}

	/**
	 * Override this method to add resource handlers for serving static resources.
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxConversionService") FormattingConversionService conversionService,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver,
			@Qualifier("webFluxValidator") Validator validator) {

		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
		adapter.setMessageReaders(serverCodecConfigurer.getReaders());
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));
		adapter.setReactiveAdapterRegistry(reactiveAdapterRegistry);
		adapter.setContentTypeResolver(contentTypeResolver);

		BlockingExecutionConfigurer executorConfigurer = getBlockingExecutionConfigurer();
		if (executorConfigurer.getExecutor() != null) {
			adapter.setBlockingExecutor(executorConfigurer.getExecutor());
		}
		if (executorConfigurer.getBlockingControllerMethodPredicate() != null) {
			adapter.setBlockingMethodPredicate(executorConfigurer.getBlockingControllerMethodPredicate());
		}

		ArgumentResolverConfigurer configurer = new ArgumentResolverConfigurer();
		configureArgumentResolvers(configurer);
		adapter.setArgumentResolverConfigurer(configurer);

		return adapter;
	}

	/**
	 * Override to plug a subclass of {@link RequestMappingHandlerAdapter}.
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * Configure resolvers for custom controller method arguments.
	 */
	protected void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
	}

	/**
	 * Return the configurer for HTTP message readers and writers.
	 * <p>Use {@link #configureHttpMessageCodecs(ServerCodecConfigurer)} to
	 * configure the readers and writers.
	 */
	@Bean
	public ServerCodecConfigurer serverCodecConfigurer() {
		ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();
		configureHttpMessageCodecs(serverCodecConfigurer);
		return serverCodecConfigurer;
	}

	/**
	 * Override to plug a subclass of {@link LocaleContextResolver}.
	 */
	protected LocaleContextResolver createLocaleContextResolver() {
		return new AcceptHeaderLocaleContextResolver();
	}

	@Bean
	public LocaleContextResolver localeContextResolver() {
		return createLocaleContextResolver();
	}

	/**
	 * Override to configure the HTTP message readers and writers to use.
	 */
	protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
	}

	/**
	 * Return the {@link ConfigurableWebBindingInitializer} to use for
	 * initializing all {@link WebDataBinder} instances.
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
			FormattingConversionService webFluxConversionService, Validator webFluxValidator) {

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(webFluxConversionService);
		initializer.setValidator(webFluxValidator);
		MessageCodesResolver messageCodesResolver = getMessageCodesResolver();
		if (messageCodesResolver != null) {
			initializer.setMessageCodesResolver(messageCodesResolver);
		}
		return initializer;
	}

	/**
	 * Return a {@link FormattingConversionService} for use with annotated controllers.
	 * <p>See {@link #addFormatters} as an alternative to overriding this method.
	 */
	@Bean
	public FormattingConversionService webFluxConversionService() {
		FormattingConversionService service = new DefaultFormattingConversionService();
		addFormatters(service);
		return service;
	}

	/**
	 * Override this method to add custom {@link Converter} and/or {@link Formatter}
	 * delegates to the common {@link FormattingConversionService}.
	 * @see #webFluxConversionService()
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Return a {@link ReactiveAdapterRegistry} to adapting reactive types.
	 */
	@Bean
	public ReactiveAdapterRegistry webFluxAdapterRegistry() {
		return new ReactiveAdapterRegistry();
	}

	/**
	 * Return a global {@link Validator} instance for example for validating
	 * {@code @RequestBody} method arguments.
	 * <p>Delegates to {@link #getValidator()} first. If that returns {@code null}
	 * checks the classpath for the presence of a JSR-303 implementations
	 * before creating a {@code OptionalValidatorFactoryBean}. If a JSR-303
	 * implementation is not available, a "no-op" {@link Validator} is returned.
	 */
	@Bean
	public Validator webFluxValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (jakartaValidatorPresent) {
				try {
					validator = new OptionalValidatorFactoryBean();
				}
				catch (Throwable ex) {
					throw new BeanInitializationException("Failed to create default validator", ex);
				}
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
	 * Override this method to provide a custom {@link MessageCodesResolver}.
	 */
	@Nullable
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * Callback to build and cache the {@link BlockingExecutionConfigurer}.
	 * This method is final, but subclasses can override
	 * {@link #configureBlockingExecution}.
	 * @since 6.1
	 */
	protected final BlockingExecutionConfigurer getBlockingExecutionConfigurer() {
		if (this.blockingExecutionConfigurer == null) {
			this.blockingExecutionConfigurer = new BlockingExecutionConfigurer();
			configureBlockingExecution(this.blockingExecutionConfigurer);
		}
		return this.blockingExecutionConfigurer;
	}

	/**
	 * Override this method to configure blocking execution.
	 * @since 6.1
	 */
	protected void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
	}

	@Bean
	public HandlerFunctionAdapter handlerFunctionAdapter() {
		return new HandlerFunctionAdapter();
	}

	@Bean
	public SimpleHandlerAdapter simpleHandlerAdapter() {
		return new SimpleHandlerAdapter();
	}

	@Bean
	public WebSocketHandlerAdapter webFluxWebSocketHandlerAdapter() {
		WebSocketHandlerAdapter adapter = new WebSocketHandlerAdapter(initWebSocketService());

		// Lower the (default) priority for now, for backwards compatibility
		int defaultOrder = adapter.getOrder();
		adapter.setOrder(defaultOrder + 1);

		return adapter;
	}

	private WebSocketService initWebSocketService() {
		WebSocketService service = getWebSocketService();
		if (service == null) {
			try {
				service = new HandshakeWebSocketService();
			}
			catch (IllegalStateException ex) {
				// Don't fail, test environment perhaps
				service = new NoUpgradeStrategyWebSocketService();
			}
		}
		return service;
	}

	@Nullable
	protected WebSocketService getWebSocketService() {
		return null;
	}

	@Bean
	public ResponseEntityResultHandler responseEntityResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		return new ResponseEntityResultHandler(serverCodecConfigurer.getWriters(),
				contentTypeResolver, reactiveAdapterRegistry, getErrorResponseInterceptors());
	}

	@Bean
	public ResponseBodyResultHandler responseBodyResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			ServerCodecConfigurer serverCodecConfigurer,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		return new ResponseBodyResultHandler(serverCodecConfigurer.getWriters(),
				contentTypeResolver, reactiveAdapterRegistry, getErrorResponseInterceptors());
	}

	@Bean
	public ViewResolutionResultHandler viewResolutionResultHandler(
			@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
			@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

		ViewResolverRegistry registry = getViewResolverRegistry();
		List<ViewResolver> resolvers = registry.getViewResolvers();
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(
				resolvers, contentTypeResolver, reactiveAdapterRegistry);
		handler.setDefaultViews(registry.getDefaultViews());
		handler.setOrder(registry.getOrder());
		return handler;
	}

	@Bean
	public ServerResponseResultHandler serverResponseResultHandler(ServerCodecConfigurer serverCodecConfigurer) {
		List<ViewResolver> resolvers = getViewResolverRegistry().getViewResolvers();
		ServerResponseResultHandler handler = new ServerResponseResultHandler();
		handler.setMessageWriters(serverCodecConfigurer.getWriters());
		handler.setViewResolvers(resolvers);
		return handler;
	}

	/**
	 * Provide access to the list of {@link ErrorResponse.Interceptor}'s to apply
	 * in result handlers when rendering error responses.
	 * <p>This method cannot be overridden; use {@link #configureErrorResponseInterceptors(List)} instead.
	 * @since 6.2
	 */
	protected final List<ErrorResponse.Interceptor> getErrorResponseInterceptors() {
		if (this.errorResponseInterceptors == null) {
			this.errorResponseInterceptors = new ArrayList<>();
			configureErrorResponseInterceptors(this.errorResponseInterceptors);
		}
		return this.errorResponseInterceptors;
	}

	/**
	 * Override this method for control over the {@link ErrorResponse.Interceptor}'s
	 * to apply in result handling when rendering error responses.
	 * @param interceptors the list to add handlers to
	 * @since 6.2
	 */
	protected void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
	}

	/**
	 * Callback for building the {@link ViewResolverRegistry}. This method is final,
	 * use {@link #configureViewResolvers} to customize view resolvers.
	 */
	protected final ViewResolverRegistry getViewResolverRegistry() {
		if (this.viewResolverRegistry == null) {
			this.viewResolverRegistry = new ViewResolverRegistry(this.applicationContext);
			configureViewResolvers(this.viewResolverRegistry);
		}
		return this.viewResolverRegistry;
	}

	/**
	 * Configure view resolution for supporting template engines.
	 * @see ViewResolverRegistry
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
	}


	private static final class EmptyHandlerMapping extends AbstractHandlerMapping {

		@Override
		public Mono<Object> getHandlerInternal(ServerWebExchange exchange) {
			return Mono.empty();
		}
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


	private static final class NoUpgradeStrategyWebSocketService implements WebSocketService {

		@Override
		public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler webSocketHandler) {
			return Mono.error(new IllegalStateException("No suitable RequestUpgradeStrategy"));
		}
	}

}
