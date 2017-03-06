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

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ContainerExecutionCondition;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionCondition;
import org.junit.jupiter.api.extension.TestExtensionContext;

/**
 * {@code DisabledIfCondition} is a composite {@link ContainerExecutionCondition}
 * and {@link TestExecutionCondition} that supports the {@link DisabledIf @DisabledIf}
 * annotation when using the <em>Spring TestContext Framework</em> in conjunction
 * with JUnit 5's <em>Jupiter</em> programming model.
 *
 * <p>Any attempt to use the {@code DisabledIfCondition} without the presence of
 * {@link DisabledIf @DisabledIf} will result in an <em>enabled</em>
 * {@link ConditionEvaluationResult}.
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 5.0
 * @see DisabledIf
 * @see EnabledIf
 * @see SpringExtension
 */
public class DisabledIfCondition extends AbstractExpressionEvaluatingCondition {

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

	private ConditionEvaluationResult evaluateDisabledIf(ExtensionContext context) {
		return evaluateAnnotation(DisabledIf.class, DisabledIf::expression, DisabledIf::reason,
			DisabledIf::loadContext, false, context);
	}

}
