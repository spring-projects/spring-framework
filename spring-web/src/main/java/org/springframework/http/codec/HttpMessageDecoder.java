/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Extension of {@code Decoder} exposing extra methods relevant in the context
 * of HTTP request or response body decoding.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the output stream
 */
public interface HttpMessageDecoder<T> extends Decoder<T> {

	/**
	 * Get decoding hints based on the server request or annotations on the
	 * target controller method parameter.
	 * @param actualType the actual target type to decode to, possibly a reactive
	 * wrapper and sourced from {@link org.springframework.core.MethodParameter},
	 * i.e. providing access to method parameter annotations
	 * @param elementType the element type within {@code Flux/Mono} that we're
	 * trying to decode to
	 * @param request the current request
	 * @param response the current response
	 * @return a Map with hints, possibly empty
	 */
	Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response);

}
