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

package org.springframework.test.context.aot;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.test.context.aot.samples.hints.DeclarativeRuntimeHintsSpringJupiterTests;
import org.springframework.test.context.aot.samples.hints.DeclarativeRuntimeHintsSpringJupiterTests.SampleClassWithGetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.resource;

/**
 * Tests for declarative support for registering run-time hints for tests, tested
 * via the {@link TestContextAotGenerator}
 *
 * @author Sam Brannen
 * @since 6.0
 */
class DeclarativeRuntimeHintsTests extends AbstractAotTests {

	private final RuntimeHints runtimeHints = new RuntimeHints();

	private final TestContextAotGenerator generator =
			new TestContextAotGenerator(new InMemoryGeneratedFiles(), this.runtimeHints);


	@Test
	void declarativeRuntimeHints() {
		Class<?> testClass = DeclarativeRuntimeHintsSpringJupiterTests.class;

		this.generator.processAheadOfTime(Stream.of(testClass));

		// @Reflective
		assertReflectionRegistered(testClass);

		// @RegisterReflectionForBinding
		assertReflectionRegistered(SampleClassWithGetter.class);
		assertReflectionRegistered(String.class);
		assertThat(reflection().onMethod(SampleClassWithGetter.class, "getName")).accepts(this.runtimeHints);

		// @ImportRuntimeHints
		assertThat(resource().forResource("org/example/config/enigma.txt")).accepts(this.runtimeHints);
	}

	private void assertReflectionRegistered(Class<?> type) {
		assertThat(reflection().onType(type)).as("Reflection hint for %s", type).accepts(this.runtimeHints);
	}

}
