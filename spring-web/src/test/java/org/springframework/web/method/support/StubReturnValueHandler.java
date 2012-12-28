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

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Supports a fixed return value type. Records the last handled return value.
 *
 * @author Rossen Stoyanchev
 */
public class StubReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final Class<?> returnType;

	private Object returnValue;

	public StubReturnValueHandler(Class<?> returnType) {
		this.returnType = returnType;
	}

	public Object getReturnValue() {
		return this.returnValue;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getParameterType().equals(this.returnType);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
		this.returnValue = returnValue;
	}

}
