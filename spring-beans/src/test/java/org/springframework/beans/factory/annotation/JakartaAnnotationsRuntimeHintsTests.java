/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.annotation;


import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JakartaAnnotationsRuntimeHints}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class JakartaAnnotationsRuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();

	@BeforeEach
	void setup() {
		AotServices.factories().load(RuntimeHintsRegistrar.class)
				.forEach(registrar -> registrar.registerHints(this.hints,
						ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void jakartaInjectAnnotationHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(Inject.class)).accepts(this.hints);
	}

	@Test
	void jakartaProviderAnnotationHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(Provider.class)).accepts(this.hints);
	}

	@Test
	void jakartaQualifierAnnotationHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(Qualifier.class)).accepts(this.hints);
	}

}
