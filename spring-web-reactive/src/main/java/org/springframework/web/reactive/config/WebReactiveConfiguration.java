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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.xml.Jaxb2Decoder;
import org.springframework.http.codec.xml.Jaxb2Encoder;
import org.springframework.http.converter.reactive.DecoderHttpMessageReader;
import org.springframework.http.converter.reactive.EncoderHttpMessageWriter;
import org.springframework.http.converter.reactive.HttpMessageReader;
import org.springframework.http.converter.reactive.HttpMessageWriter;
import org.springframework.http.converter.reactive.ResourceHttpMessageWriter;
import org.springframework.http.converter.reactive.SseEventHttpMessageWriter;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.SimpleResultHandler;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * The main class for Spring Web Reactive configuration.
 *
 * <p>Import directly or extend and override protected methods to customize.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@Configuration
public class WebReactiveConfiguration implements ApplicationContextAware {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", WebReactiveConfiguration.class.getClassLoader()) &&
			ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", WebReactiveConfiguration.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", WebReactiveConfiguration.class.getClassLoader());

	private static final boolean rxJava1Present =
			ClassUtils.isPresent("rx.Observable", WebReactiveConfiguration.class.getClassLoader());


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
		mapping.setContentTypeResolver(mvcContentTypeResolver());

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
	public RequestedContentTypeResolver mvcContentTypeResolver() {
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
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		addArgumentResolvers(resolvers);
		if (!resolvers.isEmpty()) {
			adapter.setCustomArgumentResolvers(resolvers);
		}

		adapter.setMessageReaders(getMessageReaders());
		adapter.setConversionService(mvcConversionService());
		adapter.setValidator(mvcValidator());

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
		readers.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		readers.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		readers.add(new DecoderHttpMessageReader<>(new ResourceDecoder()));
		if (jaxb2Present) {
			readers.add(new DecoderHttpMessageReader<>(new Jaxb2Decoder()));
		}
		if (jackson2Present) {
			readers.add(new DecoderHttpMessageReader<>(new JacksonJsonDecoder()));
		}
	}

	/**
	 * Override this to modify the list of message readers after it has been
	 * configured, for example to add some in addition to the default ones.
	 */
	protected void extendMessageReaders(List<HttpMessageReader<?>> messageReaders) {
	}

	@Bean
	public FormattingConversionService mvcConversionService() {
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
	public Validator mvcValidator() {
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

	@Bean
	public SimpleHandlerAdapter simpleHandlerAdapter() {
		return new SimpleHandlerAdapter();
	}

	@Bean
	public SimpleResultHandler simpleResultHandler() {
		return new SimpleResultHandler();
	}

	@Bean
	public ResponseEntityResultHandler responseEntityResultHandler() {
		return new ResponseEntityResultHandler(getMessageWriters(), mvcContentTypeResolver());
	}

	@Bean
	public ResponseBodyResultHandler responseBodyResultHandler() {
		return new ResponseBodyResultHandler(getMessageWriters(), mvcContentTypeResolver());
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
		writers.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		writers.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		writers.add(new ResourceHttpMessageWriter());
		if (jaxb2Present) {
			writers.add(new EncoderHttpMessageWriter<>(new Jaxb2Encoder()));
		}
		if (jackson2Present) {
			JacksonJsonEncoder jacksonEncoder = new JacksonJsonEncoder();
			writers.add(new EncoderHttpMessageWriter<>(jacksonEncoder));
			sseDataEncoders.add(jacksonEncoder);
		}
		writers.add(new SseEventHttpMessageWriter(sseDataEncoders));
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
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, mvcContentTypeResolver());
		handler.setDefaultViews(registry.getDefaultViews());
		handler.setOrder(registry.getOrder());
		return handler;

	}

	/**
	 * Override this to configure view resolution.
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
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
