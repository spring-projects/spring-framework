/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.tests;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.util.Assert;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * {@link ExecutionCondition} for Spring's {@link TestGroup} support.
 *
 * @author Sam Brannen
 * @since 5.2
 * @see EnabledForTestGroups @EnabledForTestGroups
 */
class TestGroupsCondition implements ExecutionCondition {

	private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledForTestGroups is not present");


	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<EnabledForTestGroups> optional = findAnnotation(context.getElement(), EnabledForTestGroups.class);
		if (!optional.isPresent()) {
			return ENABLED_BY_DEFAULT;
		}
		TestGroup[] testGroups = optional.get().value();
		Assert.state(testGroups.length > 0, "You must declare at least one TestGroup in @EnabledForTestGroups");
		return (Arrays.stream(testGroups).anyMatch(TestGroup::isActive)) ?
				enabled("Enabled for TestGroups: " + Arrays.toString(testGroups)) :
				disabled("Disabled for TestGroups: " + Arrays.toString(testGroups));
	}

}
