/*
 * Copyright 2002-2012 the original author or authors.
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
class ReflectiveMethodExecutor implements MethodExecutor {

	private final Method method;

	private final Integer varargsPosition;


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


	@Override
	public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
		try {
			if (arguments != null) {
				ReflectionHelper.convertArguments(context.getTypeConverter(), arguments, this.method, this.varargsPosition);
			}
			if (this.method.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(this.method.getParameterTypes(), arguments);
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
