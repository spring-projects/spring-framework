/*
 * Copyright 2002-present the original author or authors.
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
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

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
import org.springframework.expression.spel.support.ReflectionHelper.ArgumentsMatchKind;

/**
 * Reflection-based {@link MethodResolver} used by default in {@link StandardEvaluationContext}
 * unless explicit method resolvers have been specified.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.0
 * @see StandardEvaluationContext#addMethodResolver(MethodResolver)
 */
public class ReflectiveMethodResolver implements MethodResolver {

	// Using distance will ensure a more accurate match is discovered,
	// more closely following the Java rules.
	private final boolean useDistance;

	private @Nullable Map<Class<?>, MethodFilter> filters;


	public ReflectiveMethodResolver() {
		this(true);
	}

	/**
	 * This constructor allows the ReflectiveMethodResolver to be configured such that it
	 * will use a distance computation to check which is the better of two close matches
	 * (when there are multiple matches). Using the distance computation is intended to
	 * ensure matches are more closely representative of what a Java compiler would do
	 * when taking into account boxing/unboxing and whether the method candidates are
	 * declared to handle a supertype of the type (of the argument) being passed in.
	 * @param useDistance {@code true} if distance computation should be used when
	 * calculating matches; {@code false} otherwise
	 */
	public ReflectiveMethodResolver(boolean useDistance) {
		this.useDistance = useDistance;
	}


	/**
	 * Register a filter for methods on the given type.
	 * @param type the type to filter on
	 * @param filter the corresponding method filter,
	 * or {@code null} to clear any filter for the given type
	 */
	public void registerMethodFilter(Class<?> type, @Nullable MethodFilter filter) {
		if (this.filters == null) {
			this.filters = new HashMap<>();
		}
		if (filter != null) {
			this.filters.put(type, filter);
		}
		else {
			this.filters.remove(type);
		}
	}

	/**
	 * Locate a method on the type.
	 * <p>There are three kinds of matches that might occur:
	 * <ol>
	 * <li>An exact match where the types of the arguments match the types of the
	 * method.</li>
	 * <li>An inexact match where the types we are looking for are subtypes of
	 * those defined on the method.</li>
	 * <li>A match where we are able to convert the arguments into those expected
	 * by the method, according to the registered type converter.</li>
	 * </ol>
	 */
	@Override
	public @Nullable MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = (targetObject instanceof Class<?> clazz ? clazz : targetObject.getClass());
			ArrayList<Method> methods = new ArrayList<>(getMethods(type, targetObject));
			methods.removeIf(method -> !method.getName().equals(name));

			// If a filter is registered for this type, call it
			MethodFilter filter = (this.filters != null ? this.filters.get(type) : null);
			if (filter != null) {
				List<Method> filtered = filter.filter(methods);
				methods = (filtered instanceof ArrayList<Method> arrayList ? arrayList : new ArrayList<>(filtered));
			}

			// Sort methods into a sensible order
			if (methods.size() > 1) {
				methods.sort((m1, m2) -> {
					int m1pl = m1.getParameterCount();
					int m2pl = m2.getParameterCount();
					// vararg methods go last
					if (m1pl == m2pl) {
						if (!m1.isVarArgs() && m2.isVarArgs()) {
							return -1;
						}
						else if (m1.isVarArgs() && !m2.isVarArgs()) {
							return 1;
						}
						else {
							return 0;
						}
					}
					return Integer.compare(m1pl, m2pl);
				});
			}

			// Resolve any bridge methods
			for (int i = 0; i < methods.size(); i++) {
				methods.set(i, BridgeMethodResolver.findBridgedMethod(methods.get(i)));
			}

			// Remove duplicate methods (possible due to resolved bridge methods)
			Set<Method> methodsToIterate = new LinkedHashSet<>(methods);

			Method closeMatch = null;
			int closeMatchDistance = Integer.MAX_VALUE;
			Method matchRequiringConversion = null;
			boolean multipleOptions = false;

