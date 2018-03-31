/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolves arguments annotated with {@link MatrixVariable @MatrixVariable}.
 *
 * <p>If the method parameter is of type {@link Map} it will by resolved by
 * {@link MatrixVariableMapMethodArgumentResolver} instead unless the annotation
 * specifies a name in which case it is considered to be a single attribute of
 * type map (vs multiple attributes collected in a map).
 *
 * @author Rossen Stoyanchev
 * @since 5.0.1
 * @see MatrixVariableMapMethodArgumentResolver
 */
public class MatrixVariableMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	public MatrixVariableMethodArgumentResolver(
			@Nullable ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkAnnotatedParamNoReactiveWrapper(parameter, MatrixVariable.class,
				(ann, type) -> !Map.class.isAssignableFrom(type) || StringUtils.hasText(ann.name()));
	}


	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(ann != null, "No MatrixVariable annotation");
		return new MatrixVariableNamedValueInfo(ann);
	}

	@Nullable
	@Override
	protected Object resolveNamedValue(String name, MethodParameter param, ServerWebExchange exchange) {
		Map<String, MultiValueMap<String, String>> pathParameters =
				exchange.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);
		if (CollectionUtils.isEmpty(pathParameters)) {
			return null;
		}

		MatrixVariable ann = param.getParameterAnnotation(MatrixVariable.class);
		Assert.state(ann != null, "No MatrixVariable annotation");
		String pathVar = ann.pathVar();
		List<String> paramValues = null;

		if (!pathVar.equals(ValueConstants.DEFAULT_NONE)) {
			if (pathParameters.containsKey(pathVar)) {
				paramValues = pathParameters.get(pathVar).get(name);
			}
		}
		else {
			boolean found = false;
			paramValues = new ArrayList<>();
			for (MultiValueMap<String, String> params : pathParameters.values()) {
				if (params.containsKey(name)) {
					if (found) {
						String paramType = param.getNestedParameterType().getName();
						throw new ServerErrorException(
								"Found more than one match for URI path parameter '" + name +
								"' for parameter type [" + paramType + "]. Use 'pathVar' attribute to disambiguate.",
								param, null);
					}
					paramValues.addAll(params.get(name));
					found = true;
				}
			}
		}

		if (CollectionUtils.isEmpty(paramValues)) {
			return null;
		}
		else if (paramValues.size() == 1) {
			return paramValues.get(0);
		}
		else {
			return paramValues;
		}
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServerWebInputException {
		throw new ServerWebInputException("Missing matrix variable '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
	}


	private static class MatrixVariableNamedValueInfo extends NamedValueInfo {

		private MatrixVariableNamedValueInfo(MatrixVariable annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
