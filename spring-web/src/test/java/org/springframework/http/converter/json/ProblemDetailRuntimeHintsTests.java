/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;


import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.http.ProblemDetail;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProblemDetailRuntimeHints}.
 * @author Brian Clozel
 */
class ProblemDetailRuntimeHintsTests {

	private static final List<String> METHOD_NAMES = List.of("getType", "getTitle",
			"getStatus", "getDetail", "getInstance", "getProperties");

	private final RuntimeHints hints = new RuntimeHints();

	@BeforeEach
	void setup() {
		AotServices.factories().load(RuntimeHintsRegistrar.class)
				.forEach(registrar -> registrar.registerHints(this.hints,
						ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void getterMethodsShouldHaveReflectionHints() {
		for (String methodName : METHOD_NAMES) {
			assertThat(RuntimeHintsPredicates.reflection()
					.onMethodInvocation(ProblemDetail.class, methodName)).accepts(this.hints);
		}
	}

	@Test
	void mixinShouldHaveReflectionHints() {
		for (String methodName : METHOD_NAMES) {
			assertThat(RuntimeHintsPredicates.reflection()
					.onMethodInvocation(ProblemDetailJacksonXmlMixin.class, methodName)).accepts(this.hints);
		}
	}

}
