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

package org.springframework.aot.test;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsRecorder;

import static org.assertj.core.api.Assertions.assertThat;


@EnabledIfRuntimeHintsAgent
@SuppressWarnings("removal")
class ReflectionInvocationsTests {

	@Test
	void sampleTest() {
		RuntimeHints hints = new RuntimeHints();
		hints.reflection().registerType(String.class);

		assertThat(RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.sample(); // String.class.getMethods();
		})).match(hints);
	}

	@Test
	void multipleCallsTest() {
		RuntimeHints hints = new RuntimeHints();
		hints.reflection().registerType(String.class);
		hints.reflection().registerType(Integer.class);

		assertThat(RuntimeHintsRecorder.record(() -> {
			SampleReflection sample = new SampleReflection();
			sample.multipleCalls(); // String.class.getMethods(); Integer.class.getMethods();
		})).match(hints);
	}

}
