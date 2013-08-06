/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
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
 * Reflection-based {@link MethodResolver} used by default in
 * {@link StandardEvaluationContext} unless explicit method resolvers have been specified.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see StandardEvaluationContext#addMethodResolver(MethodResolver)
 */
public class ReflectiveMethodResolver implements MethodResolver {

	private Map<Class<?>, MethodFilter> filters = null;

	// Using distance will ensure a more accurate match is discovered,
	// more closely following the Java rules.
	private boolean useDistance = false;


	public ReflectiveMethodResolver() {
	}

	/**
	 * This constructors allows the ReflectiveMethodResolver to be configured such that it will
	 * use a distance computation to check which is the better of two close matches (when there
	 * are multiple matches).  Using the distance computation is intended to ensure matches
	 * are more closely representative of what a Java compiler would do when taking into
	 * account boxing/unboxing and whether the method candidates are declared to handle a
	 * supertype of the type (of the argument) being passed in.
	 * @param useDistance true if distance computation should be used when calculating matches
	 */
	public ReflectiveMethodResolver(boolean useDistance) {
		this.useDistance = useDistance;
	}


	/**
	 * Locate a method on a type. There are three kinds of match that might occur:
	 * <ol>
	 * <li>An exact match where the types of the arguments match the types of the constructor
	 * <li>An in-exact match where the types we are looking for are subtypes of those defined on the constructor
	 * <li>A match where we are able to convert the arguments into those expected by the constructor,
	 * according to the registered type converter.
	 * </ol>
	 */
	@Override
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
			List<Method> methods = new ArrayList<Method>(Arrays.asList(getMethods(type, targetObject)));

			// If a filter is registered for this type, call it
			MethodFilter filter = (this.filters != null ? this.filters.get(type) : null);
			if (filter != null) {
				List<Method> filtered = filter.filter(methods);
				methods = (filtered instanceof ArrayList ? filtered : new ArrayList<Method>(filtered));
			}

			// Sort methods into a sensible order
			if (methods.size() > 1) {
				Collections.sort(methods, new Comparator<Method>() {
					@Override
					public int compare(Method m1, Method m2) {
						int m1pl = m1.getParameterTypes().length;
						int m2pl = m2.getParameterTypes().length;
						return (new Integer(m1pl)).compareTo(m2pl);
					}
				});
			}

			// Resolve any bridge methods
			for (int i = 0; i < methods.size(); i++) {
				methods.set(i, BridgeMethodResolver.findBridgedMethod(methods.get(i)));
			}

			// Remove duplicate methods (possible due to resolved bridge methods)
			Set<Method> methodsToIterate = new LinkedHashSet<Method>(methods);

			Method closeMatch = null;
			int closeMatchDistance = Integer.MAX_VALUE;
			int[] argsToConvert = null;
			Method matchRequiringConversion = null;
			boolean multipleOptions = false;

			for (Method method : methodsToIterate) {
				if (method.getName().equals(name)) {
					Class<?>[] paramTypes = method.getParameterTypes();
					List<TypeDescriptor> paramDescriptors = new ArrayList<TypeDescriptor>(paramTypes.length);
					for (int i = 0; i < paramTypes.length; i++) {
						paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, i)));
					}
					ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
					if (method.isVarArgs() && argumentTypes.size() >= (paramTypes.length - 1)) {
						// *sigh* complicated
						matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
					}
					else if (paramTypes.length == argumentTypes.size()) {
						// name and parameter number match, check the arguments
						matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
					}
					if (matchInfo != null) {
						if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.EXACT) {
							return new ReflectiveMethodExecutor(method, null);
						}
						else if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.CLOSE) {
							if (!this.useDistance) {
								closeMatch = method;
							}
							else {
								int matchDistance = ReflectionHelper.getTypeDifferenceWeight(paramDescriptors, argumentTypes);
								if (matchDistance<closeMatchDistance) {
									// this is a better match
									closeMatchDistance = matchDistance;
									closeMatch = method;
								}
							}
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
		if (this.filters == null) {
			this.filters = new HashMap<Class<?>, MethodFilter>();
		}
		if (filter == null) {
			this.filters.remove(type);
		}
		else {
			this.filters.put(type,filter);
		}
	}

	private Method[] getMethods(Class<?> type, Object targetObject) {
		if (targetObject instanceof Class) {
			Set<Method> methods = new HashSet<Method>();
			methods.addAll(Arrays.asList(getMethods(type)));
			methods.addAll(Arrays.asList(getMethods(targetObject.getClass())));
			return methods.toArray(new Method[methods.size()]);
		}
		return getMethods(type);
	}

	/**
	 * Return the set of methods for this type. The default implementation returns the
	 * result of {@link Class#getMethods()} for the given {@code type}, but subclasses
	 * may override in order to alter the results, e.g. specifying static methods
	 * declared elsewhere.
	 * @param type the class for which to return the methods
	 * @since 3.1.1
	 */
	protected Method[] getMethods(Class<?> type) {
		return type.getMethods();
	}

}
