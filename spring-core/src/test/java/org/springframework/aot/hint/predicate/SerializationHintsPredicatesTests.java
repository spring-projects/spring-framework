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
