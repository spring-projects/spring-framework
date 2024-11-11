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

package org.springframework.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RestClientResponseExceptionTests {

	@Test // gh-31978
	void caseInsensitiveHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "bar");
		RestClientResponseException ex = new RestClientResponseException("message", 200, "statusText", headers, null,
				null);

		HttpHeaders responseHeaders = ex.getResponseHeaders();
		assertThat(responseHeaders).isNotNull();
		assertThat(responseHeaders).hasSize(1);
		assertThat(responseHeaders.getFirst("FOO")).isEqualTo("bar");
	}

}
