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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * An extension of {@code HttpMessageWriter} for encoding and writing the
 * response body with extra information available on the server side.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerHttpMessageWriter<T> extends HttpMessageWriter<T> {

	/**
	 * Encode and write the given object stream to the response.
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
	Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints);

}
