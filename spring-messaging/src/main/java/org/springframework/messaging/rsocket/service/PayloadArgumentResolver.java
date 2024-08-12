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

package org.springframework.messaging.rsocket.service;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.Assert;

/**
 * {@link RSocketServiceArgumentResolver} for {@link Payload @Payload}
 * annotated arguments.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 */
public class PayloadArgumentResolver implements RSocketServiceArgumentResolver {

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;

	private final boolean useDefaultResolution;


	public PayloadArgumentResolver(ReactiveAdapterRegistry reactiveAdapterRegistry, boolean useDefaultResolution) {
		this.useDefaultResolution = useDefaultResolution;
		Assert.notNull(reactiveAdapterRegistry, "ReactiveAdapterRegistry is required");
		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
	}


	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, RSocketRequestValues.Builder requestValues) {

		Payload annot = parameter.getParameterAnnotation(Payload.class);
		if (annot == null && !this.useDefaultResolution) {
			return false;
		}

		if (argument == null) {
			boolean isOptional = ((annot != null && !annot.required()) || parameter.isOptional());
			Assert.isTrue(isOptional, () -> "Missing payload");
			return true;
		}

		ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(parameter.getParameterType());
		if (adapter == null) {
			requestValues.setPayloadValue(argument);
		}
		else {
			MethodParameter nestedParameter = parameter.nested();

			String message = "Async type for @Payload should produce value(s)";
			Assert.isTrue(nestedParameter.getNestedParameterType() != Void.class, message);
			Assert.isTrue(!adapter.isNoValue(), message);

			requestValues.setPayload(
					adapter.toPublisher(argument),
					ParameterizedTypeReference.forType(nestedParameter.getNestedGenericParameterType()));
		}

		return true;
	}
}
