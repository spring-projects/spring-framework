/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultiValueMapCollector}.
 *
 * @author Florian Hof
 * @author Sam Brannen
 * @since 7.0.2
 */
class MultiValueMapCollectorTests {

	@Test
	void ofFactoryMethod() {
		Function<Integer, String> keyFunction = i -> (i % 2 == 0 ? "even" :"odd");
		Function<Integer, Integer> valueFunction = i -> -i;

		var collector = MultiValueMapCollector.of(keyFunction, valueFunction);
		var multiValueMap = Stream.of(1, 2, 3, 4, 5).collect(collector);

		assertThat(multiValueMap).containsOnlyKeys("even", "odd");
		assertThat(multiValueMap.get("odd")).containsOnly(-1, -3, -5);
		assertThat(multiValueMap.get("even")).containsOnly(-2, -4);
	}

	@Test
	void indexingByFactoryMethod() {
		var collector = MultiValueMapCollector.indexingBy(String::length);
		var multiValueMap = Stream.of("abc", "ABC", "123", "1234", "cat", "abcdef", "ABCDEF").collect(collector);

		assertThat(multiValueMap).containsOnlyKeys(3, 4, 6);
		assertThat(multiValueMap.get(3)).containsOnly("abc", "ABC", "123", "cat");
		assertThat(multiValueMap.get(4)).containsOnly("1234");
		assertThat(multiValueMap.get(6)).containsOnly("abcdef", "ABCDEF");
	}

}
