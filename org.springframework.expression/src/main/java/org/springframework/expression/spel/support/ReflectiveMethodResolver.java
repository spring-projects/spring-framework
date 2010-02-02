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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;



/**
 * A method resolver that uses reflection to locate the method that should be invoked
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class ReflectiveMethodResolver implements MethodResolver {

	private static Method[] NO_METHODS = new Method[0];

	private Map<Class<?>,MethodFilter> filters = null;

	
	/**
	 * Locate a method on a type. There are three kinds of match that might occur:
	 * <ol>
	 * <li>An exact match where the types of the arguments match the types of the constructor
	 * <li>An in-exact match where the types we are looking for are subtypes of those defined on the constructor
	 * <li>A match where we are able to convert the arguments into those expected by the constructor, according to the
	 * registered type converter.
	 * </ol>
	 */
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, Class<?>[] argumentTypes) throws AccessException {
		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
			Method[] methods = type.getMethods();
			
			// If a filter is registered for this type, call it
			MethodFilter methodfilter = (filters==null?null:filters.get(type));
			if (methodfilter!=null) {
			    List<Method> methodsForFiltering = new ArrayList<Method>();
			    for (Method method: methods) {
			    	methodsForFiltering.add(method);
			    }
				List<Method> methodsFiltered = methodfilter.filter(methodsForFiltering);
				if (methodsFiltered == null || methodsFiltered.size()==0) {
					methods = NO_METHODS;
				}
				else {
					methods = methodsFiltered.toArray(new Method[methodsFiltered.size()]);
				}
			}
			
			Method closeMatch = null;
			int[] argsToConvert = null;
			boolean multipleOptions = false;
			Method matchRequiringConversion = null;
			for (Method method : methods) {
				if (method.isBridge()) {
					continue;
				}
				if (method.getName().equals(name)) {
					ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
					if (method.isVarArgs() && argumentTypes.length >= (method.getParameterTypes().length - 1)) {
						// *sigh* complicated
						matchInfo = ReflectionHelper.compareArgumentsVarargs(method.getParameterTypes(), argumentTypes, typeConverter);
					} else if (method.getParameterTypes().length == argumentTypes.length) {
						// name and parameter number match, check the arguments
						matchInfo = ReflectionHelper.compareArguments(method.getParameterTypes(), argumentTypes, typeConverter);
					}
					if (matchInfo != null) {
						if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.EXACT) {
							return new ReflectiveMethodExecutor(method, null);
						}
						else if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.CLOSE) {
							closeMatch = method;
						}
						else if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.REQUIRES_CONVERSION) {
							if (matchRequiringConversion != null) {
								multipleOptions = true;
							}
							argsToConvert = matchInfo.argsRequiringConversion;
							matchRequiringConversion = method;
						}
					}
				}
			}
			if (closeMatch != null) {
				return new ReflectiveMethodExecutor(closeMatch, null);
			}
			else if (matchRequiringConversion != null) {
				if (multipleOptions) {
					throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
				}
				return new ReflectiveMethodExecutor(matchRequiringConversion, argsToConvert);
			}
			else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve method", ex);
		}
	}

	public void registerMethodFilter(Class<?> type, MethodFilter filter) {
		if (filters==null) {
			filters = new HashMap<Class<?>,MethodFilter>();
		}
		if (filter==null) {
			filters.remove(type);
		}
		else {
			filters.put(type,filter);
		}
	}

}
