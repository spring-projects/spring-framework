/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code DisabledIfCondition} is an {@link org.junit.jupiter.api.extension.ExecutionCondition}
 * that supports the {@link DisabledIf @DisabledIf} annotation when using the <em>Spring
 * TestContext Framework</em> in conjunction with the JUnit Jupiter testing framework.
 *
 * <p>Any attempt to use the {@code DisabledIfCondition} without the presence of
 * {@link DisabledIf @DisabledIf} will result in an <em>enabled</em>
 * {@link ConditionEvaluationResult}.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see DisabledIf
 * @see EnabledIf
 * @see SpringExtension
 */
public class DisabledIfCondition extends AbstractExpressionEvaluatingCondition {

	/**
	 * Containers and tests are disabled if {@code @DisabledIf} is present on the
	 * corresponding test class or test method and the configured expression evaluates
	 * to {@code true}.
	 */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return evaluateAnnotation(DisabledIf.class, DisabledIf::expression, DisabledIf::reason,
				DisabledIf::loadContext, false, context);
	}

}
