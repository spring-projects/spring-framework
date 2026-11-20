/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture;

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
		if (optional.isEmpty()) {
			return ENABLED_BY_DEFAULT;
		}
		TestGroup[] testGroups = optional.get().value();
		Assert.state(testGroups.length > 0, "You must declare at least one TestGroup in @EnabledForTestGroups");
		return (Arrays.stream(testGroups).anyMatch(TestGroup::isActive)) ?
				enabled("Enabled for TestGroups: " + Arrays.toString(testGroups)) :
				disabled("Disabled for TestGroups: " + Arrays.toString(testGroups));
	}

}
