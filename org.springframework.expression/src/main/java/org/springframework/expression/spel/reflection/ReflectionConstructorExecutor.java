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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;

/**
 * A simple CommandExecutor implementation that runs a constructor using reflective invocation.
 * 
 * @author Andy Clement
 */
public class ReflectionConstructorExecutor implements ConstructorExecutor {

	private final Constructor<?> c;

	// When the constructor was found, we will have determined if arguments need to be converted for it
	// to be invoked. Conversion won't be cheap so let's only do it if necessary.
	private final Integer[] argsRequiringConversion;

	public ReflectionConstructorExecutor(Constructor<?> constructor, Integer[] argsRequiringConversion) {
		c = constructor;
		this.argsRequiringConversion = argsRequiringConversion;
	}

	/**
	 * Invoke a constructor via reflection.
	 */
	public Object execute(EvaluationContext context, Object... arguments) throws AccessException {
		if (argsRequiringConversion != null && arguments != null) {
			try {
				ReflectionUtils.convertArguments(c.getParameterTypes(), c.isVarArgs(), context.getTypeUtils()
						.getTypeConverter(), argsRequiringConversion, arguments);
			} catch (EvaluationException ex) {
				throw new AccessException("Problem invoking constructor on '" + c + "': " + ex.getMessage(), ex);
			}
		}
		if (c.isVarArgs()) {
			arguments = ReflectionUtils.setupArgumentsForVarargsInvocation(c.getParameterTypes(), arguments);
		}
		try {
			if (!c.isAccessible()) {
				c.setAccessible(true);
			}
			return c.newInstance(arguments);
		} catch (IllegalArgumentException e) {
			throw new AccessException("Problem invoking constructor on '" + c + "' : " + e.getMessage(), e);
		} catch (InstantiationException e) {
			throw new AccessException("Problem invoking constructor on '" + c + "' : " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new AccessException("Problem invoking constructor on '" + c + "' : " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			throw new AccessException("Problem invoking constructor on '" + c + "' : " + e.getMessage(), e);
		}
	}
}
