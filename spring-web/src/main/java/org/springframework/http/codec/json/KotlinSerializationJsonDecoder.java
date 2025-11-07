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

package org.springframework.http.codec.json;

import java.util.function.Predicate;

import kotlinx.serialization.json.Json;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationStringDecoder;
import org.springframework.util.MimeType;

/**
 * Decode a byte stream into JSON and convert to Object's with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/json} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * <p>As of Spring Framework 7.0, by default it only decodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level since it allows combined usage with other general purpose JSON decoders
 * like {@link JacksonJsonDecoder} without conflicts.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationJsonDecoder(type -> true)} will decode all types
 * supported by Kotlin Serialization, including unannotated Kotlin enumerations,
 * numbers, characters, booleans and strings.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 * @see KotlinSerializationJsonEncoder
 */
public class KotlinSerializationJsonDecoder extends KotlinSerializationStringDecoder<Json> {

	private static final MimeType[] DEFAULT_JSON_MIME_TYPES = new MimeType[] {
			MediaType.APPLICATION_JSON,
			new MediaType("application", "*+json"),
			MediaType.APPLICATION_NDJSON
	};

	/**
	 * Construct a new decoder using {@link Json.Default} instance which
	 * only decodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationJsonDecoder() {
		this(Json.Default);
	}

	/**
	 * Construct a new decoder using {@link Json.Default} instance which
	 * only decodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationJsonDecoder(Predicate<ResolvableType> typePredicate) {
		this(Json.Default, typePredicate);
	}

	/**
	 * Construct a new decoder using the provided {@link Json} instance which
	 * only decodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationJsonDecoder(Json json) {
		super(json, DEFAULT_JSON_MIME_TYPES);
	}

	/**
	 * Construct a new decoder using the provided {@link Json} instance which
	 * only decodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationJsonDecoder(Json json, Predicate<ResolvableType> typePredicate) {
		super(json, typePredicate, DEFAULT_JSON_MIME_TYPES);
	}

}
