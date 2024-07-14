/*
 * Copyright 2002-2024 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link MethodExecutor} that works via reflection.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class ReflectiveMethodExecutor implements MethodExecutor {

	private final Method originalMethod;

	/**
	 * The method to invoke via reflection or in a compiled expression.
	 */
	private final Method methodToInvoke;

	@Nullable
	private final Integer varargsPosition;

	@Nullable
	private final Class<?> publicDeclaringClass;

	private boolean argumentConversionOccurred = false;


	/**
	 * Create a new executor for the given method.
	 * @param method the method to invoke
	 */
	public ReflectiveMethodExecutor(Method method) {
		this(method, null);
	}

	/**
	 * Create a new executor for the given method.
	 * @param method the method to invoke
	 * @param targetClass the target class to invoke the method on, or {@code null} if unknown
	 * @since 5.3.16
	 */
	public ReflectiveMethodExecutor(Method method, @Nullable Class<?> targetClass) {
		this.originalMethod = method;
		this.methodToInvoke = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, targetClass);
		Class<?> declaringClass = this.methodToInvoke.getDeclaringClass();
		this.publicDeclaringClass = (Modifier.isPublic(declaringClass.getModifiers()) ? declaringClass : null);
		this.varargsPosition = (method.isVarArgs() ? method.getParameterCount() - 1 : null);
	}


	/**
	 * Return the original method that this executor has been configured for.
	 */
	public final Method getMethod() {
		return this.originalMethod;
	}

	/**
	 * Get the public class or interface in the method's type hierarchy that declares the
	 * {@linkplain #getMethod() original method}.
	 * <p>See {@link ClassUtils#getPubliclyAccessibleMethodIfPossible(Method, Class)} for
	 * details.
	 * @return the public class or interface that declares the method, or {@code null} if
	 * no such public type could be found
	 */
	@Nullable
	public Class<?> getPublicDeclaringClass() {
		return this.publicDeclaringClass;
	}

	public boolean didArgumentConversionOccur() {
		return this.argumentConversionOccurred;
	}


	@Override
	public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
		try {
			this.argumentConversionOccurred = ReflectionHelper.convertArguments(
					context.getTypeConverter(), arguments, this.originalMethod, this.varargsPosition);
			if (this.originalMethod.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(
						this.originalMethod.getParameterTypes(), arguments);
			}
			ReflectionUtils.makeAccessible(this.methodToInvoke);
			Object value = this.methodToInvoke.invoke(target, arguments);
			return new TypedValue(value, new TypeDescriptor(new MethodParameter(this.originalMethod, -1)).narrow(value));
		}
		catch (Exception ex) {
			throw new AccessException("Problem invoking method: " + this.methodToInvoke, ex);
		}
	}

}
