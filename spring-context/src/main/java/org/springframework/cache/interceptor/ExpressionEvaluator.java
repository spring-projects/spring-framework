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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Utility class handling the SpEL expression parsing.
 * Meant to be used as a reusable, thread-safe component.
 *
 * <p>Performs internal caching for performance reasons.
 *
 * @author Costin Leau
 * @since 3.1
 */
class ExpressionEvaluator {

	private SpelExpressionParser parser = new SpelExpressionParser();

	// shared param discoverer since it caches data internally
	private ParameterNameDiscoverer paramNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private Map<String, Expression> conditionCache = new ConcurrentHashMap<String, Expression>();

	private Map<String, Expression> keyCache = new ConcurrentHashMap<String, Expression>();

	private Map<String, Method> targetMethodCache = new ConcurrentHashMap<String, Method>();


	public EvaluationContext createEvaluationContext(
			Collection<Cache> caches, Method method, Object[] args, Object target, Class<?> targetClass) {

		CacheExpressionRootObject rootObject =
				new CacheExpressionRootObject(caches, method, args, target, targetClass);
		return new LazyParamAwareEvaluationContext(rootObject,
				this.paramNameDiscoverer, method, args, targetClass, this.targetMethodCache);
	}

	public boolean condition(String conditionExpression, Method method, EvaluationContext evalContext) {
		String key = toString(method, conditionExpression);
		Expression condExp = this.conditionCache.get(key);
		if (condExp == null) {
			condExp = this.parser.parseExpression(conditionExpression);
			this.conditionCache.put(key, condExp);
		}
		return condExp.getValue(evalContext, boolean.class);
	}

	public Object key(String keyExpression, Method method, EvaluationContext evalContext) {
		String key = toString(method, keyExpression);
		Expression keyExp = this.keyCache.get(key);
		if (keyExp == null) {
			keyExp = this.parser.parseExpression(keyExpression);
			this.keyCache.put(key, keyExp);
		}
		return keyExp.getValue(evalContext);
	}

	private String toString(Method method, String expression) {
		StringBuilder sb = new StringBuilder();
		sb.append(method.getDeclaringClass().getName());
		sb.append("#");
		sb.append(method.toString());
		sb.append("#");
		sb.append(expression);
		return sb.toString();
	}
}