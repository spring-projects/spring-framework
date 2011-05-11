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

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class RequestMethodsRequestConditionTests {

	@Test
	public void methodMatch() {
		RequestCondition condition = new RequestMethodsRequestCondition(RequestMethod.GET);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		assertTrue(condition.match(request));
	}

	@Test
	public void methodNoMatch() {
		RequestCondition condition = new RequestMethodsRequestCondition(RequestMethod.GET);

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/foo");

		assertFalse(condition.match(request));
	}
	
	@Test
	public void multipleMethodsMatch() {
		RequestCondition condition = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		assertTrue(condition.match(request));
	}


	@Test
	public void compareTo() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.HEAD);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(RequestMethod.POST);
		RequestMethodsRequestCondition condition3 = new RequestMethodsRequestCondition();

		int result = condition1.compareTo(condition2);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = condition2.compareTo(condition3);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition1.compareTo(condition1);
		assertEquals("Invalid comparison result ", 0, result);
	}

	@Test
	public void combine() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(RequestMethod.GET);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(RequestMethod.POST);

		RequestMethodsRequestCondition result = condition1.combine(condition2);
		assertEquals(2, result.getConditions().size());
	}


}
