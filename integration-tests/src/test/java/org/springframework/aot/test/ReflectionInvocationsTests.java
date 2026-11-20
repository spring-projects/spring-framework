/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.test;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsInvocations;

import static org.assertj.core.api.Assertions.assertThat;


@EnabledIfRuntimeHintsAgent
@SuppressWarnings("removal")
class ReflectionInvocationsTests {

	@Test
	void sampleTest() {
		RuntimeHints hints = new RuntimeHints();
		hints.reflection().registerType(String.class);

		RuntimeHintsInvocations invocations = org.springframework.aot.test.agent.RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.sample(); // does Method[] methods = String.class.getMethods();
		});
		assertThat(invocations).match(hints);
	}

	@Test
	void multipleCallsTest() {
		RuntimeHints hints = new RuntimeHints();
		hints.reflection().registerType(String.class);
		hints.reflection().registerType(Integer.class);
		RuntimeHintsInvocations invocations = org.springframework.aot.test.agent.RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.multipleCalls(); // does Method[] methods = String.class.getMethods(); methods = Integer.class.getMethods();
		});
		assertThat(invocations).match(hints);
	}

}
