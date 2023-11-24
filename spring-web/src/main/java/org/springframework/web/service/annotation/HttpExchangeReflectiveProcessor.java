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

package org.springframework.web.service.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@link ReflectiveProcessor} implementation for {@link HttpExchange @HttpExchange}
 * annotated methods. In addition to registering reflection hints for invoking
 * the annotated method, this implementation handles reflection-based
 * binding for return types and parameters annotated with {@link RequestBody}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class HttpExchangeReflectiveProcessor implements ReflectiveProcessor {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();


	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		if (element instanceof Method method) {
			registerMethodHints(hints, method);
		}
	}

	protected void registerMethodHints(ReflectionHints hints, Method method) {
		hints.registerMethod(method, ExecutableMode.INVOKE);
		for (Parameter parameter : method.getParameters()) {
			registerParameterTypeHints(hints, MethodParameter.forParameter(parameter));
		}
		registerReturnTypeHints(hints, MethodParameter.forExecutable(method, -1));
	}

	protected void registerParameterTypeHints(ReflectionHints hints, MethodParameter methodParameter) {
		if (methodParameter.hasParameterAnnotation(RequestBody.class)) {
			this.bindingRegistrar.registerReflectionHints(hints, methodParameter.getGenericParameterType());
		}
	}

	protected void registerReturnTypeHints(ReflectionHints hints, MethodParameter returnTypeParameter) {
		if (!void.class.equals(returnTypeParameter.getParameterType())) {
			this.bindingRegistrar.registerReflectionHints(hints, returnTypeParameter.getGenericParameterType());
		}
	}

}
