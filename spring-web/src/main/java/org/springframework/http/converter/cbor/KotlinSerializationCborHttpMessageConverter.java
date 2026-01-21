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

package org.springframework.http.converter.cbor;

import java.util.function.Predicate;

import kotlinx.serialization.cbor.Cbor;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.KotlinSerializationBinaryHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write CBOR using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/cbor}.
 *
 * <p>As of Spring Framework 7.0, by default it only  types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level since it allows combined usage with other general purpose JSON decoders
 * like {@link JacksonCborHttpMessageConverter} without conflicts.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationCborHttpMessageConverter(type -> true)} will decode all types
 * supported by Kotlin Serialization, including unannotated Kotlin enumerations,
 * numbers, characters, booleans and strings.
 *
 * @author Iain Henderson
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class KotlinSerializationCborHttpMessageConverter extends KotlinSerializationBinaryHttpMessageConverter<Cbor> {

	/**
	 * Construct a new converter using {@link Cbor.Default} instance which
	 * only converts types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationCborHttpMessageConverter() {
		this(Cbor.Default);
	}

	/**
	 * Construct a new converter using {@link Cbor.Default} instance which
	 * only converts types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationCborHttpMessageConverter(Predicate<ResolvableType> typePredicate) {
		this(Cbor.Default, typePredicate);
	}

	/**
	 * Construct a new converter using the provided {@link Cbor} instance which
	 * only converts types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationCborHttpMessageConverter(Cbor cbor) {
		super(cbor, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new converter using the provided {@link Cbor} instance which
	 * only converts types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationCborHttpMessageConverter(Cbor cbor, Predicate<ResolvableType> typePredicate) {
		super(cbor, typePredicate, MediaType.APPLICATION_CBOR);
	}
}
