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

package org.springframework.web.method.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Resolves a method argument from a stub value. Records all resolved parameters.
 * 
 * @author Rossen Stoyanchev
 */
public class StubArgumentResolver implements HandlerMethodArgumentResolver {

	private final Class<?> supportedType;

	private final Object stubValue;
	
	private final boolean usesResponse;
	
	private List<MethodParameter> resolvedParameters = new ArrayList<MethodParameter>();

	public StubArgumentResolver(Class<?> supportedType, Object stubValue, boolean usesResponse) {
		this.supportedType = supportedType;
		this.stubValue = stubValue;
		this.usesResponse = usesResponse;
	}

	public List<MethodParameter> getResolvedParameterNames() {
		return resolvedParameters;
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return this.usesResponse;
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType().equals(this.supportedType);
	}

	public Object resolveArgument(MethodParameter parameter, ModelMap model, NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		this.resolvedParameters.add(parameter);
		return this.stubValue;
	}

}
