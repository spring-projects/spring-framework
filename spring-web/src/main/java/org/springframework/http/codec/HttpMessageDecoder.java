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
 */
public interface HttpMessageDecoder<T> extends Decoder<T> {

	/**
	 * Get decoding hints based on the server request or annotations on the
	 * target controller method parameter.
	 *
	 * @param actualType the actual target type to decode to, possibly a reactive
	 * wrapper and sourced from {@link org.springframework.core.MethodParameter},
	 * i.e. providing access to method parameter annotations.
	 * @param elementType the element type within {@code Flux/Mono} that we're
	 * trying to decode to.
	 * @param request the current request
	 * @param response the current response
	 * @return a Map with hints, possibly empty
	 */
	Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response);

}
