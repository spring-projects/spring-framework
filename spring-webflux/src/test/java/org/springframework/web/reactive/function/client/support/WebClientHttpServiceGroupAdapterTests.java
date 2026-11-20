/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client.support;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientHttpServiceGroupAdapter}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Stephane Nicoll
 */
class WebClientHttpServiceGroupAdapterTests {

	@Test
	void registerInstantiationHints() throws Exception {
		RuntimeHints hints = new RuntimeHints();
		AotServices.factories().load(RuntimeHintsRegistrar.class)
				.forEach(registrar -> registrar.registerHints(hints,
						ClassUtils.getDefaultClassLoader()));
		assertThat(RuntimeHintsPredicates.reflection()
				.onConstructorInvocation(WebClientHttpServiceGroupAdapter.class.getConstructor())).accepts(hints);
	}

}
