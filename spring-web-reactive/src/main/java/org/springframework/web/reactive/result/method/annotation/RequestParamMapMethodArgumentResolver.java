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

import java.util.Map;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver for {@link Map} method arguments annotated with
 * {@link RequestParam @RequestParam} where the annotation does not specify a
 * request parameter name. See {@link RequestParamMethodArgumentResolver} for
 * resolving {@link Map} method arguments with a request parameter name.
 *
 * <p>The created {@link Map} contains all request parameter name-value pairs.
 * If the method parameter type is {@link MultiValueMap} instead, the created
 * map contains all request parameters and all there values for cases where
 * request parameters have multiple values.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestParamMethodArgumentResolver
 */
public class RequestParamMapMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
		if (requestParam != null) {
			if (Map.class.isAssignableFrom(parameter.getParameterType())) {
				return !StringUtils.hasText(requestParam.name());
			}
		}
		return false;
	}

	@Override
	public Optional<Object> resolveArgumentValue(MethodParameter parameter, BindingContext context,
			ServerWebExchange exchange) {

		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		Object value = (isMultiValueMap(parameter) ? queryParams : queryParams.toSingleValueMap());
		return Optional.of(value);
	}

	private boolean isMultiValueMap(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return MultiValueMap.class.isAssignableFrom(paramType);
	}

}
