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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.mvc.method.condition.RequestConditionFactory;
import org.springframework.web.util.UrlPathHelper;

/**
 * Test fixture for {@link RequestMappingKey} tests.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestKeyTests {

	@Test
	public void equals() {
		RequestMappingKey key1 = new RequestMappingKey(singleton("/foo"), singleton(GET));
		RequestMappingKey key2 = new RequestMappingKey(singleton("/foo"), singleton(GET));

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void equalsPrependSlash() {
		RequestMappingKey key1 = new RequestMappingKey(singleton("/foo"), singleton(GET));
		RequestMappingKey key2 = new RequestMappingKey(singleton("foo"), singleton(GET));

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void combinePatterns() {
		AntPathMatcher pathMatcher = new AntPathMatcher();

		RequestMappingKey key1 = createKeyFromPatterns("/t1", "/t2");
		RequestMappingKey key2 = createKeyFromPatterns("/m1", "/m2");
		RequestMappingKey key3 = createKeyFromPatterns("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2");
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
		RequestMappingKey key = new RequestMappingKey(singleton("/foo"), null);
		RequestMappingKey match = key.getMatchingKey(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull(match);

		request = new MockHttpServletRequest("GET", "/foo/bar");
		key = new RequestMappingKey(singleton("/foo/*"), null);
		match = key.getMatchingKey(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull("Pattern match", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestMappingKey(singleton("/foo"), null);
		match = key.getMatchingKey(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull("Implicit match by extension", match);
		assertEquals("Contains matched pattern", "/foo.*", match.getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo/");
		key = new RequestMappingKey(singleton("/foo"), null);
		match = key.getMatchingKey(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNotNull("Implicit match by trailing slash", match);
		assertEquals("Contains matched pattern", "/foo/", match.getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestMappingKey(singleton("/foo.jpg"), null);
		match = key.getMatchingKey(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNull("Implicit match ignored if pattern has extension", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestMappingKey(singleton("/foo.jpg"), null);
		match = key.getMatchingKey(pathHelper.getLookupPathForRequest(request), request, pathMatcher);

		assertNull("Implicit match ignored on pattern with trailing slash", match);
	}

	@Test
	public void matchRequestMethods() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingKey key = new RequestMappingKey(singleton("/foo"), null);
		RequestMappingKey match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNotNull("No method matches any method", match);

		key = new RequestMappingKey(singleton("/foo"), singleton(GET));
		match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNotNull("Exact match", match);

		key = new RequestMappingKey(singleton("/foo"), singleton(POST));
		match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNull("No match", match);
	}

	@Test
	public void matchingKeyContent() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingKey key = new RequestMappingKey(asList("/foo*", "/bar"), asList(GET, POST));
		RequestMappingKey match = key.getMatchingKey(lookupPath, request, pathMatcher);
		RequestMappingKey expected = new RequestMappingKey(singleton("/foo*"), singleton(GET));

		assertEquals("Matching RequestKey contains matched patterns and methods only", expected, match);

		key = new RequestMappingKey(asList("/**", "/foo*", "/foo"), null);
		match = key.getMatchingKey(lookupPath, request, pathMatcher);
		expected = new RequestMappingKey(asList("/foo", "/foo*", "/**"), null);

		assertEquals("Matched patterns are sorted with best match at the top", expected, match);
	}

	@Test
	public void paramsCondition() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingKey key = new RequestMappingKey(asList("/foo"), null, RequestConditionFactory.parseParams("foo=bar"), null, null);
		RequestMappingKey match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNotNull(match);

		key = new RequestMappingKey(singleton("/foo"), null, RequestConditionFactory.parseParams("foo!=bar"), null, null);
		match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNull(match);
	}

	@Test
	public void headersCondition() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("foo", "bar");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingKey key = new RequestMappingKey(singleton("/foo"), null, null, RequestConditionFactory.parseHeaders("foo=bar"), null);
		RequestMappingKey match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNotNull(match);

		key = new RequestMappingKey(singleton("/foo"), null, null, RequestConditionFactory.parseHeaders("foo!=bar"), null);
		match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNull(match);
	}

	@Test
	public void consumesCondition() {
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setContentType("text/plain");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		RequestMappingKey key = new RequestMappingKey(singleton("/foo"), null, null, null, RequestConditionFactory.parseConsumes(
				"text/plain"));
		RequestMappingKey match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNotNull(match);

		key = new RequestMappingKey(singleton("/foo"), null, null, null, RequestConditionFactory.parseConsumes(
				"application/xml"));
		match = key.getMatchingKey(lookupPath, request, pathMatcher);

		assertNull(match);
	}

	private RequestMappingKey createKeyFromPatterns(String... patterns) {
		return new RequestMappingKey(asList(patterns), null);
	}

}