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

package org.springframework.web.reactive.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.socket.server.WebSocketService;

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
	 * Configure the HTTP message readers and writers for reading from the
	 * request body and for writing to the response body in annotated controllers
	 * and functional endpoints.
	 * <p>By default, all built-in readers and writers are configured as long as
	 * the corresponding 3rd party libraries such Jackson JSON, JAXB2, and others
	 * are present on the classpath.
	 * @param configurer the configurer to customize readers and writers
	 */
	default void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
	}

	/**
	 * Add custom {@link Converter Converters} and {@link Formatter Formatters} for
	 * performing type conversion and formatting of annotated controller method arguments.
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Provide a custom {@link Validator}.
	 * <p>By default a validator for standard bean validation is created if
	 * bean validation API is present on the classpath.
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
	 * Configure "global" cross-origin request processing. The configured CORS
	 * mappings apply to annotated controllers, functional endpoints, and static
	 * resources.
	 * <p>Annotated controllers can further declare more fine-grained config via
	 * {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin}.
	 * In such cases "global" CORS configuration declared here is
	 * {@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) combined}
	 * with local CORS configuration defined on a controller method.
	 * @see CorsRegistry
	 * @see CorsConfiguration#combine(CorsConfiguration)
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * Configure settings related to blocking execution in WebFlux.
	 * @since 6.1
	 */
	default void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
	}

	/**
	 * Configure how the content type requested for the response is resolved
	 * when handling requests with annotated controllers.
	 * @param builder for configuring the resolvers to use
	 */
	default void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
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
	 * Configure resolvers for custom {@code @RequestMapping} method arguments.
	 * @param configurer to configurer to use
	 */
	default void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
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

	/**
	 * Add resource handlers for serving static resources.
	 * @see ResourceHandlerRegistry
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * Provide the {@link WebSocketService} to create
	 * {@link org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter}
	 * with. This can be used to configure server-specific properties through the
	 * {@link org.springframework.web.reactive.socket.server.RequestUpgradeStrategy}.
	 * @since 5.3
	 */
	@Nullable
	default WebSocketService getWebSocketService() {
		return null;
	}

}
