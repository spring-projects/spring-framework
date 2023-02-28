/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.bind.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RequestMethodTests {

	@Test
	void resolveString() {
		String[] methods = new String[]{"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE"};
		for (String httpMethod : methods) {
			RequestMethod requestMethod = RequestMethod.resolve(httpMethod);
			assertThat(requestMethod).isNotNull();
			assertThat(requestMethod.name()).isEqualTo(httpMethod);
		}
		assertThat(RequestMethod.resolve("PROPFIND")).isNull();
	}

	@Test
	void resolveHttpMethod() {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			RequestMethod requestMethod = RequestMethod.resolve(httpMethod);
			assertThat(requestMethod).isNotNull();
			assertThat(requestMethod.name()).isEqualTo(httpMethod.name());
		}
		assertThat(RequestMethod.resolve(HttpMethod.valueOf("PROPFIND"))).isNull();
	}

	@Test
	void asHttpMethod() {
		for (RequestMethod requestMethod : RequestMethod.values()) {
			HttpMethod httpMethod = requestMethod.asHttpMethod();
			assertThat(httpMethod).isNotNull();
			assertThat(httpMethod.name()).isEqualTo(requestMethod.name());
		}
	}
}
