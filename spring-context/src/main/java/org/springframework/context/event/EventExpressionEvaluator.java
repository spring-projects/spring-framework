/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.event;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Utility class for handling SpEL expression parsing for application events.
 * <p>Meant to be used as a reusable, thread-safe component.
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see CachedExpressionEvaluator
 */
class EventExpressionEvaluator extends CachedExpressionEvaluator {

	private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

	private final StandardEvaluationContext originalEvaluationContext;

	EventExpressionEvaluator(StandardEvaluationContext originalEvaluationContext) {
		this.originalEvaluationContext = originalEvaluationContext;
	}

	/**
	 * Determine if the condition defined by the specified expression evaluates
	 * to {@code true}.
	 */
	public boolean condition(String conditionExpression, ApplicationEvent event, Method targetMethod,
			AnnotatedElementKey methodKey, Object[] args) {

		EventExpressionRootObject rootObject = new EventExpressionRootObject(event, args);
		EvaluationContext evaluationContext = createEvaluationContext(rootObject, targetMethod, args);
		return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(
				evaluationContext, Boolean.class)));
	}

	private EvaluationContext createEvaluationContext(EventExpressionRootObject rootObject,
			Method method, Object[] args) {

		MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject,
				method, args, getParameterNameDiscoverer());
		this.originalEvaluationContext.applyDelegatesTo(evaluationContext);
		return evaluationContext;
	}

}
