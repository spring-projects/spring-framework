/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code EnabledIfCondition} is an {@link org.junit.jupiter.api.extension.ExecutionCondition}
 * that supports the {@link EnabledIf @EnabledIf} annotation when using the <em>Spring
 * TestContext Framework</em> in conjunction with JUnit 5's <em>Jupiter</em> programming model.
 *
 * <p>Any attempt to use the {@code EnabledIfCondition} without the presence of
 * {@link EnabledIf @EnabledIf} will result in an <em>enabled</em>
 * {@link ConditionEvaluationResult}.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see EnabledIf
 * @see DisabledIf
 * @see SpringExtension
 */
public class EnabledIfCondition extends AbstractExpressionEvaluatingCondition {

	/**
	 * Containers and tests are enabled if {@code @EnabledIf} is present on the
	 * corresponding test class or test method and the configured expression
	 * evaluates to {@code true}.
	 */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return evaluateAnnotation(EnabledIf.class, EnabledIf::expression, EnabledIf::reason,
				EnabledIf::loadContext, true, context);
	}

}
