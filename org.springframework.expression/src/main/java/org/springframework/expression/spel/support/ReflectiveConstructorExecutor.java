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

import java.lang.reflect.Constructor;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;

/**
 * A simple ConstructorExecutor implementation that runs a constructor using reflective invocation.
 *
 * @author Andy Clement
 * @since 3.0
 */
class ReflectiveConstructorExecutor implements ConstructorExecutor {

	private final Constructor<?> c;

	// When the constructor was found, we will have determined if arguments need to be converted for it
	// to be invoked. Conversion won't be cheap so let's only do it if necessary.
	private final int[] argsRequiringConversion;


	public ReflectiveConstructorExecutor(Constructor<?> constructor, int[] argsRequiringConversion) {
		c = constructor;
		this.argsRequiringConversion = argsRequiringConversion;
	}

	public TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException {
		try {
			if (argsRequiringConversion != null && arguments != null) {
				ReflectionHelper.convertArguments(c.getParameterTypes(), c.isVarArgs(),
						context.getTypeConverter(), argsRequiringConversion, arguments);
			}
			if (c.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(c.getParameterTypes(), arguments);
			}
			if (!c.isAccessible()) {
				c.setAccessible(true);
			}
			return new TypedValue(c.newInstance(arguments),TypeDescriptor.valueOf(c.getClass()));
		} catch (Exception ex) {
			throw new AccessException("Problem invoking constructor: " + c, ex);
		}
	}

}
