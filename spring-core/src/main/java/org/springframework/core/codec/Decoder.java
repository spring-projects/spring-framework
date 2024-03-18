/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.codec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Strategy for decoding a {@link DataBuffer} input stream into an output stream
 * of elements of type {@code <T>}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the output stream
 */
public interface Decoder<T> {

	/**
	 * Whether the decoder supports the given target element type and the MIME
	 * type of the source stream.
	 * @param elementType the target element type for the output stream
	 * @param mimeType the mime type associated with the stream to decode
	 * (can be {@code null} if not specified)
	 * @return {@code true} if supported, {@code false} otherwise
	 */
	boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType);

	/**
	 * Decode a {@link DataBuffer} input stream into a Flux of {@code T}.
	 * @param inputStream the {@code DataBuffer} input stream to decode
	 * @param elementType the expected type of elements in the output stream;
	 * this type must have been previously passed to the {@link #canDecode}
	 * method and it must have returned {@code true}.
	 * @param mimeType the MIME type associated with the input stream (optional)
	 * @param hints additional information about how to do decode
	 * @return the output stream with decoded elements
	 */
	Flux<T> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

	/**
	 * Decode a {@link DataBuffer} input stream into a Mono of {@code T}.
	 * @param inputStream the {@code DataBuffer} input stream to decode
	 * @param elementType the expected type of elements in the output stream;
	 * this type must have been previously passed to the {@link #canDecode}
	 * method and it must have returned {@code true}.
	 * @param mimeType the MIME type associated with the input stream (optional)
	 * @param hints additional information about how to do decode
	 * @return the output stream with the decoded element
	 */
	Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

	/**
	 * Decode a data buffer to an Object of type T. This is useful for scenarios,
	 * that distinct messages (or events) are decoded and handled individually,
	 * in fully aggregated form.
	 * @param buffer the {@code DataBuffer} to decode
	 * @param targetType the expected output type
	 * @param mimeType the MIME type associated with the data
	 * @param hints additional information about how to do decode
	 * @return the decoded value, possibly {@code null}
	 * @since 5.2
	 */
	@Nullable
	default T decode(DataBuffer buffer, ResolvableType targetType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		CompletableFuture<T> future = decodeToMono(Mono.just(buffer), targetType, mimeType, hints).toFuture();
		Assert.state(future.isDone(), "DataBuffer decoding should have completed");

		try {
			return future.get();
		}
		catch (ExecutionException ex) {
			Throwable cause = ex.getCause();
			throw (cause instanceof CodecException codecException ? codecException :
					new DecodingException("Failed to decode: " + (cause != null ? cause.getMessage() : ex), cause));
		}
		catch (InterruptedException ex) {
			throw new DecodingException("Interrupted during decode", ex);
		}
	}

	/**
	 * Return the list of MIME types supported by this Decoder. The list may not
	 * apply to every possible target element type and calls to this method
	 * should typically be guarded via {@link #canDecode(ResolvableType, MimeType)
	 * canDecode(elementType, null)}. The list may also exclude MIME types
	 * supported only for a specific element type. Alternatively, use
	 * {@link #getDecodableMimeTypes(ResolvableType)} for a more precise list.
	 * @return the list of supported MIME types
	 */
	List<MimeType> getDecodableMimeTypes();

	/**
	 * Return the list of MIME types supported by this Decoder for the given type
	 * of element. This list may differ from {@link #getDecodableMimeTypes()}
	 * if the Decoder doesn't support the given element type or if it supports
	 * it only for a subset of MIME types.
	 * @param targetType the type of element to check for decoding
	 * @return the list of MIME types supported for the given target type
	 * @since 5.3.4
	 */
	default List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
		return (canDecode(targetType, null) ? getDecodableMimeTypes() : Collections.emptyList());
	}

}
