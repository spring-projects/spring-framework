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

package org.springframework.aot.hint.support;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FilePatternResourceHintsRegistrar}.
 *
 * @author Stephane Nicoll
 */
class FilePatternResourceHintsRegistrarTests {

	private final ResourceHints hints = new ResourceHints();


	@Test
	void createWithInvalidName() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FilePatternResourceHintsRegistrar(
						List.of("test*"), List.of(""), List.of(".txt")))
				.withMessageContaining("cannot contain '*'");
	}

	@Test
	void createWithInvalidExtension() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FilePatternResourceHintsRegistrar(
						List.of("test"), List.of(""), List.of("txt")))
				.withMessageContaining("should start with '.'");
	}

	@Test
	void registerWithSinglePattern() {
		new FilePatternResourceHintsRegistrar(List.of("test"), List.of(""), List.of(".txt"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt"));
	}

	@Test
	void registerWithMultipleNames() {
		new FilePatternResourceHintsRegistrar(List.of("test", "another"), List.of(""), List.of(".txt"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/" , "test*.txt", "another*.txt"));
	}

	@Test
	void registerWithMultipleLocations() {
		new FilePatternResourceHintsRegistrar(List.of("test"), List.of("", "META-INF"), List.of(".txt"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithMultipleExtensions() {
		new FilePatternResourceHintsRegistrar(List.of("test"), List.of(""), List.of(".txt", ".conf"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt", "test*.conf"));
	}

	@Test
	void registerWithLocationWithoutTrailingSlash() {
		new FilePatternResourceHintsRegistrar(List.of("test"), List.of("META-INF"), List.of(".txt"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithLocationWithLeadingSlash() {
		new FilePatternResourceHintsRegistrar(List.of("test"), List.of("/"), List.of(".txt"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt"));
	}

	@Test
	void registerWithLocationUsingResourceClasspathPrefix() {
		new FilePatternResourceHintsRegistrar(List.of("test"), List.of("classpath:META-INF"), List.of(".txt"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithLocationUsingResourceClasspathPrefixAndTrailingSlash() {
		new FilePatternResourceHintsRegistrar(List.of("test"), List.of("classpath:/META-INF"), List.of(".txt"))
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithNonExistingLocationDoesNotRegisterHint() {
		new FilePatternResourceHintsRegistrar(List.of("test"),
				List.of("does-not-exist/", "another-does-not-exist/"),
				List.of(".txt")).registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).isEmpty();
	}


	private Consumer<ResourcePatternHints> includes(String... patterns) {
		return hint -> {
			assertThat(hint.getIncludes().stream().map(ResourcePatternHint::getPattern))
					.containsExactlyInAnyOrder(patterns);
			assertThat(hint.getExcludes()).isEmpty();
		};
	}

}
