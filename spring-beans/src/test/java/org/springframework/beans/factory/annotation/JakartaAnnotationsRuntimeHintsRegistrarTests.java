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

package org.springframework.beans.factory.annotation;


import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsPredicates;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.aot.AotFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JakartaAnnotationsRuntimeHintsRegistrar}.
 * @author Brian Clozel
 */
class JakartaAnnotationsRuntimeHintsRegistrarTests {

	private final RuntimeHints hints = new RuntimeHints();

	@BeforeEach
	void setup() {
		SpringFactoriesLoader.forResourceLocation(AotFactoriesLoader.FACTORIES_RESOURCE_LOCATION)
				.load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
						.registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void jakartaInjectAnnotationHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(Inject.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.hints);
	}

	@Test
	void jakartaQualifierAnnotationHasHints() {
		assertThat(RuntimeHintsPredicates.reflection().onType(Qualifier.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.hints);
	}

}
