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

import java.util.Collections;
import java.util.Map;

import org.springframework.core.MethodParameter;
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
 * {@link MatrixVariable @MatrixVariable} where the annotation the does not
 * specify a name. If a name specified then the argument will by resolved by the
 * {@link MatrixVariableMethodArgumentResolver} instead.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MatrixVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		MatrixVariable paramAnnot = parameter.getParameterAnnotation(MatrixVariable.class);
		if (paramAnnot != null) {
			if (Map.class.isAssignableFrom(parameter.getParameterType())) {
				return !StringUtils.hasText(paramAnnot.value());
			}
		}
		return false;
	}

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) request.getAttribute(
						HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (CollectionUtils.isEmpty(matrixVariables)) {
			return Collections.emptyMap();
		}

		String pathVariable = parameter.getParameterAnnotation(MatrixVariable.class).pathVar();

		if (!pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
			MultiValueMap<String, String> map = matrixVariables.get(pathVariable);
			return (map != null) ? map : Collections.emptyMap();
		}

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		for (MultiValueMap<String, String> vars : matrixVariables.values()) {
			for (String name : vars.keySet()) {
				for (String value : vars.get(name)) {
					map.add(name, value);
				}
			}
		}

		return map;
	}

}