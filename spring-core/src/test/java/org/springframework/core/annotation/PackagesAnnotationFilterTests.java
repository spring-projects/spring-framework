/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PackagesAnnotationFilter}.
 *
 * @author Phillip Webb
 */
class PackagesAnnotationFilterTests {

	@Test
	void createWhenPackagesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PackagesAnnotationFilter((String[]) null))
			.withMessage("Packages array must not be null");
	}

	@Test
	void createWhenPackagesContainsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PackagesAnnotationFilter((String) null))
			.withMessage("Packages array must not have empty elements");
	}

	@Test
	void createWhenPackagesContainsEmptyTextThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PackagesAnnotationFilter(""))
			.withMessage("Packages array must not have empty elements");
	}

	@Test
	void matchesWhenInPackageReturnsTrue() {
		PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
		assertThat(filter.matches("com.example.Component")).isTrue();
	}

	@Test
	void matchesWhenNotInPackageReturnsFalse() {
		PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
		assertThat(filter.matches("org.springframework.stereotype.Component")).isFalse();
	}

	@Test
	void matchesWhenInSimilarPackageReturnsFalse() {
		PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
		assertThat(filter.matches("com.examples.Component")).isFalse();
	}

	@Test
	void equalsAndHashCode() {
		PackagesAnnotationFilter filter1 = new PackagesAnnotationFilter("com.example",
				"org.springframework");
		PackagesAnnotationFilter filter2 = new PackagesAnnotationFilter(
				"org.springframework", "com.example");
		PackagesAnnotationFilter filter3 = new PackagesAnnotationFilter("com.examples");
		assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
		assertThat(filter1).isEqualTo(filter1).isEqualTo(filter2).isNotEqualTo(filter3);
	}

}
