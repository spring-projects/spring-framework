/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestBody @RequestBody}
 * annotated arguments.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class RequestBodyArgumentResolver implements HttpServiceArgumentResolver {

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;


	public RequestBodyArgumentResolver(ReactiveAdapterRegistry reactiveAdapterRegistry) {
		Assert.notNull(reactiveAdapterRegistry, "ReactiveAdapterRegistry is required");
		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
	}


	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		RequestBody annot = parameter.getParameterAnnotation(RequestBody.class);
		if (annot == null) {
			return false;
		}

		if (argument != null) {
			ReactiveAdapter reactiveAdapter = this.reactiveAdapterRegistry.getAdapter(parameter.getParameterType());
			if (reactiveAdapter == null) {
				requestValues.setBodyValue(argument);
			}
			else {
				MethodParameter nestedParameter = parameter.nested();

				String message = "Async type for @RequestBody should produce value(s)";
				Assert.isTrue(!reactiveAdapter.isNoValue(), message);
				Assert.isTrue(nestedParameter.getNestedParameterType() != Void.class, message);

				requestValues.setBody(
						reactiveAdapter.toPublisher(argument),
						ParameterizedTypeReference.forType(nestedParameter.getNestedGenericParameterType()));
			}
		}

		return true;
	}

}
