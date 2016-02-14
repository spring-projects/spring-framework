/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Discovers {@linkplain ExceptionHandler @ExceptionHandler} methods in a given class,
 * including all of its superclasses, and helps to resolve a given {@link Exception}
 * to the exception types supported by a given {@link Method}.
 *
 * @author Rossen Stoyanchev
 * @author Martin Macko
 * @since 3.1
 */
public class ExceptionHandlerMethodResolver {

	/**
	 * A filter for selecting {@code @ExceptionHandler} methods.
	 */
	public static final MethodFilter EXCEPTION_HANDLER_METHODS = new MethodFilter() {
		@Override
		public boolean matches(Method method) {
			return (AnnotationUtils.findAnnotation(method, ExceptionHandler.class) != null);
		}
	};

	/**
	 * Arbitrary {@link Method} reference, indicating no method found in the cache.
	 */
	private static final Method NO_METHOD_FOUND = ClassUtils.getMethodIfAvailable(System.class, "currentTimeMillis");


	private final Map<Class<? extends Throwable>, Method> mappedMethods =
			new ConcurrentHashMap<Class<? extends Throwable>, Method>(16);

	private final Map<Class<? extends Throwable>, Boolean> excludedExceptions =
			new ConcurrentHashMap<Class<? extends Throwable>, Boolean>(16);

	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache =
			new ConcurrentHashMap<Class<? extends Throwable>, Method>(16);


	/**
	 * A constructor that finds {@link ExceptionHandler} methods in the given type.
	 * @param handlerType the type to introspect
	 */
	public ExceptionHandlerMethodResolver(Class<?> handlerType) {
		for (Method method : MethodIntrospector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS)) {
			for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
				addExceptionMapping(exceptionType, method);
			}
		}
	}


	/**
	 * Extract exception mappings from the {@code @ExceptionHandler} annotation first,
	 * and then as a fallback from the method signature itself.
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>();
		detectAnnotationExceptionMappings(method, result);
		if (result.isEmpty()) {
			for (Class<?> paramType : method.getParameterTypes()) {
				if (Throwable.class.isAssignableFrom(paramType)) {
					result.add((Class<? extends Throwable>) paramType);
				}
			}
		}
		Assert.notEmpty(result, "No exception types mapped to {" + method + "}");
		addExclusions(method, result);
		return result;
	}

	protected void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
		ExceptionHandler annot = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
		result.addAll(Arrays.asList(annot.value()));
	}

	private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
		Method oldMethod = this.mappedMethods.put(exceptionType, method);
		if (oldMethod != null && !oldMethod.equals(method)) {
			throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
					exceptionType + "]: {" + oldMethod + ", " + method + "}");
		}
	}

	private void addExclusions(Method method,  List<Class<? extends Throwable>> result) {
		ExceptionHandler annot = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
		List<Class<? extends Throwable>> exclusions = Arrays.asList(annot.exclude());

		for (Class<? extends Throwable> excludedException : exclusions) {
			if(result.contains(excludedException)) {
				throw new IllegalStateException("Conflicting @ExceptionHandler method configured: ["
					+ excludedException + "] is mapped for handling and for exclusion");
			}
			this.excludedExceptions.put(excludedException, Boolean.TRUE);
		}
	}

	/**
	 * Whether the contained type has any exception mappings.
	 */
	public boolean hasExceptionMappings() {
		return !this.mappedMethods.isEmpty();
	}

	/**
	 * Find a {@link Method} to handle the given exception.
	 * Use {@link ExceptionDepthComparator} if more than one match is found.
	 * @param exception the exception
	 * @return a Method to handle the exception, or {@code null} if none found
	 */
	public Method resolveMethod(Exception exception) {
		return resolveMethodByExceptionType(exception.getClass());
	}

	/**
	 * Find a {@link Method} to handle the given exception type. This can be
	 * useful if an {@link Exception} instance is not available (e.g. for tools).
	 * @param exceptionType the exception type
	 * @return a Method to handle the exception, or {@code null} if none found
	 */
	public Method resolveMethodByExceptionType(Class<? extends Exception> exceptionType) {
		Method method = this.exceptionLookupCache.get(exceptionType);
		if (method == null) {
			method = getMappedMethod(exceptionType);
			this.exceptionLookupCache.put(exceptionType, (method != null ? method : NO_METHOD_FOUND));
		}
		return (method != NO_METHOD_FOUND ? method : null);
	}

	/**
	 * Return the {@link Method} mapped to the given exception type, or {@code null} if none.
	 */
	private Method getMappedMethod(Class<? extends Exception> exceptionType) {
		List<Class<? extends Throwable>> matches = new ArrayList<Class<? extends Throwable>>();
		for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			if (mappedException.isAssignableFrom(exceptionType)) {
				matches.add(mappedException);
			}
		}

		if(matches.isEmpty()) {
			return null;
		}

		Collections.sort(matches, new ExceptionDepthComparator(exceptionType));

		if (!this.excludedExceptions.containsKey(exceptionType)) {
			return this.mappedMethods.get(matches.get(0));
		}
		return firstNonExcludedMethod(matches, exceptionType);
	}

	/**
	 * Finds first method that can handle {@code exceptionType} accounting for
	 * excluded exceptions
	 * @param matches list of matched exceptions that are mapped
	 * @param exceptionType exception that is being handled
	 * @return {@code null} or the first non excluded method that can handle {@code exceptionType}
	 */
	private Method firstNonExcludedMethod(List<Class<? extends Throwable>> matches, Class<? extends Exception> exceptionType) {
		for (Class<? extends Throwable> match : matches) {
			Method matchedMethod = this.mappedMethods.get(match);
			if(!hasExclusion(matchedMethod, exceptionType)) {
				return matchedMethod;
			}
		}
		return null;
	}

	/**
	 * Checks if method has {@code exceptionType} excluded
	 * @param method Method to check for excluded exceptions
	 * @param exceptionType exception to look for in exclusions
	 * @return {@code true} if {@code exceptionType} is in excluded exceptions
	 */
	private boolean hasExclusion(Method method, Class<? extends Exception> exceptionType) {
		ExceptionHandler annot = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
		List<Class<? extends Throwable>> exclusions = Arrays.asList(annot.exclude());
		return exclusions.contains(exceptionType);
	}

}
