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

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class RequestConditionFactoryTests {


	@Test
	public void andMatch() {
		RequestCondition condition1 = RequestConditionFactory.trueCondition();
		RequestCondition condition2 = RequestConditionFactory.trueCondition();
		RequestCondition and = RequestConditionFactory.and(condition1, condition2);
		assertTrue(and.match(new MockHttpServletRequest()));
	}

	@Test
	public void andNoMatch() {
		RequestCondition condition1 = RequestConditionFactory.trueCondition();
		RequestCondition condition2 = RequestConditionFactory.falseCondition();
		RequestCondition and = RequestConditionFactory.and(condition1, condition2);
		assertFalse(and.match(new MockHttpServletRequest()));
	}

	@Test
	public void orMatch() {
		RequestCondition condition1 = RequestConditionFactory.trueCondition();
		RequestCondition condition2 = RequestConditionFactory.falseCondition();
		RequestCondition and = RequestConditionFactory.or(condition1, condition2);
		assertTrue(and.match(new MockHttpServletRequest()));
	}

	@Test
	public void orNoMatch() {
		RequestCondition condition1 = RequestConditionFactory.falseCondition();
		RequestCondition condition2 = RequestConditionFactory.falseCondition();
		RequestCondition and = RequestConditionFactory.and(condition1, condition2);
		assertFalse(and.match(new MockHttpServletRequest()));
	}

	@Test
	public void paramEquals() {
		assertEquals(RequestConditionFactory.parseParams("foo"), RequestConditionFactory.parseParams("foo"));
		assertFalse(RequestConditionFactory.parseParams("foo").equals(RequestConditionFactory.parseParams("bar")));
		assertFalse(RequestConditionFactory.parseParams("foo").equals(RequestConditionFactory.parseParams("FOO")));
		assertEquals(RequestConditionFactory.parseParams("foo=bar"), RequestConditionFactory.parseParams("foo=bar"));
		assertFalse(
				RequestConditionFactory.parseParams("foo=bar").equals(RequestConditionFactory.parseParams("FOO=bar")));
	}
	
	@Test
	public void headerEquals() {
		assertEquals(RequestConditionFactory.parseHeaders("foo"), RequestConditionFactory.parseHeaders("foo"));
		assertEquals(RequestConditionFactory.parseHeaders("foo"), RequestConditionFactory.parseHeaders("FOO"));
		assertFalse(RequestConditionFactory.parseHeaders("foo").equals(RequestConditionFactory.parseHeaders("bar")));
		assertEquals(RequestConditionFactory.parseHeaders("foo=bar"), RequestConditionFactory.parseHeaders("foo=bar"));
		assertEquals(RequestConditionFactory.parseHeaders("foo=bar"), RequestConditionFactory.parseHeaders("FOO=bar"));
		assertEquals(RequestConditionFactory.parseHeaders("content-type=text/xml"),
				RequestConditionFactory.parseHeaders("Content-Type=TEXT/XML"));
	}

	@Test
	public void headerPresent() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("accept");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "");

		assertTrue(condition.match(request));
	}

	@Test
	public void headerPresentNoMatch() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("foo");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerNotPresent() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("!accept");

		MockHttpServletRequest request = new MockHttpServletRequest();

		assertTrue(condition.match(request));
	}

	@Test
	public void headerValueMatch() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertTrue(condition.match(request));
	}

	@Test
	public void headerValueNoMatch() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("foo=bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bazz");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerCaseSensitiveValueMatch() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("foo=Bar");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "bar");

		assertFalse(condition.match(request));
	}

	@Test
	public void headerValueMatchNegated() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("foo!=bar");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo", "baz");

		assertTrue(condition.match(request));
	}

	@Test
	public void mediaTypeHeaderValueMatch() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("accept=text/html");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "text/html");

		assertTrue(condition.match(request));
	}

	@Test
	public void mediaTypeHeaderValueMatchNegated() {
		RequestCondition condition = RequestConditionFactory.parseHeaders("accept!=text/html");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/html");

		assertTrue(condition.match(request));
	}
	
	@Test
	public void consumesMatch() {
		RequestCondition condition = RequestConditionFactory.parseConsumes("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertTrue(condition.match(request));
	}

	@Test
	public void consumesWildcardMatch() {
		RequestCondition condition = RequestConditionFactory.parseConsumes("text/*");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertTrue(condition.match(request));
	}

	@Test
	public void consumesMultipleMatch() {
		RequestCondition condition = RequestConditionFactory.parseConsumes("text/plain", "application/xml");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text/plain");

		assertTrue(condition.match(request));
	}

	@Test
	public void consumesSingleNoMatch() {
		RequestCondition condition = RequestConditionFactory.parseConsumes("text/plain");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("application/xml");

		assertFalse(condition.match(request));
	}

}
