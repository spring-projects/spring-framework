/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aot.hint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ResourceHintsTests.Nested.Inner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DescriptiveResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ResourceHints}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class ResourceHintsTests {

	private final ResourceHints resourceHints = new ResourceHints();

	@Test
	void registerType() {
		this.resourceHints.registerType(String.class);
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("java/lang/String.class"));
	}

	@Test
	void registerTypeWithNestedType() {
		this.resourceHints.registerType(TypeReference.of(Nested.class));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("org/springframework/aot/hint/ResourceHintsTests$Nested.class"));
	}

	@Test
	void registerTypeWithInnerNestedType() {
		this.resourceHints.registerType(TypeReference.of(Inner.class));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("org/springframework/aot/hint/ResourceHintsTests$Nested$Inner.class"));
	}

	@Test
	void registerTypeSeveralTimesAddsOnlyOneEntry() {
		this.resourceHints.registerType(String.class);
		this.resourceHints.registerType(TypeReference.of(String.class));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("java/lang/String.class"));
	}

	@Test
	void registerExactMatch() {
		this.resourceHints.registerPattern("com/example/test.properties");
		this.resourceHints.registerPattern("com/example/another.properties");
		assertThat(this.resourceHints.resourcePatternHints())
				.anySatisfy(patternOf("com/example/test.properties"))
				.anySatisfy(patternOf("com/example/another.properties"))
				.hasSize(2);
	}

	@Test
	void registerPattern() {
		this.resourceHints.registerPattern("com/example/*.properties");
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("com/example/*.properties"));
	}

	@Test
	void registerPatternWithIncludesAndExcludes() {
		this.resourceHints.registerPattern(resourceHint ->
				resourceHint.includes("com/example/*.properties").excludes("com/example/to-ignore.properties"));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(patternOf(
				List.of("com/example/*.properties"),
				List.of("com/example/to-ignore.properties")));
	}

	@Test
	void registerIfPresentRegisterExistingLocation() {
		this.resourceHints.registerPatternIfPresent(null, "META-INF/",
				resourceHint -> resourceHint.includes("com/example/*.properties"));
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(
				patternOf("com/example/*.properties"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerIfPresentIgnoreMissingLocation() {
		Consumer<ResourcePatternHints.Builder> hintBuilder = mock(Consumer.class);
		this.resourceHints.registerPatternIfPresent(null, "location/does-not-exist/", hintBuilder);
		assertThat(this.resourceHints.resourcePatternHints()).isEmpty();
		verifyNoInteractions(hintBuilder);
	}

	@Test
	void registerResourceWithUnsupportedResourceType() {
		DescriptiveResource resource = new DescriptiveResource("bogus");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.resourceHints.registerResource(resource))
			.withMessage("Resource must be a ClassPathResource that exists: %s", resource);
	}

	@Test
	void registerResourceWithNonexistentClassPathResource() {
		ClassPathResource resource = new ClassPathResource("bogus", getClass());
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.resourceHints.registerResource(resource))
			.withMessage("Resource must be a ClassPathResource that exists: %s", resource);
	}

	@Test
	void registerResourceWithExistingClassPathResource() {
		String path = "org/springframework/aot/hint/support";
		ClassPathResource resource = new ClassPathResource(path);
		this.resourceHints.registerResource(resource);
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(patternOf(path));
	}

	@Test
	void registerResourceWithExistingRelativeClassPathResource() {
		String path = "org/springframework/aot/hint/support";
		ClassPathResource resource = new ClassPathResource("support", RuntimeHints.class);
		this.resourceHints.registerResource(resource);
		assertThat(this.resourceHints.resourcePatternHints()).singleElement().satisfies(patternOf(path));
	}

	@Test
	void registerResourceBundle() {
		this.resourceHints.registerResourceBundle("com.example.message");
		assertThat(this.resourceHints.resourceBundleHints()).singleElement()
				.satisfies(resourceBundle("com.example.message"));
	}

	@Test
	void registerResourceBundleSeveralTimesAddsOneEntry() {
		this.resourceHints.registerResourceBundle("com.example.message")
				.registerResourceBundle("com.example.message");
		assertThat(this.resourceHints.resourceBundleHints()).singleElement()
				.satisfies(resourceBundle("com.example.message"));
	}


	private Consumer<ResourcePatternHints> patternOf(String... includes) {
		return patternOf(Arrays.asList(includes), Collections.emptyList());
	}

	private Consumer<ResourceBundleHint> resourceBundle(String baseName) {
		return resourceBundleHint -> assertThat(resourceBundleHint.getBaseName()).isEqualTo(baseName);
	}

	private Consumer<ResourcePatternHints> patternOf(List<String> includes, List<String> excludes) {
		return pattern -> {
			assertThat(pattern.getIncludes()).map(ResourcePatternHint::getPattern).containsExactlyElementsOf(includes);
			assertThat(pattern.getExcludes()).map(ResourcePatternHint::getPattern).containsExactlyElementsOf(excludes);
		};
	}

	static class Nested {

		static class Inner {

		}
	}

}
