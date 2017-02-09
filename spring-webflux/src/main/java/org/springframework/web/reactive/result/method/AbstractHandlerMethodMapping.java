/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.patterns.PathPattern;

/**
 * Abstract base class for {@link HandlerMapping} implementations that define
 * a mapping between a request and a {@link HandlerMethod}.
 *
 * <p>For each registered handler method, a unique mapping is maintained with
 * subclasses defining the details of the mapping type {@code <T>}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
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

	/**
	 * HandlerMethod to return on a pre-flight request match when the request
	 * mappings are more nuanced than the access control headers.
	 */
	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new PreFlightAmbiguousMatchHandler(),
					ClassUtils.getMethod(PreFlightAmbiguousMatchHandler.class, "handle"));

	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		ALLOW_CORS_CONFIG.addAllowedOrigin("*");
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}

	private final MultiValueMap<PathPattern, T> mappingLookup = new LinkedMultiValueMap<>();

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final MappingRegistry mappingRegistry = new MappingRegistry();


	// TODO: handlerMethodMappingNamingStrategy

	/**
	 * Return a (read-only) map with all mappings and HandlerMethod's.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		this.readWriteLock.readLock().lock();
		try {
			return Collections.unmodifiableMap(this.mappingRegistry.getMappings());
		}
		finally {
			this.readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Return the internal mapping registry. Provided for testing purposes.
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * Register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 * @param mapping the mapping for the handler method
	 * @param handler the handler
	 * @param method the method
	 */
	public void registerMapping(T mapping, Object handler, Method method) {
		this.readWriteLock.writeLock().lock();
		try {
			getMappingPathPatterns(mapping).forEach(pattern -> {
				getPatternRegistry().register(pattern).forEach(
						pathPattern -> this.mappingLookup.add(pathPattern, mapping)
				);
			});
			this.mappingRegistry.register(mapping, handler, method);
		}
		finally {
			this.readWriteLock.writeLock().unlock();
		}
	}

	/**
	 * Un-register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 * @param mapping the mapping to unregister
	 */
	public void unregisterMapping(T mapping) {
		this.readWriteLock.writeLock().lock();
		try {
			getMappingPathPatterns(mapping).forEach(pattern -> {
				getPatternRegistry().unregister(pattern).forEach(
						pathPattern -> this.mappingLookup.remove(pathPattern, mapping)
				);
			});
			this.mappingRegistry.unregister(mapping);
		}
		finally {
			this.readWriteLock.writeLock().unlock();
		}
	}


	// Handler method detection

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
		String[] beanNames = getApplicationContext().getBeanNamesForType(Object.class);

		for (String beanName : beanNames) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = getApplicationContext().getType(beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (beanType != null && isHandler(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * Look for handler methods in a handler.
	 * @param handler the bean name of a handler or a handler instance
	 */
	protected void detectHandlerMethods(final Object handler) {
		Class<?> handlerType = (handler instanceof String ?
				getApplicationContext().getType((String) handler) : handler.getClass());
		final Class<?> userType = ClassUtils.getUserClass(handlerType);

		Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
				(MethodIntrospector.MetadataLookup<T>) method -> getMappingForMethod(method, userType));

		if (logger.isDebugEnabled()) {
			logger.debug(methods.size() + " request handler methods found on " + userType + ": " + methods);
		}
		for (Map.Entry<Method, T> entry : methods.entrySet()) {
			Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), userType);
			T mapping = entry.getValue();
			registerMapping(mapping, handler, invocableMethod);
		}
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


	// Handler method lookup

	/**
	 * Look up a handler method for the given request.
	 * @param exchange the current exchange
	 */
	@Override
	public Mono<HandlerMethod> getHandlerInternal(ServerWebExchange exchange) {
		String lookupPath = getPathHelper().getLookupPathForRequest(exchange);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up handler method for path " + lookupPath);
		}

		// Ensure form data is parsed for "params" conditions...
		return exchange.getRequestParams()
				.then(() -> {
					HandlerMethod handlerMethod = null;
					try {
						handlerMethod = lookupHandlerMethod(lookupPath, exchange);
					}
					catch (Exception ex) {
						return Mono.error(ex);
					}
					if (logger.isDebugEnabled()) {
						if (handlerMethod != null) {
							logger.debug("Returning handler method [" + handlerMethod + "]");
						}
						else {
							logger.debug("Did not find handler method for [" + lookupPath + "]");
						}
					}
					if (handlerMethod != null) {
						handlerMethod = handlerMethod.createWithResolvedBean();
					}
					return Mono.justOrEmpty(handlerMethod);
				});
	}

	/**
	 * Look up the best-matching handler method for the current request.
	 * If multiple matches are found, the best match is selected.
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param exchange the current exchange
	 * @return the best-matching handler method, or {@code null} if no match
	 * @see #handleMatch(Object, String, ServerWebExchange)
	 * @see #handleNoMatch(Set, String, ServerWebExchange)
	 */
	protected HandlerMethod lookupHandlerMethod(String lookupPath, ServerWebExchange exchange)
			throws Exception {

		List<Match> matches = findMatchingMappings(lookupPath, exchange);

		if (!matches.isEmpty()) {
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(exchange));
			matches.sort(comparator);
			if (logger.isTraceEnabled()) {
				logger.trace("Found " + matches.size() + " matching mapping(s) for [" +
						lookupPath + "] : " + matches);
			}
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
					return PREFLIGHT_AMBIGUOUS_MATCH;
				}
				Match secondBestMatch = matches.get(1);
				if (comparator.compare(bestMatch, secondBestMatch) == 0) {
					Method m1 = bestMatch.handlerMethod.getMethod();
					Method m2 = secondBestMatch.handlerMethod.getMethod();
					throw new IllegalStateException("Ambiguous handler methods mapped for HTTP path '" +
							lookupPath + "': {" + m1 + ", " + m2 + "}");
				}
			}
			handleMatch(bestMatch.mapping, lookupPath, exchange);
			return bestMatch.handlerMethod;
		}
		else {
			return handleNoMatch(this.mappingRegistry.getMappings().keySet(), lookupPath, exchange);
		}
	}

	private List<Match> findMatchingMappings(String lookupPath, ServerWebExchange exchange) {
		List<Match> matches = new ArrayList<>();
		try {
			this.readWriteLock.readLock().lock();
			// Fast lookup of potential matching mappings given the current lookup path
			Set<PathPattern> matchingPatterns = getPatternRegistry().findMatches(lookupPath);
			Set<T> mappings = matchingPatterns.stream()
					.flatMap(pattern -> this.mappingLookup.get(pattern).stream())
					.collect(Collectors.toSet());

			for (T mapping : mappings) {
				T match = getMatchingMapping(mapping, exchange);
				if (match != null) {
					matches.add(new Match(match, this.mappingRegistry.getMappings().get(mapping)));
				}
			}
		}
		finally {
			this.readWriteLock.readLock().unlock();
		}
		return matches;
	}

	/**
	 * Invoked when a matching mapping is found.
	 * @param mapping the matching mapping
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param exchange the current exchange
	 */
	protected void handleMatch(T mapping, String lookupPath, ServerWebExchange exchange) {
	}

	/**
	 * Invoked when no matching mapping is not found.
	 * @param mappings all registered mappings
	 * @param lookupPath mapping lookup path within the current servlet mapping
	 * @param exchange the current exchange
	 * @return an alternative HandlerMethod or {@code null}
	 * @throws Exception provides details that can be translated into an error status code
	 */
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, ServerWebExchange exchange)
			throws Exception {

		return null;
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, exchange);
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				return ALLOW_CORS_CONFIG;
			}
			CorsConfiguration methodConfig = this.mappingRegistry.getCorsConfiguration(handlerMethod);
			corsConfig = (corsConfig != null ? corsConfig.combine(methodConfig) : methodConfig);
		}
		return corsConfig;
	}


	// Abstract template methods

	/**
	 * Whether the given type is a handler with handler methods.
	 * @param beanType the type of the bean being checked
	 * @return "true" if this a handler type, "false" otherwise.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

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
	 * Extract and return the URL paths contained in a mapping.
	 */
	protected abstract Set<String> getMappingPathPatterns(T mapping);

	/**
	 * Check if a mapping matches the current request and return a (potentially
	 * new) mapping with conditions relevant to the current request.
	 * @param mapping the mapping to get a match for
	 * @param exchange the current exchange
	 * @return the match, or {@code null} if the mapping doesn't match
	 */
	protected abstract T getMatchingMapping(T mapping, ServerWebExchange exchange);

	/**
	 * Return a comparator for sorting matching mappings.
	 * The returned comparator should sort 'better' matches higher.
	 * @param exchange the current exchange
	 * @return the comparator (never {@code null})
	 */
	protected abstract Comparator<T> getMappingComparator(ServerWebExchange exchange);


	/**
	 * A registry that maintains all mappings to handler methods, exposing methods
	 * to perform lookups.
	 *
	 * <p>Package-private for testing purposes.
	 */
	class MappingRegistry {

		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

		private final Map<T, HandlerMethod> handlerMethodLookup = new LinkedHashMap<>();

		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

		/**
		 * Return all mappings and handler methods. Not thread-safe.
		 */
		public Map<T, HandlerMethod> getMappings() {
			return this.handlerMethodLookup;
		}

		/**
		 * Return CORS configuration. Thread-safe for concurrent use.
		 */
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
			return this.corsLookup.get(original != null ? original : handlerMethod);
		}

		public void register(T mapping, Object handler, Method method) {
			HandlerMethod handlerMethod = createHandlerMethod(handler, method);
			assertUniqueMethodMapping(handlerMethod, mapping);

			if (logger.isInfoEnabled()) {
				logger.info("Mapped \"" + mapping + "\" onto " + handlerMethod);
			}
			this.handlerMethodLookup.put(mapping, handlerMethod);

			CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
			if (corsConfig != null) {
				this.corsLookup.put(handlerMethod, corsConfig);
			}

			this.registry.put(mapping, new MappingRegistration<>(mapping, handlerMethod));
		}

		private void assertUniqueMethodMapping(HandlerMethod newHandlerMethod, T mapping) {
			HandlerMethod handlerMethod = this.handlerMethodLookup.get(mapping);
			if (handlerMethod != null && !handlerMethod.equals(newHandlerMethod)) {
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" + newHandlerMethod.getBean() + "' method \n" +
								newHandlerMethod + "\nto " + mapping + ": There is already '" +
								handlerMethod.getBean() + "' bean method\n" + handlerMethod + " mapped.");
			}
		}

		public void unregister(T mapping) {
			MappingRegistration<T> definition = this.registry.remove(mapping);
			if (definition == null) {
				return;
			}

			this.handlerMethodLookup.remove(definition.getMapping());

			this.corsLookup.remove(definition.getHandlerMethod());
		}
	}


	private static class MappingRegistration<T> {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		public MappingRegistration(T mapping, HandlerMethod handlerMethod) {
			Assert.notNull(mapping, "Mapping must not be null");
			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		public T getMapping() {
			return this.mapping;
		}

		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
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


	private static class PreFlightAmbiguousMatchHandler {

		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
