/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.aot.AotDetector;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * {@link ExecutionCondition} implementation for {@link DisabledInAotMode @DisabledInAotMode}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.1.2
 */
class DisabledInAotModeCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		boolean aotEnabled = AotDetector.useGeneratedArtifacts();
		if (aotEnabled) {
			AnnotatedElement element = context.getElement().orElseThrow(() -> new IllegalStateException("No AnnotatedElement"));
			String customReason = findMergedAnnotation(element, DisabledInAotMode.class)
					.map(DisabledInAotMode::value).orElse(null);
			return ConditionEvaluationResult.disabled("Disabled in Spring AOT mode", customReason);
		}
		return ConditionEvaluationResult.enabled("Spring AOT mode is not enabled");
	}

	private static <A extends Annotation> Optional<A> findMergedAnnotation(
			AnnotatedElement element, Class<A> annotationType) {

		return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, annotationType));
	}

}
