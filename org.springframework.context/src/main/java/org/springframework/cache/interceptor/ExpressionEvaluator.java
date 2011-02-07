/*
 * Copyright 2010 the original author or authors.
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
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Utility class handling the SpEL expression parsing.
 * Meant to be used as a reusable, thread-safe component.
 * 
 * Performs internal caching for performance reasons. 
 * 
 * @author Costin Leau
 */
class ExpressionEvaluator {

	private SpelExpressionParser parser = new SpelExpressionParser();
	// shared param discoverer since it caches data internally
	private ParameterNameDiscoverer paramNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private Map<Method, Expression> conditionCache = new ConcurrentHashMap<Method, Expression>();
	private Map<Method, Expression> keyCache = new ConcurrentHashMap<Method, Expression>();
	private Map<Method, Method> targetMethodCache = new ConcurrentHashMap<Method, Method>();

	EvaluationContext createEvaluationContext(Collection<Cache<?, ?>> caches, Method method, Object[] args, Class<?> targetClass) {
		DefaultCacheExpressionRootObject rootObject = new DefaultCacheExpressionRootObject(caches, method.getName());
		StandardEvaluationContext evaluationContext = new LazyParamAwareEvaluationContext(rootObject,
				paramNameDiscoverer, method, args, targetClass, targetMethodCache);

		return evaluationContext;
	}

	boolean condition(String conditionExpression, Method method, EvaluationContext evalContext) {
		Expression condExp = conditionCache.get(conditionExpression);
		if (condExp == null) {
			condExp = parser.parseExpression(conditionExpression);
			conditionCache.put(method, condExp);
		}
		return condExp.getValue(evalContext, boolean.class);
	}

	Object key(String keyExpression, Method method, EvaluationContext evalContext) {
		Expression keyExp = keyCache.get(keyExpression);
		if (keyExp == null) {
			keyExp = parser.parseExpression(keyExpression);
			keyCache.put(method, keyExp);
		}
		return keyExp.getValue(evalContext);
	}
}