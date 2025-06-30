/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.method.annotation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link Map}, {@link MultiValueMap} and {@link HttpHeaders} method
 * arguments annotated with {@code @RequestHeader}. For individual header values
 * annotated with {@code @RequestHeader} see
 * {@link RequestHeaderMethodArgumentResolver} instead.
 *
 * <p>The created {@link Map} contains all request header name/value pairs.
 * The method parameter type may be a {@link HttpHeaders} or a {@link MultiValueMap}
 * to receive all values for a header, not only the first one.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestHeaderMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				(Map.class.isAssignableFrom(parameter.getParameterType()) ||
						HttpHeaders.class.isAssignableFrom(parameter.getParameterType())));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();

		if (HttpHeaders.class.isAssignableFrom(paramType)) {
			HttpHeaders result = new HttpHeaders();
			copyHeaderValues(webRequest, result::add);
			return result;
		}
		else if (MultiValueMap.class.isAssignableFrom(paramType)) {
			MultiValueMap<Object, Object> result = new LinkedMultiValueMap<>();
			copyHeaderValues(webRequest, result::add);
			return result;
		}
		else {
			Map<String, String> result = new LinkedHashMap<>();
			for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext();) {
				String headerName = iterator.next();
				String headerValue = webRequest.getHeader(headerName);
				if (headerValue != null) {
					result.put(headerName, headerValue);
				}
			}
			return result;
		}
	}

	private void copyHeaderValues(NativeWebRequest webRequest, BiConsumer<String, String> consumer) {
		for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext();) {
			String headerName = iterator.next();
			String[] headerValues = webRequest.getHeaderValues(headerName);
			if (headerValues != null) {
				for (String headerValue : headerValues) {
					consumer.accept(headerName, headerValue);
				}
			}
		}
	}

}
