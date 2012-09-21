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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Arjen Poutsma
 */
public class RequestMethodsRequestConditionTests {

	@Test
	public void methodMatch() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition(RequestMethod.GET);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void methodNoMatch() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition(RequestMethod.GET);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/foo");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void multipleMethodsMatch() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void noMethodsMatchAll() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition();

		assertNotNull(condition.getMatchingCondition(new MockHttpServletRequest("GET", "")));
		assertNotNull(condition.getMatchingCondition(new MockHttpServletRequest("POST", "")));
		assertNotNull(condition.getMatchingCondition(new MockHttpServletRequest("HEAD", "")));
	}

	@Test
	public void unknownMethodType() throws Exception {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);

		MockHttpServletRequest request = new MockHttpServletRequest("PROPFIND", "/foo");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void compareTo() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.HEAD);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(RequestMethod.POST);
		RequestMethodsRequestCondition condition3 = new RequestMethodsRequestCondition();

		MockHttpServletRequest request = new MockHttpServletRequest();

		int result = condition1.compareTo(condition2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = condition2.compareTo(condition3, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition1.compareTo(condition1, request);
		assertEquals("Invalid comparison result ", 0, result);
	}

	@Test
	public void combine() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(RequestMethod.GET);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(RequestMethod.POST);

		RequestMethodsRequestCondition result = condition1.combine(condition2);
		assertEquals(2, result.getContent().size());
	}


}
