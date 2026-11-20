/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class InstanceFilterTests {

	@Test
	void emptyFilterApplyMatchIfEmpty() {
		InstanceFilter<String> filter = new InstanceFilter<>(null, null);
		match(filter, "foo");
		match(filter, "bar");
	}

	@Test
	void includesFilter() {
		InstanceFilter<String> filter = new InstanceFilter<>(List.of("First", "Second"), null);
		match(filter, "Second");
		doNotMatch(filter, "foo");
	}

	@Test
	void excludesFilter() {
		InstanceFilter<String> filter = new InstanceFilter<>(null, List.of("First", "Second"));
		doNotMatch(filter, "Second");
		match(filter, "foo");
	}

	@Test
	void includesAndExcludesFilters() {
		InstanceFilter<String> filter = new InstanceFilter<>(List.of("foo", "Bar"), List.of("First", "Second"));
		doNotMatch(filter, "Second");
		match(filter, "foo");
	}

	@Test
	void includesAndExcludesFiltersConflict() {
		InstanceFilter<String> filter = new InstanceFilter<>(List.of("First"), List.of("First"));
		doNotMatch(filter, "First");
	}


	private static <T> void match(InstanceFilter<T> filter, T candidate) {
		assertThat(filter.match(candidate)).as("filter '" + filter + "' should match " + candidate).isTrue();
	}

	private static <T> void doNotMatch(InstanceFilter<T> filter, T candidate) {
		assertThat(filter.match(candidate)).as("filter '" + filter + "' should not match " + candidate).isFalse();
	}

}
