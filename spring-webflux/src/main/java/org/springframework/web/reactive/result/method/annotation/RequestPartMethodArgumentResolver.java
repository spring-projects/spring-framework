/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolver for method arguments annotated with @{@link RequestPart}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestPartMethodArgumentResolver extends AbstractNamedValueArgumentResolver {

	/**
	 * Class constructor with a default resolution mode flag.
	 * @param registry for checking reactive type wrappers
	 */
	public RequestPartMethodArgumentResolver(ReactiveAdapterRegistry registry) {
		super(null, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestPart.class);
	}


	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestPart ann = parameter.getParameterAnnotation(RequestPart.class);
		return (ann != null ? new RequestPartNamedValueInfo(ann) : new RequestPartNamedValueInfo());
	}

	@Override
	protected Mono<Object> resolveName(String name, MethodParameter param, ServerWebExchange exchange) {
		return exchange.getMultipartData().flatMap(allParts -> {
			List<Part> parts = allParts.get(name);
			if (CollectionUtils.isEmpty(parts)) {
				return Mono.empty();
			}
			return Mono.just(parts.size() == 1 ? parts.get(0) : parts);
		});
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter param, ServerWebExchange exchange) {
		String type = param.getNestedParameterType().getSimpleName();
		String reason = "Required " + type + " parameter '" + name + "' is not present";
		throw new ServerWebInputException(reason, param);
	}


	private static class RequestPartNamedValueInfo extends NamedValueInfo {

		RequestPartNamedValueInfo() {
			super("", false, ValueConstants.DEFAULT_NONE);
		}

		RequestPartNamedValueInfo(RequestPart annotation) {
			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
		}
	}

}
