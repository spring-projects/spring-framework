/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aot.hints.testing;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsInvocations;
import org.springframework.core.SpringVersion;

import static org.assertj.core.api.Assertions.assertThat;

// @EnabledIfRuntimeHintsAgent signals that the annotated test class or test
// method is only enabled if the RuntimeHintsAgent is loaded on the current JVM.
// It also tags tests with the "RuntimeHints" JUnit tag.
@EnabledIfRuntimeHintsAgent
@SuppressWarnings("removal")
class SampleReflectionRuntimeHintsTests {

	@Test
	void shouldRegisterReflectionHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		// Call a RuntimeHintsRegistrar that contributes hints like:
		runtimeHints.reflection().registerType(SpringVersion.class, typeHint ->
				typeHint.withMethod("getVersion", List.of(), ExecutableMode.INVOKE));

		// Invoke the relevant piece of code we want to test within a recording lambda
		RuntimeHintsInvocations invocations = org.springframework.aot.test.agent.RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.performReflection();
		});
		// assert that the recorded invocations are covered by the contributed hints
		assertThat(invocations).match(runtimeHints);
	}

}
