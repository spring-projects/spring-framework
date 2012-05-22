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

import java.lang.reflect.Constructor;

import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;

/**
 * A simple ConstructorExecutor implementation that runs a constructor using reflective invocation.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
class ReflectiveConstructorExecutor implements ConstructorExecutor {

	private final Constructor<?> ctor;

	private final Integer varargsPosition;

	// When the constructor was found, we will have determined if arguments need to be converted for it
	// to be invoked. Conversion won't be cheap so let's only do it if necessary.
	private final int[] argsRequiringConversion;


	public ReflectiveConstructorExecutor(Constructor<?> ctor, int[] argsRequiringConversion) {
		this.ctor = ctor;
		if (ctor.isVarArgs()) {
			Class<?>[] paramTypes = ctor.getParameterTypes();
			this.varargsPosition = paramTypes.length - 1;
		}
		else {
			this.varargsPosition = null;
		}
		this.argsRequiringConversion = argsRequiringConversion;
	}

	public TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException {
		try {
			if (arguments != null) {
				ReflectionHelper.convertArguments(context.getTypeConverter(), arguments,
						this.ctor, this.argsRequiringConversion, this.varargsPosition);
			}
			if (this.ctor.isVarArgs()) {
				arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(this.ctor.getParameterTypes(), arguments);
			}
			ReflectionUtils.makeAccessible(this.ctor);
			return new TypedValue(this.ctor.newInstance(arguments));
		}
		catch (Exception ex) {
			throw new AccessException("Problem invoking constructor: " + this.ctor, ex);
		}
	}

}
