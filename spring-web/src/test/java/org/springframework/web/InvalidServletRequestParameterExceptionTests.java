/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link InvalidServletRequestParameterException}.
 *
 * @author Lordwill Kandiro
 */
class InvalidServletRequestParameterExceptionTests {

	@Test
	void parsingFailureWithoutAStatusIsBadRequest() {
		IllegalStateException cause = new IllegalStateException("Invalid character found in the request target");

		InvalidServletRequestParameterException ex = new InvalidServletRequestParameterException(cause);

		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(ex.getCause()).isSameAs(cause);
		assertThat(ex.getBody().getDetail()).isEqualTo("Failed to parse the request parameters");
	}

	@Test
	void containerErrorCodeIsKept() {
		IllegalStateException cause = new IllegalStateException("The request body was too large") {
			@SuppressWarnings("unused")
			public int getErrorCode() {
				return 413;
			}
		};

		assertThat(new InvalidServletRequestParameterException(cause).getStatusCode().value()).isEqualTo(413);
	}

	@Test
	void containerCodeIsKept() {
		IllegalStateException cause = new IllegalStateException("Request URI too long") {
			@SuppressWarnings("unused")
			public int getCode() {
				return 414;
			}
		};

		assertThat(new InvalidServletRequestParameterException(cause).getStatusCode().value()).isEqualTo(414);
	}

	@Test
	void readTranslatesTheFailureAndReturnsTheValue() {
		assertThat(InvalidServletRequestParameterException.read(() -> "ok")).isEqualTo("ok");
		assertThatExceptionOfType(InvalidServletRequestParameterException.class)
				.isThrownBy(() -> InvalidServletRequestParameterException.read(() -> {
					throw new IllegalStateException("bad encoding");
				}));
	}

}
