/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
