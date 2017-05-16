/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.CompositeContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * Defines callback methods to customize the configuration for WebFlux
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
	 * the requested content type (accept HTTP header, path extension,
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
	 * Configure resolvers for custom controller method arguments.
	 * @param configurer to configurer to use
	 */
	default void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
	}

	/**
	 * Configure custom HTTP message readers and writers or override built-in ones.
	 * @param configurer the configurer to use
	 */
	default void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
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
