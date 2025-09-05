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
 * instances for {@link #forClient() client} or {@link #forServer() server} usage. You can
 * ask to {@link Builder#registerDefaults() register default converters with classpath detection},
 * add custom converters and post-process configured converters.
 *
 * @author Brian Clozel
 * @since 7.0
 */
public interface HttpMessageConverters extends Iterable<HttpMessageConverter<?>> {


	/**
	 * Create a builder instance, tailored for HTTP client usage.
	 * <p>The following HTTP message converters can be detected and registered if available, in order:
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
	 *     <li>A ProtoBuf converter
	 *     <li>ATOM and RSS converters
	 * </ol>
	 */
	static ClientBuilder forClient() {
		return new DefaultHttpMessageConverters.DefaultClientBuilder();
	}

	/**
	 * Create a builder instance, tailored for HTTP server usage.
	 * <p>The following HTTP message converters can be detected and registered if available, in order:
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
	 *     <li>A ProtoBuf converter
	 *     <li>ATOM and RSS converters
	 *     <li>a Multipart converter, using all detected and custom converters for part conversion
	 * </ol>
	 */
	static ServerBuilder forServer() {
		return new DefaultHttpMessageConverters.DefaultServerBuilder();
	}

	interface Builder<T extends Builder<T>> {

		/**
		 * Register default converters using classpath detection.
		 * Manual registrations like {@link #jsonMessageConverter(HttpMessageConverter)} will
		 * override auto-detected ones.
		 */
		T registerDefaults();

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
		T customMessageConverter(HttpMessageConverter<?> customConverter);

		/**
		 * Add a consumer for configuring the selected message converters.
		 * @param configurer the configurer to use
		 */
		T configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer);

		/**
		 * Build and return the {@link HttpMessageConverters} instance configured by this builder.
		 */
		HttpMessageConverters build();
	}

	/**
	 * Client builder for an {@link HttpMessageConverters} instance.
	 */
	interface ClientBuilder extends Builder<ClientBuilder> {

	}

	/**
	 * Server builder for an {@link HttpMessageConverters} instance.
	 */
	interface ServerBuilder extends Builder<ServerBuilder> {

	}

}
