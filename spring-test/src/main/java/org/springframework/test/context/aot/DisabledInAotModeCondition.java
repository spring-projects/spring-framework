/*
 * Copyright 2002-2024 the original author or authors.
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
