/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.cbor;

import java.util.function.Predicate;

import kotlinx.serialization.cbor.Cbor;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationBinaryDecoder;

/**
 * Decode a byte stream into CBOR and convert to Objects with
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/cbor}.
 *
 * <p>As of Spring Framework 7.0, by default it only decodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level since it allows combined usage with other general purpose CBOR decoders
 * like {@link JacksonCborDecoder} without conflicts.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationCborDecoder(type -> true)} will decode all types
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
 * @see KotlinSerializationCborEncoder
 */
public class KotlinSerializationCborDecoder extends KotlinSerializationBinaryDecoder<Cbor> {

	/**
	 * Construct a new decoder using {@link Cbor.Default} instance which
	 * only decodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationCborDecoder() {
		this(Cbor.Default);
	}

	/**
	 * Construct a new decoder using {@link Cbor.Default} instance which
	 * only decodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationCborDecoder(Predicate<ResolvableType> typePredicate) {
		this(Cbor.Default, typePredicate);
	}

	/**
	 * Construct a new decoder using the provided {@link Cbor} instance which
	 * only decodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationCborDecoder(Cbor cbor) {
		super(cbor, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new decoder using the provided {@link Cbor} instance which
	 * only decodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationCborDecoder(Cbor cbor, Predicate<ResolvableType> typePredicate) {
		super(cbor, typePredicate, MediaType.APPLICATION_CBOR);
	}

}
