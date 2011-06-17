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

package org.springframework.web.servlet.mvc.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition.ParamExpression;

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
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void paramPresentNoMatch() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void paramNotPresent() {
		ParamsRequestCondition condition = new ParamsRequestCondition("!foo");

		MockHttpServletRequest request = new MockHttpServletRequest();

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void paramValueMatch() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bar");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void paramValueNoMatch() {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bazz");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void compareTo() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Collection<ParamExpression> conditions = result.getContent();
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
