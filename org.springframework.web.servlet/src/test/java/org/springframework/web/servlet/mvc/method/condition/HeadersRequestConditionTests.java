/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.condition;

import java.util.Set;

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class HeadersRequestConditionTests {

	@Test
	public void headerEquals() {
		assertEquals(new HeadersRequestCondition("foo"), new HeadersRequestCondition("foo"));
		assertEquals(new HeadersRequestCondition("foo"), new HeadersRequestCondition("FOO"));
		assertFalse(new HeadersRequestCondition("foo").equals(new HeadersRequestCondition("bar")));
		assertEquals(new HeadersRequestCondition("foo=bar"), new HeadersRequestCondition("foo=bar"));
		assertEquals(new HeadersRequestCondition("foo=bar"), new HeadersRequestCondition("FOO=bar"));
	}

	@Test
	public void headerPresent() {
		RequestCondition condition = new HeadersRequestCondition("accept");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "");

		assertTrue(condition.match(request));
	}

	@Test
	public void headerPresentNoMatch() {
		RequestCondition condition = new HeadersRequestCondition("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerNotPresent() {
		RequestCondition condition = new HeadersRequestCondition("!accept");

		MockHttpServletRequest request = new MockHttpServletRequest();

		assertTrue(condition.match(request));
	}

	@Test
	public void headerValueMatch() {
		RequestCondition condition = new HeadersRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertTrue(condition.match(request));
	}

	@Test
	public void headerValueNoMatch() {
		RequestCondition condition = new HeadersRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bazz");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerCaseSensitiveValueMatch() {
		RequestCondition condition = new HeadersRequestCondition("foo=Bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerValueMatchNegated() {
		RequestCondition condition = new HeadersRequestCondition("foo!=bar");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "baz");

		assertTrue(condition.match(request));
	}

	@Test
	public void compareTo() {
		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo", "bar", "baz");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	
	@Test
	public void combine() {
		HeadersRequestCondition condition1 = new HeadersRequestCondition("foo=bar");
		HeadersRequestCondition condition2 = new HeadersRequestCondition("foo=baz");

		HeadersRequestCondition result = condition1.combine(condition2);
		Set<HeadersRequestCondition.HeaderRequestCondition> conditions = result.getConditions();
		assertEquals(2, conditions.size());
	}

	@Test
	public void getMatchingCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		HeadersRequestCondition condition = new HeadersRequestCondition("foo");

		HeadersRequestCondition result = condition.getMatchingCondition(request);
		assertEquals(condition, result);

		condition = new HeadersRequestCondition("bar");

		result = condition.getMatchingCondition(request);
		assertNull(result);
	}



}
