/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Collections;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMethodsRequestConditionTests {

	@Test
	public void methodMatch() {
		RequestCondition condition = new RequestMethodsRequestCondition(GET);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void methodNoMatch() {
		RequestCondition condition = new RequestMethodsRequestCondition(GET);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/foo");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void multipleMethodsMatch() {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition(GET, POST);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		RequestMethodsRequestCondition actual = condition.getMatchingCondition(request);

		assertNotNull(actual);
		assertEquals(Collections.singleton(GET), actual.getContent());
	}

	@Test
	public void methodHeadMatch() throws Exception {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition(GET, POST);
		MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/foo");
		RequestMethodsRequestCondition actual = condition.getMatchingCondition(request);

		assertNotNull(actual);
		assertEquals("GET should also match HEAD", Collections.singleton(HEAD), actual.getContent());
	}

	@Test
	public void methodHeadNoMatch() throws Exception {
		RequestMethodsRequestCondition condition = new RequestMethodsRequestCondition(POST);
		MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/foo");
		RequestMethodsRequestCondition actual = condition.getMatchingCondition(request);

		assertNull("HEAD should match only if GET is declared", actual);
	}

	@Test
	public void noDeclaredMethodsMatchesAllMethodsExceptOptions() {
		RequestCondition condition = new RequestMethodsRequestCondition();

		assertNotNull(condition.getMatchingCondition(new MockHttpServletRequest("GET", "")));
		assertNotNull(condition.getMatchingCondition(new MockHttpServletRequest("POST", "")));
		assertNotNull(condition.getMatchingCondition(new MockHttpServletRequest("HEAD", "")));
		assertNull(condition.getMatchingCondition(new MockHttpServletRequest("OPTIONS", "")));
	}

	@Test
	public void unknownMethodType() throws Exception {
		RequestCondition condition = new RequestMethodsRequestCondition(GET, POST);
		MockHttpServletRequest request = new MockHttpServletRequest("PROPFIND", "/foo");

		assertNull(condition.getMatchingCondition(request));
	}

	@Test
	public void compareTo() {
		RequestMethodsRequestCondition c1 = new RequestMethodsRequestCondition(GET, HEAD);
		RequestMethodsRequestCondition c2 = new RequestMethodsRequestCondition(POST);
		RequestMethodsRequestCondition c3 = new RequestMethodsRequestCondition();

		MockHttpServletRequest request = new MockHttpServletRequest();

		int result = c1.compareTo(c2, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = c2.compareTo(c1, request);
		assertTrue("Invalid comparison result: " + result, result > 0);

		result = c2.compareTo(c3, request);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = c1.compareTo(c1, request);
		assertEquals("Invalid comparison result ", 0, result);
	}

	@Test
	public void combine() {
		RequestMethodsRequestCondition condition1 = new RequestMethodsRequestCondition(GET);
		RequestMethodsRequestCondition condition2 = new RequestMethodsRequestCondition(POST);

		RequestMethodsRequestCondition result = condition1.combine(condition2);
		assertEquals(2, result.getContent().size());
	}

}
