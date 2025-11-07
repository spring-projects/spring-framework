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

package org.springframework.http.codec.cbor;

import java.util.function.Predicate;

import kotlinx.serialization.cbor.Cbor;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationBinaryEncoder;

/**
 * Encode from an {@code Object} stream to a byte stream of CBOR objects using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/cbor}.
 *
 * <p>As of Spring Framework 7.0, by default it only encodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level since it allows combined usage with other general purpose JSON encoders
 * like {@link JacksonCborEncoder} without conflicts.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationCborEncoder(type -> true)} will encode all types
 * supported by Kotlin Serialization, including unannotated Kotlin enumerations,
 * numbers, characters, booleans and strings.
 *
 * @author Iain Henderson
 * @author Sebastien Deleuze
 * @since 6.0
 * @see KotlinSerializationCborDecoder
 */
public class KotlinSerializationCborEncoder extends KotlinSerializationBinaryEncoder<Cbor> {

	/**
	 * Construct a new encoder using {@link Cbor.Default} instance which
	 * only encodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationCborEncoder() {
		this(Cbor.Default);
	}

	/**
	 * Construct a new encoder using {@link Cbor.Default} instance which
	 * only encodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationCborEncoder(Predicate<ResolvableType> typePredicate) {
		this(Cbor.Default, typePredicate);
	}

	/**
	 * Construct a new encoder using the provided {@link Cbor} instance which
	 * only encodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationCborEncoder(Cbor cbor) {
		super(cbor, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new encoder using the provided {@link Cbor} instance which
	 * only encodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationCborEncoder(Cbor cbor, Predicate<ResolvableType> typePredicate) {
		super(cbor, typePredicate, MediaType.APPLICATION_CBOR);
	}

}
