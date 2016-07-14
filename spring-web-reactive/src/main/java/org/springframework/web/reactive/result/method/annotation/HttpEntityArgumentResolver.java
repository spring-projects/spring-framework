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
package org.springframework.web.reactive.result.method.annotation;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ModelMap;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments of type {@link HttpEntity} or {@link RequestEntity}
 * by reading the body of the request through a compatible
 * {@code HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpEntityArgumentResolver extends AbstractMessageConverterArgumentResolver
		implements HandlerMethodArgumentResolver {


	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 */
	public HttpEntityArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service) {

		this(converters, service, null);
	}

	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 * @param validator validator to validate decoded objects with
	 */
	public HttpEntityArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service, Validator validator) {

		super(converters, service, validator);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		return (HttpEntity.class.equals(clazz) || RequestEntity.class.equals(clazz));
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter param, ModelMap model, ServerWebExchange exchange) {

		ResolvableType entityType;
		MethodParameter bodyParameter;

		if (getConversionService().canConvert(Mono.class, param.getParameterType())) {
			entityType = ResolvableType.forMethodParameter(param).getGeneric(0);
			bodyParameter = new MethodParameter(param);
			bodyParameter.increaseNestingLevel();
			bodyParameter.increaseNestingLevel();
		}
		else {
			entityType = ResolvableType.forMethodParameter(param);
			bodyParameter = new MethodParameter(param);
			bodyParameter.increaseNestingLevel();
		}

		return readBody(bodyParameter, false, exchange)
				.map(body -> createHttpEntity(body, entityType, exchange))
				.defaultIfEmpty(createHttpEntity(null, entityType, exchange));
	}

	private Object createHttpEntity(Object body, ResolvableType entityType,
			ServerWebExchange exchange) {

		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders headers = request.getHeaders();
		if (RequestEntity.class == entityType.getRawClass()) {
			return new RequestEntity<>(body, headers, request.getMethod(), request.getURI());
		}
		else {
			return new HttpEntity<>(body, headers);
		}
	}

}
