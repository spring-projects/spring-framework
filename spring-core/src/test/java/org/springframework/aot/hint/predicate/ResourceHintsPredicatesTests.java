/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.predicate;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReflectionHintsPredicates}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class ResourceHintsPredicatesTests {

	private final ResourceHintsPredicates resources = new ResourceHintsPredicates();

	private final RuntimeHints runtimeHints = new RuntimeHints();


	@Test
	void resourcePatternMatchesResourceName() {
		this.runtimeHints.resources().registerPattern("test/*");
		assertPredicateMatches(resources.forResource("/test/spring.properties"));
	}

	@Test
	void resourcePatternDoesNotMatchResourceName() {
		this.runtimeHints.resources().registerPattern("test/spring.*");
		assertPredicateDoesNotMatch(resources.forResource("/test/other.properties"));
	}

	@Test
	void resourcePatternMatchesTypeAndResourceName() {
		this.runtimeHints.resources().registerPattern("org/springframework/aot/hint/predicate/spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of(getClass()), "spring.properties"));
	}

	@Test
	void resourcePatternMatchesTypeAndAbsoluteResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of(getClass()), "/spring.properties"));
	}

	@Test
	void resourcePatternMatchesTypeInDefaultPackageAndResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of("DummyClass"), "spring.properties"));
	}

	@Test
	void resourcePatternMatchesTypeInDefaultPackageAndAbsoluteResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateMatches(resources.forResource(TypeReference.of("DummyClass"), "/spring.properties"));
	}

	@Test
	void resourcePatternDoesNotMatchTypeAndResourceName() {
		this.runtimeHints.resources().registerPattern("spring.*");
		assertPredicateDoesNotMatch(resources.forResource(TypeReference.of(getClass()), "spring.properties"));
	}

	@Test
	void resourceBundleMatchesBundleName() {
		this.runtimeHints.resources().registerResourceBundle("spring");
		assertPredicateMatches(resources.forBundle("spring"));
	}

	@Test
	void resourceBundleDoesNotMatchBundleName() {
		this.runtimeHints.resources().registerResourceBundle("spring");
		assertPredicateDoesNotMatch(resources.forBundle("other"));
	}


	private void assertPredicateMatches(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).accepts(this.runtimeHints);
	}

	private void assertPredicateDoesNotMatch(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).rejects(this.runtimeHints);
	}

}