			for (Method method : methodsToIterate) {
				int paramCount = method.getParameterCount();
				List<TypeDescriptor> paramDescriptors = new ArrayList<>(paramCount);
				for (int i = 0; i < paramCount; i++) {
					paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, i)));
				}
				ArgumentsMatchKind matchKind = null;
				if (method.isVarArgs() && argumentTypes.size() >= (paramCount - 1)) {
					// *sigh* complicated
					matchKind = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
				}
				else if (paramCount == argumentTypes.size()) {
					// Name and parameter number match, check the arguments
					matchKind = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
				}
				if (matchKind != null) {
					if (matchKind.isExactMatch()) {
						return new ReflectiveMethodExecutor(method, type);
					}
					else if (matchKind.isCloseMatch()) {
						if (this.useDistance) {
							int matchDistance = ReflectionHelper.getTypeDifferenceWeight(paramDescriptors, argumentTypes);
							if (closeMatch == null || matchDistance < closeMatchDistance) {
								// This is a better match...
								closeMatch = method;
								closeMatchDistance = matchDistance;
							}
						}
						else {
							// Take this as a close match if there isn't one already
							if (closeMatch == null) {
								closeMatch = method;
							}
						}
					}
					else if (matchKind.isMatchRequiringConversion()) {
						if (matchRequiringConversion != null) {
							multipleOptions = true;
						}
						matchRequiringConversion = method;
					}
				}
			}
			if (closeMatch != null) {
				return new ReflectiveMethodExecutor(closeMatch, type);
			}
			else if (matchRequiringConversion != null) {
				if (multipleOptions) {
					throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
				}
				return new ReflectiveMethodExecutor(matchRequiringConversion, type);
			}
			else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve method", ex);
		}
	}

	private Set<Method> getMethods(Class<?> type, Object targetObject) {
		if (targetObject instanceof Class) {
			Set<Method> result = new LinkedHashSet<>();
			// Add these so that static methods are invocable on the type: for example, Float.valueOf(..)
			for (Method method : getMethods(type)) {
				if (Modifier.isStatic(method.getModifiers())) {
					result.add(method);
				}
			}
			// Also expose methods from java.lang.Class itself
			Collections.addAll(result, getMethods(Class.class));
			return result;
		}
		else if (Proxy.isProxyClass(type)) {
			Set<Method> result = new LinkedHashSet<>();
			// Expose interface methods (not proxy-declared overrides) for proper vararg introspection
			for (Class<?> ifc : type.getInterfaces()) {
				for (Method method : getMethods(ifc)) {
					if (isCandidateForInvocation(method, type)) {
						result.add(method);
					}
				}
			}
			// Ensure methods defined in java.lang.Object are exposed for JDK proxies.
			for (Method method : getMethods(Object.class)) {
				if (isCandidateForInvocation(method, type)) {
					result.add(method);
				}
			}
			return result;
		}
		else {
			Set<Method> result = new LinkedHashSet<>();
			for (Method method : getMethods(type)) {
				if (isCandidateForInvocation(method, type)) {
					result.add(method);
				}
			}
			return result;
		}
	}

	/**
	 * Return the set of methods for this type. The default implementation returns the
	 * result of {@link Class#getMethods()} for the given {@code type}, but subclasses
	 * may override in order to alter the results, for example, specifying static methods
	 * declared elsewhere.
	 * @param type the class for which to return the methods
	 * @since 3.1.1
	 */
	protected Method[] getMethods(Class<?> type) {
		return type.getMethods();
	}

	/**
	 * Determine whether the given {@code Method} is a candidate for method resolution
	 * on an instance of the given target class.
	 * <p>The default implementation considers any method as a candidate, even for
	 * static methods and non-user-declared methods on the {@link Object} base class.
	 * @param method the Method to evaluate
	 * @param targetClass the concrete target class that is being introspected
	 * @since 4.3.15
	 */
	protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
		return true;
	}

}
