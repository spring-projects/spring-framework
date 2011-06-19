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

package org.springframework.web.servlet.mvc.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;

/**
 * @author Rossen Stoyanchev
 */
public class CustomRequestConditionTests {

	@Test
	public void combineEmpty() {
		CustomRequestCondition empty = new CustomRequestCondition();
		CustomRequestCondition custom = new CustomRequestCondition(new ParamsRequestCondition("name"));
		
		assertSame(empty, empty.combine(new CustomRequestCondition()));
		assertSame(custom, custom.combine(empty));
		assertSame(custom, empty.combine(custom));
	}

	@Test
	public void combine() {
		CustomRequestCondition params1 = new CustomRequestCondition(new ParamsRequestCondition("name1"));
		CustomRequestCondition params2 = new CustomRequestCondition(new ParamsRequestCondition("name2"));
		CustomRequestCondition expected = new CustomRequestCondition(new ParamsRequestCondition("name1", "name2"));
		
		assertEquals(expected, params1.combine(params2));
	}

	@Test(expected=ClassCastException.class)
	public void combineIncompatible() {
		CustomRequestCondition params = new CustomRequestCondition(new ParamsRequestCondition("name"));
		CustomRequestCondition headers = new CustomRequestCondition(new HeadersRequestCondition("name"));
		params.combine(headers);
	}

	@Test
	public void match() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setParameter("name1", "value1");
		
		RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);
		CustomRequestCondition custom = new CustomRequestCondition(rm);
		RequestMethodsRequestCondition expected = new RequestMethodsRequestCondition(RequestMethod.GET);
		
		assertEquals(expected, custom.getMatchingCondition(request).getCondition());
	}
	
	@Test
	public void matchEmpty() {
		CustomRequestCondition empty = new CustomRequestCondition();
		assertSame(empty, empty.getMatchingCondition(new MockHttpServletRequest()));
	}

	@Test
	public void compare() {
		HttpServletRequest request = new MockHttpServletRequest();

		CustomRequestCondition params11 = new CustomRequestCondition(new ParamsRequestCondition("1"));
		CustomRequestCondition params12 = new CustomRequestCondition(new ParamsRequestCondition("1", "2"));

		assertEquals(1, params11.compareTo(params12, request));
		assertEquals(-1, params12.compareTo(params11, request));
	}
	
	@Test
	public void compareEmpty() {
		HttpServletRequest request = new MockHttpServletRequest();

		CustomRequestCondition empty = new CustomRequestCondition();
		CustomRequestCondition empty2 = new CustomRequestCondition();
		CustomRequestCondition custom = new CustomRequestCondition(new ParamsRequestCondition("name"));

		assertEquals(0, empty.compareTo(empty2, request));
		assertEquals(-1, custom.compareTo(empty, request));
		assertEquals(1, empty.compareTo(custom, request));
	}

	@Test(expected=ClassCastException.class)
	public void compareIncompatible() {
		CustomRequestCondition params = new CustomRequestCondition(new ParamsRequestCondition("name"));
		CustomRequestCondition headers = new CustomRequestCondition(new HeadersRequestCondition("name"));
		params.compareTo(headers, new MockHttpServletRequest());
	}

}
