/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;

/**
 * Unit tests for {@link ParamsRequestCondition}.
 * @author Rossen Stoyanchev
 */
public class ParamsRequestConditionTests {

	@Test
	public void paramEquals() {
		assertEquals(new ParamsRequestCondition("foo"), new ParamsRequestCondition("foo"));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("bar")));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("FOO")));
		assertEquals(new ParamsRequestCondition("foo=bar"), new ParamsRequestCondition("foo=bar"));
		assertFalse(new ParamsRequestCondition("foo=bar").equals(new ParamsRequestCondition("FOO=bar")));
	}

	@Test
	public void paramPresent() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertNotNull(condition.getMatchingCondition(get("/path?foo=").toExchange()));
	}

	@Test
	public void paramPresentNoMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");
		assertNull(condition.getMatchingCondition(get("/path?bar=").toExchange()));
	}

	@Test
	public void paramNotPresent() throws Exception {
		MockServerWebExchange exchange = get("/").toExchange();
		assertNotNull(new ParamsRequestCondition("!foo").getMatchingCondition(exchange));
	}

	@Test
	public void paramValueMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");
		assertNotNull(condition.getMatchingCondition(get("/path?foo=bar").toExchange()));
	}

	@Test
	public void paramValueNoMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");
		assertNull(condition.getMatchingCondition(get("/path?foo=bazz").toExchange()));
	}

	@Test
	public void compareTo() throws Exception {
		ServerWebExchange exchange = get("/").toExchange();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Collection<?> conditions = result.getContent();
		assertEquals(2, conditions.size());
	}

}
