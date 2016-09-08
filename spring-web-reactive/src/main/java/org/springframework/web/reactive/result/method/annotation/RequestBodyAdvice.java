/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;

/**
 * Allows customizing the request before its body is read and converted into an
 * Object and also allows for processing of the resulting Object before it is
 * passed into a controller method as an {@code @RequestBody} or an
 * {@code HttpEntity} method argument.
 *
 * <p>Implementations of this contract may be registered directly with the
 * {@code RequestMappingHandlerAdapter} or more likely annotated with
 * {@code @ControllerAdvice} in which case they are auto-detected.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface RequestBodyAdvice {

	/**
	 * Invoked first to determine if this interceptor applies.
	 * @param methodParameter the method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param readerType the selected reader type
	 * @param supportedHints thins supported by this {@link HttpMessageReader}
	 * @return whether this interceptor should be invoked or not
	 */
	boolean supports(MethodParameter methodParameter, ResolvableType targetType,
			Class<? extends HttpMessageReader<?>> readerType, List<String> supportedHints);

	/**
	 * Return hints that can be used to customize how the body should be read
	 * @return Additional information about how to read the body
	 */
	default Map<String, Object> getHints(MethodParameter methodParameter, ResolvableType targetType,
			Class<? extends HttpMessageReader<?>> readerType, List<String> supportedHints) {
		return Collections.emptyMap();
	}

	/**
	 * Invoked second before the request body is read and converted.
	 * @param inputMessage the request
	 * @param parameter the target method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param readerType the reader used to deserialize the body
	 * @return the input request or a new instance, never {@code null}
	 */
	default ReactiveHttpInputMessage beforeRead(ReactiveHttpInputMessage inputMessage, MethodParameter parameter,
			ResolvableType targetType, Class<? extends HttpMessageReader<?>> readerType,
			List<String> supportedHints) {
		return inputMessage;
	}

	/**
	 * Invoked third (and last) after the request body is converted to a {@code Flux<Object>}.
	 * @param body set to the converter {@code Publisher<Object>} before the 1st advice is called
	 * @param inputMessage the request
	 * @param parameter the target method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param readerType the reader used to deserialize the body
	 * @return the same body or a new instance
	 */
	default Flux<Object> afterRead(Flux<Object> body, ReactiveHttpInputMessage inputMessage,
			MethodParameter parameter, ResolvableType targetType, Class<? extends HttpMessageReader<?>> readerType,
			List<String> supportedHints) {
		return body;
	}

	/**
	 * Invoked third (and last) after the request body is converted to a {@code Mono<Object>}.
	 * @param body set to the converter {@code Publisher<Object>} before the 1st advice is called
	 * @param inputMessage the request
	 * @param parameter the target method parameter
	 * @param targetType the target type, not necessarily the same as the method
	 * parameter type, e.g. for {@code HttpEntity<String>}.
	 * @param readerType the reader used to deserialize the body
	 * @return the same body or a new instance
	 */
	default Mono<Object> afterReadMono(Mono<Object> body, ReactiveHttpInputMessage inputMessage,
			MethodParameter parameter, ResolvableType targetType, Class<? extends HttpMessageReader<?>> readerType,
			List<String> supportedHints) {
		return body;
	}

}
