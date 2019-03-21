/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectiveMethodExecutor implements MethodExecutor {

	private final Method method;

	private final Integer varargsPosition;

	private boolean computedPublicDeclaringClass = false;

	private Class<?> publicDeclaringClass;

	private boolean argumentConversionOccurred = false;


	public ReflectiveMethodExecutor(Method method) {
		this.method = method;
		if (method.isVarArgs()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			this.varargsPosition = paramTypes.length - 1;
		}
		else {
			this.varargsPosition = null;
		}
	}


	public Method getMethod() {
		return this.method;
	}

	/**
	 * Find the first public class in the methods declaring class hierarchy that declares this method.
	 * Sometimes the reflective method discovery logic finds a suitable method that can easily be
	 * called via reflection but cannot be called from generated code when compiling the expression
	 * because of visibility restrictions. For example if a non public class overrides toString(), this
	 * helper method will walk up the type hierarchy to find the first public type that declares the
	 * method (if there is one!). For toString() it may walk as far as Object.
	 */
	public Class<?> getPublicDeclaringClass() {
		if (!this.computedPublicDeclaringClass) {
			this.publicDeclaringClass = discoverPublicClass(this.method, this.method.getDeclaringClass());
			this.computedPublicDeclaringClass = true;
		}
		return this.publicDeclaringClass;
	}

	private Class<?> discoverPublicClass(Method method, Class<?> clazz) {
		if (Modifier.isPublic(clazz.getModifiers())) {
			try {
				clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
				return clazz;
			}
			catch (NoSuchMethodException ex) {
				// Continue below...
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc: ifcs) {
			discoverPublicClass(method, ifc);
		}
		if (clazz.getSuperclass() != null) {
			return discoverPublicClass(method, clazz.getSuperclass());
		}
		return null;
	}

	public boolean didArgumentConversionOccur() {
		return this.argumentConversionOccurred;
	}


	@Override
	public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
		try {
			if (arguments != null) {
				this.argumentConversionOccurred = ReflectionHelper.convertArguments(
						context.getTypeConverter(), arguments, this.method, this.varargsPosition);
				if (this.method.isVarArgs()) {
					arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
							this.method.getParameterTypes(), arguments);
				}
			}
			ReflectionUtils.makeAccessible(this.method);
			Object value = this.method.invoke(target, arguments);
			return new TypedValue(value, new TypeDescriptor(new MethodParameter(this.method, -1)).narrow(value));
		}
		catch (Exception ex) {
			throw new AccessException("Problem invoking method: " + this.method, ex);
		}
	}

}
