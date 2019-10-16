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

package org.springframework.http;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ResponseCookie}.
 * @author Rossen Stoyanchev
 */
public class ResponseCookieTests {

	@Test
	public void basic() {

		assertThat(ResponseCookie.from("id", null).build().toString()).isEqualTo("id=");
		assertThat(ResponseCookie.from("id", "1fWa").build().toString()).isEqualTo("id=1fWa");

		ResponseCookie cookie = ResponseCookie.from("id", "1fWa")
				.domain("abc").path("/path").maxAge(0).httpOnly(true).secure(true).sameSite("None")
				.build();

		assertThat(cookie.toString()).isEqualTo("id=1fWa; Path=/path; Domain=abc; " +
				"Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; " +
				"Secure; HttpOnly; SameSite=None");
	}

	@Test
	public void nameChecks() {

		Arrays.asList("id", "i.d.", "i-d", "+id", "i*d", "i$d", "#id")
				.forEach(name -> ResponseCookie.from(name, "value").build());

		Arrays.asList("\"id\"", "id\t", "i\td", "i d", "i;d", "{id}", "[id]", "\"", "id\u0091")
				.forEach(name -> assertThatThrownBy(() -> ResponseCookie.from(name, "value").build())
						.hasMessageContaining("RFC2616 token"));
	}

	@Test
	public void valueChecks() {

		Arrays.asList("1fWa", "", null, "1f=Wa", "1f-Wa", "1f/Wa", "1.f.W.a.")
				.forEach(value -> ResponseCookie.from("id", value).build());

		Arrays.asList("1f\tWa", "\t", "1f Wa", "1f;Wa", "\"1fWa", "1f\\Wa", "1f\"Wa", "\"", "1fWa\u0005", "1f\u0091Wa")
				.forEach(value -> assertThatThrownBy(() -> ResponseCookie.from("id", value).build())
						.hasMessageContaining("RFC2616 cookie value"));
	}

}
