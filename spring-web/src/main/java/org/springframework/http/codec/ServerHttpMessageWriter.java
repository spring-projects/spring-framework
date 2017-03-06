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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Server oriented {@link HttpMessageWriter} that allows to resolve hints using annotations or
 * perform additional operation using {@link ServerHttpRequest} or {@link ServerHttpResponse}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerHttpMessageWriter<T> extends HttpMessageWriter<T> {

	/**
	 * Write a given object to the given output message with additional server related
	 * parameters which could be used to create some hints or set the response status for example.
	 *
	 * @param streamType the original type used for the method return value. For annotation
	 * based controllers, the {@link MethodParameter} is available via {@link ResolvableType#getSource()}.
	 * Can be {@code null}.
	 * @param elementType the stream element type to process
	 * @param mediaType the content type to use when writing. May be {@code null} to
	 * indicate that the default content type of the converter must be used.
	 * @param request the current HTTP request
	 * @param response the current HTTP response
	 * @return a {@link Mono} that indicates completion or error
	 */
	Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints);

}
