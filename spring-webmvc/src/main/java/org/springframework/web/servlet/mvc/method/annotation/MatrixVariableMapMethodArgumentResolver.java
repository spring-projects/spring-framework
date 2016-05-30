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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves method arguments of type Map annotated with
 * {@link MatrixVariable @MatrixVariable} where the annotation does not
 * specify a name. If a name is specified then the argument will by resolved by the
 * {@link MatrixVariableMethodArgumentResolver} instead.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MatrixVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
		if (matrixVariable != null) {
			if (Map.class.isAssignableFrom(parameter.getParameterType())) {
				return !StringUtils.hasText(matrixVariable.name());
			}
		}
		return false;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) request.getAttribute(
						HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (CollectionUtils.isEmpty(matrixVariables)) {
			return Collections.emptyMap();
		}

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		String pathVariable = parameter.getParameterAnnotation(MatrixVariable.class).pathVar();

		if (!pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
			MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
			if (mapForPathVariable == null) {
				return Collections.emptyMap();
			}
			map.putAll(mapForPathVariable);
		}
		else {
			for (MultiValueMap<String, String> vars : matrixVariables.values()) {
				for (String name : vars.keySet()) {
					for (String value : vars.get(name)) {
						map.add(name, value);
					}
				}
			}
		}

		return (isSingleValueMap(parameter) ? map.toSingleValueMap() : map);
	}

	private boolean isSingleValueMap(MethodParameter parameter) {
		if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
			ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
			if (genericTypes.length == 2) {
				return !List.class.isAssignableFrom(genericTypes[1].getRawClass());
			}
		}
		return false;
	}

}