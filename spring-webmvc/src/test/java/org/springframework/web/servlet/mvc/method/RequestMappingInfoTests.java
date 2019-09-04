/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc.method;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.servlet.mvc.method.RequestMappingInfo.paths;

/**
 * Test fixture for {@link RequestMappingInfo} tests.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoTests {

	@Test
	public void createEmpty() {
		RequestMappingInfo info = paths().build();

		assertEquals(0, info.getPatternsCondition().getPatterns().size());
		assertEquals(0, info.getMethodsCondition().getMethods().size());
		assertEquals(true, info.getConsumesCondition().isEmpty());
		assertEquals(true, info.getProducesCondition().isEmpty());
		assertNotNull(info.getParamsCondition());
		assertNotNull(info.getHeadersCondition());
		assertNull(info.getCustomCondition());
	}

	@Test
	public void matchPatternsCondition() {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		RequestMappingInfo info = paths("/foo*", "/bar").build();
		RequestMappingInfo expected = paths("/foo*").build();

		assertEquals(expected, info.getMatchingCondition(request));

		info = paths("/**", "/foo*", "/foo").build();
		expected = paths("/foo", "/foo*", "/**").build();

		assertEquals(expected, info.getMatchingCondition(request));
	}

	@Test
	public void matchParamsCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = paths("/foo").params("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchHeadersCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("foo", "bar");

		RequestMappingInfo info = paths("/foo").headers("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = paths("/foo").headers("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchConsumesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContentType("text/plain");

		RequestMappingInfo info = paths("/foo").consumes("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = paths("/foo").consumes("application/xml").build();
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchProducesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "text/plain");

		RequestMappingInfo info = paths("/foo").produces("text/plain").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = paths("/foo").produces("application/xml").build();
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchCustomCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestMappingInfo info = paths("/foo").params("foo=bar").build();
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = paths("/foo").params("foo!=bar").params("foo!=bar").build();
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void compareToWithImpicitVsExplicitHttpMethodDeclaration() {
		RequestMappingInfo noMethods = paths().build();
		RequestMappingInfo oneMethod = paths().methods(GET).build();
		RequestMappingInfo oneMethodOneParam = paths().methods(GET).params("foo").build();

		Comparator<RequestMappingInfo> comparator =
				(info, otherInfo) -> info.compareTo(otherInfo, new MockHttpServletRequest());

		List<RequestMappingInfo> list = asList(noMethods, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		Collections.sort(list, comparator);

		assertEquals(oneMethodOneParam, list.get(0));
		assertEquals(oneMethod, list.get(1));
		assertEquals(noMethods, list.get(2));
	}

	@Test // SPR-14383
	public void compareToWithHttpHeadMapping() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("HEAD");
		request.addHeader("Accept", "application/json");

		RequestMappingInfo noMethods = paths().build();
		RequestMappingInfo getMethod = paths().methods(GET).produces("application/json").build();
		RequestMappingInfo headMethod = paths().methods(HEAD).build();

		Comparator<RequestMappingInfo> comparator = (info, otherInfo) -> info.compareTo(otherInfo, request);

		List<RequestMappingInfo> list = asList(noMethods, getMethod, headMethod);
		Collections.shuffle(list);
		Collections.sort(list, comparator);

		assertEquals(headMethod, list.get(0));
		assertEquals(getMethod, list.get(1));
		assertEquals(noMethods, list.get(2));
	}

	@Test
	public void equals() {
		RequestMappingInfo info1 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		RequestMappingInfo info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertEquals(info1, info2);
		assertEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo", "/NOOOOOO").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(GET, RequestMethod.POST)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("/NOOOOOO", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("/NOOOOOO")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/NOOOOOO").produces("text/plain")
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=customBar").headers("foo=bar")
				.consumes("text/plain").produces("text/NOOOOOO")
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = paths("/foo").methods(GET)
				.params("foo=bar", "customFoo=NOOOOOO").headers("foo=bar")
				.consumes("text/plain").produces("text/plain")
				.build();

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());
	}

	@Test
	public void preFlightRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/foo");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");

		RequestMappingInfo info = paths("/foo").methods(RequestMethod.POST).build();
		RequestMappingInfo match = info.getMatchingCondition(request);
		assertNotNull(match);

		info = paths("/foo").methods(RequestMethod.OPTIONS).build();
		match = info.getMatchingCondition(request);
		assertNull("Pre-flight should match the ACCESS_CONTROL_REQUEST_METHOD", match);
	}

}
