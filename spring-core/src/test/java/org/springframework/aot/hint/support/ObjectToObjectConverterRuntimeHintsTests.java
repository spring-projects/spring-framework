/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.support;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for {@link ObjectToObjectConverterRuntimeHints}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class ObjectToObjectConverterRuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();


	@BeforeEach
	void setup() {
		ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
		SpringFactoriesLoader.forResourceLocation("META-INF/spring/aot.factories")
				.load(RuntimeHintsRegistrar.class)
				.forEach(registrar -> registrar.registerHints(this.hints, classLoader));
	}

	@Test
	void javaSqlDateHasHints() throws NoSuchMethodException {
		assertThat(reflection().onMethodInvocation(java.sql.Date.class, "toLocalDate")).accepts(this.hints);
		assertThat(reflection().onMethodInvocation(java.sql.Date.class.getMethod("valueOf", LocalDate.class))).accepts(this.hints);
	}

	@Test  // gh-35156
	void javaSqlTimestampHasHints() throws NoSuchMethodException {
		assertThat(reflection().onMethodInvocation(java.sql.Timestamp.class.getMethod("from", Instant.class))).accepts(this.hints);
	}

	@Test
	void uriHasHints() {
		assertThat(reflection().onType(URI.class)).accepts(this.hints);
	}

}
