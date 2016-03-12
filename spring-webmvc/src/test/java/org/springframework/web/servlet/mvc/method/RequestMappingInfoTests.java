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

package org.springframework.web.servlet.mvc.method;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

/**
 * Test fixture for {@link RequestMappingInfo} tests.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoTests {

	@Test
	public void createEmpty() {
		RequestMappingInfo info = new RequestMappingInfo(null, null, null, null, null, null, null);

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
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		RequestMappingInfo info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo*", "/bar"), null, null, null, null, null, null);
		RequestMappingInfo expected = new RequestMappingInfo(
				new PatternsRequestCondition("/foo*"), null, null, null, null, null, null);

		assertEquals(expected, info.getMatchingCondition(request));

		info = new RequestMappingInfo(
				new PatternsRequestCondition("/**", "/foo*", "/foo"), null, null, null, null, null, null);
		expected = new RequestMappingInfo(
				new PatternsRequestCondition("/foo", "/foo*", "/**"), null, null, null, null, null, null);

		assertEquals(expected, info.getMatchingCondition(request));
	}

	@Test
	public void matchParamsCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestMappingInfo info =
				new RequestMappingInfo(
						new PatternsRequestCondition("/foo"), null,
						new ParamsRequestCondition("foo=bar"), null, null, null, null);
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null,
				new ParamsRequestCondition("foo!=bar"), null, null, null, null);
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchHeadersCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("foo", "bar");

		RequestMappingInfo info =
				new RequestMappingInfo(
						new PatternsRequestCondition("/foo"), null, null,
						new HeadersRequestCondition("foo=bar"), null, null, null);
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null,
				new HeadersRequestCondition("foo!=bar"), null, null, null);
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchConsumesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContentType("text/plain");

		RequestMappingInfo info =
			new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null, null,
				new ConsumesRequestCondition("text/plain"), null, null);
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null, null,
				new ConsumesRequestCondition("application/xml"), null, null);
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchProducesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "text/plain");

		RequestMappingInfo info =
			new RequestMappingInfo(
					new PatternsRequestCondition("/foo"), null, null, null, null,
					new ProducesRequestCondition("text/plain"), null);
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null, null, null,
				new ProducesRequestCondition("application/xml"), null);
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void matchCustomCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestMappingInfo info =
				new RequestMappingInfo(
						new PatternsRequestCondition("/foo"), null, null, null, null, null,
						new ParamsRequestCondition("foo=bar"));
		RequestMappingInfo match = info.getMatchingCondition(request);

		assertNotNull(match);

		info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null,
				new ParamsRequestCondition("foo!=bar"), null, null, null,
				new ParamsRequestCondition("foo!=bar"));
		match = info.getMatchingCondition(request);

		assertNull(match);
	}

	@Test
	public void compareTwoHttpMethodsOneParam() {
		RequestMappingInfo none = new RequestMappingInfo(null, null, null, null, null, null, null);
		RequestMappingInfo oneMethod =
			new RequestMappingInfo(null,
					new RequestMethodsRequestCondition(RequestMethod.GET), null, null, null, null, null);
		RequestMappingInfo oneMethodOneParam =
				new RequestMappingInfo(null,
						new RequestMethodsRequestCondition(RequestMethod.GET),
						new ParamsRequestCondition("foo"), null, null, null, null);

		Comparator<RequestMappingInfo> comparator = new Comparator<RequestMappingInfo>() {
			@Override
			public int compare(RequestMappingInfo info, RequestMappingInfo otherInfo) {
				return info.compareTo(otherInfo, new MockHttpServletRequest());
			}
		};

		List<RequestMappingInfo> list = asList(none, oneMethod, oneMethodOneParam);
		Collections.shuffle(list);
		Collections.sort(list, comparator);

		assertEquals(oneMethodOneParam, list.get(0));
		assertEquals(oneMethod, list.get(1));
		assertEquals(none, list.get(2));
	}

	@Test
	public void equals() {
		RequestMappingInfo info1 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=customBar"));

		RequestMappingInfo info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=customBar"));

		assertEquals(info1, info2);
		assertEquals(info1.hashCode(), info2.hashCode());

		info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo", "/NOOOOOO"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=customBar"));

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=customBar"));

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("/NOOOOOO"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=customBar"));

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("/NOOOOOO"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=customBar"));

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/NOOOOOO"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=customBar"));

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/NOOOOOO"),
				new ParamsRequestCondition("customFoo=customBar"));

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());

		info2 = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"),
				new RequestMethodsRequestCondition(RequestMethod.GET),
				new ParamsRequestCondition("foo=bar"),
				new HeadersRequestCondition("foo=bar"),
				new ConsumesRequestCondition("text/plain"),
				new ProducesRequestCondition("text/plain"),
				new ParamsRequestCondition("customFoo=NOOOOOO"));

		assertFalse(info1.equals(info2));
		assertNotEquals(info1.hashCode(), info2.hashCode());
	}

	@Test
	public void preFlightRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/foo");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");

		RequestMappingInfo info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), new RequestMethodsRequestCondition(RequestMethod.POST), null,
				null, null, null, null);
		RequestMappingInfo match = info.getMatchingCondition(request);
		assertNotNull(match);

		info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), new RequestMethodsRequestCondition(RequestMethod.OPTIONS), null,
				null, null, null, null);
		match = info.getMatchingCondition(request);
		assertNull("Pre-flight should match the ACCESS_CONTROL_REQUEST_METHOD", match);
	}

}
