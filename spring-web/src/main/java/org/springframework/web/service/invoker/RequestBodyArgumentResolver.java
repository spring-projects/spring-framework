/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.StreamingHttpOutputMessage;
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


	private final @Nullable ReactiveAdapterRegistry reactiveAdapterRegistry;


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

		if (parameter.getParameterType().equals(StreamingHttpOutputMessage.Body.class)) {
			requestValues.setBodyValue(argument);
			return true;
		}

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
				MethodParameter nestedParam = parameter.nested();

				String message = "Async type for @RequestBody should produce value(s)";
				Assert.isTrue(!adapter.isNoValue(), message);
				Assert.isTrue(nestedParam.getNestedParameterType() != Void.class, message);

				if (requestValues instanceof ReactiveHttpRequestValues.Builder rrv) {
					rrv.setBodyPublisher(
							adapter.toPublisher(argument),
							ParameterizedTypeReference.forType(nestedParam.getNestedGenericParameterType()));
				}
				else {
					throw new IllegalStateException(
							"RequestBody with a reactive type is only supported with reactive client");
				}

				return true;
			}
		}

		// Not a reactive type
		requestValues.setBodyValue(argument, ParameterizedTypeReference.forType(parameter.getGenericParameterType()));
		return true;
	}

}
