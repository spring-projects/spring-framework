/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
