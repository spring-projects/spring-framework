/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolver for {@code @RequestPart} arguments where the named part is decoded
 * much like an {@code @RequestBody} argument but based on the content of an
 * individual part instead. The arguments may be wrapped with a reactive type
 * for a single value (e.g. Reactor {@code Mono}, RxJava {@code Single}).
 *
 * <p>This resolver also supports arguments of type {@link Part} which may be
 * wrapped with are reactive type for a single or multiple values.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageReaderArgumentResolver {

	public RequestPartMethodArgumentResolver(List<HttpMessageReader<?>> readers, ReactiveAdapterRegistry registry) {
		super(readers, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(RequestPart.class) ||
				checkParameterType(parameter, Part.class::isAssignableFrom));
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
		boolean isRequired = (requestPart == null || requestPart.required());
		String name = getPartName(parameter, requestPart);

		Flux<Part> values = exchange.getMultipartData()
				.flatMapMany(map -> {
					List<Part> parts = map.get(name);
					if (CollectionUtils.isEmpty(parts)) {
						return (isRequired ? Flux.error(getMissingPartException(name, parameter)) : Flux.empty());
					}
					return Flux.fromIterable(parts);
				});

		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(parameter.getParameterType());
		MethodParameter elementType = adapter != null ? parameter.nested() : parameter;

		if (Part.class.isAssignableFrom(elementType.getNestedParameterType())) {
			if (adapter != null) {
				values = adapter.isMultiValue() ? values : values.take(1);
				return Mono.just(adapter.fromPublisher(values));
			}
			else {
				return values.next().cast(Object.class);
			}
		}

		return values.next().flatMap(part -> {
			ServerHttpRequest partRequest = new PartServerHttpRequest(exchange.getRequest(), part);
			ServerWebExchange partExchange = exchange.mutate().request(partRequest).build();
			return readBody(parameter, isRequired, bindingContext, partExchange);
		});
	}

	private String getPartName(MethodParameter methodParam, @Nullable RequestPart requestPart) {
		String partName = (requestPart != null ? requestPart.name() : "");
		if (partName.isEmpty()) {
			partName = methodParam.getParameterName();
			if (partName == null) {
				throw new IllegalArgumentException("Request part name for argument type [" +
						methodParam.getNestedParameterType().getName() +
						"] not specified, and parameter name information not found in class file either.");
			}
		}
		return partName;
	}

	private ServerWebInputException getMissingPartException(String name, MethodParameter param) {
		String reason = "Required request part '" + name + "' is not present";
		return new ServerWebInputException(reason, param);
	}


	private static class PartServerHttpRequest extends ServerHttpRequestDecorator {

		private final Part part;

		public PartServerHttpRequest(ServerHttpRequest delegate, Part part) {
			super(delegate);
			this.part = part;
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.part.headers();
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return this.part.content();
		}
	}

}
