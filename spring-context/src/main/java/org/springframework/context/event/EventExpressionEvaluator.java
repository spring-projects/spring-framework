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

package org.springframework.context.event;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

/**
 * Utility class handling the SpEL expression parsing. Meant to be used
 * as a reusable, thread-safe component.
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see CachedExpressionEvaluator
 */
class EventExpressionEvaluator extends CachedExpressionEvaluator {

	// shared param discoverer since it caches data internally
	private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

	private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

	private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<AnnotatedElementKey, Method>(64);

	/**
	 * Create the suitable {@link EvaluationContext} for the specified event handling
	 * on the specified method.
	 */
	public EvaluationContext createEvaluationContext(ApplicationEvent event, Class<?> targetClass,
			Method method, Object[] args) {

		Method targetMethod = getTargetMethod(targetClass, method);
		EventExpressionRootObject root = new EventExpressionRootObject(event, args);
		return new MethodBasedEvaluationContext(root, targetMethod, args, this.paramNameDiscoverer);
	}

	/**
	 * Specify if the condition defined by the specified expression matches.
	 */
	public boolean condition(String conditionExpression,
			AnnotatedElementKey elementKey, EvaluationContext evalContext) {

		return getExpression(this.conditionCache, elementKey, conditionExpression)
				.getValue(evalContext, boolean.class);
	}

	private Method getTargetMethod(Class<?> targetClass, Method method) {
		AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
		Method targetMethod = this.targetMethodCache.get(methodKey);
		if (targetMethod == null) {
			targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
			if (targetMethod == null) {
				targetMethod = method;
			}
			this.targetMethodCache.put(methodKey, targetMethod);
		}
		return targetMethod;
	}

}
