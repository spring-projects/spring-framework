/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		FilePatternResourceHintsRegistrar.forClassPathLocations("").withClassPathLocations("META-INF")
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
				.withClassPathLocations("another-does-not-exist/")
				.withFilePrefixes("test").withFileExtensions(".txt")
				.registerHints(this.hints, null);
		assertThat(this.hints.resourcePatternHints()).isEmpty();
	}

	private Consumer<ResourcePatternHints> includes(String... patterns) {
		return hint -> assertThat(hint.getIncludes().stream().map(ResourcePatternHint::getPattern))
				.containsExactlyInAnyOrder(patterns);
	}

}
