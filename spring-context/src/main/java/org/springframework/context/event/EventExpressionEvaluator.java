/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

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
			AnnotatedElementKey methodKey, @Nullable Object[] args) {

		EventExpressionRootObject rootObject = new EventExpressionRootObject(event, args);
		EvaluationContext evaluationContext = createEvaluationContext(rootObject, targetMethod, args);
		return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(
				evaluationContext, Boolean.class)));
	}

	private EvaluationContext createEvaluationContext(EventExpressionRootObject rootObject,
			Method method, @Nullable Object[] args) {

		MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject,
				method, args, getParameterNameDiscoverer());
		this.originalEvaluationContext.applyDelegatesTo(evaluationContext);
		return evaluationContext;
	}

}
