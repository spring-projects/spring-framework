/*
 * Copyright 2002-2009 the original author or authors.
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
 * @since 3.0
 */
class ReflectiveMethodExecutor implements MethodExecutor {

	private final Method method;

	// When the method was found, we will have determined if arguments need to be converted for it
	// to be invoked. Conversion won't be cheap so let's only do it if necessary.
	private final int[] argsRequiringConversion;


	public ReflectiveMethodExecutor(Method theMethod, int[] argumentsRequiringConversion) {
		this.method = theMethod;
		this.argsRequiringConversion = argumentsRequiringConversion;
	}


	public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
		try {
			if (this.argsRequiringConversion != null && arguments != null) {
				ReflectionHelper.convertArguments(this.method.getParameterTypes(), this.method.isVarArgs(),
						context.getTypeConverter(), this.argsRequiringConversion, arguments);
			}
			if (this.method.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(this.method.getParameterTypes(), arguments);
			}
			ReflectionUtils.makeAccessible(this.method);
			return new TypedValue(this.method.invoke(target, arguments), new TypeDescriptor(new MethodParameter(method,-1)));
		} catch (Exception ex) {
			throw new AccessException("Problem invoking method: " + this.method, ex);
		}
	}

}
