/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MockCookie}.
 *
 * @author Vedran Pavic
 */
public class MockCookieTests {

	@Test
	public void constructCookie() {
		MockCookie cookie = new MockCookie("SESSION", "123");

		assertEquals("SESSION", cookie.getName());
		assertEquals("123", cookie.getValue());
	}

	@Test
	public void setSameSite() {
		MockCookie cookie = new MockCookie("SESSION", "123");
		cookie.setSameSite("Strict");

		assertEquals("Strict", cookie.getSameSite());
	}

	@Test
	public void parseValidHeader() {
		MockCookie cookie = MockCookie.parse(
				"SESSION=123; Domain=example.com; Max-Age=60; Path=/; Secure; HttpOnly; SameSite=Lax");

		assertEquals("SESSION", cookie.getName());
		assertEquals("123", cookie.getValue());
		assertEquals("example.com", cookie.getDomain());
		assertEquals(60, cookie.getMaxAge());
		assertEquals("/", cookie.getPath());
		assertTrue(cookie.getSecure());
		assertTrue(cookie.isHttpOnly());
		assertEquals("Lax", cookie.getSameSite());
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseInvalidHeader() {
		MockCookie.parse("invalid");
	}

}
