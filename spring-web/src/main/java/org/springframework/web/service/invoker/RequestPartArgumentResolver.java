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

import org.reactivestreams.Publisher;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.Assert;
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

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;


	public RequestPartArgumentResolver(ReactiveAdapterRegistry reactiveAdapterRegistry) {
		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
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

		Class<?> type = parameter.getParameterType();
		ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(type);
		if (adapter != null) {
			Assert.isTrue(!adapter.isNoValue(), "Expected publisher that produces a value");
			Publisher<?> publisher = adapter.toPublisher(value);
			requestValues.addRequestPart(name, publisher, ResolvableType.forMethodParameter(parameter.nested()));
		}
		else {
			requestValues.addRequestPart(name, value);
		}
	}

}
