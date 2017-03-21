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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * An extension of {@code HttpMessageReader} for decoding and reading the
 * request body with extra information available on the server side.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerHttpMessageReader<T> extends HttpMessageReader<T> {

	/**
	 * Decode and read the request body to an object stream.
	 *
	 * @param actualType the actual type of the target method parameter; for
	 * annotated controllers, the {@link MethodParameter} can be accessed via
	 * {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the output stream
	 * @param request the current request
	 * @param response the current response
	 * @param hints additional information about how to read the body
	 * @return the decoded stream of elements
	 */
	Flux<T> read(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints);

	/**
	 * Decode and read the request body to a single object.
	 *
	 * @param actualType the actual type of the target method parameter; for
	 * annotated controllers, the {@link MethodParameter} can be accessed via
	 * {@link ResolvableType#getSource()}.
	 * @param elementType the type of Objects in the output stream
	 * @param request the current request
	 * @param response the current response
	 * @param hints additional information about how to read the body
	 * @return the decoded stream of elements
	 */
	Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints);

}
