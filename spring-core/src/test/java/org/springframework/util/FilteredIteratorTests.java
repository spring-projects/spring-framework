/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Yanming Zhou
 */
final class FilteredIteratorTests {

	@Test
	void filter() {
		List<String> list = List.of("foo", "bar", "baz");
		FilteredIterator<String> filtered = new FilteredIterator<>(list.iterator(), s -> !s.equals("bar"));

		assertThat(filtered).toIterable().containsExactly("foo", "baz");
	}

	@Test
	void nullable() {
		List<@Nullable String> list = Arrays.asList("foo", "bar", null);
		FilteredIterator<@Nullable String> filtered = new FilteredIterator<>(list.iterator(), s -> !"bar".equals(s));

		assertThat(filtered).toIterable().containsExactly("foo", null);
	}

}
