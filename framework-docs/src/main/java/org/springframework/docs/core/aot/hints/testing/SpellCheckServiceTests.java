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

package org.springframework.docs.core.aot.hints.testing;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class SpellCheckServiceTests {

	// tag::hintspredicates[]
	@Test
	void shouldRegisterResourceHints() {
		RuntimeHints hints = new RuntimeHints();
		new SpellCheckServiceRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("dicts/en.txt"))
				.accepts(hints);
	}
	// end::hintspredicates[]

	// Copied here because it is package private in SpellCheckService
	static class SpellCheckServiceRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("dicts/*");
		}
	}

}
