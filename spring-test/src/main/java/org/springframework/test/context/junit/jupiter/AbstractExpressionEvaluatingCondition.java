/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ContainerExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionCondition;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for implementations of {@link ContainerExecutionCondition}
 * and {@link TestExecutionCondition} that evaluate expressions configured via
 * annotations to determine if a container or test is enabled.
 *
 * <p>Expressions can be any of the following.
 *
 * <ul>
 * <li>Spring Expression Language (SpEL) expression &mdash; for example:
 * <pre style="code">#{systemProperties['os.name'].toLowerCase().contains('mac')}</pre>
 * <li>Placeholder for a property available in the Spring
 * {@link org.springframework.core.env.Environment Environment} &mdash; for example:
 * <pre style="code">${smoke.tests.enabled}</pre>
 * <li>Text literal &mdash; for example:
 * <pre style="code">true</pre>
 * </ul>
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 5.0
 * @see EnabledIf
 * @see DisabledIf
 */
abstract class AbstractExpressionEvaluatingCondition implements ContainerExecutionCondition, TestExecutionCondition {

	private static final Log logger = LogFactory.getLog(AbstractExpressionEvaluatingCondition.class);


	/**
	 * Evaluate the expression configured via the supplied annotation type on
	 * the {@link AnnotatedElement} for the supplied {@link ExtensionContext}.
	 *
	 * @param annotationType the type of annotation to process
	 * @param expressionExtractor a function that extracts the expression from
	 * the annotation
	 * @param reasonExtractor a function that extracts the reason from the
	 * annotation
	 * @param enabledOnTrue indicates whether the returned {@code ConditionEvaluationResult}
	 * should be {@link ConditionEvaluationResult#enabled enabled} if the expression
	 * evaluates to {@code true}
	 * @param context the {@code ExtensionContext}
	 * @return {@link ConditionEvaluationResult#enabled enabled} if the container
	 * or test should be enabled; otherwise {@link ConditionEvaluationResult#disabled disabled}
	 */
	protected <A extends Annotation> ConditionEvaluationResult evaluateAnnotation(Class<A> annotationType,
			Function<A, String> expressionExtractor, Function<A, String> reasonExtractor, boolean enabledOnTrue,
			ExtensionContext context) {

		AnnotatedElement element = context.getElement().get();
		Optional<A> annotation = findMergedAnnotation(element, annotationType);

		if (!annotation.isPresent()) {
			String reason = String.format("%s is enabled since @%s is not present", element,
					annotationType.getSimpleName());
			if (logger.isDebugEnabled()) {
				logger.debug(reason);
			}
			return ConditionEvaluationResult.enabled(reason);
		}

		// @formatter:off
		String expression = annotation.map(expressionExtractor).map(String::trim).filter(StringUtils::hasLength)
				.orElseThrow(() -> new IllegalStateException(String.format(
						"The expression in @%s on [%s] must not be blank", annotationType.getSimpleName(), element)));
		// @formatter:on

		boolean evaluatedToTrue = evaluateExpression(expression, annotationType, context);

		if (evaluatedToTrue) {
			String adjective = (enabledOnTrue ? "enabled" : "disabled");
			String reason = annotation.map(reasonExtractor).filter(StringUtils::hasText).orElseGet(
					() -> String.format("%s is %s because @%s(\"%s\") evaluated to true", element, adjective,
						annotationType.getSimpleName(), expression));
			if (logger.isInfoEnabled()) {
				logger.info(reason);
			}
			return (enabledOnTrue ? ConditionEvaluationResult.enabled(reason)
					: ConditionEvaluationResult.disabled(reason));
		}
		else {
			String adjective = (enabledOnTrue ? "disabled" : "enabled");
			String reason = String.format("%s is %s because @%s(\"%s\") did not evaluate to true",
					element, adjective, annotationType.getSimpleName(), expression);
			if (logger.isDebugEnabled()) {
				logger.debug(reason);
			}
			return (enabledOnTrue ? ConditionEvaluationResult.disabled(reason)
					: ConditionEvaluationResult.enabled(reason));
		}
	}

	private <A extends Annotation> boolean evaluateExpression(String expression, Class<A> annotationType,
			ExtensionContext extensionContext) {

		ApplicationContext applicationContext = SpringExtension.getApplicationContext(extensionContext);

		if (!(applicationContext instanceof ConfigurableApplicationContext)) {
			if (logger.isWarnEnabled()) {
				String contextType = (applicationContext != null ? applicationContext.getClass().getName() : "null");
				logger.warn(String.format("@%s(\"%s\") could not be evaluated on [%s] since the test " +
						"ApplicationContext [%s] is not a ConfigurableApplicationContext",
						annotationType.getSimpleName(), expression, extensionContext.getElement(), contextType));
			}
			return false;
		}

		ConfigurableBeanFactory configurableBeanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
		BeanExpressionResolver expressionResolver = configurableBeanFactory.getBeanExpressionResolver();
		BeanExpressionContext beanExpressionContext = new BeanExpressionContext(configurableBeanFactory, null);

		Object result = expressionResolver.evaluate(configurableBeanFactory.resolveEmbeddedValue(expression),
			beanExpressionContext);

		Assert.state((result instanceof Boolean || result instanceof String),
			() -> String.format("@%s(\"%s\") must evaluate to a String or a Boolean, not %s",
				annotationType.getSimpleName(), expression, (result != null ? result.getClass().getName() : "null")));

		return (result instanceof Boolean && ((Boolean) result).booleanValue())
				|| (result instanceof String && Boolean.parseBoolean((String) result));
	}

	private static <A extends Annotation> Optional<A> findMergedAnnotation(AnnotatedElement element,
			Class<A> annotationType) {
		return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, annotationType));
	}

}
