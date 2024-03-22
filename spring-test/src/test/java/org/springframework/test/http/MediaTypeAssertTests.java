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

package org.springframework.test.http;


import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MediaTypeAssert}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
class MediaTypeAssertTests {

	@Test
	void actualCanBeNull() {
		new MediaTypeAssert((MediaType) null).isNull();
	}

	@Test
	void actualStringCanBeNull() {
		new MediaTypeAssert((String) null).isNull();
	}

	@Test
	void isEqualWhenSameShouldPass() {
		assertThat(mediaType("application/json")).isEqualTo("application/json");
	}

	@Test
	void isEqualWhenDifferentShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mediaType("application/json")).isEqualTo("text/html"))
				.withMessageContaining("Media type");
	}

	@Test
	void isEqualWhenActualIsNullShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(null).isEqualTo(MediaType.APPLICATION_JSON))
				.withMessageContaining("Media type");
	}

	@Test
	void isEqualWhenSameTypeShouldPass() {
		assertThat(mediaType("application/json")).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void isEqualWhenDifferentTypeShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mediaType("application/json")).isEqualTo(MediaType.TEXT_HTML))
				.withMessageContaining("Media type");
	}

	@Test
	void isCompatibleWhenSameShouldPass() {
		assertThat(mediaType("application/json")).isCompatibleWith("application/json");
	}

	@Test
	void isCompatibleWhenCompatibleShouldPass() {
		assertThat(mediaType("application/json")).isCompatibleWith("application/*");
	}

	@Test
	void isCompatibleWhenDifferentShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mediaType("application/json")).isCompatibleWith("text/html"))
				.withMessageContaining("check media type 'application/json' is compatible with 'text/html'");
	}

	@Test
	void isCompatibleWithStringAndNullActual() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(null).isCompatibleWith("text/html"))
				.withMessageContaining("Expecting null to be compatible with 'text/html'");
	}

	@Test
	void isCompatibleWithStringAndNullExpected() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mediaType("application/json")).isCompatibleWith((String) null))
				.withMessageContainingAll("Expecting:", "null", "To be a valid media type but got:",
						"'mimeType' must not be empty");
	}

	@Test
	void isCompatibleWithStringAndEmptyExpected() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mediaType("application/json")).isCompatibleWith(""))
				.withMessageContainingAll("Expecting:", "", "To be a valid media type but got:",
						"'mimeType' must not be empty");
	}

	@Test
	void isCompatibleWithMediaTypeAndNullActual() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(null).isCompatibleWith(MediaType.TEXT_HTML))
				.withMessageContaining("Expecting null to be compatible with 'text/html'");
	}

	@Test
	void isCompatibleWithMediaTypeAndNullExpected() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mediaType("application/json")).isCompatibleWith((MediaType) null))
				.withMessageContaining("Expecting 'application/json' to be compatible with null");
	}

	@Test
	void isCompatibleWhenSameTypeShouldPass() {
		assertThat(mediaType("application/json")).isCompatibleWith(MediaType.APPLICATION_JSON);
	}

	@Test
	void isCompatibleWhenCompatibleTypeShouldPass() {
		assertThat(mediaType("application/json")).isCompatibleWith(MediaType.parseMediaType("application/*"));
	}

	@Test
	void isCompatibleWhenDifferentTypeShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mediaType("application/json")).isCompatibleWith(MediaType.TEXT_HTML))
				.withMessageContaining("check media type 'application/json' is compatible with 'text/html'");
	}


	@Nullable
	private static MediaType mediaType(@Nullable String mediaType) {
		return (mediaType != null ? MediaType.parseMediaType(mediaType) : null);
	}

	private static MediaTypeAssert assertThat(@Nullable MediaType mediaType) {
		return new MediaTypeAssert(mediaType);
	}

}
