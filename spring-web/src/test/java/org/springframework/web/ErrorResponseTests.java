/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ErrorResponse}.
 *
 * @author Stephane Nicoll
 */
class ErrorResponseTests {

	@Test
	void createWithHttpHeader() {
		ErrorResponse response = ErrorResponse
				.builder(new IllegalStateException(), HttpStatus.BAD_REQUEST, "test")
				.header("header", "value")
				.build();
		assertThat(response.getHeaders().containsHeaderValue("header", "value")).isTrue();
	}

	@Test
	void createWithHttpHeadersConsumer() {
		ErrorResponse response = ErrorResponse.builder(new IllegalStateException(), HttpStatus.BAD_REQUEST, "test")
				.header("header", "value")
				.headers(headers -> {
					headers.add("header", "value2");
					headers.add("another", "value3");
				}).build();
		assertThat(response.getHeaders().get("header")).containsExactly("value", "value2");
		assertThat(response.getHeaders().get("another")).containsExactly("value3");
	}

}
