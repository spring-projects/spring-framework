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

package org.springframework.web.reactive.method.annotation;

import reactor.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.method.HandlerMethodArgumentResolver;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * Support {@code @RequestParam} but for query params only.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamArgumentResolver implements HandlerMethodArgumentResolver {


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestParam.class);
	}


	@Override
	public Mono<Object> resolveArgument(MethodParameter param, ServerHttpRequest request) {
		RequestParam annotation = param.getParameterAnnotation(RequestParam.class);
		String name = (annotation.value().length() != 0 ? annotation.value() : param.getParameterName());
		UriComponents uriComponents = UriComponentsBuilder.fromUri(request.getURI()).build();
		String value = uriComponents.getQueryParams().getFirst(name);
		return (value != null ? Mono.just(value) : Mono.empty());
	}

}
