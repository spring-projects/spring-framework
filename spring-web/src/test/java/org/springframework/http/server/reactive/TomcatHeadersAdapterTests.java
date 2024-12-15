/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.server.reactive;

import org.apache.tomcat.util.http.MimeHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatHeadersAdapter}.
 *
 * @author Johnny Lim
 * @author Sam Brannen
 * @since 7.0
 */
class TomcatHeadersAdapterTests {

	private final TomcatHeadersAdapter adapter = new TomcatHeadersAdapter(new MimeHeaders());


	@Test
	void clear() {
		adapter.add("key1", "value1");
		adapter.add("key2", "value2");
		assertThat(adapter).isNotEmpty();
		assertThat(adapter).hasSize(2);
		assertThat(adapter).containsKeys("key1", "key2");

		adapter.clear();
		assertThat(adapter).isEmpty();
		assertThat(adapter).hasSize(0);
	}

}
