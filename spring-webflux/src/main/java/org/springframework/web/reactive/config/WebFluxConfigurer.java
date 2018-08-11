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

package org.springframework.web.reactive.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * Defines callback methods to customize the configuration for WebFlux
 * applications enabled via {@link EnableWebFlux @EnableWebFlux}.
 *
 * <p>{@code @EnableWebFlux}-annotated configuration classes may implement
 * this interface to be called back and given a chance to customize the
 * default configuration. Consider implementing this interface and
 * overriding the relevant methods for your needs.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebFluxConfigurationSupport
 * @see DelegatingWebFluxConfiguration
 */
public interface WebFluxConfigurer {

	/**
	 * Configure how the content type requested for the response is resolved
	 * when  handling reqests with annotated controllers.
	 * @param builder for configuring the resolvers to use
	 */
	default void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
	}

	/**
	 * Configure "global" cross origin request processing.
	 * <p>The configured readers and writers will apply to all requests including
	 * annotated controllers and functional endpoints. Annotated controllers can
	 * further declare more fine-grained configuration via
	 * {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin}.
	 * @see CorsRegistry
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * Configure path matching options.
	 * <p>The configured path matching options will be used for mapping to
	 * annotated controllers and also
	 * {@link #addResourceHandlers(ResourceHandlerRegistry) static resources}.
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
	 * Configure resolvers for custom {@code @RequestMapping} method arguments.
	 * @param configurer to configurer to use
	 */
	default void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
	}

	/**
	 * Configure custom HTTP message readers and writers or override built-in ones.
	 * <p>The configured readers and writers will be used for both annotated
	 * controllers and functional endpoints.
	 * @param configurer the configurer to use
	 */
	default void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
	}

	/**
	 * Add custom {@link Converter}s and {@link Formatter Converter}s and {@link Formatters} for performing type
	 * conversion and formatting of annotated controller method arguments.
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Provide a custom {@link Validator}.
	 * <p>By default a validator for standard bean validation is created if
	 * bean validation api is present on the classpath.
	 * <p>The configured validator is used for validating annotated controller
	 * method arguments.
	 */
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * Provide a custom {@link MessageCodesResolver} to use for data binding in
	 * annotated controller method arguments instead of the one created by
	 * default in {@link org.springframework.validation.DataBinder}.
	 */
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * Configure view resolution for rendering responses with a view and a model,
	 * where the view is typically an HTML template but could also be based on
	 * an HTTP message writer (e.g. JSON, XML).
	 * <p>The configured view resolvers will be used for both annotated
	 * controllers and functional endpoints.
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

}
