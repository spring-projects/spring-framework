/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.aot.hint.support.FilePatternResourceHintsRegistrar.Builder;

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
	void configureWithNoClasspathLocation() {
		assertThatIllegalArgumentException().isThrownBy(FilePatternResourceHintsRegistrar::forClassPathLocations)
				.withMessageContaining("At least one classpath location must be specified");
	}

	@Test
	void configureWithInvalidFilePrefix() {
		Builder builder = FilePatternResourceHintsRegistrar.forClassPathLocations("");
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withFilePrefixes("test*"))
				.withMessageContaining("cannot contain '*'");
	}

	@Test
	void configureWithInvalidFileExtension() {
		Builder builder = FilePatternResourceHintsRegistrar.forClassPathLocations("");
		assertThatIllegalArgumentException().isThrownBy(() -> builder.withFileExtensions("txt"))
				.withMessageContaining("must start with '.'");
	}

	@Test
	void registerWithSinglePattern() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt"));
	}

	@Test
	void registerWithMultipleFilePrefixes() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("")
				.withFilePrefixes("test").withFilePrefixes("another")
				.withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt", "another*.txt"));
	}

	@Test
	void registerWithMultipleClasspathLocations() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("").withClasspathLocations("META-INF")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithMultipleFileExtensions() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("")
				.withFilePrefixes("test").withFileExtensions(".txt").withFileExtensions(".conf")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt", "test*.conf"));
	}

	@Test
	void registerWithClasspathLocationWithoutTrailingSlash() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("META-INF")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithClasspathLocationWithLeadingSlash() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("/")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "test*.txt"));
	}

	@Test
	void registerWithClasspathLocationUsingResourceClasspathPrefix() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("classpath:META-INF")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithClasspathLocationUsingResourceClasspathPrefixAndTrailingSlash() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("classpath:/META-INF")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).singleElement()
				.satisfies(includes("/", "META-INF", "META-INF/test*.txt"));
	}

	@Test
	void registerWithNonExistingLocationDoesNotRegisterHint() {
		FilePatternResourceHintsRegistrar.forClassPathLocations("does-not-exist/")
				.withClasspathLocations("another-does-not-exist/")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
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
