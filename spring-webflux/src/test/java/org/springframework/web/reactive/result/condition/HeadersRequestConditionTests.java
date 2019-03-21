/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.Collection;

import org.junit.Test;

import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;

/**
 * Unit tests for {@link HeadersRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class HeadersRequestConditionTests {

	@Test
	public void headerEquals() {
		assertEquals(new HeadersRequestCondition("foo"), new HeadersRequestCondition("foo"));
		assertEquals(new HeadersRequestCondition("foo"), new HeadersRequestCondition("FOO"));
		assertNotEquals(new HeadersRequestCondition("foo"), new HeadersRequestCondition("bar"));
		assertEquals(new HeadersRequestCondition("foo=bar"), new HeadersRequestCondition("foo=bar"));
		assertEquals(new HeadersRequestCondition("foo=bar"), new HeadersRequestCondition("FOO=bar"));
	}

	@Test
	public void headerPresent() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("Accept", ""));
		HeadersRequestCondition condition = new HeadersRequestCondition("accept");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerPresentNoMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("bar", ""));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerNotPresent() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));
		HeadersRequestCondition condition = new HeadersRequestCondition("!accept");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueNoMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bazz"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerCaseSensitiveValueMatch() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=Bar");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueMatchNegated() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "baz"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueNoMatchNegated() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void compareTo() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo", "bar", "baz");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=a", "bar");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test // SPR-16674
	public void compareToWithMoreSpecificMatchByValue() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=a");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);
	}

	@Test
	public void compareToWithNegatedMatch() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo!=a");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo");

		assertEquals("Negated match should not count as more specific",
				0, condition1.compareTo(condition2, exchange));
	}

	@Test
	public void combine() {
		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=bar");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=baz");

		HeadersRequestCondition result = condition1.combine(condition2);
		Collection<?> conditions = result.getContent();
		assertEquals(2, conditions.size());
	}

	@Test
	public void getMatchingCondition() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		HeadersRequestCondition result = condition.getMatchingCondition(exchange);
		assertEquals(condition, result);

		condition = new HeadersRequestCondition("bar");

		result = condition.getMatchingCondition(exchange);
		assertNull(result);
	}

}
