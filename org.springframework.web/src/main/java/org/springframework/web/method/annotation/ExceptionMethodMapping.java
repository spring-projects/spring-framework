/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Extracts and stores method-to-exception type mappings from a set of {@link ExceptionHandler}-annotated methods.
 * Subsequently {@link #getMethod(Exception)} can be used to matches an {@link Exception} to a method.
 * 
 * <p>Method-to-exception type mappings are usually derived from a method's {@link ExceptionHandler} annotation value.
 * The method argument list may also be checked for {@link Throwable} types if that's empty. Exception types can be
 * mapped to one method only.
 * 
 * <p>When multiple exception types match a given exception, the best matching exception type is selected by sorting 
 * the list of matches with {@link ExceptionDepthComparator}.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ExceptionMethodMapping {

	protected static final Method NO_METHOD_FOUND = ClassUtils.getMethodIfAvailable(System.class, "currentTimeMillis");

	private final Map<Class<? extends Throwable>, Method> mappedExceptionTypes = 
		new HashMap<Class<? extends Throwable>, Method>();

	private final Map<Class<? extends Throwable>, Method> resolvedExceptionTypes = 
		new ConcurrentHashMap<Class<? extends Throwable>, Method>();
	
	/**
	 * Creates an {@link ExceptionMethodMapping} instance from a set of {@link ExceptionHandler} methods.
	 * <p>While any {@link ExceptionHandler} methods can be provided, it is expected that the exception types
	 * handled by any one method do not overlap with the exception types handled by any other method. 
	 * If two methods map to the same exception type, an exception is raised.
	 * @param methods the {@link ExceptionHandler}-annotated methods to add to the mappings 
	 */
	public ExceptionMethodMapping(Set<Method> methods) {
		initExceptionMap(methods);
	}

	/**
	 * Examines the provided methods and populates mapped exception types.
	 */
	private void initExceptionMap(Set<Method> methods) {
		for (Method method : methods) {
			for (Class<? extends Throwable> exceptionType : getMappedExceptionTypes(method)) {
				Method prevMethod = mappedExceptionTypes.put(exceptionType, method);
				
				if (prevMethod != null && !prevMethod.equals(method)) {
					throw new IllegalStateException(
							"Ambiguous exception handler mapped for [" + exceptionType + "]: {" +
									prevMethod + ", " + method + "}.");
				}
			}			
		}
	}

	/**
	 * Derive the list of exception types mapped to the given method in one of the following ways:
	 * <ol>
	 * <li>The {@link ExceptionHandler} annotation value
	 * <li>{@link Throwable} types that appear in the method parameter list
	 * </ol> 
	 * @param method the method to derive mapped exception types for
	 * @return the list of exception types the method is mapped to, or an empty list
	 */
	@SuppressWarnings("unchecked")
	protected List<Class<? extends Throwable>> getMappedExceptionTypes(Method method) {
		ExceptionHandler annotation = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
		if (annotation.value().length != 0) {
			return Arrays.asList(annotation.value());
		}
		else {
			List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>();
			for (Class<?> paramType : method.getParameterTypes()) {
				if (Throwable.class.isAssignableFrom(paramType)) {
					result.add((Class<? extends Throwable>) paramType);
				}
			}
			Assert.notEmpty(result, "No exception types mapped to {" + method + "}");
			return result;
		}
	}

	/**
	 * Get the {@link ExceptionHandler} method that matches the type of the provided {@link Exception}. 
	 * In case of multiple matches, the best match is selected with {@link ExceptionDepthComparator}.
	 * @param exception the exception to find a matching {@link ExceptionHandler} method for
	 * @return the mapped method, or {@code null} if none
	 */
	public Method getMethod(Exception exception) {
		Class<? extends Exception> exceptionType = exception.getClass();
		Method method = resolvedExceptionTypes.get(exceptionType);
		if (method == null) {
			method = resolveExceptionType(exceptionType);
			resolvedExceptionTypes.put(exceptionType, method);
		}
		return (method != NO_METHOD_FOUND) ? method : null;
	}

	/**
	 * Resolve the given exception type by iterating mapped exception types. 
	 * Uses {@link #getBestMatchingExceptionType(List, Class)} to select the best match. 
	 * @param exceptionType the exception type to resolve
	 * @return the best matching method, or {@link ExceptionMethodMapping#NO_METHOD_FOUND}
	 */
	protected final Method resolveExceptionType(Class<? extends Exception> exceptionType) {
		List<Class<? extends Throwable>> matches = new ArrayList<Class<? extends Throwable>>();
		for(Class<? extends Throwable> mappedExceptionType : mappedExceptionTypes.keySet()) {
			if (mappedExceptionType.isAssignableFrom(exceptionType)) {
				matches.add(mappedExceptionType);
			}
		}
		if (matches.isEmpty()) {
			return NO_METHOD_FOUND;
		}
		else {
			return mappedExceptionTypes.get(getBestMatchingExceptionType(matches, exceptionType));
		}
	}

	/**
	 * Select the best match from the given list of exception types.
	 */
	protected Class<? extends Throwable> getBestMatchingExceptionType(List<Class<? extends Throwable>> exceptionTypes, 
																	  Class<? extends Exception> exceptionType) {
		Assert.isTrue(exceptionTypes.size() > 0, "No exception types to select from!");
		if (exceptionTypes.size() > 1) {
			Collections.sort(exceptionTypes, new ExceptionDepthComparator(exceptionType));
		}
		return exceptionTypes.get(0);
	}
	
}
