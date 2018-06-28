/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.Collection;

import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;

import static org.junit.Assert.*;

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
	public void headerPresent() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("Accept", ""));
		HeadersRequestCondition condition = new HeadersRequestCondition("accept");
	
		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerPresentNoMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("bar", ""));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerNotPresent() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		HeadersRequestCondition condition = new HeadersRequestCondition("!accept");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueNoMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("foo", "bazz"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=bar");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerCaseSensitiveValueMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo=Bar");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueMatchNegated() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("foo", "baz"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void headerValueNoMatchNegated() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo!=bar");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void compareTo() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo", "bar", "baz");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
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
	public void getMatchingCondition() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("foo", "bar"));
		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		HeadersRequestCondition result = condition.getMatchingCondition(exchange);
		assertEquals(condition, result);

		condition = new HeadersRequestCondition("bar");

		result = condition.getMatchingCondition(exchange);
		assertNull(result);
	}

}
