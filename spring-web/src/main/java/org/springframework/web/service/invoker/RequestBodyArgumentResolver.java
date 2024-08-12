/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestBody @RequestBody}
 * annotated arguments.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 */
public class RequestBodyArgumentResolver implements HttpServiceArgumentResolver {

	private static final boolean REACTOR_PRESENT =
			ClassUtils.isPresent("reactor.core.publisher.Mono", RequestBodyArgumentResolver.class.getClassLoader());


	@Nullable
	private final ReactiveAdapterRegistry reactiveAdapterRegistry;


	/**
	 * Constructor with a {@link HttpExchangeAdapter}, for access to config settings.
	 * @since 6.1
	 */
	public RequestBodyArgumentResolver(HttpExchangeAdapter exchangeAdapter) {
		if (REACTOR_PRESENT) {
			this.reactiveAdapterRegistry =
					(exchangeAdapter instanceof ReactorHttpExchangeAdapter reactorAdapter ?
							reactorAdapter.getReactiveAdapterRegistry() :
							ReactiveAdapterRegistry.getSharedInstance());
		}
		else {
			this.reactiveAdapterRegistry = null;
		}
	}


	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		RequestBody annot = parameter.getParameterAnnotation(RequestBody.class);
		if (annot == null) {
			return false;
		}

		if (argument instanceof Optional<?> optionalValue) {
			argument = optionalValue.orElse(null);
		}

		if (argument == null) {
			Assert.isTrue(!annot.required() || parameter.isOptional(), "RequestBody is required");
			return true;
		}

		if (this.reactiveAdapterRegistry != null) {
			ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(parameter.getParameterType());
			if (adapter != null) {
				MethodParameter nestedParameter = parameter.nested();

				String message = "Async type for @RequestBody should produce value(s)";
				Assert.isTrue(!adapter.isNoValue(), message);
				Assert.isTrue(nestedParameter.getNestedParameterType() != Void.class, message);

				if (requestValues instanceof ReactiveHttpRequestValues.Builder reactiveRequestValues) {
					reactiveRequestValues.setBodyPublisher(
							adapter.toPublisher(argument), asParameterizedTypeRef(nestedParameter));
				}
				else {
					throw new IllegalStateException(
							"RequestBody with a reactive type is only supported with reactive client");
				}

				return true;
			}
		}

		// Not a reactive type
		requestValues.setBodyValue(argument);
		return true;
	}

	private static ParameterizedTypeReference<Object> asParameterizedTypeRef(MethodParameter nestedParam) {
		return ParameterizedTypeReference.forType(nestedParam.getNestedGenericParameterType());
	}

}
