/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.predicate;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SerializationHintsPredicates}.
 *
 * @author Stephane Nicoll
 */
@Deprecated
@SuppressWarnings("removal")
class SerializationHintsPredicatesTests {

	private final SerializationHintsPredicates serialization = new SerializationHintsPredicates();

	private final RuntimeHints runtimeHints = new RuntimeHints();

	@Test
	void shouldMatchRegisteredClass() {
		runtimeHints.serialization().registerType(String.class);
		assertThat(serialization.onType(String.class).test(runtimeHints)).isTrue();
	}

	@Test
	void shouldMatchRegisteredTypeReference() {
		runtimeHints.serialization().registerType(TypeReference.of(String.class));
		assertThat(serialization.onType(String.class).test(runtimeHints)).isTrue();
	}

	@Test
	void shouldNotMatchUnregisteredType() {
		runtimeHints.serialization().registerType(Integer.class);
		assertThat(serialization.onType(Long.class).test(runtimeHints)).isFalse();
	}

}
