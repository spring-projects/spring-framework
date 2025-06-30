/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
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
 * Resolves arguments of type {@link Map} annotated with {@link MatrixVariable @MatrixVariable}
 * where the annotation does not specify a name. In other words the purpose of this resolver
 * is to provide access to multiple matrix variables, either all or associated with a specific
 * path variable.
 *
 * <p>When a name is specified, an argument of type Map is considered to be a single attribute
 * with a Map value, and is resolved by {@link MatrixVariableMethodArgumentResolver} instead.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MatrixVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
		return (matrixVariable != null && Map.class.isAssignableFrom(parameter.getParameterType()) &&
				!StringUtils.hasText(matrixVariable.name()));
	}

	@Override
	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest request, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) request.getAttribute(
						HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		MultiValueMap<String, String> map = mapMatrixVariables(parameter, matrixVariables);
		return (isSingleValueMap(parameter) ? map.toSingleValueMap() : map);
	}

	private MultiValueMap<String,String> mapMatrixVariables(MethodParameter parameter,
			@Nullable Map<String, MultiValueMap<String, String>> matrixVariables) {

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		if (CollectionUtils.isEmpty(matrixVariables)) {
			return map;
		}
		MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
		Assert.state(ann != null, "No MatrixVariable annotation");
		String pathVariable = ann.pathVar();

		if (!pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
			MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
			if (mapForPathVariable == null) {
				return map;
			}
			map.putAll(mapForPathVariable);
		}
		else {
			for (MultiValueMap<String, String> vars : matrixVariables.values()) {
				vars.forEach((name, values) -> {
					for (String value : values) {
						map.add(name, value);
					}
				});
			}
		}
		return map;
	}

	private boolean isSingleValueMap(MethodParameter parameter) {
		if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
			ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
			if (genericTypes.length == 2) {
				return !List.class.isAssignableFrom(genericTypes[1].toClass());
			}
		}
		return false;
	}

}
