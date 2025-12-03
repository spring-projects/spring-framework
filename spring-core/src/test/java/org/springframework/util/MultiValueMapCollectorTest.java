/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultiValueMapCollector}.
 * 
 * @author Florian Hof
 */
class MultiValueMapCollectorTest {

	@Test
	void indexingBy() {
		MultiValueMapCollector<String, Integer, String> collector = MultiValueMapCollector.indexingBy(String::length);
		MultiValueMap<Integer, String> content = Stream.of("abc", "ABC", "123", "1234", "abcdef", "ABCDEF").collect(collector);
		assertThat(content.get(3)).containsOnly("abc", "ABC", "123");
		assertThat(content.get(4)).containsOnly("abcdef", "ABCDEF");
		assertThat(content.get(6)).containsOnly("1234");
		assertThat(content.get(1)).isNull();
	}
}
