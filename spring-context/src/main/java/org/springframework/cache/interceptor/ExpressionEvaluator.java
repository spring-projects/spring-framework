/*
 * Copyright 2002-2013 the original author or authors.
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
 * @author Phillip Webb
 * @since 3.1
 */
class ExpressionEvaluator {

	public static final Object NO_RESULT = new Object();

	private final SpelExpressionParser parser = new SpelExpressionParser();

	// shared param discoverer since it caches data internally
	private final ParameterNameDiscoverer paramNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private final Map<String, Expression> keyCache = new ConcurrentHashMap<String, Expression>(64);

	private final Map<String, Expression> conditionCache = new ConcurrentHashMap<String, Expression>(64);

	private final Map<String, Expression> unlessCache = new ConcurrentHashMap<String, Expression>(64);

	private final Map<String, Method> targetMethodCache = new ConcurrentHashMap<String, Method>(64);


	/**
	 * Create an {@link EvaluationContext} without a return value.
	 * @see #createEvaluationContext(Collection, Method, Object[], Object, Class, Object)
	 */
	public EvaluationContext createEvaluationContext(Collection<Cache> caches,
			Method method, Object[] args, Object target, Class<?> targetClass) {
		return createEvaluationContext(caches, method, args, target, targetClass,
				NO_RESULT);
	}

	/**
	 * Create an {@link EvaluationContext}.
	 *
	 * @param caches the current caches
	 * @param method the method
	 * @param args the method arguments
	 * @param target the target object
	 * @param targetClass the target class
	 * @param result the return value (can be {@code null}) or
	 *        {@link #NO_RESULT} if there is no return at this time
	 * @return the evalulation context
	 */
	public EvaluationContext createEvaluationContext(Collection<Cache> caches,
			Method method, Object[] args, Object target, Class<?> targetClass,
			final Object result) {
		CacheExpressionRootObject rootObject = new CacheExpressionRootObject(caches,
				method, args, target, targetClass);
		LazyParamAwareEvaluationContext evaluationContext = new LazyParamAwareEvaluationContext(rootObject,
				this.paramNameDiscoverer, method, args, targetClass, this.targetMethodCache);
		if(result != NO_RESULT) {
			evaluationContext.setVariable("result", result);
		}
		return evaluationContext;
	}

	public Object key(String keyExpression, Method method, EvaluationContext evalContext) {
		return getExpression(this.keyCache, keyExpression, method).getValue(evalContext);
	}

	public boolean condition(String conditionExpression, Method method, EvaluationContext evalContext) {
		return getExpression(this.conditionCache, conditionExpression, method).getValue(
				evalContext, boolean.class);
	}

	public boolean unless(String unlessExpression, Method method, EvaluationContext evalContext) {
		return getExpression(this.unlessCache, unlessExpression, method).getValue(
				evalContext, boolean.class);
	}

	private Expression getExpression(Map<String, Expression> cache, String expression, Method method) {
		String key = toString(method, expression);
		Expression rtn = cache.get(key);
		if (rtn == null) {
			rtn = this.parser.parseExpression(expression);
			cache.put(key, rtn);
		}
		return rtn;
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
