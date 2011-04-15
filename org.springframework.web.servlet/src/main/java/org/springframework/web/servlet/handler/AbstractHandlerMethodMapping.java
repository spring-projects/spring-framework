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

package org.springframework.web.servlet.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContextException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;

/**
 * Abstract base class for {@link org.springframework.web.servlet.HandlerMapping HandlerMapping} implementations that
 * support mapping requests to {@link HandlerMethod}s rather than to handlers.
 * 
 * <p>Each {@link HandlerMethod} is registered with a unique key. Subclasses define the key type and how to create it
 * for a given handler method. Keys represent conditions for matching a handler method to a request. 
 *
 * <p>Subclasses must also define how to create a key for an incoming request. The resulting key is used to perform 
 * a {@link HandlerMethod} lookup possibly resulting in a direct match. However, when a map lookup is insufficient, 
 * the keys of all handler methods are iterated and subclasses are allowed to make an exhaustive check of key 
 * conditions against the request.
 * 
 * <p>Since there can be more than one matching key for a request, subclasses must define a comparator for sorting
 * the keys of matching handler methods in order to find the most specific match.
 *
 * @param <T> A unique key for the registration of mapped {@link HandlerMethod}s representing the conditions to
 * match a handler method to a request.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping {

	private final Map<T, HandlerMethod> handlerMethods = new LinkedHashMap<T, HandlerMethod>();

	/**
	 * Calls the initialization of the superclass and detects handlers.
	 */
	@Override
	public void initApplicationContext() throws ApplicationContextException {
		super.initApplicationContext();
		initHandlerMethods();
	}

	/**
	 * Register handler methods found in beans of the current ApplicationContext.
	 * <p>The actual key determination for a handler is up to the concrete
	 * {@link #getKeyForMethod(Method)} implementation. A method in a bean for which no key
	 * could be determined is simply not considered a handler method.
	 * @see #getKeyForMethod(Method)
	 */
	protected void initHandlerMethods() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for URL mappings in application context: " + getApplicationContext());
		}
		for (String beanName : getApplicationContext().getBeanNamesForType(Object.class)) {
			if (isHandler(beanName)){
				detectHandlerMethods(beanName);
			}
		}
	}

	/**
	 * Determines if the given bean is a handler that should be introspected for handler methods.
	 * @param beanName the name of the bean to check
	 * @return true if the bean is a handler and may contain handler methods, false otherwise.
	 */
	protected abstract boolean isHandler(String beanName);

	/**
	 * Detect and register handler methods for the specified handler.
	 */
	private void detectHandlerMethods(final String beanName) {
		Class<?> handlerType = getApplicationContext().getType(beanName);

		Set<Method> methods = HandlerMethodSelector.selectMethods(handlerType, new MethodFilter() {
			public boolean matches(Method method) {
				return getKeyForMethod(beanName, method) != null;
			}
		});
		for (Method method : methods) {
			T key = getKeyForMethod(beanName, method);
			HandlerMethod handlerMethod = new HandlerMethod(beanName, getApplicationContext(), method);
			registerHandlerMethod(key, handlerMethod);
		}
	}

	/**
	 * Provides a lookup key for the given bean method. A method for which no key can be determined is
	 * not considered a handler method.
	 *
	 * @param beanName the name of the bean the method belongs to
	 * @param method the method to create a key for
	 * @return the lookup key, or {@code null} if the method has none
	 */
	protected abstract T getKeyForMethod(String beanName, Method method);

	/**
	 * Registers a {@link HandlerMethod} under the given key.
	 *
	 * @param key the key to register the method under
	 * @param handlerMethod the handler method to register
	 * @throws IllegalStateException if another method was already register under the key
	 */
	protected void registerHandlerMethod(T key, HandlerMethod handlerMethod) {
		Assert.notNull(key, "'key' must not be null");
		Assert.notNull(handlerMethod, "'handlerMethod' must not be null");
		HandlerMethod mappedHandlerMethod = handlerMethods.get(key);
		if (mappedHandlerMethod != null && !mappedHandlerMethod.equals(handlerMethod)) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + handlerMethod.getBean()
					+ "' bean method \n" + handlerMethod + "\nto " + key + ": There is already '"
					+ mappedHandlerMethod.getBean() + "' bean method\n" + mappedHandlerMethod + " mapped.");
		}
		handlerMethods.put(key, handlerMethod);
		if (logger.isInfoEnabled()) {
			logger.info("Mapped \"" + key + "\" onto " + handlerMethod);
		}
	}

	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		T key = getKeyForRequest(request);
		if (key == null) {
			return null;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up handler method with key [" + key + "]");
		}

		HandlerMethod handlerMethod = lookupHandlerMethod(key, request);

		if (logger.isDebugEnabled()) {
			if (handlerMethod != null) {
				logger.debug("Returning handler method [" + handlerMethod + "]");
			}
			else {
				logger.debug("Did not find handler method for [" + key + "]");
			}
		}

		return (handlerMethod != null) ? handlerMethod.createWithResolvedBean() : null;
	}

	/**
	 * Abstract template method that returns the lookup key for the given HTTP servlet request.
	 *
	 * @param request the request to look up the key for
	 * @return the key, or {@code null} if the request does not have one
	 * @throws Exception in case of errors
	 */
	protected abstract T getKeyForRequest(HttpServletRequest request) throws Exception;

	/**
	 * Looks up the best-matching {@link HandlerMethod} for the given request.
	 *
	 * <p>This implementation iterators through all handler methods, calls {@link #getMatchingKey(Object,
	 * HttpServletRequest)} for each of them, {@linkplain #getKeyComparator(HttpServletRequest) sorts} all matches, and
	 * returns the 1st entry, if any. If no matches are found, {@link #handleNoMatch(Set, HttpServletRequest)} is
	 * invoked.
	 *
	 * @param lookupKey current lookup key
	 * @param request the current HTTP servlet request
	 * @return the best-matching handler method, or {@code null} if there is no match
	 */
	protected HandlerMethod lookupHandlerMethod(T lookupKey, HttpServletRequest request) throws Exception {
		if (handlerMethods.containsKey(lookupKey)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found direct match for [" + lookupKey + "]");
			}

			handleMatch(lookupKey, request);
			return handlerMethods.get(lookupKey);
		}
		else {
			List<Match> matches = new ArrayList<Match>();

			for (Map.Entry<T, HandlerMethod> entry : handlerMethods.entrySet()) {
				T match = getMatchingKey(entry.getKey(), request);
				if (match != null) {
					matches.add(new Match(match, entry.getValue()));
				}
			}

			if (!matches.isEmpty()) {
				Comparator<Match> comparator = getMatchComparator(request);
				Collections.sort(matches, comparator);

				if (logger.isTraceEnabled()) {
					logger.trace("Found " + matches.size() + " matching key(s) for [" + lookupKey + "] : " + matches);
				}

				Match bestMatch = matches.get(0);
				if (matches.size() > 1) {
					Match secondBestMatch = matches.get(1);
					if (comparator.compare(bestMatch, secondBestMatch) == 0) {
						Method m1 = bestMatch.handlerMethod.getMethod();
						Method m2 = secondBestMatch.handlerMethod.getMethod();
						throw new IllegalStateException(
								"Ambiguous handler methods mapped for HTTP path '" + request.getRequestURL() + "': {" +
										m1 + ", " + m2 + "}");
					}
				}

				handleMatch(bestMatch.key, request);
				return bestMatch.handlerMethod;
			}
			else {
				return handleNoMatch(handlerMethods.keySet(), request);
			}
		}
	}

	/**
	 * Invoked when a key matching to a request has been identified.
	 *
	 * @param key the key selected for the request returned by {@link #getMatchingKey(Object, HttpServletRequest)}.
	 * @param request the current request
	 */
	protected void handleMatch(T key, HttpServletRequest request) {
	}

	/**
	 * Returns the matching variant of the given key, given the current HTTP servlet request.
	 *
	 * @param key the key to get the matches for
	 * @param request the current HTTP servlet request
	 * @return the matching key, or {@code null} if the given key does not match against the servlet request
	 */
	protected abstract T getMatchingKey(T key, HttpServletRequest request);

	private Comparator<Match> getMatchComparator(HttpServletRequest request) {
		final Comparator<T> keyComparator = getKeyComparator(request);
		return new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return keyComparator.compare(m1.key, m2.key);
			}
		};
	}

	/**
	 * Returns a comparator to sort the keys with. The returned comparator should sort 'better' matches higher.
	 *
	 * @param request the current HTTP servlet request
	 * @return the comparator
	 */
	protected abstract Comparator<T> getKeyComparator(HttpServletRequest request);

	/**
	 * Invoked when no match was found. Default implementation returns {@code null}.
	 *
	 * @param requestKeys the registered request keys
	 * @param request the current HTTP request
	 * @throws ServletException in case of errors
	 */
	protected HandlerMethod handleNoMatch(Set<T> requestKeys, HttpServletRequest request) throws Exception {
		return null;
	}

	private class Match {

		private final T key;

		private final HandlerMethod handlerMethod;

		private Match(T key, HandlerMethod handlerMethod) {
			this.key = key;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return key.toString();
		}
	}

}
