/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.codec;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * Strategy for reading from a {@link ReactiveHttpInputMessage} and decoding
 * the stream of bytes to Objects of type {@code <T>}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 * @param <T> the type of objects in the decoded output stream
 */
public interface HttpMessageReader<T> {

	/**
	 * Return the list of media types supported by this reader. The list may not
	 * apply to every possible target element type and calls to this method
	 * should typically be guarded via {@link #canRead(ResolvableType, MediaType)
	 * canWrite(elementType, null)}. The list may also exclude media types
	 * supported only for a specific element type. Alternatively, use
	 * {@link #getReadableMediaTypes(ResolvableType)} for a more precise list.
	 * @return the general list of supported media types
	 */
	List<MediaType> getReadableMediaTypes();

	/**
	 * Return the list of media types supported by this Reader for the given type
	 * of element. This list may differ from {@link #getReadableMediaTypes()}
	 * if the Reader doesn't support the element type, or if it supports it
	 * only for a subset of media types.
	 * @param elementType the type of element to read
	 * @return the list of media types supported for the given class
	 * @since 5.3.4
	 */
	default List<MediaType> getReadableMediaTypes(ResolvableType elementType) {
		return (canRead(elementType, null) ? getReadableMediaTypes() : Collections.emptyList());
	}

	/**
	 * Whether the given object type is supported by this reader.
	 * @param elementType the type of object to check
	 * @param mediaType the media type for the read (possibly {@code null})
	 * @return {@code true} if readable, {@code false} otherwise
	 */
	boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType);

	/**
	 * Read from the input message and decode to a stream of objects.
	 * @param elementType the type of objects in the stream which must have been
	 * previously checked via {@link #canRead(ResolvableType, MediaType)}
	 * @param message the message to read from
	 * @param hints additional information about how to read and decode the input
	 * @return the decoded stream of elements
	 */
	Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

	/**
	 * Read from the input message and decode to a single object.
	 * @param elementType the type of objects in the stream which must have been
	 * previously checked via {@link #canRead(ResolvableType, MediaType)}
	 * @param message the message to read from
	 * @param hints additional information about how to read and decode the input
	 * @return the decoded object
	 */
	Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

	/**
	 * Server-side only alternative to
	 * {@link #read(ResolvableType, ReactiveHttpInputMessage, Map)}
	 * with additional context available.
	 * @param actualType the actual type of the target method parameter;
	 * for annotated controllers, the {@link MethodParameter} can be accessed
	 * via {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the output stream
	 * @param request the current request
	 * @param response the current response
	 * @param hints additional information about how to read the body
	 * @return the decoded stream of elements
	 */
	default Flux<T> read(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		return read(elementType, request, hints);
	}

	/**
	 * Server-side only alternative to
	 * {@link #readMono(ResolvableType, ReactiveHttpInputMessage, Map)}
	 * with additional, context available.
	 * @param actualType the actual type of the target method parameter;
	 * for annotated controllers, the {@link MethodParameter} can be accessed
	 * via {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the output stream
	 * @param request the current request
	 * @param response the current response
	 * @param hints additional information about how to read the body
	 * @return the decoded stream of elements
	 */
	default Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		return readMono(elementType, request, hints);
	}

}
