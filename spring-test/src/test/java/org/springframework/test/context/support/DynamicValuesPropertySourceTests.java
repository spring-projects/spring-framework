/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicValuesPropertySource}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class DynamicValuesPropertySourceTests {

	private final DynamicValuesPropertySource source = new DynamicValuesPropertySource(
			Map.of("a", () -> "A", "b", () -> "B"));


	@Test
	void getPropertyReturnsSuppliedProperty() {
		assertThat(this.source.getProperty("a")).isEqualTo("A");
		assertThat(this.source.getProperty("b")).isEqualTo("B");
	}

	@Test
	void getPropertyWhenMissingReturnsNull() {
		assertThat(this.source.getProperty("c")).isNull();
	}

	@Test
	void containsPropertyWhenPresentReturnsTrue() {
		assertThat(this.source.containsProperty("a")).isTrue();
		assertThat(this.source.containsProperty("b")).isTrue();
	}

	@Test
	void containsPropertyWhenMissingReturnsFalse() {
		assertThat(this.source.containsProperty("c")).isFalse();
	}

	@Test
	void getPropertyNamesReturnsNames() {
		assertThat(this.source.getPropertyNames()).containsExactlyInAnyOrder("a", "b");
	}

}
