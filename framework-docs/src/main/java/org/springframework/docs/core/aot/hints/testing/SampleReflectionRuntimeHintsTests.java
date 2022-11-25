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

package org.springframework.docs.core.aot.hints.testing;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsInvocations;
import org.springframework.aot.test.agent.RuntimeHintsRecorder;
import org.springframework.core.SpringVersion;

import static org.assertj.core.api.Assertions.assertThat;

// @EnabledIfRuntimeHintsAgent signals that the annotated test class or test
// method is only enabled if the RuntimeHintsAgent is loaded on the current JVM.
// It also tags tests with the "RuntimeHints" JUnit tag.
@EnabledIfRuntimeHintsAgent
class SampleReflectionRuntimeHintsTests {

	@Test
	void shouldRegisterReflectionHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		// Call a RuntimeHintsRegistrar that contributes hints like:
		runtimeHints.reflection().registerType(SpringVersion.class, typeHint ->
				typeHint.withMethod("getVersion", List.of(), ExecutableMode.INVOKE));

		// Invoke the relevant piece of code we want to test within a recording lambda
		RuntimeHintsInvocations invocations = RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.performReflection();
		});
		// assert that the recorded invocations are covered by the contributed hints
		assertThat(invocations).match(runtimeHints);
	}

}
