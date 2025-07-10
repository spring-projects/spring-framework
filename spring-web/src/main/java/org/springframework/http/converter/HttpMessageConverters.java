/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.converter;

import java.util.function.Consumer;

/**
 * Utility for building and configuring an immutable collection of {@link HttpMessageConverter}
 * instances for client and server usage. You can {@link #create() create}
 * a new empty instance or ask to {@link #withDefaults() register default converters},
 * if available in your classpath.
 *
 * <p>This class offers a flexible arrangement for {@link HttpMessageConverters.Builder configuring message converters shared between}
 * client and server, or {@link HttpMessageConverters.Builder#configureClient(Consumer) configuring client-specific}
 * and {@link HttpMessageConverters.Builder#configureServer(Consumer) server-specific} converters.
 *
 * <p>The following HTTP message converters will be detected and registered if available, in order.
 * For {@link #forClient() client side converters}:
 * <ol>
 *     <li>All custom message converters configured with the builder
 *     <li>{@link ByteArrayHttpMessageConverter}
 *     <li>{@link StringHttpMessageConverter} with the {@link java.nio.charset.StandardCharsets#ISO_8859_1} charset
 *     <li>{@link ResourceHttpMessageConverter}, with resource streaming support disabled
 *     <li>a Multipart converter, using all detected and custom converters for part conversion
 *     <li>A JSON converter
 *     <li>A Smile converter
 *     <li>A CBOR converter
 *     <li>A YAML converter
 *     <li>An XML converter
 *     <li>An ProtoBuf converter
 *     <li>ATOM and RSS converters
 * </ol>
 *
 * For {@link #forClient() client side converters}:
 * <ol>
 *     <li>All custom message converters configured with the builder
 *     <li>{@link ByteArrayHttpMessageConverter}
 *     <li>{@link StringHttpMessageConverter} with the {@link java.nio.charset.StandardCharsets#ISO_8859_1} charset
 *     <li>{@link ResourceHttpMessageConverter}
 *     <li>{@link ResourceRegionHttpMessageConverter}
 *     <li>A JSON converter
 *     <li>A Smile converter
 *     <li>A CBOR converter
 *     <li>A YAML converter
 *     <li>An XML converter
 *     <li>An ProtoBuf converter
 *     <li>ATOM and RSS converters
 *     <li>a Multipart converter, using all detected and custom converters for part conversion
 * </ol>
 *
 * @author Brian Clozel
 * @since 7.0
 */
public interface HttpMessageConverters {

	/**
	 * Return the list of configured message converters, tailored for HTTP client usage.
	 */
	Iterable<HttpMessageConverter<?>> forClient();

	/**
	 * Return the list of configured message converters, tailored for HTTP server usage.
	 */
	Iterable<HttpMessageConverter<?>> forServer();

	/**
	 * Create a builder instance, without any message converter pre-configured.
	 */
	static Builder create() {
		return new DefaultHttpMessageConverters.DefaultBuilder(false);
	}

	/**
	 * Create a builder instance with default message converters pre-configured.
	 */
	static Builder withDefaults() {
		return new DefaultHttpMessageConverters.DefaultBuilder(true);
	}


	interface MessageConverterConfigurer<T extends MessageConverterConfigurer<T>> {

		/**
		 * Override the default String {@code HttpMessageConverter}
		 * with any converter supporting String conversion.
		 * @param stringMessageConverter the converter instance to use
		 * @see StringHttpMessageConverter
		 */
		T stringMessageConverter(HttpMessageConverter<?> stringMessageConverter);

		/**
		 * Override the default Jackson 3.x JSON {@code HttpMessageConverter}
		 * with any converter supporting the JSON format.
		 * @param jsonMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
		 */
		T jsonMessageConverter(HttpMessageConverter<?> jsonMessageConverter);

		/**
		 * Override the default Jackson 3.x XML {@code HttpMessageConverter}
		 * with any converter supporting the XML format.
		 * @param xmlMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
		 */
		T xmlMessageConverter(HttpMessageConverter<?> xmlMessageConverter);

		/**
		 * Override the default Jackson 3.x Smile {@code HttpMessageConverter}
		 * with any converter supporting the Smile format.
		 * @param smileMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.smile.JacksonSmileHttpMessageConverter
		 */
		T smileMessageConverter(HttpMessageConverter<?> smileMessageConverter);

		/**
		 * Override the default Jackson 3.x CBOR {@code HttpMessageConverter}
		 * with any converter supporting the CBOR format.
		 * @param cborMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter
		 */
		T cborMessageConverter(HttpMessageConverter<?> cborMessageConverter);

		/**
		 * Override the default Jackson 3.x Yaml {@code HttpMessageConverter}
		 * with any converter supporting the Yaml format.
		 * @param yamlMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter
		 */
		T yamlMessageConverter(HttpMessageConverter<?> yamlMessageConverter);

		/**
		 * Add a custom {@code HttpMessageConverter} to the list of converters.
		 * @param customConverter the converter instance to add
		 */
		T additionalMessageConverter(HttpMessageConverter<?> customConverter);

	}

	/**
	 * Builder for an {@link HttpMessageConverters}.
	 * This builder manages the configuration of common and client/server-specific message converters.
	 */
	interface Builder extends MessageConverterConfigurer<Builder> {

		/**
		 * Configure client-specific message converters.
		 * If no opinion is provided here, message converters defined in this builder will be used.
		 */
		Builder configureClient(Consumer<ClientMessageConverterConfigurer> consumer);

		/**
		 * Configure server-specific message converters.
		 * If no opinion is provided here, message converters defined in this builder will be used.
		 */
		Builder configureServer(Consumer<ServerMessageConverterConfigurer> consumer);

		/**
		 * Build and return the {@link HttpMessageConverters} instance configured by this builder.
		 */
		HttpMessageConverters build();
	}

	interface ClientMessageConverterConfigurer extends MessageConverterConfigurer<ClientMessageConverterConfigurer> {

		/**
		 * Register a consumer to apply to configured converter instances.
		 * This can be used to configure rather than replace one or more specific converters.
		 * @param configurer the consumer to apply
		 */
		ClientMessageConverterConfigurer configureClientMessageConverters(Consumer<HttpMessageConverter<?>> configurer);

	}

	interface ServerMessageConverterConfigurer extends MessageConverterConfigurer<ServerMessageConverterConfigurer> {

		/**
		 * Register a consumer to apply to configured converter instances.
		 * This can be used to configure rather than replace one or more specific converters.
		 * @param configurer the consumer to apply
		 */
		ServerMessageConverterConfigurer configureServerMessageConverters(Consumer<HttpMessageConverter<?>> configurer);

	}

}
