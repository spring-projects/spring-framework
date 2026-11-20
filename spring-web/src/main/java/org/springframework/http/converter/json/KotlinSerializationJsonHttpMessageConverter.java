/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;

import java.util.function.Predicate;

import kotlinx.serialization.json.Json;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.KotlinSerializationStringHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write JSON using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/json} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * <p>As of Spring Framework 7.0, by default it only  types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level since it allows combined usage with other general purpose JSON decoders
 * like {@link JacksonJsonHttpMessageConverter} without conflicts.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationJsonHttpMessageConverter(type -> true)} will decode all types
 * supported by Kotlin Serialization, including unannotated Kotlin enumerations,
 * numbers, characters, booleans and strings.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Iain Henderson
 * @since 5.3
 */
public class KotlinSerializationJsonHttpMessageConverter extends KotlinSerializationStringHttpMessageConverter<Json> {

	private static final MediaType[] DEFAULT_JSON_MIME_TYPES = new MediaType[] {
			MediaType.APPLICATION_JSON, new MediaType("application", "*+json") };

	/**
	 * Construct a new converter using {@link Json.Default} instance which
	 * only converts types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationJsonHttpMessageConverter() {
		this(Json.Default);
	}

	/**
	 * Construct a new converter using {@link Json.Default} instance which
	 * only converts types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationJsonHttpMessageConverter(Predicate<ResolvableType> typePredicate) {
		this(Json.Default, typePredicate);
	}

	/**
	 * Construct a new converter using the provided {@link Json} instance which
	 * only converts types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationJsonHttpMessageConverter(Json json) {
		super(json, DEFAULT_JSON_MIME_TYPES);
	}

	/**
	 * Construct a new converter using the provided {@link Json} instance which
	 * only converts types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationJsonHttpMessageConverter(Json json, Predicate<ResolvableType> typePredicate) {
		super(json, typePredicate, DEFAULT_JSON_MIME_TYPES);
	}
}
