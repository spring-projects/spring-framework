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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;


/**
 * Extension of {@code Encoder} exposing extra methods relevant in the context
 * of HTTP applications.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface HttpEncoder<T> extends Encoder<T> {

	/**
	 * Return "streaming" media types for which flushing should be performed
	 * automatically vs at the end of the input stream.
	 */
	List<MediaType> getStreamingMediaTypes();

	/**
	 * Get decoding hints based on the server request or annotations on the
	 * target controller method parameter.
	 *
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
			MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		return Collections.emptyMap();
	}

}
