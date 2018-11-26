/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract base class for {@link HandlerMapping} implementations that define
 * a mapping between a request and a {@link HandlerMethod}.
 *
 * <p>For each registered handler method, a unique mapping is maintained with
 * subclasses defining the details of the mapping type {@code <T>}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @param <T> the mapping for a {@link HandlerMethod} containing the conditions
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

    /**
     * Mapping 注册表
     */
	private final MappingRegistry mappingRegistry = new MappingRegistry();

	// TODO: handlerMethodMappingNamingStrategy

	/**
	 * Return a (read-only) map with all mappings and HandlerMethod's.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
	    // 获得读锁
		this.mappingRegistry.acquireReadLock();
		try {
		    // TODO 芋艿
			return Collections.unmodifiableMap(this.mappingRegistry.getMappings());
		} finally {
		    // 释放读锁
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * Return the internal mapping registry. Provided for testing purposes.
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
     * 注册 Mapping
     *
	 * Register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 * @param mapping the mapping for the handler method
	 * @param handler the handler
	 * @param method the method
	 */
	public void registerMapping(T mapping, Object handler, Method method) {
		if (logger.isTraceEnabled()) {
			logger.trace("Register \"" + mapping + "\" to " + method.toGenericString());
		}
		// 注册
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
     * 取消注册 Mapping
     *
	 * Un-register the given mapping.
	 * <p>This method may be invoked at runtime after initialization has completed.
	 * @param mapping the mapping to unregister
	 */
	public void unregisterMapping(T mapping) {
		if (logger.isTraceEnabled()) {
			logger.trace("Unregister mapping \"" + mapping);
		}
		// 取消注册
		this.mappingRegistry.unregister(mapping);
	}


	// Handler method detection

	/**
	 * Detects handler methods at initialization.
	 */
	@Override
	public void afterPropertiesSet() {
	    // 初始化处理器的方法们
		initHandlerMethods();

		// 打印日志
		// Total includes detected mappings + explicit registrations via registerMapping..
		int total = this.getHandlerMethods().size();
		if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0) ) {
			logger.debug(total + " mappings in " + formatMappingName());
		}
	}

	/**
	 * Scan beans in the ApplicationContext, detect and register handler methods.
	 * @see #isHandler(Class)
	 * @see #getMappingForMethod(Method, Class)
	 * @see #handlerMethodsInitialized(Map)
	 */
	protected void initHandlerMethods() {
	    // 获得所有 Bean 的名字的集合
		String[] beanNames = obtainApplicationContext().getBeanNamesForType(Object.class);

		// 遍历 Bean ，逐个判断 Bean 是否为处理器，如果是，则扫描处理器方法
		for (String beanName : beanNames) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
			    // 获得 Bean 类型
				Class<?> beanType = null;
				try {
					beanType = obtainApplicationContext().getType(beanName);
				} catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isTraceEnabled()) {
						logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
					}
				}
				// 判断 Bean 是否为处理器，如果是，则扫描处理器方法
				if (beanType != null && isHandler(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}

		// 初始化处理器的方法们。目前是空方法，暂无具体的实现
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
     * 扫描处理器的方法们
     *
	 * Look for handler methods in a handler.
	 * @param handler the bean name of a handler or a handler instance
	 */
	protected void detectHandlerMethods(final Object handler) {
	    // 获得处理器类型
		Class<?> handlerType = (handler instanceof String ?
				obtainApplicationContext().getType((String) handler) : handler.getClass());

		if (handlerType != null) {
		    // 获得真实的类。因为，handlerType 可能是代理类
			final Class<?> userType = ClassUtils.getUserClass(handlerType);
			// 获得匹配的方法的集合
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> getMappingForMethod(method, userType)); // 抽象方法，子类实现
			if (logger.isTraceEnabled()) {
				logger.trace("Mapped " + methods.size() + " handler method(s) for " + userType + ": " + methods);
			}
			// 遍历方法，逐个注册 HandlerMethod
			methods.forEach((key, mapping) -> {
				Method invocableMethod = AopUtils.selectInvocableMethod(key, userType);
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
		}
	}

	/**
     * 注册 HandlerMethod
     *
	 * Register a handler method and its unique mapping. Invoked at startup for
	 * each detected handler method.
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 * @throws IllegalStateException if another method was already registered
	 * under the same mapping
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
     * 创建 HandlerMethod 对象
     *
	 * Create the HandlerMethod instance.
	 * @param handler either a bean name or an actual handler instance
	 * @param method the target method
	 * @return the created HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
	    // 如果 handler 类型为 String， 说明对应一个 Bean 对象，例如 UserController 使用 @Controller 注解后，默认 handler 为它的 beanName ，即 `userController`
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName,
					obtainApplicationContext().getAutowireCapableBeanFactory(), method);
        // 如果 handler 类型非 String ，说明是一个已经是一个 handler 对象，就无需处理，直接创建 HandlerMethod 对象
		} else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 * Extract and return the CORS configuration for the mapping.
	 */
	@Nullable
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
	    // 获得读锁
		this.mappingRegistry.acquireReadLock();
		try {
		    // 获得 HandlerMethod 对象
			HandlerMethod handlerMethod;
			try {
				handlerMethod = lookupHandlerMethod(exchange);
			} catch (Exception ex) {
				return Mono.error(ex);
			}
			// 进一步，获得 HandlerMethod 对象
			if (handlerMethod != null) {
				handlerMethod = handlerMethod.createWithResolvedBean();
			}
			// 封装成 Mono 对象返回
			return Mono.justOrEmpty(handlerMethod);
		} finally {
		    // 释放读锁
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * Look up the best-matching handler method for the current request.
	 * If multiple matches are found, the best match is selected.
	 * @param exchange the current exchange
	 * @return the best-matching handler method, or {@code null} if no match
	 * @see #handleMatch
	 * @see #handleNoMatch
	 */
	@Nullable
	protected HandlerMethod lookupHandlerMethod(ServerWebExchange exchange) throws Exception {
	    // 将当前请求和注册表中的 Mapping 进行匹配。若匹配成功，则生成 Mapping 记录，添加到 matches 中
		List<Match> matches = new ArrayList<>();
		addMatchingMappings(this.mappingRegistry.getMappings().keySet(), matches, exchange);

		// 如果匹配到，则获取最佳匹配的 Match 对象的 handlerMethod 属性
		if (!matches.isEmpty()) {
		    // 创建 MatchComparator 对象，排序 matches 结果
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(exchange));
			matches.sort(comparator);
			// 获得首个 Match 对象
			Match bestMatch = matches.get(0);
			// 处理存在多个 Match 对象的情况！！！
			if (matches.size() > 1) {
				if (logger.isTraceEnabled()) {
					logger.trace(exchange.getLogPrefix() + matches.size() + " matching mappings: " + matches);
				}
				// TODO 1012 cors
				if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
					return PREFLIGHT_AMBIGUOUS_MATCH;
				}
				// 获得第二个 Match 对象
				Match secondBestMatch = matches.get(1);
				// 比较 bestMatch 和 secondBestMatch ，如果相等，说明有问题，抛出 IllegalStateException 异常
                // 因为，两个优先级一样高，说明无法判断谁更优先
				if (comparator.compare(bestMatch, secondBestMatch) == 0) {
					Method m1 = bestMatch.handlerMethod.getMethod();
					Method m2 = secondBestMatch.handlerMethod.getMethod();
					RequestPath path = exchange.getRequest().getPath();
					throw new IllegalStateException(
							"Ambiguous handler methods mapped for '" + path + "': {" + m1 + ", " + m2 + "}");
				}
			}
			// 处理首个 Match 对象
			handleMatch(bestMatch.mapping, bestMatch.handlerMethod, exchange);
			// 返回首个 Match 对象的 handlerMethod 属性
			return bestMatch.handlerMethod;
        // 如果匹配不到，则处理不匹配的情况
		} else {
			return handleNoMatch(this.mappingRegistry.getMappings().keySet(), exchange);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, ServerWebExchange exchange) {
	    // 遍历 Mapping 数组
		for (T mapping : mappings) {
		    // 执行匹配
			T match = getMatchingMapping(mapping, exchange);
			// 如果匹配，则创建 Match 对象，添加到 matches 中
			if (match != null) {
				matches.add(new Match(match, this.mappingRegistry.getMappings().get(mapping)));
			}
		}
	}

	/**
	 * Invoked when a matching mapping is found.
	 * @param mapping the matching mapping
	 * @param handlerMethod the matching method
	 * @param exchange the current exchange
	 */
	protected void handleMatch(T mapping, HandlerMethod handlerMethod, ServerWebExchange exchange) {
	}

	/**
	 * Invoked when no matching mapping is not found.
	 * @param mappings all registered mappings
	 * @param exchange the current exchange
	 * @return an alternative HandlerMethod or {@code null}
	 * @throws Exception provides details that can be translated into an error status code
	 */
	@Nullable
	protected HandlerMethod handleNoMatch(Set<T> mappings, ServerWebExchange exchange) throws Exception {
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
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * Check if a mapping matches the current request and return a (potentially
	 * new) mapping with conditions relevant to the current request.
	 * @param mapping the mapping to get a match for
	 * @param exchange the current exchange
	 * @return the match, or {@code null} if the mapping doesn't match
	 */
	@Nullable
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
	 * to perform lookups and providing concurrent access.
	 *
	 * <p>Package-private for testing purposes.
	 */
	class MappingRegistry {

        /**
         * 注册表
         *
         * KEY: Mapping
         */
		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

        /**
         * 注册表2
         *
         * KEY：Mapping
         */
		private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<>();

        /**
         * TODO 1012 cors
         */
		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

        /**
         * 读写锁
         */
		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

		/**
		 * Return all mappings and handler methods. Not thread-safe.
		 * @see #acquireReadLock()
		 */
		public Map<T, HandlerMethod> getMappings() {
			return this.mappingLookup;
		}

		/**
         * TODO 1012 cors
         *
		 * Return CORS configuration. Thread-safe for concurrent use.
		 */
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
			return this.corsLookup.get(original != null ? original : handlerMethod);
		}

		/**
         * 获得读锁
         *
		 * Acquire the read lock when using getMappings and getMappingsByUrl.
		 */
		public void acquireReadLock() {
			this.readWriteLock.readLock().lock();
		}

		/**
         * 获得写锁
         *
		 * Release the read lock after using getMappings and getMappingsByUrl.
		 */
		public void releaseReadLock() {
			this.readWriteLock.readLock().unlock();
		}

		public void register(T mapping, Object handler, Method method) {
		    // 获得写锁
			this.readWriteLock.writeLock().lock();
			try {
			    // 创建 HandlerMethod 对象
				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
				// 校验当前 mapping 不存在，否则抛出 IllegalStateException 异常
				assertUniqueMethodMapping(handlerMethod, mapping);
				// 添加 mapping + HandlerMethod 到 mappingLookup 中
				this.mappingLookup.put(mapping, handlerMethod);

				// TODO 1012 cors
				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
				if (corsConfig != null) {
					this.corsLookup.put(handlerMethod, corsConfig);
				}

				// 创建 MappingRegistration 对象，并 mapping + MappingRegistration 添加到 registry 中
				this.registry.put(mapping, new MappingRegistration<>(mapping, handlerMethod));
			} finally {
			    // 释放写锁
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void assertUniqueMethodMapping(HandlerMethod newHandlerMethod, T mapping) {
			HandlerMethod handlerMethod = this.mappingLookup.get(mapping);
			if (handlerMethod != null && !handlerMethod.equals(newHandlerMethod)) { // 存在，且不相等，说明不唯一
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" + newHandlerMethod.getBean() + "' method \n" +
								newHandlerMethod + "\nto " + mapping + ": There is already '" +
								handlerMethod.getBean() + "' bean method\n" + handlerMethod + " mapped.");
			}
		}

		public void unregister(T mapping) {
            // 获得写锁
			this.readWriteLock.writeLock().lock();
			try {
			    // 从 registry 中移除
                MappingRegistration<T> definition = this.registry.remove(mapping);
				if (definition == null) {
					return;
				}
				// 从 mappingLookup 中移除
				this.mappingLookup.remove(definition.getMapping());
				// 从 corsLookup 中移除
				this.corsLookup.remove(definition.getHandlerMethod());
			} finally {
			    // 释放写锁
				this.readWriteLock.writeLock().unlock();
			}
		}

	}

	private static class MappingRegistration<T> {

        /**
         * Mapping 对象
         */
		private final T mapping;
        /**
         * HandlerMethod 对象
         */
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

        /**
         * Mapping 对象
         */
		private final T mapping;
        /**
         * HandlerMethod 对象
         */
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

		@SuppressWarnings("unused")
		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
