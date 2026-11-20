/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
