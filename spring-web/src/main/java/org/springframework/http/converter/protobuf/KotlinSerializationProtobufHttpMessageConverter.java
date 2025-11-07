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

package org.springframework.http.converter.protobuf;

import java.util.function.Predicate;

import kotlinx.serialization.protobuf.ProtoBuf;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.KotlinSerializationBinaryHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write Protocol Buffers using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/x-protobuf}, {@code application/octet-stream}, and {@code application/vnd.google.protobuf}.
 *
 * <p>As of Spring Framework 7.0, by default it only converts types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationProtobufHttpMessageConverter(type -> true)} will convert all types
 * supported by Kotlin Serialization, including unannotated Kotlin enumerations,
 * numbers, characters, booleans and strings.
 *
 * @author Iain Henderson
 * @author Sebstien Deleuze
 * @since 6.0
 */
public class KotlinSerializationProtobufHttpMessageConverter extends
		KotlinSerializationBinaryHttpMessageConverter<ProtoBuf> {

	/**
	 * Construct a new converter using {@link ProtoBuf.Default} instance which
	 * only converts types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationProtobufHttpMessageConverter() {
		this(ProtoBuf.Default);
	}

	/**
	 * Construct a new converter using {@link ProtoBuf.Default} instance which
	 * only converts types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationProtobufHttpMessageConverter(Predicate<ResolvableType> typePredicate) {
		this(ProtoBuf.Default, typePredicate);
	}

	/**
	 * Construct a new converter using the provided {@link ProtoBuf} instance which
	 * only converts types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationProtobufHttpMessageConverter(ProtoBuf protobuf) {
		super(protobuf, MediaType.APPLICATION_PROTOBUF, MediaType.APPLICATION_OCTET_STREAM,
				new MediaType("application", "vnd.google.protobuf"));
	}

	/**
	 * Construct a new converter using the provided {@link ProtoBuf} instance which
	 * only converts types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationProtobufHttpMessageConverter(ProtoBuf protobuf, Predicate<ResolvableType> typePredicate) {
		super(protobuf, typePredicate, MediaType.APPLICATION_PROTOBUF, MediaType.APPLICATION_OCTET_STREAM,
				new MediaType("application", "vnd.google.protobuf"));
	}

}
