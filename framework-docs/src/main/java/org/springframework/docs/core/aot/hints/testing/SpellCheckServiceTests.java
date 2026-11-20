/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
