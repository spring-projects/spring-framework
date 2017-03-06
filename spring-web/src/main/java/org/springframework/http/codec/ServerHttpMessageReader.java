/*
 * Copyright 2002-2016 the original author or authors.
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
 * Server oriented {@link HttpMessageReader} that allows to resolve hints using annotations or
 * perform additional operation using {@link ServerHttpRequest} or {@link ServerHttpResponse}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerHttpMessageReader<T> extends HttpMessageReader<T> {

	/**
	 * Read a {@link Flux} of the given type form the given input message with additional server related
	 * parameters which could be used to create some hints or set the response status for example.
	 *
	 * Return hints that can be used to customize how the body should be read
	 * @param streamType the original type used in the method parameter. For annotation
	 * based controllers, the {@link MethodParameter} is available via {@link ResolvableType#getSource()}.
	 * @param elementType the stream element type to return
	 * Typically the value of a {@code Content-Type} header.
	 * @param request the current HTTP request
	 * @param response the current HTTP response
	 * @param hints additional information about how to read the body
	 * @return the converted {@link Flux} of elements
	 */
	Flux<T> read(ResolvableType streamType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints);

	/**
	 * Read a {@link Mono} of the given type form the given input message with additional server related
	 * parameters which could be used to create some hints or set the response status for example.
	 *
	 * Return hints that can be used to customize how the body should be read
	 * @param streamType the original type used in the method parameter. For annotation
	 * based controllers, the {@link MethodParameter} is available via {@link ResolvableType#getSource()}.
	 * @param elementType the stream element type to return
	 * Typically the value of a {@code Content-Type} header.
	 * @param request the current HTTP request
	 * @param response the current HTTP response
	 * @param hints additional information about how to read the body
	 * @return the converted {@link Mono} of object
	 */
	Mono<T> readMono(ResolvableType streamType, ResolvableType elementType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints);

}
