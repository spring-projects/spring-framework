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

package org.springframework.aot.generate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedMethods}.
 *
 * @author Phillip Webb
 */
class GeneratedMethodsTests {

	private final GeneratedMethods methods = new GeneratedMethods();

	@Test
	void createWhenMethodNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedMethods(null))
				.withMessage("'methodNameGenerator' must not be null");
	}

	@Test
	void createWithExistingGeneratorUsesGenerator() {
		MethodNameGenerator generator = new MethodNameGenerator();
		generator.generateMethodName("test");
		GeneratedMethods methods = new GeneratedMethods(generator);
		assertThat(methods.add("test").getName()).hasToString("test1");
	}

	@Test
	void addAddsMethod() {
		this.methods.add("spring", "beans").using(this::build);
		this.methods.add("spring", "context").using(this::build);
		assertThat(
				this.methods.stream().map(GeneratedMethod::getName).map(Object::toString))
						.containsExactly("springBeans", "springContext");
	}

	@Test
	void doWithMethodSpecsAcceptsMethodSpecs() {
		this.methods.add("spring", "beans").using(this::build);
		this.methods.add("spring", "context").using(this::build);
		List<String> names = new ArrayList<>();
		this.methods.doWithMethodSpecs(spec -> names.add(spec.name));
		assertThat(names).containsExactly("springBeans", "springContext");
	}

	@Test
	void doWithMethodSpecsWhenMethodHasNotHadSpecDefinedThrowsException() {
		this.methods.add("spring");
		assertThatIllegalStateException()
				.isThrownBy(() -> this.methods.doWithMethodSpecs(spec -> {
				})).withMessage("Method 'spring' has no method spec defined");
	}

	@Test
	void iteratorIteratesMethods() {
		this.methods.add("spring", "beans").using(this::build);
		this.methods.add("spring", "context").using(this::build);
		Iterator<GeneratedMethod> iterator = this.methods.iterator();
		assertThat(iterator.next().getName()).hasToString("springBeans");
		assertThat(iterator.next().getName()).hasToString("springContext");
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void streamStreamsMethods() {
		this.methods.add("spring", "beans").using(this::build);
		this.methods.add("spring", "context").using(this::build);
		assertThat(this.methods.stream()).hasSize(2);
	}

	private void build(MethodSpec.Builder builder) {
	}

}
