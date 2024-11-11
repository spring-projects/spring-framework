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

package org.springframework.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class HttpMethodTests {

	@Test
	void comparison() {
		HttpMethod method1 = HttpMethod.valueOf("FOO");
		HttpMethod method2 = HttpMethod.valueOf("FOO");
		HttpMethod method3 = HttpMethod.valueOf("BAR");

		assertThat(method1).isEqualTo(method2);
		assertThat(method1).isNotEqualTo(method3);

		assertThat(method1.hashCode()).isEqualTo(method2.hashCode());

		assertThat(method1.compareTo(method2)).isEqualTo(0);
		assertThat(method1.compareTo(method3)).isNotEqualTo(0);
	}

	@Test
	void values() {
		HttpMethod[] values = HttpMethod.values();
		assertThat(values).containsExactly(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT,
				HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.TRACE);

		// check defensive copy
		values[0] = HttpMethod.POST;
		assertThat(HttpMethod.values()).containsExactly(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT,
				HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.TRACE);
	}

	@Test
	void valueOf() {
		HttpMethod get = HttpMethod.valueOf("GET");
		assertThat(get).isSameAs(HttpMethod.GET);

		HttpMethod foo = HttpMethod.valueOf("FOO");
		HttpMethod other = HttpMethod.valueOf("FOO");
		assertThat(foo).isEqualTo(other);
	}

	@Test
	void name() {
		HttpMethod method = HttpMethod.valueOf("FOO");
		assertThat(method.name()).isEqualTo("FOO");
	}

	@Test
	void matches() {
		assertThat(HttpMethod.GET.matches("GET")).isTrue();
		assertThat(HttpMethod.GET.matches("FOO")).isFalse();
	}
}
