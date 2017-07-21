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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.Cache;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

/**
 * Utility class handling the SpEL expression parsing.
 * Meant to be used as a reusable, thread-safe component.
 *
 * <p>Performs internal caching for performance reasons
 * using {@link AnnotatedElementKey}.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.1
 */
class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

	/**
	 * Indicate that there is no result variable.
	 */
	public static final Object NO_RESULT = new Object();

	/**
	 * Indicate that the result variable cannot be used at all.
	 */
	public static final Object RESULT_UNAVAILABLE = new Object();

	/**
	 * The name of the variable holding the result object.
	 */
	public static final String RESULT_VARIABLE = "result";


	private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);

	private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

	private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<>(64);

	private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<>(64);


	/**
	 * Create an {@link EvaluationContext} without a return value.
	 * @see #createEvaluationContext(Collection, Method, Object[], Object, Class, Object, BeanFactory)
	 */
	public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
			Method method, Object[] args, Object target, Class<?> targetClass, BeanFactory beanFactory) {

		return createEvaluationContext(caches, method, args, target, targetClass, NO_RESULT, beanFactory);
	}

	/**
	 * Create an {@link EvaluationContext}.
	 * @param caches the current caches
	 * @param method the method
	 * @param args the method arguments
	 * @param target the target object
	 * @param targetClass the target class
	 * @param result the return value (can be {@code null}) or
	 * {@link #NO_RESULT} if there is no return at this time
	 * @return the evaluation context
	 */
	public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
			Method method, Object[] args, Object target, Class<?> targetClass, @Nullable Object result,
			@Nullable BeanFactory beanFactory) {

		CacheExpressionRootObject rootObject = new CacheExpressionRootObject(
				caches, method, args, target, targetClass);
		Method targetMethod = getTargetMethod(targetClass, method);
		CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
				rootObject, targetMethod, args, getParameterNameDiscoverer());
		if (result == RESULT_UNAVAILABLE) {
			evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
		}
		else if (result != NO_RESULT) {
			evaluationContext.setVariable(RESULT_VARIABLE, result);
		}
		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		return evaluationContext;
	}

	@Nullable
	public Object key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext);
	}

	public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(
				evalContext, Boolean.class)));
	}

	public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return (Boolean.TRUE.equals(getExpression(this.unlessCache, methodKey, unlessExpression).getValue(
				evalContext, Boolean.class)));
	}

	/**
	 * Clear all caches.
	 */
	void clear() {
		this.keyCache.clear();
		this.conditionCache.clear();
		this.unlessCache.clear();
		this.targetMethodCache.clear();
	}

	private Method getTargetMethod(Class<?> targetClass, Method method) {
		AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
		Method targetMethod = this.targetMethodCache.get(methodKey);
		if (targetMethod == null) {
			targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
			this.targetMethodCache.put(methodKey, targetMethod);
		}
		return targetMethod;
	}


}
