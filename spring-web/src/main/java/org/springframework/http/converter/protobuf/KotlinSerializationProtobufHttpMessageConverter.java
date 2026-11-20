/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
