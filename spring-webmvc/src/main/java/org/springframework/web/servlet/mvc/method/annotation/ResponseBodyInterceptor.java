/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * Allows customizing the response after the execution of an {@code @ResponseBody}
 * or an {@code ResponseEntity} controller method but before the body is written
 * with an {@code HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface ResponseBodyInterceptor {

	/**
	 * Invoked after an {@code HttpMessageConverter} is selected and just before
	 * its write method is invoked.
	 *
	 * @param body the body to be written
	 * @param contentType the selected content type
	 * @param converterType the selected converter that will write the body
	 * @param returnType the return type of the controller method
	 * @param request the current request
	 * @param response the current response
	 * @param <T> the type supported by the message converter
	 *
	 * @return the body that was passed in or a modified, possibly new instance
	 */
	<T> T beforeBodyWrite(T body, MediaType contentType, Class<? extends HttpMessageConverter<T>> converterType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response);

}
