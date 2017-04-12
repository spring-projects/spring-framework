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
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolver for method arguments annotated with @{@link RequestParam}.
 *
 * <p>This resolver can also be created in default resolution mode in which
 * simple types (int, long, etc.) not annotated with @{@link RequestParam} are
 * also treated as request parameters with the parameter name derived from the
 * argument name.
 *
 * <p>If the method parameter type is {@link Map}, the name specified in the
 * annotation is used to resolve the request parameter String value. The value is
 * then converted to a {@link Map} via type conversion assuming a suitable
 * {@link Converter} has been registered. Or if a request parameter name is not
 * specified the {@link RequestParamMapMethodArgumentResolver} is used instead
 * to provide access to all request parameters in the form of a map.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestParamMapMethodArgumentResolver
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	private final boolean useDefaultResolution;


	/**
	 * Class constructor with a default resolution mode flag.
	 * @param factory a bean factory used for resolving  ${...} placeholder
	 * and #{...} SpEL expressions in default values, or {@code null} if default
	 * values are not expected to contain expressions
	 * @param registry for checking reactive type wrappers
	 * @param useDefaultResolution in default resolution mode a method argument
	 * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
	 * is treated as a request parameter even if it isn't annotated, the
	 * request parameter name is derived from the method parameter name.
	 */
	public RequestParamMethodArgumentResolver(
			ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry, boolean useDefaultResolution) {

		super(factory, registry);
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		if (checkAnnotatedParamNoReactiveWrapper(param, RequestParam.class, this::singleParam)) {
			return true;
		}
		else if (this.useDefaultResolution) {
			return checkParameterTypeNoReactiveWrapper(param, BeanUtils::isSimpleProperty) ||
					BeanUtils.isSimpleProperty(param.nestedIfOptional().getNestedParameterType());
		}
		return false;
	}

	private boolean singleParam(RequestParam requestParam, Class<?> type) {
		return !Map.class.isAssignableFrom(type) || StringUtils.hasText(requestParam.name());
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
		return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
	}

	@Override
	protected Optional<Object> resolveNamedValue(String name, MethodParameter parameter,
			ServerWebExchange exchange) {

		List<String> paramValues = getRequestParams(exchange).get(name);
		Object result = null;
		if (paramValues != null) {
			result = (paramValues.size() == 1 ? paramValues.get(0) : paramValues);
		}
		return Optional.ofNullable(result);
	}

	private MultiValueMap<String, String> getRequestParams(ServerWebExchange exchange) {
		MultiValueMap<String, String> params = exchange.getRequestParams().subscribe().peek();
		Assert.notNull(params, "Expected form data (if any) to be parsed.");
		return params;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Required " + type + " parameter '" + name + "' is not present";
		throw new ServerWebInputException(reason, parameter);
	}


	private static class RequestParamNamedValueInfo extends NamedValueInfo {

		RequestParamNamedValueInfo() {
			super("", false, ValueConstants.DEFAULT_NONE);
		}

		RequestParamNamedValueInfo(RequestParam annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
