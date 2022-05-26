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

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DefaultGenerationContext}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefaultGenerationContextTests {

	private final ClassNameGenerator classNameGenerator = new ClassNameGenerator();

	private final GeneratedFiles generatedFiles = new InMemoryGeneratedFiles();

	private final RuntimeHints runtimeHints = new RuntimeHints();


	@Test
	void createWithOnlyGeneratedFilesCreatesContext() {
		DefaultGenerationContext context = new DefaultGenerationContext(
				this.generatedFiles);
		assertThat(context.getClassNameGenerator())
				.isInstanceOf(ClassNameGenerator.class);
		assertThat(context.getGeneratedFiles()).isSameAs(this.generatedFiles);
		assertThat(context.getRuntimeHints()).isInstanceOf(RuntimeHints.class);
	}

	@Test
	void createCreatesContext() {
		DefaultGenerationContext context = new DefaultGenerationContext(
				this.classNameGenerator, this.generatedFiles, this.runtimeHints);
		assertThat(context.getClassNameGenerator()).isNotNull();
		assertThat(context.getGeneratedFiles()).isNotNull();
		assertThat(context.getRuntimeHints()).isNotNull();
	}

	@Test
	void createWhenClassNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultGenerationContext(null, this.generatedFiles,
						this.runtimeHints))
				.withMessage("'classNameGenerator' must not be null");
	}

	@Test
	void createWhenGeneratedFilesIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultGenerationContext(this.classNameGenerator,
						null, this.runtimeHints))
				.withMessage("'generatedFiles' must not be null");
	}

	@Test
	void createWhenRuntimeHintsIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultGenerationContext(this.classNameGenerator,
						this.generatedFiles, null))
				.withMessage("'runtimeHints' must not be null");
	}

	@Test
	void getClassNameGeneratorReturnsClassNameGenerator() {
		DefaultGenerationContext context = new DefaultGenerationContext(
				this.classNameGenerator, this.generatedFiles, this.runtimeHints);
		assertThat(context.getClassNameGenerator()).isSameAs(this.classNameGenerator);
	}

	@Test
	void getGeneratedFilesReturnsGeneratedFiles() {
		DefaultGenerationContext context = new DefaultGenerationContext(
				this.classNameGenerator, this.generatedFiles, this.runtimeHints);
		assertThat(context.getGeneratedFiles()).isSameAs(this.generatedFiles);
	}

	@Test
	void getRuntimeHintsReturnsRuntimeHints() {
		DefaultGenerationContext context = new DefaultGenerationContext(
				this.classNameGenerator, this.generatedFiles, this.runtimeHints);
		assertThat(context.getRuntimeHints()).isSameAs(this.runtimeHints);
	}

}
