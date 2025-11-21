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
 * ask to {@link Builder#registerDefaults() register default converters with classpath detection}
 * and {@link Builder#withJsonConverter(HttpMessageConverter) override specific converters} that were detected.
 * Custom converters can be independently added in front of default ones.
 * Finally, {@link Builder#configureMessageConverters(Consumer) default and custom converters can be configured}.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 7.0
 */
public interface HttpMessageConverters extends Iterable<HttpMessageConverter<?>> {

	/**
	 * Return true if this instance does not contain any message converters.
	 */
	boolean isEmpty();

	/**
	 * Create a builder instance, tailored for HTTP client usage.
	 * <p>The following HTTP message converters can be detected and registered if available, in order:
	 * <ol>
	 * <li>All custom message converters configured with the builder
	 * <li>{@link ByteArrayHttpMessageConverter}
	 * <li>{@link StringHttpMessageConverter} with the {@link java.nio.charset.StandardCharsets#ISO_8859_1} charset
	 * <li>{@link ResourceHttpMessageConverter}, with resource streaming support disabled
	 * <li>a Multipart converter, using all detected and custom converters for part conversion
	 * <li>A Kotlin Serialization JSON converter
	 * <li>A JSON converter
	 * <li>A Smile converter
	 * <li>A Kotlin Serialization CBOR converter
	 * <li>A CBOR converter
	 * <li>A YAML converter
	 * <li>An XML converter
	 * <li>A ProtoBuf converter
	 * <li>ATOM and RSS converters
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
	 *     <li>A Kotlin Serialization JSON converter
	 *     <li>A JSON converter
	 *     <li>A Smile converter
	 *     <li>A Kotlin Serialization CBOR converter
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
		 * Manual registrations like {@link #withJsonConverter(HttpMessageConverter)} will
		 * override auto-detected ones.
		 */
		T registerDefaults();

		/**
		 * Override the default String {@code HttpMessageConverter}
		 * with any converter supporting String conversion.
		 * @param stringMessageConverter the converter instance to use
		 * @see StringHttpMessageConverter
		 */
		T withStringConverter(HttpMessageConverter<?> stringMessageConverter);

		/**
		 * Override the default String {@code HttpMessageConverter}
		 * with any converter supporting the Kotlin Serialization conversion for JSON.
		 * @param kotlinSerializationConverter the converter instance to use
		 * @see org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
		 */
		T withKotlinSerializationJsonConverter(HttpMessageConverter<?> kotlinSerializationConverter);

		/**
		 * Override the default Jackson 3.x JSON {@code HttpMessageConverter}
		 * with any converter supporting the JSON format.
		 * @param jsonMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
		 */
		T withJsonConverter(HttpMessageConverter<?> jsonMessageConverter);

		/**
		 * Override the default Jackson 3.x XML {@code HttpMessageConverter}
		 * with any converter supporting the XML format.
		 * @param xmlMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
		 */
		T withXmlConverter(HttpMessageConverter<?> xmlMessageConverter);

		/**
		 * Override the default Jackson 3.x Smile {@code HttpMessageConverter}
		 * with any converter supporting the Smile format.
		 * @param smileMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.smile.JacksonSmileHttpMessageConverter
		 */
		T withSmileConverter(HttpMessageConverter<?> smileMessageConverter);

		/**
		 * Override the default String {@code HttpMessageConverter}
		 * with any converter supporting the Kotlin Serialization conversion for CBOR.
		 * @param kotlinSerializationConverter the converter instance to use
		 * @see org.springframework.http.converter.cbor.KotlinSerializationCborHttpMessageConverter
		 */
		T withKotlinSerializationCborConverter(HttpMessageConverter<?> kotlinSerializationConverter);

		/**
		 * Override the default Jackson 3.x CBOR {@code HttpMessageConverter}
		 * with any converter supporting the CBOR format.
		 * @param cborMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter
		 */
		T withCborConverter(HttpMessageConverter<?> cborMessageConverter);

		/**
		 * Override the default Jackson 3.x Yaml {@code HttpMessageConverter}
		 * with any converter supporting the Yaml format.
		 * @param yamlMessageConverter the converter instance to use
		 * @see org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter
		 */
		T withYamlConverter(HttpMessageConverter<?> yamlMessageConverter);

		/**
		 * Add a custom {@code HttpMessageConverter} to the list of converters, ahead of the default converters.
		 * @param customConverter the converter instance to add
		 */
		T addCustomConverter(HttpMessageConverter<?> customConverter);

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
