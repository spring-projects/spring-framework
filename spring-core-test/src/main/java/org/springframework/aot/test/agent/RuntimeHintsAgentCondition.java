/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.test.agent;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.aot.agent.RuntimeHintsAgent;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * {@link ExecutionCondition} for {@link EnabledIfRuntimeHintsAgent @EnabledIfRuntimeHintsAgent}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
class RuntimeHintsAgentCondition implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return findAnnotation(context.getElement(), EnabledIfRuntimeHintsAgent.class)
				.map(annotation -> checkRuntimeHintsAgentPresence())
				.orElse(ConditionEvaluationResult.enabled("@EnabledIfRuntimeHintsAgent is not present"));
	}

	static ConditionEvaluationResult checkRuntimeHintsAgentPresence() {
		return RuntimeHintsAgent.isLoaded() ? ConditionEvaluationResult.enabled("RuntimeHintsAgent is loaded")
				: ConditionEvaluationResult.disabled("RuntimeHintsAgent is not loaded on the current JVM");
	}

}
