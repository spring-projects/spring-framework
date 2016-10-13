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
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

/**
 * Defines callback methods to customize the configuration for Web Reactive
 * applications enabled via {@code @EnableWebReactive}.
 *
 * <p>{@code @EnableWebReactive}-annotated configuration classes may implement
 * this interface to be called back and given a chance to customize the
 * default configuration. Consider implementing this interface and
 * overriding the relevant methods for your needs.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface WebReactiveConfigurer {

	/**
	 * Provide a custom sub-class of {@link RequestMappingHandlerMapping}
	 * instead of the one created by default.
	 * The default implementation returns {@code Optional.empty()}.
	 */
	default Optional<RequestMappingHandlerMapping> createRequestMappingHandlerMapping() {
		return Optional.empty();
	}

	/**
	 * Configure how the requested content type is resolved.
	 * <p>The given builder will create a composite of multiple
	 * {@link RequestedContentTypeResolver}s, each defining a way to resolve the
	 * the requested content type (accept HTTP header, path extension, parameter, etc).
	 * @param builder factory that creates a {@link CompositeContentTypeResolver} instance
	 */
	default void configureRequestedContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
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
	 * Provide a custom sub-class of {@link RequestMappingHandlerAdapter}
	 * instead of the one created by default.
	 * The default implementation returns {@code Optional.empty()}.
	 */
	default Optional<RequestMappingHandlerAdapter> createRequestMappingHandlerAdapter() {
		return Optional.empty();
	}

	/**
	 * Provide custom argument resolvers without overriding the built-in ones.
	 * @param resolvers a list of resolvers to add to the built-in ones
	 */
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * Configure the message readers to use for decoding controller method arguments.
	 * <p>If no message readers are specified, default readers will be added via
	 * {@link WebReactiveConfigurationSupport#addDefaultHttpMessageReaders}.
	 * @param messageReaders a list to add message readers to, initially an empty list
	 */
	default void configureMessageReaders(List<HttpMessageReader<?>> messageReaders) {
	}

	/**
	 * Modify the list of message readers to use for decoding controller method arguments,
	 * for example to add some in addition to the ones already configured.
	 */
	default void extendMessageReaders(List<HttpMessageReader<?>> messageReaders) {
	}

	/**
	 * Add custom {@link Converter}s and {@link Formatter}s.
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Provide a custom {@link Validator}, instead of the instance configured by default.
	 * <p>Only a single instance is allowed, an error will be thrown if multiple
	 * {@code Validator}s are returned by {@code WebReactiveConfigurer}s.
	 * The default implementation returns {@code Optional.empty()}.
	 */
	default Optional<Validator> getValidator() {
		return Optional.empty();
	}

	/**
	 * Provide a custom {@link MessageCodesResolver}, instead of using the one
	 * provided by {@link org.springframework.validation.DataBinder} instances.
	 * The default implementation returns {@code Optional.empty()}.
	 */
	default Optional<MessageCodesResolver> getMessageCodesResolver() {
		return Optional.empty();
	}

	/**
	 * Configure the message writers to use for encoding return values.
	 * <p>If no message writers are specified, default writers will be added via
	 * {@link WebReactiveConfigurationSupport#addDefaultHttpMessageWriters(List)}.
	 * @param messageWriters a list to add message writers to, initially an empty list
	 */
	default void configureMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
	}

	/**
	 * Modify the list of message writers to use for encoding return values,
	 * for example to add some in addition to the ones already configured.
	 */
	default void extendMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
	}

	/**
	 * Configure view resolution for supporting template engines.
	 * @see ViewResolverRegistry
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

}
