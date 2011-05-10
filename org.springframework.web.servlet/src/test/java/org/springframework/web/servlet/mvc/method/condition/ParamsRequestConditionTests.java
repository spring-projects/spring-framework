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
public class ParamsRequestConditionTests {

	@Test
	public void paramEquals() {
		assertEquals(new ParamsRequestCondition("foo"), new ParamsRequestCondition("foo"));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("bar")));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("FOO")));
		assertEquals(new ParamsRequestCondition("foo=bar"), new ParamsRequestCondition("foo=bar"));
		assertFalse(
				new ParamsRequestCondition("foo=bar").equals(new ParamsRequestCondition("FOO=bar")));
	}

		@Test
	public void paramPresent() {
		RequestCondition condition = new ParamsRequestCondition("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "");

		assertTrue(condition.match(request));
	}

	@Test
	public void paramPresentNoMatch() {
		RequestCondition condition = new ParamsRequestCondition("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertFalse(condition.match(request));
	}

	@Test
	public void paramNotPresent() {
		RequestCondition condition = new ParamsRequestCondition("!foo");

		MockHttpServletRequest request = new MockHttpServletRequest();

		assertTrue(condition.match(request));
	}

	@Test
	public void paramValueMatch() {
		RequestCondition condition = new ParamsRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bar");

		assertTrue(condition.match(request));
	}

	@Test
	public void paramValueNoMatch() {
		RequestCondition condition = new ParamsRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bazz");

		assertFalse(condition.match(request));
	}

	@Test
	public void compareTo() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Set<ParamsRequestCondition.ParamRequestCondition> conditions = result.getConditions();
		assertEquals(2, conditions.size());
	}

	@Test
	public void getMatchingCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bar");

		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		ParamsRequestCondition result = condition.getMatchingCondition(request);
		assertEquals(condition, result);

		condition = new ParamsRequestCondition("bar");

		result = condition.getMatchingCondition(request);
		assertNull(result);
	}

}
