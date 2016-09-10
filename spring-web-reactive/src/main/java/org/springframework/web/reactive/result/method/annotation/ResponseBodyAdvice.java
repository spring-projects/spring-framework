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

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Allows customizing the response after the execution of an {@code @ResponseBody}
 * or a {@code ResponseEntity} controller method but before the body is written
 * with an {@code HttpMessageConverter}.
 *
 * <p>Implementations may be may be registered directly with
 * {@code RequestMappingHandlerAdapter} and {@code ExceptionHandlerExceptionResolver}
 * or more likely annotated with {@code @ControllerAdvice} in which case they
 * will be auto-detected by both.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ResponseBodyAdvice {

	/**
	 * Whether this component supports the given controller method return type
	 * and the selected {@code HttpMessageConverter} type.
	 * @param returnType the return type
	 * @param writerType the selected writer type
	 * @param supportedHints thins supported by this {@link HttpMessageWriter}
	 * @return {@code true} if {@link #beforeBodyWrite} should be invoked, {@code false} otherwise
	 */
	boolean supports(MethodParameter returnType, Class<? extends HttpMessageWriter<?>> writerType, List<String> supportedHints);

	/**
	 * Return hints that can be used to customize how the body should be written
	 * @return Additional information about how to write the body
	 */
	default Map<String, Object> getHints(MethodParameter returnType, ResolvableType targetType,
			Class<? extends HttpMessageWriter<?>> writerType, List<String> supportedHints) {
		return Collections.emptyMap();
	}

	/**
	 * Invoked after an {@code HttpMessageConverter} is selected and just before
	 * its write method is invoked.
	 * @param body the body stream to be written
	 * @param returnType the return type of the controller method
	 * @param selectedContentType the content type selected through content negotiation
	 * @param selectedWriterType the converter type selected to write to the response
	 * @param response the current HTTP response
	 *
	 * @return the body stream that was passed in or a modified, possibly new instance
	 */
	default Publisher<Object> beforeBodyWrite(Publisher<Object> body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageWriter<?>> selectedWriterType,
			ServerHttpResponse response, List<String> supportedHints) {
		return body;
	}

}
