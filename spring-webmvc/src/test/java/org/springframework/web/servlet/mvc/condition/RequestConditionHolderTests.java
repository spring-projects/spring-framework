/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.*;

/**
 * A test fixture for {@link RequestConditionHolder} tests.
 *
 * @author Rossen Stoyanchev
 */
public class RequestConditionHolderTests {

	@Test
	public void combine() {
		RequestConditionHolder params1 = new RequestConditionHolder(new ParamsRequestCondition("name1"));
		RequestConditionHolder params2 = new RequestConditionHolder(new ParamsRequestCondition("name2"));
		RequestConditionHolder expected = new RequestConditionHolder(new ParamsRequestCondition("name1", "name2"));

		assertEquals(expected, params1.combine(params2));
	}

	@Test
	public void combineEmpty() {
		RequestConditionHolder empty = new RequestConditionHolder(null);
		RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

		assertSame(empty, empty.combine(empty));
		assertSame(notEmpty, notEmpty.combine(empty));
		assertSame(notEmpty, empty.combine(notEmpty));
	}

	@Test(expected=ClassCastException.class)
	public void combineIncompatible() {
		RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
		RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
		params.combine(headers);
	}

	@Test
	public void match() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setParameter("name1", "value1");

		RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);
		RequestConditionHolder custom = new RequestConditionHolder(rm);
		RequestMethodsRequestCondition expected = new RequestMethodsRequestCondition(RequestMethod.GET);

		assertEquals(expected, custom.getMatchingCondition(request).getCondition());
	}

	@Test
	public void noMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

		RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(RequestMethod.POST);
		RequestConditionHolder custom = new RequestConditionHolder(rm);

		assertNull(custom.getMatchingCondition(request));
	}

	@Test
	public void matchEmpty() {
		RequestConditionHolder empty = new RequestConditionHolder(null);
		assertSame(empty, empty.getMatchingCondition(new MockHttpServletRequest()));
	}

	@Test
	public void compare() {
		HttpServletRequest request = new MockHttpServletRequest();

		RequestConditionHolder params11 = new RequestConditionHolder(new ParamsRequestCondition("1"));
		RequestConditionHolder params12 = new RequestConditionHolder(new ParamsRequestCondition("1", "2"));

		assertEquals(1, params11.compareTo(params12, request));
		assertEquals(-1, params12.compareTo(params11, request));
	}

	@Test
	public void compareEmpty() {
		HttpServletRequest request = new MockHttpServletRequest();

		RequestConditionHolder empty = new RequestConditionHolder(null);
		RequestConditionHolder empty2 = new RequestConditionHolder(null);
		RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

		assertEquals(0, empty.compareTo(empty2, request));
		assertEquals(-1, notEmpty.compareTo(empty, request));
		assertEquals(1, empty.compareTo(notEmpty, request));
	}

	@Test(expected=ClassCastException.class)
	public void compareIncompatible() {
		RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
		RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
		params.compareTo(headers, new MockHttpServletRequest());
	}

}
