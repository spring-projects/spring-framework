/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec;

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Strategy for encoding a stream of objects of type {@code <T>} and writing
 * the encoded stream of bytes to an {@link ReactiveHttpOutputMessage}.
 *
 * @param <T> the type of objects in the input stream
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HttpMessageWriter<T> {

	/**
	 * Return the {@link MediaType}'s that this writer supports.
	 */
	List<MediaType> getWritableMediaTypes();

	/**
	 * Whether the given object type is supported by this writer.
	 * @param elementType the type of object to check
	 * @param mediaType the media type for the write, possibly {@code null}
	 * @return {@code true} if writable, {@code false} otherwise
	 */
	boolean canWrite(ResolvableType elementType, MediaType mediaType);

	/**
	 * Write an given stream of object to the output message.
	 * @param inputStream the objects to write
	 * @param elementType the type of objects in the stream which must have been
	 * previously checked via {@link #canWrite(ResolvableType, MediaType)}
	 * @param mediaType the content type for the write, possibly {@code null} to
	 * indicate that the default content type of the writer must be used.
	 * @param message the message to write to
	 * @param hints additional information about how to encode and write
	 * @return indicates completion or error
	 */
	Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints);


	/**
	 * Server-side only alternative to
	 * {@link #write(Publisher, ResolvableType, MediaType, ReactiveHttpOutputMessage, Map)}
	 * with additional context available.
	 *
	 * @param actualType the actual return type of the method that returned the
	 * value; for annotated controllers, the {@link MethodParameter} can be
	 * accessed via {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the input stream
	 * @param mediaType the content type to use, possibly {@code null} indicating
	 * the default content type of the writer should be used.
	 * @param request the current request
	 * @param response the current response
	 * @return a {@link Mono} that indicates completion of writing or error
	 */
	default Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		return write(inputStream, elementType, mediaType, response, hints);
	}

}
