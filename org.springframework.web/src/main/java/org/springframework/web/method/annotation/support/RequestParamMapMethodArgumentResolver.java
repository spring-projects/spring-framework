/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.annotation.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports {@link Map} arguments annotated with
 * {@link RequestParam @RequestParam}.
 *
 * @author Arjen Poutsma
 */
public class RequestParamMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		RequestParam requestParamAnnot = parameter.getParameterAnnotation(RequestParam.class);
		if (requestParamAnnot != null) {
			if (Map.class.isAssignableFrom(parameter.getParameterType())) {
				return !StringUtils.hasText(requestParamAnnot.value());
			}
		}
		return false;
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public Object resolveArgument(MethodParameter parameter,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) throws Exception {
		Class<?> paramType = parameter.getParameterType();

		Map<String, String[]> parameterMap = webRequest.getParameterMap();
		if (MultiValueMap.class.isAssignableFrom(paramType)) {
			MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(parameterMap.size());
			for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
				for (String value : entry.getValue()) {
					result.add(entry.getKey(), value);
				}
			}
			return result;
		}
		else {
			Map<String, String> result = new LinkedHashMap<String, String>(parameterMap.size());
			for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
				if (entry.getValue().length > 0) {
					result.put(entry.getKey(), entry.getValue()[0]);
				}
			}
			return result;
		}
	}
}
