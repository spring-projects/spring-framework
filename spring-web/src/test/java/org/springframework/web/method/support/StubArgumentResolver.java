/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.method.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Supports parameters of a given type and resolves them using a stub value.
 * Also records the resolved parameter value.
 *
 * @author Rossen Stoyanchev
 */
public class StubArgumentResolver implements HandlerMethodArgumentResolver {

	private final Class<?> parameterType;

	private final Object stubValue;

	private List<MethodParameter> resolvedParameters = new ArrayList<>();

	public StubArgumentResolver(Class<?> supportedParameterType, Object stubValue) {
		this.parameterType = supportedParameterType;
		this.stubValue = stubValue;
	}

	public List<MethodParameter> getResolvedParameters() {
		return resolvedParameters;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType().equals(this.parameterType);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		this.resolvedParameters.add(parameter);
		return this.stubValue;
	}
}