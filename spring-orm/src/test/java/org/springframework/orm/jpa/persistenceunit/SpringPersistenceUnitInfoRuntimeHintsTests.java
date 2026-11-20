/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringPersistenceUnitInfoRuntimeHints}.
 *
 * @author Brian Clozel
 */
class SpringPersistenceUnitInfoRuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();

	@BeforeEach
	void setup() {
		AotServices.factories().load(RuntimeHintsRegistrar.class)
				.forEach(registrar -> registrar.registerHints(this.hints,
						ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void springPersistenceUnitInfoHasReflectionHints() {
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethodInvocation(SpringPersistenceUnitInfo.class, "setPersistenceProviderPackageName"))
				.accepts(this.hints);
	}

}
