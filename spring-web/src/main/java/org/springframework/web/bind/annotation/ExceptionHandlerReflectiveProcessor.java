/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind.annotation;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;

/**
 * {@link ControllerMappingReflectiveProcessor} specific implementation that
 * handles {@link ExceptionHandler @ExceptionHandler}-specific types.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
class ExceptionHandlerReflectiveProcessor extends ControllerMappingReflectiveProcessor{

	@Override
	protected void registerReturnTypeHints(ReflectionHints hints, MethodParameter returnTypeParameter) {
		Class<?> returnType = returnTypeParameter.getParameterType();
		if (ProblemDetail.class.isAssignableFrom(returnType)) {
			getBindingRegistrar().registerReflectionHints(hints, returnTypeParameter.getGenericParameterType());
		}
		super.registerReturnTypeHints(hints, returnTypeParameter);
	}

}
