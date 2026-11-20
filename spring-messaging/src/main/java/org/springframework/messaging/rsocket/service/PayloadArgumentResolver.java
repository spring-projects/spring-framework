/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
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
