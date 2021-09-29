/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A simple {@link CacheResolver} that resolves the {@link Cache} instance(s)
 * based on a configurable {@link CacheManager} and the name of the cache(s)
 * (which can be templated SpEL expressions)
 * as provided by {@link BasicOperation#getCacheNames() getCacheNames()}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sam Kruglov
 * @since 4.1
 * @see BasicOperation#getCacheNames()
 */
public class SimpleCacheResolver extends AbstractCacheResolver {

	private static final Log logger = LogFactory.getLog(SimpleCacheResolver.class);

	private final CacheOperationExpressionEvaluator cacheOperationExpressionEvaluator;
	private final TemplateParserContext templateParserContext = new TemplateParserContext();
	@Nullable
	private final BeanFactory beanFactory;

	/**
	 * Construct a new {@code SimpleCacheResolver}.
	 * @see #setCacheManager
	 */
	public SimpleCacheResolver() {
		cacheOperationExpressionEvaluator = new CacheOperationExpressionEvaluator();
		beanFactory = null;
	}

	/**
	 * Construct a new {@code SimpleCacheResolver} for the given {@link CacheManager}.
	 * @param cacheManager the CacheManager to use
	 */
	public SimpleCacheResolver(CacheManager cacheManager) {
		super(cacheManager);
		cacheOperationExpressionEvaluator = new CacheOperationExpressionEvaluator();
		beanFactory = null;
	}

	public SimpleCacheResolver(
			CacheManager cacheManager,
			BeanFactory beanFactory
	) {
		super(cacheManager);
		this.cacheOperationExpressionEvaluator = new CacheOperationExpressionEvaluator();
		this.beanFactory = beanFactory;
	}

	public SimpleCacheResolver(
			CacheManager cacheManager,
			CacheOperationExpressionEvaluator cacheOperationExpressionEvaluator
	) {
		super(cacheManager);
		this.cacheOperationExpressionEvaluator = cacheOperationExpressionEvaluator;
		beanFactory = null;
	}

	public SimpleCacheResolver(
			CacheManager cacheManager,
			@Nullable CacheOperationExpressionEvaluator cacheOperationExpressionEvaluator,
			@Nullable BeanFactory beanFactory
	) {
		super(cacheManager);
		this.cacheOperationExpressionEvaluator = cacheOperationExpressionEvaluator != null
				? cacheOperationExpressionEvaluator
				: new CacheOperationExpressionEvaluator();
		this.beanFactory = beanFactory;
	}


	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return context.getOperation().getCacheNames()
				.stream()
				.map(name -> parseExpression(context, name))
				.toList();
	}

	private String parseExpression(CacheOperationInvocationContext<?> context, String expression) {
		if (!expression.contains(templateParserContext.getExpressionPrefix())) return expression;
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(context.getTarget());
		Method method = context.getMethod();
		EvaluationContext evaluationContext = cacheOperationExpressionEvaluator.createEvaluationContext(
				null,
				method,
				context.getArgs(),
				context.getTarget(),
				targetClass,
				(!Proxy.isProxyClass(targetClass) ? AopUtils.getMostSpecificMethod(method, targetClass) : method),
				null,
				beanFactory
		);
		String cacheName = cacheOperationExpressionEvaluator.cacheName(
				expression, new AnnotatedElementKey(method, targetClass),
				evaluationContext, templateParserContext
		);
		Assert.state(cacheName != null,
				"Null cache name returned for cache operation (maybe you are " +
						"using named params on classes without debug info?) " + context.getOperation()
		);
		if (logger.isTraceEnabled()) {
			logger.trace("Computed cache name '" + cacheName + "' for operation " + context.getOperation());
		}
		return cacheName;
	}


	/**
	 * Return a {@code SimpleCacheResolver} for the given {@link CacheManager}.
	 * @param cacheManager the CacheManager (potentially {@code null})
	 * @return the SimpleCacheResolver ({@code null} if the CacheManager was {@code null})
	 * @since 5.1
	 */
	@Nullable
	static SimpleCacheResolver of(@Nullable CacheManager cacheManager) {
		return (cacheManager != null ? new SimpleCacheResolver(cacheManager) : null);
	}

	/**
	 * Return a {@code SimpleCacheResolver} for the given {@link CacheManager}.
	 * @return the SimpleCacheResolver ({@code null} if the CacheManager was {@code null})
	 * @since 5.1
	 */
	@Nullable
	static SimpleCacheResolver of(
			@Nullable CacheManager cacheManager,
			@Nullable CacheOperationExpressionEvaluator evaluator,
			@Nullable BeanFactory beanFactory
	) {
		return (cacheManager != null ? new SimpleCacheResolver(cacheManager, evaluator, beanFactory) : null);
	}

}
