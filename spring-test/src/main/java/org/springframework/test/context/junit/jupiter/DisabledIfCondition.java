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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ContainerExecutionCondition;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionCondition;
import org.junit.jupiter.api.extension.TestExtensionContext;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code DisabledIfCondition} is a composite {@link ContainerExecutionCondition}
 * and {@link TestExecutionCondition} that supports the {@link DisabledIf @DisabledIf}
 * annotation when using the <em>Spring TestContext Framework</em> in conjunction
 * with JUnit 5's <em>Jupiter</em> programming model.
 *
 * <p>Any attempt to use {@code DisabledIfCondition} without the presence of
 * {@link DisabledIf @DisabledIf} will result in an {@link IllegalStateException}.
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 5.0
 * @see org.springframework.test.context.junit.jupiter.DisabledIf
 * @see org.springframework.test.context.junit.jupiter.SpringExtension
 */
public class DisabledIfCondition implements ContainerExecutionCondition, TestExecutionCondition {

	private static final Log logger = LogFactory.getLog(DisabledIfCondition.class);


	/**
	 * Containers are disabled if {@code @DisabledIf} is present on the test class
	 * and the configured expression evaluates to {@code true}.
	 */
	@Override
	public ConditionEvaluationResult evaluate(ContainerExtensionContext context) {
		return evaluateDisabledIf(context);
	}

	/**
	 * Tests are disabled if {@code @DisabledIf} is present on the test method
	 * and the configured expression evaluates to {@code true}.
	 */
	@Override
	public ConditionEvaluationResult evaluate(TestExtensionContext context) {
		return evaluateDisabledIf(context);
	}

	private ConditionEvaluationResult evaluateDisabledIf(ExtensionContext extensionContext) {
		AnnotatedElement element = extensionContext.getElement().get();
		Optional<DisabledIf> disabledIf = findMergedAnnotation(element, DisabledIf.class);
		Assert.state(disabledIf.isPresent(), () -> "@DisabledIf must be present on " + element);

		// @formatter:off
		String expression = disabledIf.map(DisabledIf::expression).map(String::trim).filter(StringUtils::hasLength)
				.orElseThrow(() -> new IllegalStateException(String.format(
						"The expression in @DisabledIf on [%s] must not be blank", element)));
		// @formatter:on

		if (isDisabled(expression, extensionContext)) {
			String reason = disabledIf.map(DisabledIf::reason).filter(StringUtils::hasText).orElseGet(
				() -> String.format("%s is disabled because @DisabledIf(\"%s\") evaluated to true", element,
					expression));
			logger.info(reason);
			return ConditionEvaluationResult.disabled(reason);
		}
		else {
			String reason = String.format("%s is enabled because @DisabledIf(\"%s\") did not evaluate to true",
					element, expression);
			logger.debug(reason);
			return ConditionEvaluationResult.enabled(reason);
		}
	}

	private boolean isDisabled(String expression, ExtensionContext extensionContext) {
		ApplicationContext applicationContext = SpringExtension.getApplicationContext(extensionContext);

		if (!(applicationContext instanceof ConfigurableApplicationContext)) {
			if (logger.isWarnEnabled()) {
				String contextType = (applicationContext != null ? applicationContext.getClass().getName() : "null");
				logger.warn(String.format("@DisabledIf(\"%s\") could not be evaluated on [%s] since the test " +
						"ApplicationContext [%s] is not a ConfigurableApplicationContext",
						expression, extensionContext.getElement(), contextType));
			}
			return false;
		}

		ConfigurableBeanFactory configurableBeanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
		BeanExpressionResolver expressionResolver = configurableBeanFactory.getBeanExpressionResolver();
		BeanExpressionContext beanExpressionContext = new BeanExpressionContext(configurableBeanFactory, null);

		Object result = expressionResolver.evaluate(configurableBeanFactory.resolveEmbeddedValue(expression),
			beanExpressionContext);

		Assert.state((result instanceof Boolean || result instanceof String), () ->
				String.format("@DisabledIf(\"%s\") must evaluate to a String or a Boolean, not %s", expression,
						(result != null ? result.getClass().getName() : "null")));

		boolean disabled = (result instanceof Boolean && ((Boolean) result).booleanValue()) ||
				(result instanceof String && Boolean.parseBoolean((String) result));

		return disabled;
	}

	private static <A extends Annotation> Optional<A> findMergedAnnotation(AnnotatedElement element,
			Class<A> annotationType) {
		return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, annotationType));
	}

}
