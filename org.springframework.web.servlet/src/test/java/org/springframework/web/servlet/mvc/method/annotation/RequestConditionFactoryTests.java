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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Set;

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestConditionFactory;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class RequestConditionFactoryTests {

	@Test
	public void paramEquals() {
		assertEquals(getSingleParamCondition("foo"), getSingleParamCondition("foo"));
		assertFalse(getSingleParamCondition("foo").equals(getSingleParamCondition("bar")));
		assertFalse(getSingleParamCondition("foo").equals(getSingleParamCondition("FOO")));
		assertEquals(getSingleParamCondition("foo=bar"), getSingleParamCondition("foo=bar"));
		assertFalse(getSingleParamCondition("foo=bar").equals(getSingleParamCondition("FOO=bar")));
	}
	
	@Test
	public void headerEquals() {
		assertEquals(getSingleHeaderCondition("foo"), getSingleHeaderCondition("foo"));
		assertEquals(getSingleHeaderCondition("foo"), getSingleHeaderCondition("FOO"));
		assertFalse(getSingleHeaderCondition("foo").equals(getSingleHeaderCondition("bar")));
		assertEquals(getSingleHeaderCondition("foo=bar"), getSingleHeaderCondition("foo=bar"));
		assertEquals(getSingleHeaderCondition("foo=bar"), getSingleHeaderCondition("FOO=bar"));
		assertEquals(getSingleHeaderCondition("content-type=text/xml"),
				getSingleHeaderCondition("Content-Type=TEXT/XML"));
	}

	@Test
	public void headerPresent() {
		RequestCondition condition = getSingleHeaderCondition("accept");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "");

		assertTrue(condition.match(request));
	}

	@Test
	public void headerPresentNoMatch() {
		RequestCondition condition = getSingleHeaderCondition("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerNotPresent() {
		RequestCondition condition = getSingleHeaderCondition("!accept");

		MockHttpServletRequest request = new MockHttpServletRequest();

		assertTrue(condition.match(request));
	}

	@Test
	public void headerValueMatch() {
		RequestCondition condition = getSingleHeaderCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertTrue(condition.match(request));
	}

	@Test
	public void headerValueNoMatch() {
		RequestCondition condition = getSingleHeaderCondition("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bazz");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerCaseSensitiveValueMatch() {
		RequestCondition condition = getSingleHeaderCondition("foo=Bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerValueMatchNegated() {
		RequestCondition condition = getSingleHeaderCondition("foo!=bar");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "baz");

		assertTrue(condition.match(request));
	}

	@Test
	public void mediaTypeHeaderValueMatch() {
		RequestCondition condition = getSingleHeaderCondition("accept=text/html");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/html");

		assertTrue(condition.match(request));
	}

	@Test
	public void mediaTypeHeaderValueMatchNegated() {
		RequestCondition condition = getSingleHeaderCondition("accept!=text/html");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/html");

		assertTrue(condition.match(request));
	}

	private RequestCondition getSingleHeaderCondition(String expression) {
		Set<RequestCondition> conditions = RequestConditionFactory.parseHeaders(expression);
		assertEquals(1, conditions.size());
		return conditions.iterator().next();
	}

	private RequestCondition getSingleParamCondition(String expression) {
		Set<RequestCondition> conditions = RequestConditionFactory.parseParams(expression);
		assertEquals(1, conditions.size());
		return conditions.iterator().next();
	}


}
