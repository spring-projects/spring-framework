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

package org.springframework.web;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ErrorResponse}.
 *
 * @author Stephane Nicoll
 */
class ErrorResponseTests {

	@Test
	void createWithHttpHeader() {
		ErrorResponse response = ErrorResponse.builder(new IllegalStateException(), HttpStatus.BAD_REQUEST, "test")
				.header("header", "value").build();
		assertThat(response.getHeaders()).containsOnly(entry("header", List.of("value")));
	}

	@Test
	void createWithHttpHeadersConsumer() {
		ErrorResponse response = ErrorResponse.builder(new IllegalStateException(), HttpStatus.BAD_REQUEST, "test")
				.header("header", "value")
				.headers(headers -> {
					headers.add("header", "value2");
					headers.add("another", "value3");
				}).build();
		assertThat(response.getHeaders()).containsOnly(entry("header", List.of("value", "value2")),
				entry("another", List.of("value3")));
	}

}
