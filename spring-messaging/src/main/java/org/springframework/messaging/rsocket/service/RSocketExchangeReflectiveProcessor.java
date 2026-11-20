/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.core.MethodParameter;

/**
 * A {@link ReflectiveProcessor} implementation for {@link RSocketExchange @RSocketExchange}
 * annotated methods. In addition to registering reflection hints for invoking
 * the annotated method, this implementation handles reflection-based binding for
 * return types and parameters.
 *
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @since 6.0.5
 * @see org.springframework.web.service.annotation.HttpExchangeReflectiveProcessor
 */
class RSocketExchangeReflectiveProcessor implements ReflectiveProcessor {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();


	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		if (element instanceof Method method) {
			this.registerMethodHints(hints, method);
		}
	}

	protected void registerMethodHints(ReflectionHints hints, Method method) {
		hints.registerMethod(method, ExecutableMode.INVOKE);
		for (Parameter parameter : method.getParameters()) {
			// Also register non-annotated parameters to handle metadata
			this.bindingRegistrar.registerReflectionHints(hints,
					MethodParameter.forParameter(parameter).getGenericParameterType());
		}
		registerReturnTypeHints(hints, MethodParameter.forExecutable(method, -1));
	}

	protected void registerReturnTypeHints(ReflectionHints hints, MethodParameter returnTypeParameter) {
		if (!void.class.equals(returnTypeParameter.getParameterType())) {
			this.bindingRegistrar.registerReflectionHints(hints, returnTypeParameter.getGenericParameterType());
		}
	}

}
