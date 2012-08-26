/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves method arguments annotated with an {@link MatrixVariable @PathParam}.
 *
 * <p>If the method parameter is of type Map and no name is specified, then it will
 * by resolved by the {@link MatrixVariableMapMethodArgumentResolver} instead.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MatrixVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public MatrixVariableMethodArgumentResolver() {
		super(null);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		if (!parameter.hasParameterAnnotation(MatrixVariable.class)) {
			return false;
		}
		if (Map.class.isAssignableFrom(parameter.getParameterType())) {
			String paramName = parameter.getParameterAnnotation(MatrixVariable.class).value();
			return StringUtils.hasText(paramName);
		}
		return true;
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		MatrixVariable annotation = parameter.getParameterAnnotation(MatrixVariable.class);
		return new PathParamNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, MultiValueMap<String, String>> pathParameters =
			(Map<String, MultiValueMap<String, String>>) request.getAttribute(
					HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (CollectionUtils.isEmpty(pathParameters)) {
			return null;
		}

		String pathVar = parameter.getParameterAnnotation(MatrixVariable.class).pathVar();
		List<String> paramValues = null;

		if (!pathVar.equals(ValueConstants.DEFAULT_NONE)) {
			if (pathParameters.containsKey(pathVar)) {
				paramValues = pathParameters.get(pathVar).get(name);
			}
		}
		else {
			boolean found = false;
			paramValues = new ArrayList<String>();
			for (MultiValueMap<String, String> params : pathParameters.values()) {
				if (params.containsKey(name)) {
					if (found) {
						String paramType = parameter.getParameterType().getName();
						throw new ServletRequestBindingException(
								"Found more than one match for URI path parameter '" + name +
								"' for parameter type [" + paramType + "]. Use pathVar attribute to disambiguate.");
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
	protected void handleMissingValue(String name, MethodParameter param) throws ServletRequestBindingException {
		String paramType = param.getParameterType().getName();
		throw new ServletRequestBindingException(
				"Missing URI path parameter '" + name + "' for method parameter type [" + paramType + "]");
	}


	private static class PathParamNamedValueInfo extends NamedValueInfo {

		private PathParamNamedValueInfo(MatrixVariable annotation) {
			super(annotation.value(), annotation.required(), annotation.defaultValue());
		}
	}
}