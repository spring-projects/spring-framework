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

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.condition.RequestConditionFactory;
import org.springframework.web.util.UrlPathHelper;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Test fixture for {@link RequestMappingInfo} tests.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoTests {

	@Test
	public void equals() {
		RequestMappingInfo key1 = new RequestMappingInfo(singleton("/foo"), methods(GET));
		RequestMappingInfo key2 = new RequestMappingInfo(singleton("/foo"), methods(GET));

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void equalsPrependSlash() {
		RequestMappingInfo key1 = new RequestMappingInfo(singleton("/foo"), methods(GET));
		RequestMappingInfo key2 = new RequestMappingInfo(singleton("foo"), methods(GET));

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void combinePatterns() {
		AntPathMatcher pathMatcher = new AntPathMatcher();

		RequestMappingInfo key1 = createKeyFromPatterns("/t1", "/t2");
		RequestMappingInfo key2 = createKeyFromPatterns("/m1", "/m2");
		RequestMappingInfo key3 = createKeyFromPatterns("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2");
		assertEquals(key3.getPatterns(), key1.combine(key2, pathMatcher).getPatterns());

		key1 = createKeyFromPatterns("/t1");
		key2 = createKeyFromPatterns();
		key3 = createKeyFromPatterns("/t1");
		assertEquals(key3.getPatterns(), key1.combine(key2, pathMatcher).getPatterns());

		key1 = createKeyFromPatterns();
		key2 = createKeyFromPatterns("/m1");
		key3 = createKeyFromPatterns("/m1");
		assertEquals(key3.getPatterns(), key1.combine(key2, pathMatcher).getPatterns());

		key1 = createKeyFromPatterns();
		key2 = createKeyFromPatterns();
		key3 = createKeyFromPatterns("");
		assertEquals(key3.getPatterns(), key1.combine(key2, pathMatcher).getPatterns());

		key1 = createKeyFromPatterns("/t1");
		key2 = createKeyFromPatterns("");
		key3 = createKeyFromPatterns("/t1");
		assertEquals(key3.getPatterns(), key1.combine(key2, pathMatcher).getPatterns());
	}

	@Test
	public void matchPatternsToRequest() {
		UrlPathHelper pathHelper = new UrlPathHelper();
		PathMatcher pathMatcher = new AntPathMatcher();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		RequestMappingInfo key = new RequestMappingInfo(singleton("/foo"), null);
		RequestMappingInfo match =
				key.getMatchingRequestMapping(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull(match);

		request = new MockHttpServletRequest("GET", "/foo/bar");
		key = new RequestMappingInfo(singleton("/foo/*"), null);
		match = key.getMatchingRequestMapping(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull("Pattern match", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestMappingInfo(singleton("/foo"), null);
		match = key.getMatchingRequestMapping(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull("Implicit match by extension", match);
		assertEquals("Contains matched pattern", "/foo.*", match.getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo/");
		key = new RequestMappingInfo(singleton("/foo"), null);
		match = key.getMatchingRequestMapping(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull("Implicit match by trailing slash", match);
		assertEquals("Contains matched pattern", "/foo/", match.getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestMappingInfo(singleton("/foo.jpg"), null);
		match = key.getMatchingRequestMapping(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNull("Implicit match ignored if pattern has extension", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestMappingInfo(singleton("/foo.jpg"), null);
		match = key.getMatchingRequestMapping(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNull("Implicit match ignored on pattern with trailing slash", match);
	}

	@Test
	public void matchRequestMethods() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingInfo key = new RequestMappingInfo(singleton("/foo"), null);
		RequestMappingInfo match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNotNull("No method matches any method", match);

		key = new RequestMappingInfo(singleton("/foo"), methods(GET));
		match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNotNull("Exact match", match);

		key = new RequestMappingInfo(singleton("/foo"), methods(POST));
		match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNull("No match", match);
	}

	@Test
	public void matchingKeyContent() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingInfo key = new RequestMappingInfo(asList("/foo*", "/bar"), methods(GET, POST));
		RequestMappingInfo match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);
		RequestMappingInfo expected = new RequestMappingInfo(singleton("/foo*"), methods(GET));

		assertEquals("Matching RequestKey contains matched patterns and methods only", expected, match);

		key = new RequestMappingInfo(asList("/**", "/foo*", "/foo"), null);
		match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);
		expected = new RequestMappingInfo(asList("/foo", "/foo*", "/**"), null);

		assertEquals("Matched patterns are sorted with best match at the top", expected, match);
	}

	@Test
	public void paramsCondition() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingInfo key =
				new RequestMappingInfo(asList("/foo"), null, RequestConditionFactory.parseParams("foo=bar"), null,
						null);
		RequestMappingInfo match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNotNull(match);

		key = new RequestMappingInfo(singleton("/foo"), null, RequestConditionFactory.parseParams("foo!=bar"), null,
				null);
		match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNull(match);
	}

	@Test
	public void headersCondition() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("foo", "bar");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingInfo key =
				new RequestMappingInfo(singleton("/foo"), null, null, RequestConditionFactory.parseHeaders("foo=bar"),
						null);
		RequestMappingInfo match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNotNull(match);

		key = new RequestMappingInfo(singleton("/foo"), null, null, RequestConditionFactory.parseHeaders("foo!=bar"),
				null);
		match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNull(match);
	}

	@Test
	public void consumesCondition() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContentType("text/plain");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingInfo key = new RequestMappingInfo(singleton("/foo"), null, null, null,
				RequestConditionFactory.parseConsumes("text/plain"));
		RequestMappingInfo match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNotNull(match);

		key = new RequestMappingInfo(singleton("/foo"), null, null, null,
				RequestConditionFactory.parseConsumes("application/xml"));
		match = key.getMatchingRequestMapping(lookupPath, request, pathMatcher);

		assertNull(match);
	}

	private RequestMappingInfo createKeyFromPatterns(String... patterns) {
		return new RequestMappingInfo(asList(patterns), null);
	}

	private RequestMethod[] methods(RequestMethod... methods) {
		if (methods != null) {
			return methods;
		}
		else {
			return new RequestMethod[0];
		}
	}

}