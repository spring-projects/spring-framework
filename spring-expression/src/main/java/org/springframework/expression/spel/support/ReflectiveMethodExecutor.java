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

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
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
	 * The method to invoke via reflection, which is not necessarily the method
	 * to invoke in a compiled expression.
	 */
	private final Method methodToInvoke;

	@Nullable
	private final Integer varargsPosition;

	private boolean computedPublicDeclaringClass = false;

	@Nullable
	private Class<?> publicDeclaringClass;

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
	 * @param targetClass the target class to invoke the method on
	 * @since 5.3.16
	 */
	public ReflectiveMethodExecutor(Method method, @Nullable Class<?> targetClass) {
		this.originalMethod = method;
		this.methodToInvoke = ClassUtils.getInterfaceMethodIfPossible(method, targetClass);
		if (method.isVarArgs()) {
			this.varargsPosition = method.getParameterCount() - 1;
		}
		else {
			this.varargsPosition = null;
		}
	}


	/**
	 * Return the original method that this executor has been configured for.
	 */
	public final Method getMethod() {
		return this.originalMethod;
	}

	/**
	 * Find a public class or interface in the method's class hierarchy that
	 * declares the {@linkplain #getMethod() original method}.
	 * <p>See {@link CodeFlow#findPublicDeclaringClass(Method)} for
	 * details.
	 * @return the public class or interface that declares the method, or
	 * {@code null} if no such public type could be found
	 */
	@Nullable
	public Class<?> getPublicDeclaringClass() {
		if (!this.computedPublicDeclaringClass) {
			this.publicDeclaringClass = CodeFlow.findPublicDeclaringClass(this.originalMethod);
			this.computedPublicDeclaringClass = true;
		}
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
