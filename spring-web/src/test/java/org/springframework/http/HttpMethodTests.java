/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpMethod}.
 *
 * @author Hyunjin Choi
 */
public class HttpMethodTests {

	@Test
	void resolveWithCorrectInput() {
		assertThat(HttpMethod.resolve("GET")).isEqualTo(HttpMethod.GET);
		assertThat(HttpMethod.resolve("HEAD")).isEqualTo(HttpMethod.HEAD);
		assertThat(HttpMethod.resolve("POST")).isEqualTo(HttpMethod.POST);
		assertThat(HttpMethod.resolve("PUT")).isEqualTo(HttpMethod.PUT);
		assertThat(HttpMethod.resolve("PATCH")).isEqualTo(HttpMethod.PATCH);
		assertThat(HttpMethod.resolve("DELETE")).isEqualTo(HttpMethod.DELETE);
		assertThat(HttpMethod.resolve("OPTIONS")).isEqualTo(HttpMethod.OPTIONS);
		assertThat(HttpMethod.resolve("TRACE")).isEqualTo(HttpMethod.TRACE);
	}

	@ParameterizedTest
	@ValueSource(strings = {"foo", "bar", "baz", "qux"})
	void resolveWithWrongInput(String input) {
		assertThat(HttpMethod.resolve(input)).isNull();
	}

	@Test
	void resolveWithNullInput() {
		assertThat(HttpMethod.resolve(null)).isNull();
	}

	@Test
	void matchesWithCorrectInput() {
		assertThat(HttpMethod.GET.matches("GET")).isTrue();
		assertThat(HttpMethod.HEAD.matches("HEAD")).isTrue();
		assertThat(HttpMethod.POST.matches("POST")).isTrue();
		assertThat(HttpMethod.PUT.matches("PUT")).isTrue();
		assertThat(HttpMethod.PATCH.matches("PATCH")).isTrue();
		assertThat(HttpMethod.DELETE.matches("DELETE")).isTrue();
		assertThat(HttpMethod.OPTIONS.matches("OPTIONS")).isTrue();
		assertThat(HttpMethod.TRACE.matches("TRACE")).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"foo", "bar", "baz", "qux"})
	void matchesWithWrongInput(String input) {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			assertThat(httpMethod.matches(input)).isFalse();
		}
	}

}
