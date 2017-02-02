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

import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.CompositeContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;

/**
 * Defines callback methods to customize the configuration for Web Reactive
 * applications enabled via {@code @EnableWebFlux}.
 *
 * <p>{@code @EnableWebFlux}-annotated configuration classes may implement
 * this interface to be called back and given a chance to customize the
 * default configuration. Consider implementing this interface and
 * overriding the relevant methods for your needs.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebFluxConfigurer {

	/**
	 * Configure how the content type requested for the response is resolved.
	 * <p>The given builder will create a composite of multiple
	 * {@link RequestedContentTypeResolver}s, each defining a way to resolve
	 * the the requested content type (accept HTTP header, path extension,
	 * parameter, etc).
	 * @param builder factory that creates a {@link CompositeContentTypeResolver}
	 */
	default void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * Configure cross origin requests processing.
	 * @see CorsRegistry
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * Configure path matching options.
	 * <p>The given configurer assists with configuring
	 * {@code HandlerMapping}s with path matching options.
	 * @param configurer the {@link PathMatchConfigurer} instance
	 */
	default void configurePathMatching(PathMatchConfigurer configurer) {
	}

	/**
	 * Add resource handlers for serving static resources.
	 * @see ResourceHandlerRegistry
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * Provide custom controller method argument resolvers. Such resolvers do
	 * not override and will be invoked after the built-in ones.
	 * @param resolvers a list of resolvers to add
	 */
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * Configure the message readers to use for decoding the request body where
	 * {@code @RequestBody} and {@code HttpEntity} controller method arguments
	 * are used. If none are specified, default ones are added based on
	 * {@link WebFluxConfigurationSupport#addDefaultHttpMessageReaders}.
	 * <p>See {@link #extendMessageReaders(List)} for adding readers
	 * in addition to the default ones.
	 * @param readers an empty list to add message readers to
	 */
	default void configureMessageReaders(List<HttpMessageReader<?>> readers) {
	}

	/**
	 * An alternative to {@link #configureMessageReaders(List)} that allows
	 * modifying the message readers to use after default ones have been added.
	 */
	default void extendMessageReaders(List<HttpMessageReader<?>> readers) {
	}

	/**
	 * Add custom {@link Converter}s and {@link Formatter}s for performing type
	 * conversion and formatting of controller method arguments.
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Provide a custom {@link Validator}.
	 * <p>By default a validator for standard bean validation is created if
	 * bean validation api is present on the classpath.
	 */
	default Optional<Validator> getValidator() {
		return Optional.empty();
	}

	/**
	 * Provide a custom {@link MessageCodesResolver} to use for data binding
	 * instead of the one created by default in
	 * {@link org.springframework.validation.DataBinder}.
	 */
	default Optional<MessageCodesResolver> getMessageCodesResolver() {
		return Optional.empty();
	}

	/**
	 * Configure the message writers to use to encode the response body based on
	 * the return values of {@code @ResponseBody}, and {@code ResponseEntity}
	 * controller methods. If none are specified, default ones are added based on
	 * {@link WebFluxConfigurationSupport#addDefaultHttpMessageWriters(List)}.
	 * <p>See {@link #extendMessageWriters(List)} for adding writers
	 * in addition to the default ones.
	 * @param writers a empty list to add message writers to
	 */
	default void configureMessageWriters(List<HttpMessageWriter<?>> writers) {
	}

	/**
	 * An alternative to {@link #configureMessageWriters(List)} that allows
	 * modifying the message writers to use after default ones have been added.
	 */
	default void extendMessageWriters(List<HttpMessageWriter<?>> writers) {
	}

	/**
	 * Configure view resolution for processing the return values of controller
	 * methods that rely on resolving a
	 * {@link org.springframework.web.reactive.result.view.View} to render
	 * the response with. By default all controller methods rely on view
	 * resolution unless annotated with {@code @ResponseBody} or explicitly
	 * return {@code ResponseEntity}. A view may be specified explicitly with
	 * a String return value or implicitly, e.g. {@code void} return value.
	 * @see ViewResolverRegistry
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

}
