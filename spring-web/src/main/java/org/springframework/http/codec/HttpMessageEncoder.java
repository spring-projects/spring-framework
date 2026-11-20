/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Extension of {@code Encoder} exposing extra methods relevant in the context
 * of HTTP request or response body encoding.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the input stream
 */
public interface HttpMessageEncoder<T> extends Encoder<T> {

	/**
	 * Return "streaming" media types for which flushing should be performed
	 * automatically vs at the end of the input stream.
	 */
	List<MediaType> getStreamingMediaTypes();

	/**
	 * Get decoding hints based on the server request or annotations on the
	 * target controller method parameter.
	 * @param actualType the actual source type to encode, possibly a reactive
	 * wrapper and sourced from {@link org.springframework.core.MethodParameter},
	 * i.e. providing access to method annotations.
	 * @param elementType the element type within {@code Flux/Mono} that we're
	 * trying to encode.
	 * @param request the current request
	 * @param response the current response
	 * @return a Map with hints, possibly empty
	 */
	default Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		return Hints.none();
	}

}
