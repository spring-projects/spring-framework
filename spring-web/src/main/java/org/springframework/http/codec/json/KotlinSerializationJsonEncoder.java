/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.json;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import kotlinx.serialization.json.Json;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationStringEncoder;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 * It supports {@code application/json}, {@code application/x-ndjson} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * <p>As of Spring Framework 7.0, by default it only encodes types annotated with
 * {@link kotlinx.serialization.Serializable @Serializable} at type or generics
 * level.
 *
 * <p>Alternative constructors with a {@code Predicate<ResolvableType>}
 * parameter can be used to customize this behavior. For example,
 * {@code new KotlinSerializationJsonEncoder(type -> true)} will encode all types
 * supported by Kotlin Serialization, including unannotated Kotlin enumerations,
 * numbers, characters, booleans and strings.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 * @see KotlinSerializationJsonDecoder
 */
public class KotlinSerializationJsonEncoder extends KotlinSerializationStringEncoder<Json> {

	private static final MimeType[] DEFAULT_JSON_MIME_TYPES = new MimeType[] {
			MediaType.APPLICATION_JSON,
			new MediaType("application", "*+json"),
			MediaType.APPLICATION_NDJSON
	};

	/**
	 * Construct a new encoder using {@link Json.Default} instance which
	 * only encodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationJsonEncoder() {
		this(Json.Default);
	}

	/**
	 * Construct a new encoder using {@link Json.Default} instance which
	 * only encodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationJsonEncoder(Predicate<ResolvableType> typePredicate) {
		this(Json.Default, typePredicate);
	}

	/**
	 * Construct a new encoder using the provided {@link Json} instance which
	 * only encodes types annotated with {@link kotlinx.serialization.Serializable @Serializable}
	 * at type or generics level.
	 */
	public KotlinSerializationJsonEncoder(Json json) {
		super(json, DEFAULT_JSON_MIME_TYPES);
		setStreamingMediaTypes(List.of(MediaType.APPLICATION_NDJSON));
	}

	/**
	 * Construct a new encoder using the provided {@link Json} instance which
	 * only encodes types for which the specified predicate returns {@code true}.
	 * @since 7.0
	 */
	public KotlinSerializationJsonEncoder(Json json, Predicate<ResolvableType> typePredicate) {
		super(json, typePredicate, DEFAULT_JSON_MIME_TYPES);
		setStreamingMediaTypes(List.of(MediaType.APPLICATION_NDJSON));
	}

	@Override
	public Flux<DataBuffer> encodeNonStream(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		JsonArrayJoinHelper helper = new JsonArrayJoinHelper();
		return Flux.from(inputStream)
				.map(value -> encodeStreamingValue(value, bufferFactory, elementType, mimeType, hints,
						helper.getPrefix(), EMPTY_BYTES))
				.switchIfEmpty(Mono.fromCallable(() -> bufferFactory.wrap(helper.getPrefix())))
				.concatWith(Mono.fromCallable(() -> bufferFactory.wrap(helper.getSuffix())));
	}


	private static class JsonArrayJoinHelper {

		private static final byte[] COMMA_SEPARATOR = {','};

		private static final byte[] OPEN_BRACKET = {'['};

		private static final byte[] CLOSE_BRACKET = {']'};

		private boolean firstItemEmitted;

		public byte[] getPrefix() {
			byte[] prefix = (this.firstItemEmitted ? COMMA_SEPARATOR : OPEN_BRACKET);
			this.firstItemEmitted = true;
			return prefix;
		}

		public byte[] getSuffix() {
			return CLOSE_BRACKET;
		}
	}

}
