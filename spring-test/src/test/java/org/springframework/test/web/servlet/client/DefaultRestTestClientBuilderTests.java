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

package org.springframework.test.web.servlet.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RestTestClient.Builder}.
 */
class DefaultRestTestClientBuilderTests {

	@Test
	void mutateOriginalBuilderHasNoSideEffects() {
		RestTestClient.Builder<?> builder = new DefaultRestTestClientBuilder<>().baseUrl("http://localhost");
		RestTestClient client = builder.build();
		builder.defaultHeader("foo", "bar");
		client.mutate().defaultHeaders(headers -> assertThat(headers.containsHeader("foo")).isFalse());
	}

	@Test
	void mutateSameClientTwiceHasNoSideEffects() {
		RestTestClient client = new DefaultRestTestClientBuilder<>().baseUrl("http://localhost").build();
		client.mutate().defaultHeader("foo", "bar").build();
		client.mutate().defaultHeaders(headers -> assertThat(headers.containsHeader("foo")).isFalse());
	}

}
