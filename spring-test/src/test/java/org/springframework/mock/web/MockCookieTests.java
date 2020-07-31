/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.mock.web;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link MockCookie}.
 *
 * @author Vedran Pavic
 * @author Sam Brannen
 * @since 5.1
 */
class MockCookieTests {

	@Test
	void constructCookie() {
		MockCookie cookie = new MockCookie("SESSION", "123");

		assertCookie(cookie, "SESSION", "123");
		assertThat(cookie.getDomain()).isNull();
		assertThat(cookie.getMaxAge()).isEqualTo(-1);
		assertThat(cookie.getPath()).isNull();
		assertThat(cookie.isHttpOnly()).isFalse();
		assertThat(cookie.getSecure()).isFalse();
		assertThat(cookie.getSameSite()).isNull();
	}

	@Test
	void setSameSite() {
		MockCookie cookie = new MockCookie("SESSION", "123");
		cookie.setSameSite("Strict");

		assertThat(cookie.getSameSite()).isEqualTo("Strict");
	}

	@Test
	void parseHeaderWithoutAttributes() {
		MockCookie cookie = MockCookie.parse("SESSION=123");
		assertCookie(cookie, "SESSION", "123");

		cookie = MockCookie.parse("SESSION=123;");
		assertCookie(cookie, "SESSION", "123");
	}

	@Test
	void parseHeaderWithAttributes() {
		MockCookie cookie = MockCookie.parse("SESSION=123; Domain=example.com; Max-Age=60; " +
				"Expires=Tue, 8 Oct 2019 19:50:00 GMT; Path=/; Secure; HttpOnly; SameSite=Lax");

		assertCookie(cookie, "SESSION", "123");
		assertThat(cookie.getDomain()).isEqualTo("example.com");
		assertThat(cookie.getMaxAge()).isEqualTo(60);
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getExpires()).isEqualTo(ZonedDateTime.parse("Tue, 8 Oct 2019 19:50:00 GMT",
				DateTimeFormatter.RFC_1123_DATE_TIME));
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "bogus"})
	void parseHeaderWithInvalidExpiresAttribute(String expiresValue) {
		MockCookie cookie = MockCookie.parse("SESSION=123; Expires=" + expiresValue);

		assertCookie(cookie, "SESSION", "123");
		assertThat(cookie.getExpires()).isNull();
	}

	private void assertCookie(MockCookie cookie, String name, String value) {
		assertThat(cookie.getName()).isEqualTo(name);
		assertThat(cookie.getValue()).isEqualTo(value);
	}

	@Test
	void parseNullHeader() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> MockCookie.parse(null))
			.withMessageContaining("Set-Cookie header must not be null");
	}

	@Test
	void parseInvalidHeader() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> MockCookie.parse("BOOM"))
			.withMessageContaining("Invalid Set-Cookie header 'BOOM'");
	}

	@Test
	void parseInvalidAttribute() {
		String header = "SESSION=123; Path=";

		assertThatIllegalArgumentException()
			.isThrownBy(() -> MockCookie.parse(header))
			.withMessageContaining("No value in attribute 'Path' for Set-Cookie header '" + header + "'");
	}

	@Test
	void parseHeaderWithAttributesCaseSensitivity() {
		MockCookie cookie = MockCookie.parse("SESSION=123; domain=example.com; max-age=60; " +
				"expires=Tue, 8 Oct 2019 19:50:00 GMT; path=/; secure; httponly; samesite=Lax");

		assertCookie(cookie, "SESSION", "123");
		assertThat(cookie.getDomain()).isEqualTo("example.com");
		assertThat(cookie.getMaxAge()).isEqualTo(60);
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getExpires()).isEqualTo(ZonedDateTime.parse("Tue, 8 Oct 2019 19:50:00 GMT",
				DateTimeFormatter.RFC_1123_DATE_TIME));
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
	}

}
