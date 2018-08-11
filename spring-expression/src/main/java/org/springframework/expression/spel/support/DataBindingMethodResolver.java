/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.lang.Nullable;

/**
 * A {@link org.springframework.expression.MethodResolver} variant for data binding
 * purposes, using reflection to access instance methods on a given target object.
 *
 * <p>This accessor does not resolve static methods and also no technical methods
 * on {@code java.lang.Object} or {@code java.lang.Class}.
 * For unrestricted resolution, choose {@link ReflectiveMethodResolver} instead.
 *
 * @author Juergen Hoeller
 * @since 4.3.15
 * @see #forInstanceMethodInvocation()
 * @see DataBindingPropertyAccessor
 */
public final class DataBindingMethodResolver extends ReflectiveMethodResolver {

	private DataBindingMethodResolver() {
		super();
	}

	@Override
	@Nullable
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		if (targetObject instanceof Class) {
			throw new IllegalArgumentException("DataBindingMethodResolver does not support Class targets");
		}
		return super.resolve(context, targetObject, name, argumentTypes);
	}

	@Override
	protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
		if (Modifier.isStatic(method.getModifiers())) {
			return false;
		}
		Class<?> clazz = method.getDeclaringClass();
		return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
	}


	/**
	 * Create a new data-binding method resolver for instance method resolution.
	 */
	public static DataBindingMethodResolver forInstanceMethodInvocation() {
		return new DataBindingMethodResolver();
	}

}
