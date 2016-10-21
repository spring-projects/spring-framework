/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.Jackson2ServerHttpMessageReader;
import org.springframework.http.codec.Jackson2ServerHttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.CompositeContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * The main class for Spring Web Reactive configuration.
 *
 * <p>Import directly or extend and override protected methods to customize.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebReactiveConfigurationSupport implements ApplicationContextAware {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					WebReactiveConfigurationSupport.class.getClassLoader()) &&
			ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
					WebReactiveConfigurationSupport.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", WebReactiveConfigurationSupport.class.getClassLoader());


	private Map<String, CorsConfiguration> corsConfigurations;

	private PathMatchConfigurer pathMatchConfigurer;

	private List<HttpMessageReader<?>> messageReaders;

	private List<HttpMessageWriter<?>> messageWriters;

	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
		mapping.setOrder(0);
		mapping.setContentTypeResolver(webReactiveContentTypeResolver());
		mapping.setCorsConfigurations(getCorsConfigurations());

		PathMatchConfigurer configurer = getPathMatchConfigurer();
		if (configurer.isUseSuffixPatternMatch() != null) {
			mapping.setUseSuffixPatternMatch(configurer.isUseSuffixPatternMatch());
		}
		if (configurer.isUseRegisteredSuffixPatternMatch() != null) {
			mapping.setUseRegisteredSuffixPatternMatch(configurer.isUseRegisteredSuffixPatternMatch());
		}
		if (configurer.isUseTrailingSlashMatch() != null) {
			mapping.setUseTrailingSlashMatch(configurer.isUseTrailingSlashMatch());
		}
		if (configurer.getPathMatcher() != null) {
			mapping.setPathMatcher(configurer.getPathMatcher());
		}
		if (configurer.getPathHelper() != null) {
			mapping.setPathHelper(configurer.getPathHelper());
		}

		return mapping;
	}

	/**
	 * Override to plug a sub-class of {@link RequestMappingHandlerMapping}.
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	@Bean
	public CompositeContentTypeResolver webReactiveContentTypeResolver() {
		RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
		builder.mediaTypes(getDefaultMediaTypeMappings());
		configureRequestedContentTypeResolver(builder);
		return builder.build();
	}

	/**
	 * Override to configure media type mappings.
	 * @see RequestedContentTypeResolverBuilder#mediaTypes(Map)
	 */
	protected Map<String, MediaType> getDefaultMediaTypeMappings() {
		Map<String, MediaType> map = new HashMap<>();
		if (jackson2Present) {
			map.put("json", MediaType.APPLICATION_JSON);
		}
		return map;
	}

	/**
	 * Override to configure how the requested content type is resolved.
	 */
	protected void configureRequestedContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * Callback for building the global CORS configuration. This method is final.
	 * Use {@link #addCorsMappings(CorsRegistry)} to customize the CORS conifg.
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

	/**
	 * Return a handler mapping ordered at Integer.MAX_VALUE-1 with mapped
	 * resource handlers. To configure resource handling, override
	 * {@link #addResourceHandlers}.
	 */
	@Bean
	public HandlerMapping resourceHandlerMapping() {
		ResourceHandlerRegistry registry =
				new ResourceHandlerRegistry(this.applicationContext, webReactiveContentTypeResolver());
		addResourceHandlers(registry);

		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		if (handlerMapping != null) {
			PathMatchConfigurer pathMatchConfigurer = getPathMatchConfigurer();
			if (pathMatchConfigurer.getPathMatcher() != null) {
				handlerMapping.setPathMatcher(pathMatchConfigurer.getPathMatcher());
			}
			if (pathMatchConfigurer.getPathHelper() != null) {
				handlerMapping.setPathHelper(pathMatchConfigurer.getPathHelper());
			}
		}
		else {
			handlerMapping = new EmptyHandlerMapping();
		}
		return handlerMapping;
	}

	/**
	 * Override this method to add resource handlers for serving static resources.
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
		adapter.setMessageReaders(getMessageReaders());
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer());

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		addArgumentResolvers(resolvers);
		if (!resolvers.isEmpty()) {
			adapter.setCustomArgumentResolvers(resolvers);
		}

		return adapter;
	}

	/**
	 * Override to plug a sub-class of {@link RequestMappingHandlerAdapter}.
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * Provide custom argument resolvers without overriding the built-in ones.
	 */
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * Main method to access message readers to use for decoding
	 * controller method arguments with.
	 * <p>Use {@link #configureMessageReaders} to configure the list or
	 * {@link #extendMessageReaders} to add in addition to the default ones.
	 */
	protected final List<HttpMessageReader<?>> getMessageReaders() {
		if (this.messageReaders == null) {
			this.messageReaders = new ArrayList<>();
			configureMessageReaders(this.messageReaders);
			if (this.messageReaders.isEmpty()) {
				addDefaultHttpMessageReaders(this.messageReaders);
			}
			extendMessageReaders(this.messageReaders);
		}
		return this.messageReaders;
	}

	/**
	 * Override to configure the message readers to use for decoding
	 * controller method arguments.
	 * <p>If no message readres are specified, default will be added via
	 * {@link #addDefaultHttpMessageReaders}.
	 * @param messageReaders a list to add message readers to, initially an empty
	 */
	protected void configureMessageReaders(List<HttpMessageReader<?>> messageReaders) {
	}

	/**
	 * Adds default converters that sub-classes can call from
	 * {@link #configureMessageReaders(List)}.
	 */
	protected final void addDefaultHttpMessageReaders(List<HttpMessageReader<?>> readers) {
		readers.add(new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		readers.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		readers.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		readers.add(new DecoderHttpMessageReader<>(new ResourceDecoder()));
		if (jaxb2Present) {
			readers.add(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
		}
		if (jackson2Present) {
			readers.add(new Jackson2ServerHttpMessageReader(
					new  DecoderHttpMessageReader<>(new Jackson2JsonDecoder())));
		}
	}

	/**
	 * Override this to modify the list of message readers after it has been
	 * configured, for example to add some in addition to the default ones.
	 */
	protected void extendMessageReaders(List<HttpMessageReader<?>> messageReaders) {
	}

	/**
	 * Return the {@link ConfigurableWebBindingInitializer} to use for
	 * initializing all {@link WebDataBinder} instances.
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer() {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(webReactiveConversionService());
		initializer.setValidator(webReactiveValidator());
		initializer.setMessageCodesResolver(getMessageCodesResolver());
		return initializer;
	}

	@Bean
	public FormattingConversionService webReactiveConversionService() {
		FormattingConversionService service = new DefaultFormattingConversionService();
		addFormatters(service);
		return service;
	}

	/**
	 * Override to add custom {@link Converter}s and {@link Formatter}s.
	 */
	protected void addFormatters(FormatterRegistry registry) {
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
	public Validator webReactiveValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String name = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(name, getClass().getClassLoader());
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException("Could not find default validator class", ex);
				}
				catch (LinkageError ex) {
					throw new BeanInitializationException("Could not load default validator class", ex);
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
	protected Validator getValidator() {
		return null;
	}

	/**
	 * Override this method to provide a custom {@link MessageCodesResolver}.
	 */
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	@Bean
	public SimpleHandlerAdapter simpleHandlerAdapter() {
		return new SimpleHandlerAdapter();
	}

	@Bean
	public ResponseEntityResultHandler responseEntityResultHandler() {
		return new ResponseEntityResultHandler(getMessageWriters(), webReactiveContentTypeResolver());
	}

	@Bean
	public ResponseBodyResultHandler responseBodyResultHandler() {
		return new ResponseBodyResultHandler(getMessageWriters(), webReactiveContentTypeResolver());
	}

	/**
	 * Main method to access message writers to use for encoding return values.
	 * <p>Use {@link #configureMessageWriters(List)} to configure the list or
	 * {@link #extendMessageWriters(List)} to add in addition to the default ones.
	 */
	protected final List<HttpMessageWriter<?>> getMessageWriters() {
		if (this.messageWriters == null) {
			this.messageWriters = new ArrayList<>();
			configureMessageWriters(this.messageWriters);
			if (this.messageWriters.isEmpty()) {
				addDefaultHttpMessageWriters(this.messageWriters);
			}
			extendMessageWriters(this.messageWriters);
		}
		return this.messageWriters;
	}
	/**
	 * Override to configure the message writers to use for encoding
	 * return values.
	 * <p>If no message readers are specified, default will be added via
	 * {@link #addDefaultHttpMessageWriters}.
	 * @param messageWriters a list to add message writers to, initially an empty
	 */
	protected void configureMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
	}
	/**
	 * Adds default converters that sub-classes can call from
	 * {@link #configureMessageWriters(List)}.
	 */
	protected final void addDefaultHttpMessageWriters(List<HttpMessageWriter<?>> writers) {
		List<Encoder<?>> sseDataEncoders = new ArrayList<>();
		writers.add(new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
		writers.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		writers.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		writers.add(new ResourceHttpMessageWriter());
		if (jaxb2Present) {
			writers.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		}
		if (jackson2Present) {
			Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
			writers.add(new Jackson2ServerHttpMessageWriter(encoder));
			sseDataEncoders.add(encoder);
			HttpMessageWriter<Object> writer = new ServerSentEventHttpMessageWriter(sseDataEncoders);
			writers.add(new Jackson2ServerHttpMessageWriter(writer));
		}
		else {
			writers.add(new ServerSentEventHttpMessageWriter(sseDataEncoders));
		}
	}

	/**
	 * Override this to modify the list of message writers after it has been
	 * configured, for example to add some in addition to the default ones.
	 */
	protected void extendMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
	}

	@Bean
	public ViewResolutionResultHandler viewResolutionResultHandler() {
		ViewResolverRegistry registry = new ViewResolverRegistry(getApplicationContext());
		configureViewResolvers(registry);
		List<ViewResolver> resolvers = registry.getViewResolvers();
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, webReactiveContentTypeResolver());
		handler.setDefaultViews(registry.getDefaultViews());
		handler.setOrder(registry.getOrder());
		return handler;

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
		public void validate(Object target, Errors errors) {
		}
	}

}
