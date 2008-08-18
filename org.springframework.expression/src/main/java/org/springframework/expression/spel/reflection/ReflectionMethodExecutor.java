/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;

public class ReflectionMethodExecutor implements MethodExecutor {

	private final Method m;

	// When the method was found, we will have determined if arguments need to be converted for it
	// to be invoked. Conversion won't be cheap so let's only do it if necessary.
	private final Integer[] argsRequiringConversion;

	public ReflectionMethodExecutor(Method theMethod, Integer[] argumentsRequiringConversion) {
		m = theMethod;
		argsRequiringConversion = argumentsRequiringConversion;
	}

	public Object execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
		if (argsRequiringConversion != null && arguments != null) {
			try {
				ReflectionUtils.convertArguments(m.getParameterTypes(), m.isVarArgs(), context.getTypeUtils()
						.getTypeConverter(), argsRequiringConversion, arguments);
			} catch (EvaluationException ex) {
				throw new AccessException("Problem invoking method '" + m.getName() + "' on '" + target.getClass()
						+ "': " + ex.getMessage(), ex);
			}
		}
		if (m.isVarArgs()) {
			arguments = ReflectionUtils.setupArgumentsForVarargsInvocation(m.getParameterTypes(), arguments);
		}
		try {
			if (!m.isAccessible()) {
				m.setAccessible(true);
			}
			return m.invoke(target, arguments);
		} catch (IllegalArgumentException e) {
			throw new AccessException("Problem invoking method '" + m.getName() + "' on '" + target.getClass() + "': "
					+ e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new AccessException("Problem invoking method '" + m.getName() + "' on '" + target.getClass() + "': "
					+ e.getMessage(), e);
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
			throw new AccessException("Problem invoking method '" + m.getName() + "' on '" + target.getClass() + "': "
					+ e.getMessage(), e);
		}
	}

}
