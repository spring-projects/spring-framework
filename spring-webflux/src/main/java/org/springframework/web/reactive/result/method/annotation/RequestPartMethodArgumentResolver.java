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
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolver for method arguments annotated with @{@link RequestPart}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see RequestParamMapMethodArgumentResolver
 */
public class RequestPartMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

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
	public RequestPartMethodArgumentResolver(
			ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry, boolean useDefaultResolution) {

		super(factory, registry);
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		if (checkAnnotatedParamNoReactiveWrapper(param, RequestPart.class, this::singleParam)) {
			return true;
		}
		else if (this.useDefaultResolution) {
			return checkParameterTypeNoReactiveWrapper(param, BeanUtils::isSimpleProperty) ||
					BeanUtils.isSimpleProperty(param.nestedIfOptional().getNestedParameterType());
		}
		return false;
	}

	private boolean singleParam(RequestPart requestParam, Class<?> type) {
		return !Map.class.isAssignableFrom(type) || StringUtils.hasText(requestParam.name());
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestPart ann = parameter.getParameterAnnotation(RequestPart.class);
		return (ann != null ? new RequestPartNamedValueInfo(ann) : new RequestPartNamedValueInfo());
	}

	@Override
	protected Optional<Object> resolveNamedValue(String name, MethodParameter parameter,
			ServerWebExchange exchange) {

		List<?> paramValues = getMultipartData(exchange).get(name);
		Object result = null;
		if (paramValues != null) {
			result = (paramValues.size() == 1 ? paramValues.get(0) : paramValues);
		}
		return Optional.ofNullable(result);
	}

	private MultiValueMap<String, Part> getMultipartData(ServerWebExchange exchange) {
		MultiValueMap<String, Part> params = exchange.getMultipartData().subscribe().peek();
		Assert.notNull(params, "Expected multipart data (if any) to be parsed.");
		return params;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Required " + type + " parameter '" + name + "' is not present";
		throw new ServerWebInputException(reason, parameter);
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
