/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedDatabaseFactoryRuntimeHints}.
 *
 * @author Sebastien Deleuze
 */
class EmbeddedDatabaseFactoryRuntimeHintsTests {

	private RuntimeHints hints;

	@BeforeEach
	void setup() {
		this.hints = new RuntimeHints();
		SpringFactoriesLoader.forResourceLocation("META-INF/spring/aot.factories")
				.load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
						.registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void embeddedDataSourceProxyTypeHasHint() throws ClassNotFoundException {
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethodInvocation("org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy", "shutdown"))
				.accepts(this.hints);
	}

}
