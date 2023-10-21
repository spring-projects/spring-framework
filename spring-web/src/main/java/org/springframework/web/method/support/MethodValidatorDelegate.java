/*
 * Copyright 2002-2023 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.validation.method.MethodValidator;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

/**
 * Delegate {@link MethodValidator} to adapt {@link ControllerInterceptor} implementation method validation
 *
 * @author Bruce
 */
public class MethodValidatorDelegate implements ControllerInterceptor, Ordered {

	private final MethodValidator methodValidator;

	public MethodValidatorDelegate(MethodValidator methodValidator) {
		this.methodValidator = methodValidator;
	}

	@Override
	public void beforeInvoke(HandlerMethod handlerMethod, Method bridged, Object[] args, HttpServletRequest req) {
		if (handlerMethod.shouldValidateArguments()) {
			InvocableHandlerMethod invocableHandlerMethod = (InvocableHandlerMethod) handlerMethod;
			Class<?>[] validationGroups = invocableHandlerMethod.getValidationGroups();
			if (validationGroups != null) {
				this.methodValidator.applyArgumentValidation(
						handlerMethod.getBean(), handlerMethod.getBridgedMethod(), handlerMethod.getMethodParameters(), args, validationGroups);
			}
		}
	}

	@Override
	public Object afterInvoke(HandlerMethod handlerMethod, Method bridged, Object[] args, Object returnValue, HttpServletRequest req) {
		if (handlerMethod.shouldValidateReturnValue()) {
			InvocableHandlerMethod invocableHandlerMethod = (InvocableHandlerMethod) handlerMethod;
			Class<?>[] validationGroups = invocableHandlerMethod.getValidationGroups();
			if (validationGroups != null) {
				this.methodValidator.applyReturnValueValidation(
						handlerMethod.getBean(), handlerMethod.getBridgedMethod(), handlerMethod.getReturnType(), returnValue, validationGroups);
			}
		}
		return returnValue;
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
