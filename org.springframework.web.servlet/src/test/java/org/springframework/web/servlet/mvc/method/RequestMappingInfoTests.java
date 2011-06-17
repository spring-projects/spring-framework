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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.condition.ProducesRequestCondition;

/**
 * Test fixture for {@link RequestMappingInfo} tests.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoTests {

	@Test
	public void equals() {
		RequestMappingInfo key1 = new RequestMappingInfo(new String[] {"/foo"}, GET);
		RequestMappingInfo key2 = new RequestMappingInfo(new String[] {"/foo"}, GET);

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void equalsPrependSlash() {
		RequestMappingInfo key1 = new RequestMappingInfo(new String[] {"/foo"}, GET);
		RequestMappingInfo key2 = new RequestMappingInfo(new String[] {"foo"}, GET);

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void combinePatterns() {
		RequestMappingInfo key1 = createFromPatterns("/t1", "/t2");
		RequestMappingInfo key2 = createFromPatterns("/m1", "/m2");
		RequestMappingInfo key3 = createFromPatterns("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2");
		assertEquals(key3.getPatternsCondition(), key1.combine(key2).getPatternsCondition());

		key1 = createFromPatterns("/t1");
		key2 = createFromPatterns();
		key3 = createFromPatterns("/t1");
		assertEquals(key3.getPatternsCondition(), key1.combine(key2).getPatternsCondition());

		key1 = createFromPatterns();
		key2 = createFromPatterns("/m1");
		key3 = createFromPatterns("/m1");
		assertEquals(key3.getPatternsCondition(), key1.combine(key2).getPatternsCondition());

		key1 = createFromPatterns();
		key2 = createFromPatterns();
		key3 = createFromPatterns("");
		assertEquals(key3.getPatternsCondition(), key1.combine(key2).getPatternsCondition());

		key1 = createFromPatterns("/t1");
		key2 = createFromPatterns("");
		key3 = createFromPatterns("/t1");
		assertEquals(key3.getPatternsCondition(), key1.combine(key2).getPatternsCondition());
	}

	@Test
	public void matchPatternsToRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		RequestMappingInfo match = createFromPatterns("/foo").getMatchingRequestMappingInfo(request);

		assertNotNull(match);

		request = new MockHttpServletRequest("GET", "/foo/bar");
		match = createFromPatterns("/foo/*").getMatchingRequestMappingInfo(request);

		assertNotNull("Pattern match", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		match = createFromPatterns("/foo").getMatchingRequestMappingInfo(request);

		assertNotNull("Implicit match by extension", match);
		assertEquals("Contains matched pattern", "/foo.*", match.getPatternsCondition().getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo/");
		match = createFromPatterns("/foo").getMatchingRequestMappingInfo(request);

		assertNotNull("Implicit match by trailing slash", match);
		assertEquals("Contains matched pattern", "/foo/", match.getPatternsCondition().getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo.html");
		match = createFromPatterns("/foo.jpg").getMatchingRequestMappingInfo(request);

		assertNull("Implicit match ignored if pattern has extension", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		match = createFromPatterns("/foo.jpg").getMatchingRequestMappingInfo(request);

		assertNull("Implicit match ignored on pattern with trailing slash", match);
	}

	@Test
	public void matchRequestMethods() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		RequestMappingInfo key = createFromPatterns("/foo");
		RequestMappingInfo match = createFromPatterns("/foo").getMatchingRequestMappingInfo(request);

		assertNotNull("No method matches any method", match);

		key = new RequestMappingInfo(new String[]{"/foo"}, GET);
		match = key.getMatchingRequestMappingInfo(request);

		assertNotNull("Exact match", match);

		key = new RequestMappingInfo(new String[]{"/foo"}, POST);
		match = key.getMatchingRequestMappingInfo(request);

		assertNull("No match", match);
	}

	@Test
	public void matchingKeyContent() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		RequestMappingInfo key = new RequestMappingInfo(new String[] {"/foo*", "/bar"}, GET, POST);
		RequestMappingInfo match = key.getMatchingRequestMappingInfo(request);
		RequestMappingInfo expected = new RequestMappingInfo(new String[] {"/foo*"}, GET);

		assertEquals("Matching RequestKey contains matched patterns and methods only", expected, match);

		key = createFromPatterns("/**", "/foo*", "/foo");
		match = key.getMatchingRequestMappingInfo(request);
		expected = createFromPatterns("/foo", "/foo*", "/**");

		assertEquals("Matched patterns are sorted with best match at the top", expected, match);
	}

	@Test
	public void paramsCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestMappingInfo key =
				new RequestMappingInfo(
						new PatternsRequestCondition("/foo"), null, 
						new ParamsRequestCondition("foo=bar"), null, null, null);
		RequestMappingInfo match = key.getMatchingRequestMappingInfo(request);

		assertNotNull(match);

		key = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, 
				new ParamsRequestCondition("foo!=bar"), null, null, null);
		match = key.getMatchingRequestMappingInfo(request);

		assertNull(match);
	}

	@Test
	public void headersCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("foo", "bar");

		RequestMappingInfo key =
				new RequestMappingInfo(
						new PatternsRequestCondition("/foo"), null, null, 
						new HeadersRequestCondition("foo=bar"), null, null);
		RequestMappingInfo match = key.getMatchingRequestMappingInfo(request);

		assertNotNull(match);

		key = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null, 
				new HeadersRequestCondition("foo!=bar"), null, null);
		match = key.getMatchingRequestMappingInfo(request);

		assertNull(match);
	}

	@Test
	public void consumesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContentType("text/plain");

		RequestMappingInfo key = 
			new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null, null,
				new ConsumesRequestCondition("text/plain"), null);
		RequestMappingInfo match = key.getMatchingRequestMappingInfo(request);

		assertNotNull(match);

		key = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null, null,
				new ConsumesRequestCondition("application/xml"), null);
		match = key.getMatchingRequestMappingInfo(request);

		assertNull(match);
	}

	@Test
	public void producesCondition() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("Accept", "text/plain");

		RequestMappingInfo key = 
			new RequestMappingInfo(
					new PatternsRequestCondition("/foo"), null, null, null, null, 
					new ProducesRequestCondition("text/plain"));
		RequestMappingInfo match = key.getMatchingRequestMappingInfo(request);

		assertNotNull(match);

		key = new RequestMappingInfo(
				new PatternsRequestCondition("/foo"), null, null, null, null,
				new ProducesRequestCondition("application/xml"));
		match = key.getMatchingRequestMappingInfo(request);

		assertNull(match);
	}

	private RequestMappingInfo createFromPatterns(String... patterns) {
		return new RequestMappingInfo(patterns);
	}

}