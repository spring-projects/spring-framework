/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Abstract base class for {@link HandlerMapping} implementations that define
 * a mapping between a request and a {@link HandlerMethod}.
 *
 * <p>For each registered handler method, a unique mapping is maintained with
 * subclasses defining the details of the mapping type {@code <T>}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 * @param <T> The mapping for a {@link HandlerMethod} containing the conditions
 * needed to match the handler method to incoming request.
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * Bean name prefix for target beans behind scoped proxies. Used to exclude those
	 * targets from handler method detection, in favor of the corresponding proxies.
	 * <p>We're not checking the autowire-candidate status here, which is how the
	 * proxy target filtering problem is being handled at the autowiring level,
	 * since autowire-candidate may have been turned to {@code false} for other
	 * reasons, while still expecting the bean to be eligible for handler methods.
	 * <p>Originally defined in {@link org.springframework.aop.scope.ScopedProxyUtils}
	 * but duplicated here to avoid a hard dependency on the spring-aop module.
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new EmptyHandler(), ClassUtils.getMethod(EmptyHandler.class, "handle"));

	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		ALLOW_CORS_CONFIG.addAllowedOrigin("*");
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}


	private boolean detectHandlerMethodsInAncestorContexts = false;

	private HandlerMethodMappingNamingStrategy<T> namingStrategy;

	private final MappingDefinitionRegistry mappingRegistry = new MappingDefinitionRegistry();


	/**
	 * Whether to detect handler methods in beans in ancestor ApplicationContexts.
	 * <p>Default is "false": Only beans in the current ApplicationContext are
	 * considered, i.e. only in the context that this HandlerMapping itself
	 * is defined in (typically the current DispatcherServlet's context).
	 * <p>Switch this flag on to detect handler beans in ancestor contexts
	 * (typically the Spring root WebApplicationContext) as well.
	 */
	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
		this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
	}

	/**
	 * Configure the naming strategy to use for assigning a default name to every
	 * mapped handler method.
	 * <p>The default naming strategy is based on the capital letters of the
	 * class name followed by "#" and then the method name, e.g. "TC#getFoo"
	 * for a class named TestController with method getFoo.
	 */
	public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * Return the configured naming strategy or {@code null}.
	 */
	public HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
		return this.namingStrategy;
	}

	/**
	 * Return a read-only map with all mapped HandlerMethod's.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		return this.mappingRegistry.getMappings();
	}

	/**
	 * Return the handler methods for the given mapping name.
	 * @param mappingName the mapping name
	 * @return a list of matching HandlerMethod's or {@code null}; the returned
	 * list will never be modified and is safe to iterate.
	 * @see #setHandlerMethodMappingNamingStrategy
	 */
	public List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
		return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
	}


	/**
	 * Detects handler methods at initialization.
	 */
	@Override
	public void afterPropertiesSet() {
		initHandlerMethods();
	}

	/**
	 * Scan beans in the ApplicationContext, detect and register handler methods.
	 * @see #isHandler(Class)
	 * @see #getMappingForMethod(Method, Class)
	 * @see #handlerMethodsInitialized(Map)
	 */
	protected void initHandlerMethods() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for request mappings in application context: " + getApplicationContext());
		}
		String[] beanNames = (this.detectHandlerMethodsInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getApplicationContext(), Object.class) :
				getApplicationContext().getBeanNamesForType(Object.class));

		for (String name : beanNames) {
			if (!name.startsWith(SCOPED_TARGET_NAME_PREFIX) && isHandler(getApplicationContext().getType(name))) {
				detectHandlerMethods(name);
			}
		}
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * Whether the given type is a handler with handler methods.
	 * @param beanType the type of the bean being checked
	 * @return "true" if this a handler type, "false" otherwise.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * Look for handler methods in a handler.
	 * @param handler the bean name of a handler or a handler instance
	 */
	protected void detectHandlerMethods(final Object handler) {
		Class<?> handlerType =
				(handler instanceof String ? getApplicationContext().getType((String) handler) : handler.getClass());

		// Avoid repeated calls to getMappingForMethod which would rebuild RequestMappingInfo instances
		final Map<Method, T> mappings = new IdentityHashMap<Method, T>();
		final Class<?> userType = ClassUtils.getUserClass(handlerType);

		Set<Method> methods = HandlerMethodSelector.selectMethods(userType, new MethodFilter() {
			@Override
			public boolean matches(Method method) {
				T mapping = getMappingForMethod(method, userType);
				if (mapping != null) {
					mappings.put(method, mapping);
					return true;
				}
				else {
					return false;
				}
			}
		});

		for (Method method : methods) {
			registerHandlerMethod(handler, method, mappings.get(method));
		}
	}

	/**
	 * Provide the mapping for a handler method. A method for which no
	 * mapping can be provided is not a handler method.
	 * @param method the method to provide a mapping for
	 * @param handlerType the handler type, possibly a sub-type of the method's
	 * declaring class
	 * @return the mapping, or {@code null} if the method is not mapped
	 */
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * Register a handler method and its unique mapping.
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 * @throws IllegalStateException if another method was already registered
	 * under the same mapping
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(handler, method, mapping);
	}

	/**
	 * Create the HandlerMethod instance.
	 * @param handler either a bean name or an actual handler instance
	 * @param method the target method
	 * @return the created HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName,
					getApplicationContext().getAutowireCapableBeanFactory(), method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 * Extract and return the URL paths contained in a mapping.
	 */
	protected abstract Set<String> getMappingPathPatterns(T mapping);

	/**
	 * Extract and return the CORS configuration for the mapping.
	 */
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
		return null;
	}

	/**
	 * Invoked after all handler methods have been detected.
	 * @param handlerMethods a read-only map with handler methods and mappings.
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
	}


	/**
	 * Look up a handler method for the given request.
	 */
	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
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
		return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
	}

	/**
	 * Look up the best-matching handler method for the current request.
	 * If multiple matches are found, the best match is selected.
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request the current request
	 * @return the best-matching handler method, or {@code null} if no match
	 * @see #handleMatch(Object, String, HttpServletRequest)
	 * @see #handleNoMatch(Set, String, HttpServletRequest)
	 */
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
		List<Match> matches = new ArrayList<Match>();
		List<T> directPathMatches = this.mappingRegistry.getMappingKeysByUrl(lookupPath);
		if (directPathMatches != null) {
			addMatchingMappings(directPathMatches, matches, request);
		}
		if (matches.isEmpty()) {
			// No choice but to go through all mappings...
			addMatchingMappings(this.mappingRegistry.getMappingKeys(), matches, request);
		}

		if (!matches.isEmpty()) {
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
			Collections.sort(matches, comparator);
			if (logger.isTraceEnabled()) {
				logger.trace("Found " + matches.size() + " matching mapping(s) for [" +
						lookupPath + "] : " + matches);
			}
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				if (CorsUtils.isPreFlightRequest(request)) {
					return PREFLIGHT_AMBIGUOUS_MATCH;
				}
				Match secondBestMatch = matches.get(1);
				if (comparator.compare(bestMatch, secondBestMatch) == 0) {
					Method m1 = bestMatch.handlerMethod.getMethod();
					Method m2 = secondBestMatch.handlerMethod.getMethod();
					throw new IllegalStateException("Ambiguous handler methods mapped for HTTP path '" +
							request.getRequestURL() + "': {" + m1 + ", " + m2 + "}");
				}
			}
			handleMatch(bestMatch.mapping, lookupPath, request);
			return bestMatch.handlerMethod;
		}
		else {
			return handleNoMatch(this.mappingRegistry.getMappingKeys(), lookupPath, request);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
		for (T mapping : mappings) {
			T match = getMatchingMapping(mapping, request);
			if (match != null) {
				matches.add(new Match(match, this.mappingRegistry.getHandlerMethod(mapping)));
			}
		}
	}

	/**
	 * Check if a mapping matches the current request and return a (potentially
	 * new) mapping with conditions relevant to the current request.
	 * @param mapping the mapping to get a match for
	 * @param request the current HTTP servlet request
	 * @return the match, or {@code null} if the mapping doesn't match
	 */
	protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);

	/**
	 * Return a comparator for sorting matching mappings.
	 * The returned comparator should sort 'better' matches higher.
	 * @param request the current request
	 * @return the comparator (never {@code null})
	 */
	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);

	/**
	 * Invoked when a matching mapping is found.
	 * @param mapping the matching mapping
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request the current request
	 */
	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * Invoked when no matching mapping is not found.
	 * @param mappings all registered mappings
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param request the current request
	 * @throws ServletException in case of errors
	 */
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
			throws Exception {

		return null;
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			CorsConfiguration corsConfig = this.mappingRegistry.getCorsConfiguration(handlerMethod);
			if (corsConfig != null) {
				return corsConfig;
			}
			else if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
			}
		}
		return null;
	}


	private class MappingDefinitionRegistry {

		private final Map<Method, MappingDefinition<T>> mappingDefinitions =
				new ConcurrentHashMap<Method, MappingDefinition<T>>();

		private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<T, HandlerMethod>();

		private final MultiValueMap<String, T> urlLookup = new LinkedMultiValueMap<String, T>();

		private final Map<String, List<HandlerMethod>> nameLookup =
				new ConcurrentHashMap<String, List<HandlerMethod>>();


		/**
		 * Return a read-only copy of all mappings.
		 * Safe for concurrent use.
		 */
		public Map<T, HandlerMethod> getMappings() {
			return Collections.unmodifiableMap(this.mappingLookup);
		}

		public List<T> getMappingKeysByUrl(String urlPath) {
			return this.urlLookup.get(urlPath);
		}

		public Set<T> getMappingKeys() {
			return this.mappingLookup.keySet();
		}

		public HandlerMethod getHandlerMethod(T mapping) {
			return this.mappingLookup.get(mapping);
		}

		/**
		 * Return HandlerMethod matches for the given mapping name.
		 * Safe for concurrent use.
		 */
		public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
			return this.nameLookup.get(mappingName);
		}

		/**
		 * Return the CorsConfiguration for the given HandlerMethod, if any.
		 * Safe for concurrent use.
		 */
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			Method method = handlerMethod.getMethod();
			MappingDefinition<T> definition = this.mappingDefinitions.get(method);
			return (definition != null ? definition.getCorsConfiguration() : null);
		}


		public void register(Object handler, Method method, T mapping) {

			HandlerMethod handlerMethod = createHandlerMethod(handler, method);
			assertUniqueMapping(handlerMethod, mapping);

			if (logger.isInfoEnabled()) {
				logger.info("Mapped \"" + mapping + "\" onto " + handlerMethod);
			}
			this.mappingLookup.put(mapping, handlerMethod);

			List<String> directUrls = extractDirectUrls(mapping);
			for (String url : directUrls) {
				this.urlLookup.add(url, mapping);
			}

			String name = null;
			if (getNamingStrategy() != null) {
				name = getNamingStrategy().getName(handlerMethod, mapping);
				addName(name, handlerMethod);
			}

			CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);

			this.mappingDefinitions.put(method,
					new MappingDefinition<T>(mapping, handlerMethod, directUrls, name, corsConfig));
		}

		private void assertUniqueMapping(HandlerMethod handlerMethod, T mapping) {
			HandlerMethod existing = this.mappingLookup.get(mapping);
			if (existing != null && !existing.equals(handlerMethod)) {
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" +	handlerMethod.getBean() + "' method \n" +
								handlerMethod + "\nto " +	mapping + ": There is already '" +
								existing.getBean() + "' bean method\n" + existing + " mapped.");
			}
		}

		private List<String> extractDirectUrls(T mapping) {
			List<String> urls = new ArrayList<String>(1);
			for (String path : getMappingPathPatterns(mapping)) {
				if (!getPathMatcher().isPattern(path)) {
					urls.add(path);
				}
			}
			return urls;
		}

		private void addName(String name, HandlerMethod handlerMethod) {

			List<HandlerMethod> oldHandlerMethods = this.nameLookup.containsKey(name) ?
					this.nameLookup.get(name) : Collections.<HandlerMethod>emptyList();

			for (HandlerMethod oldHandlerMethod : oldHandlerMethods) {
				if (oldHandlerMethod.getMethod().equals(handlerMethod.getMethod())) {
					return;
				}
			}

			if (logger.isTraceEnabled()) {
				logger.trace("Mapping name=" + name);
			}

			int size = oldHandlerMethods.size() + 1;
			List<HandlerMethod> definitions = new ArrayList<HandlerMethod>(size);
			definitions.addAll(oldHandlerMethods);
			definitions.add(handlerMethod);
			this.nameLookup.put(name, definitions);

			if (definitions.size() > 1) {
				if (logger.isDebugEnabled()) {
					logger.debug("Mapping name clash for handlerMethods=" + definitions +
							". Consider assigning explicit names.");
				}
			}
		}
	}

	private static class MappingDefinition<T> {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		private final List<String> urls;

		private final String name;

		private final CorsConfiguration corsConfiguration;


		public MappingDefinition(T mapping, HandlerMethod handlerMethod, List<String> urls,
				String name, CorsConfiguration corsConfiguration) {

			Assert.notNull(mapping);
			Assert.notNull(handlerMethod);

			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
			this.urls = (urls != null ? urls : Collections.<String>emptyList());
			this.name = name;
			this.corsConfiguration = corsConfiguration;
		}


		public T getMapping() {
			return this.mapping;
		}

		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public List<String> getUrls() {
			return this.urls;
		}

		public String getName() {
			return this.name;
		}

		public CorsConfiguration getCorsConfiguration() {
			return this.corsConfiguration;
		}
	}


	/**
	 * A thin wrapper around a matched HandlerMethod and its mapping, for the purpose of
	 * comparing the best match with a comparator in the context of the current request.
	 */
	private class Match {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		public Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	private static class EmptyHandler {

		public void handle() {
			throw new UnsupportedOperationException("not implemented");
		}
	}

}
