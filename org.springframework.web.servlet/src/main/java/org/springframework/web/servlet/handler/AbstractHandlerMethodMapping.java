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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Abstract base class for {@link org.springframework.web.servlet.HandlerMapping HandlerMapping} implementations that
 * support mapping requests to {@link HandlerMethod}s rather than to handlers.
 * 
 * @param <T> A type containing request mapping conditions required to match a request to a {@link HandlerMethod}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private final MultiValueMap<String, T> urlMap = new LinkedMultiValueMap<String, T>();
	
	private final Map<T, HandlerMethod> handlerMethods = new LinkedHashMap<T, HandlerMethod>();

	/**
	 * Set if URL lookup should always use the full path within the current servlet context. Else, the path within the
	 * current servlet mapping is used if applicable (that is, in the case of a ".../*" servlet mapping in web.xml).
	 * <p>Default is "false".
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Set if context path and request URI should be URL-decoded. Both are returned <i>undecoded</i> by the Servlet API, in
	 * contrast to the servlet path. <p>Uses either the request encoding or the default encoding according to the Servlet
	 * spec (ISO-8859-1).
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths. <p>Use this to override the default UrlPathHelper 
	 * with a custom subclass, or to share common UrlPathHelper settings across multiple HandlerMappings and
	 * MethodNameResolvers.
	 *
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}
	
	/**
	 * Return the {@link UrlPathHelper} to use for resolution of lookup paths.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return urlPathHelper;
	}

	/**
	 * Return the map with all {@link HandlerMethod}s. The key of the map is the generic type 
	 * <strong>{@code <T>}</strong> containing request mapping conditions.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		return Collections.unmodifiableMap(handlerMethods);
	}

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
	 * <p>The actual mapping for a handler is up to the concrete {@link #getMappingForMethod(String, Method)}
	 * implementation.
	 */
	protected void initHandlerMethods() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for request mappings in application context: " + getApplicationContext());
		}
		for (String beanName : getApplicationContext().getBeanNamesForType(Object.class)) {
			if (isHandler(getApplicationContext().getType(beanName))){
				detectHandlerMethods(beanName);
			}
		}
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * Determines if the given type could contain handler methods.
	 * @param beanType the type to check
	 * @return true if this a type that could contain handler methods, false otherwise.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * Invoked after all handler methods found in beans of the current ApplicationContext have been registered.
	 * @param handlerMethods a read-only map with mapping conditions (generic type {@code <T>}) and HandlerMethods.
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
	}

	/**
	 * Detect and register handler methods for the specified handler.
	 * @param handler the bean name of a handler or a handler instance
	 */
	protected void detectHandlerMethods(final Object handler) {
		final Class<?> handlerType = (handler instanceof String) ? 
				getApplicationContext().getType((String) handler) : handler.getClass();
				
		Set<Method> methods = HandlerMethodSelector.selectMethods(handlerType, new MethodFilter() {
			public boolean matches(Method method) {
				return getMappingForMethod(method, handlerType) != null;
			}
		});
		
		for (Method method : methods) {
			T mapping = getMappingForMethod(method, handlerType);
			registerHandlerMethod(handler, method, mapping);
		}
	}

	/**
	 * Provides a request mapping for the given bean method. A method for which no request mapping can be determined 
	 * is not considered a handler method.
	 *
	 * @param method the method to create a mapping for
	 * @param handlerType the actual handler type (possibly a subtype of {@code method.getDeclaringClass()})
	 * @return the mapping, or {@code null} if the method is not mapped
	 */
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * Registers a {@link HandlerMethod} with the given mapping.
	 * 
	 * @param handler the bean name of the handler or the actual handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 * @throws IllegalStateException if another method was already register under the same mapping
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName, getApplicationContext(), method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		
		HandlerMethod oldHandlerMethod = handlerMethods.get(mapping);
		if (oldHandlerMethod != null && !oldHandlerMethod.equals(handlerMethod)) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + handlerMethod.getBean()
					+ "' bean method \n" + handlerMethod + "\nto " + mapping + ": There is already '"
					+ oldHandlerMethod.getBean() + "' bean method\n" + oldHandlerMethod + " mapped.");
		}
		
		handlerMethods.put(mapping, handlerMethod);
		if (logger.isInfoEnabled()) {
			logger.info("Mapped \"" + mapping + "\" onto " + handlerMethod);
		}
		
		Set<String> patterns = getMappingPathPatterns(mapping);
		for (String pattern : patterns) {
			if (!getPathMatcher().isPattern(pattern)) {
				urlMap.add(pattern, mapping);
			}
		}
	}

	/**
	 * Get the URL paths associated with the given mapping. 
	 */
	protected abstract Set<String> getMappingPathPatterns(T mapping);

	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = urlPathHelper.getLookupPathForRequest(request);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up handler method for path " + lookupPath);
		}

		HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);

		if (logger.isDebugEnabled()) {
			if (handlerMethod != null) {
				logger.debug("Returning handler method [" + handlerMethod + "]");
			}
			else {
				logger.debug("Did not find handler method for [" + lookupPath + "]");
			}
		}

		return (handlerMethod != null) ? handlerMethod.createWithResolvedBean() : null;
	}

	/**
	 * Looks up the best-matching {@link HandlerMethod} for the given request.
	 *
	 * <p>This implementation iterators through all handler methods, calls 
	 * {@link #getMatchingMapping(Object, String, HttpServletRequest)} for each of them, 
	 * sorts all matches via {@linkplain #getMappingComparator(String, HttpServletRequest)} , and returns the 
	 * top match, if any. If no matches are found, {@link #handleNoMatch(Set, HttpServletRequest)} is invoked.
	 *
	 * @param lookupPath mapping lookup path within the current servlet mapping if applicable
	 * @param request the current HTTP servlet request
	 * @return the best-matching handler method, or {@code null} if there is no match
	 */
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
		List<T> mappings = urlMap.get(lookupPath);
		if (mappings == null) {
			mappings = new ArrayList<T>(handlerMethods.keySet());
		}
			
		List<Match> matches = new ArrayList<Match>();
		
		for (T mapping : mappings) {
			T match = getMatchingMapping(mapping, request);
			if (match != null) {
				matches.add(new Match(match, handlerMethods.get(mapping)));
			}
		}

		if (!matches.isEmpty()) {
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
			Collections.sort(matches, comparator);

			if (logger.isTraceEnabled()) {
				logger.trace("Found " + matches.size() + " matching mapping(s) for [" + lookupPath + "] : " + matches);
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

			handleMatch(bestMatch.mapping, lookupPath, request);
			return bestMatch.handlerMethod;
		}
		else {
			return handleNoMatch(handlerMethods.keySet(), lookupPath, request);
		}
	}

	/**
	 * Invoked when a request has been matched to a mapping.
	 *
	 * @param mapping the mapping selected for the request returned by 
	 * {@link #getMatchingMapping(Object, String, HttpServletRequest)}.
	 * @param lookupPath mapping lookup path within the current servlet mapping if applicable
	 * @param request the current request
	 */
	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * Checks if the mapping matches the current request and returns a mapping updated to contain only conditions 
	 * relevant to the current request (for example a mapping may have several HTTP methods, the matching mapping
	 * will contain only 1).
	 *
	 * @param mapping the mapping to get a match for
	 * @param request the current HTTP servlet request
	 * @return a matching mapping, or {@code null} if the given mapping does not match the request
	 */
	protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);

	/**
	 * Returns a comparator to sort request mappings with. The returned comparator should sort 'better' matches higher.
	 *
	 * @param request the current HTTP servlet request
	 * @return the comparator
	 */
	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);

	/**
	 * Invoked when no match was found. Default implementation returns {@code null}.
	 *
	 * @param mappings all registered request mappings
	 * @param lookupPath mapping lookup path within the current servlet mapping if applicable
	 * @param request the current HTTP request
	 * @throws ServletException in case of errors
	 */
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
			throws Exception {
		return null;
	}

	private class Match {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		private Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return mapping.toString();
		}
	}

	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		public int compare(Match match1, Match match2) {
			return comparator.compare(match1.mapping, match2.mapping);
		}
	}
	
}