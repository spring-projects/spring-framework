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
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolves method arguments annotated with {@code @RequestBody} by reading the
 * body of the request through a compatible {@code HttpMessageReader}.
 *
 * <p>An {@code @RequestBody} method argument is also validated if it is
 * annotated with {@code @javax.validation.Valid} or
 * {@link org.springframework.validation.annotation.Validated}. Validation
 * failure results in an {@link ServerWebInputException}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestBodyArgumentResolver extends AbstractMessageReaderArgumentResolver
		implements HandlerMethodArgumentResolver {


	/**
	 * Constructor with {@link HttpMessageReader}'s and a {@link Validator}.
	 * @param readers readers for de-serializing the request body with
	 */
	public RequestBodyArgumentResolver(List<HttpMessageReader<?>> readers) {
		super(readers);
	}

	/**
	 * Constructor that also accepts a {@link ReactiveAdapterRegistry}.
	 * @param readers readers for de-serializing the request body with
	 * @param registry for adapting to other reactive types from Flux and Mono
	 */
	public RequestBodyArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter param, BindingContext bindingContext,
			ServerWebExchange exchange) {

		boolean isRequired = param.getParameterAnnotation(RequestBody.class).required();
		return readBody(param, isRequired, bindingContext, exchange);
	}

}
