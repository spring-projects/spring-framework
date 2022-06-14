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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.aot.hint.support.BindingReflectionHintsRegistrar;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * {@link ReflectiveProcessor} implementation for {@link RequestMapping}
 * annotated types. On top of registering reflection hints for invoking
 * the annotated method, this implementation handles return types annotated
 * with {@link ResponseBody} and parameters annotated with {@link RequestBody}
 * which are serialized as well.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 6.0
 */
class RequestMappingReflectiveProcessor implements ReflectiveProcessor {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		if (element instanceof Class<?> type) {
			registerTypeHint(hints, type);
		}
		else if (element instanceof Method method) {
			registerMethodHint(hints, method);
		}
	}

	protected void registerTypeHint(ReflectionHints hints, Class<?> type) {
		hints.registerType(type, hint -> {});
	}

	protected void registerMethodHint(ReflectionHints hints, Method method) {
		hints.registerMethod(method, hint -> hint.setModes(ExecutableMode.INVOKE));
		for (Parameter parameter : method.getParameters()) {
			MethodParameter methodParameter = MethodParameter.forParameter(parameter);
			if (methodParameter.hasParameterAnnotation(RequestBody.class)) {
				this.bindingRegistrar.registerReflectionHints(hints, methodParameter.getGenericParameterType());
			}
		}
		MethodParameter returnType = MethodParameter.forExecutable(method, -1);
		if (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||
				returnType.hasMethodAnnotation(ResponseBody.class)) {
			this.bindingRegistrar.registerReflectionHints(hints, returnType.getGenericParameterType());
		}
	}
}
