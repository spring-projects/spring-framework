/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * A convenient base class for {@code ResponseBodyInterceptor} implementations
 * that customize the response before JSON serialization with
 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
 * MappingJackson2HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class AbstractMappingJacksonResponseBodyInterceptor implements ResponseBodyInterceptor {


	protected AbstractMappingJacksonResponseBodyInterceptor() {
	}


	@SuppressWarnings("unchecked")
	@Override
	public final <T> T beforeBodyWrite(T body, MediaType contentType,
			Class<? extends HttpMessageConverter<T>> converterType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

		if (!MappingJackson2HttpMessageConverter.class.equals(converterType)) {
			return body;
		}
		MappingJacksonValue container = getOrCreateContainer(body);
		beforeBodyWriteInternal(container, contentType, returnType, request, response);
		return (T) container;
	}

	/**
	 * Wrap the body in a {@link MappingJacksonValue} value container (for providing
	 * additional serialization instructions) or simply cast it if already wrapped.
	 */
	protected MappingJacksonValue getOrCreateContainer(Object body) {
		return (body instanceof MappingJacksonValue) ? (MappingJacksonValue) body : new MappingJacksonValue(body);
	}

	/**
	 * Invoked only if the converter type is {@code MappingJackson2HttpMessageConverter}.
	 */
	protected abstract void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response);


}
