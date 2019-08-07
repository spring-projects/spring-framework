/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolves method arguments annotated with {@code @RequestHeader} except for
 * {@link Map} arguments. See {@link RequestHeaderMapMethodArgumentResolver} for
 * details on {@link Map} arguments annotated with {@code @RequestHeader}.
 *
 * <p>An {@code @RequestHeader} is a named value resolved from a request header.
 * It has a required flag and a default value to fall back on when the request
 * header does not exist.
 *
 * <p>A {@link ConversionService} is invoked to apply type conversion to resolved
 * request header values that don't yet match the method parameter type.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestHeaderMapMethodArgumentResolver
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * @param factory a bean factory to use for resolving  ${...}
	 * placeholder and #{...} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to have expressions
	 * @param registry for checking reactive type wrappers
	 */
	public RequestHeaderMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
			ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, RequestHeader.class, this::singleParam);
	}

	private boolean singleParam(RequestHeader annotation, Class<?> type) {
		return !Map.class.isAssignableFrom(type);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
		Assert.state(ann != null, "No RequestHeader annotation");
		return new RequestHeaderNamedValueInfo(ann);
	}

	@Override
	protected Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		List<String> headerValues = exchange.getRequest().getHeaders().get(name);
		Object result = null;
		if (headerValues != null) {
			result = (headerValues.size() == 1 ? headerValues.get(0) : headerValues);
		}
		return result;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		throw new ServerWebInputException("Missing request header '" + name +
				"' for method parameter of type " + type);
	}


	private static class RequestHeaderNamedValueInfo extends NamedValueInfo {

		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
