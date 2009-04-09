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

import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;

/**
 * A constructor resolver that uses reflection to locate the constructor that should be invoked
 * 
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectiveConstructorResolver implements ConstructorResolver {

	/**
	 * Locate a constructor on the type. There are three kinds of match that might occur:
	 * <ol>
	 * <li>An exact match where the types of the arguments match the types of the constructor
	 * <li>An in-exact match where the types we are looking for are subtypes of those defined on the constructor
	 * <li>A match where we are able to convert the arguments into those expected by the constructor, according to the
	 * registered type converter.
	 * </ol>
	 */
	public ConstructorExecutor resolve(EvaluationContext context, String typename, Class<?>[] argumentTypes)
			throws AccessException {
		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = context.getTypeLocator().findType(typename);
			Constructor[] ctors = type.getConstructors();
			Constructor closeMatch = null;
			int[] argsToConvert = null;
			Constructor matchRequiringConversion = null;
			for (Constructor ctor : ctors) {
				ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
				if (ctor.isVarArgs() && argumentTypes.length >= (ctor.getParameterTypes().length - 1)) {
					// *sigh* complicated
					// Basically.. we have to have all parameters match up until the varargs one, then the rest of what is
					// being provided should be
					// the same type whilst the final argument to the method must be an array of that (oh, how easy...not) -
					// or the final parameter
					// we are supplied does match exactly (it is an array already).
					matchInfo = ReflectionHelper.compareArgumentsVarargs(ctor.getParameterTypes(), argumentTypes, typeConverter);
				}
				else if (ctor.getParameterTypes().length == argumentTypes.length) {
					// worth a closer look
					matchInfo = ReflectionHelper.compareArguments(ctor.getParameterTypes(), argumentTypes, typeConverter);
				}
				if (matchInfo != null) {
					if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.EXACT) {
						return new ReflectiveConstructorExecutor(ctor, null);
					} else if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.CLOSE) {
						closeMatch = ctor;
					} else if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.REQUIRES_CONVERSION) {
						argsToConvert = matchInfo.argsRequiringConversion;
						matchRequiringConversion = ctor;
					}
				}
			}
			if (closeMatch != null) {
				return new ReflectiveConstructorExecutor(closeMatch, null);
			} else if (matchRequiringConversion != null) {
				return new ReflectiveConstructorExecutor(matchRequiringConversion, argsToConvert);
			} else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve constructor", ex);
		}
	}

}
