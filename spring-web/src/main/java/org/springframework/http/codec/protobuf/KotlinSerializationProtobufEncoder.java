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

package org.springframework.http.codec.protobuf;

import java.util.function.Predicate;

import kotlinx.serialization.protobuf.ProtoBuf;

import org.springframework.core.ResolvableType;
import org.springframework.http.codec.KotlinSerializationBinaryEncoder;

/**
 * Decode a byte stream into a Protocol Buffer and convert to Objects with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/x-protobuf}, {@code application/octet-stream}, and {@code application/vnd.google.protobuf}.
 *
 * <p>As of Spring Framework 7.0, by default it only encodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationProtobufEncoder(type -> true)} will encode all types
 * supported by Kotlin Serialization, including unannotated Kotlin enumerations,
 * numbers, characters, booleans and strings.
 *
 * <p>Decoding streams is not supported yet, see
 * <a href="https://github.com/Kotlin/kotlinx.serialization/issues/1073">kotlinx.serialization/issues/1073</a>
 * related issue.
 *
 * @author Iain Henderson
 * @author Sebastien Deleuze
 * @since 6.0
 * @see KotlinSerializationProtobufDecoder
 */
public class KotlinSerializationProtobufEncoder extends KotlinSerializationBinaryEncoder<ProtoBuf> {

	/**
	 * Construct a new encoder using {@link ProtoBuf.Default} instance which
	 * only encodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationProtobufEncoder() {
		this(ProtoBuf.Default);
	}

	/**
	 * Construct a new encoder using {@link ProtoBuf.Default} instance which
	 * only encodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationProtobufEncoder(Predicate<ResolvableType> typePredicate) {
		this(ProtoBuf.Default, typePredicate);
	}

	/**
	 * Construct a new encoder using the provided {@link ProtoBuf} instance which
	 * only encodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationProtobufEncoder(ProtoBuf protobuf) {
		super(protobuf, ProtobufCodecSupport.MIME_TYPES);
	}

	/**
	 * Construct a new encoder using the provided {@link ProtoBuf} instance which
	 * only encodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationProtobufEncoder(ProtoBuf protobuf, Predicate<ResolvableType> typePredicate) {
		super(protobuf, typePredicate, ProtobufCodecSupport.MIME_TYPES);
	}
}
