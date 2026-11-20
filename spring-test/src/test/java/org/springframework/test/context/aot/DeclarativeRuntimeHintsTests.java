/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		assertThat(reflection().onMethodInvocation(SampleClassWithGetter.class, "getName")).accepts(this.runtimeHints);

		// @ImportRuntimeHints
		assertThat(resource().forResource("org/example/config/enigma.txt")).accepts(this.runtimeHints);
	}

	private void assertReflectionRegistered(Class<?> type) {
		assertThat(reflection().onType(type)).as("Reflection hint for %s", type).accepts(this.runtimeHints);
	}

}
