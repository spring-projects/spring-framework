/*
 * Copyright 2002-2023 the original author or authors.
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

import org.reactivestreams.Publisher;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestPart;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestPart @RequestPart}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>String -- form field
 * <li>{@link org.springframework.core.io.Resource Resource} -- file part
 * <li>Object -- content to be encoded (e.g. to JSON)
 * <li>{@link HttpEntity} -- part content and headers although generally it's
 * easier to add headers through the returned builder
 * <li>{@link Part} -- a part from a server request
 * <li>{@link Publisher} of any of the above
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class RequestPartArgumentResolver extends AbstractNamedValueArgumentResolver {

	private static final boolean REACTOR_PRESENT =
			ClassUtils.isPresent("reactor.core.publisher.Mono", RequestPartArgumentResolver.class.getClassLoader());


	@Nullable
	private final ReactiveAdapterRegistry reactiveAdapterRegistry;


	/**
	 * Constructor with a {@link HttpExchangeAdapter}, for access to config settings.
	 * @since 6.1
	 */
	public RequestPartArgumentResolver(HttpExchangeAdapter exchangeAdapter) {
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
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestPart annot = parameter.getParameterAnnotation(RequestPart.class);
		return (annot == null ? null :
				new NamedValueInfo(annot.name(), annot.required(), null, "request part", true));
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		if (this.reactiveAdapterRegistry != null) {
			Class<?> type = parameter.getParameterType();
			ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(type);
			if (adapter != null) {
				MethodParameter nestedParameter = parameter.nested();

				String message = "Async type for @RequestPart should produce value(s)";
				Assert.isTrue(!adapter.isNoValue(), message);
				Assert.isTrue(nestedParameter.getNestedParameterType() != Void.class, message);

				if (requestValues instanceof ReactiveHttpRequestValues.Builder reactiveValues) {
					reactiveValues.addRequestPartPublisher(
							name, adapter.toPublisher(value), asParameterizedTypeRef(nestedParameter));
				}
				else {
					throw new IllegalStateException(
							"RequestPart with a reactive type is only supported with reactive client");
				}
				return;
			}
		}

		requestValues.addRequestPart(name, value);
	}

	private static ParameterizedTypeReference<Object> asParameterizedTypeRef(MethodParameter nestedParam) {
		return ParameterizedTypeReference.forType(nestedParam.getNestedGenericParameterType());
	}

}
