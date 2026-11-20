/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.io.support.DummyFactory;
import org.springframework.core.io.support.MyDummyFactory1;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringFactoriesLoaderRuntimeHints}.
 *
 * @author Phillip Webb
 */
class SpringFactoriesLoaderRuntimeHintsTests {

	private RuntimeHints hints;

	@BeforeEach
	void setup() {
		this.hints = new RuntimeHints();
		SpringFactoriesLoader.forResourceLocation("META-INF/spring/aot.factories")
				.load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
						.registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void resourceLocationHasHints() {
		assertThat(RuntimeHintsPredicates.resource().forResource(SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION)).accepts(this.hints);
	}

	@Test
	void factoryTypeHasHint() {
		assertThat(RuntimeHintsPredicates.reflection().onType(DummyFactory.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
	}

	@Test
	void factoryImplementationHasHint() {
		assertThat(RuntimeHintsPredicates.reflection().onType(MyDummyFactory1.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
	}

}
