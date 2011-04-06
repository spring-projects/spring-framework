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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestConditionFactory;
import org.springframework.web.servlet.mvc.method.annotation.RequestKey;
import org.springframework.web.util.UrlPathHelper;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestKeyTests {

	@Test
	public void equals() {
		RequestKey key1 = new RequestKey(asList("/foo"), asList(GET), null, null);
		RequestKey key2 = new RequestKey(asList("/foo"), asList(GET), null, null);

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void equalsPrependSlash() {
		RequestKey key1 = new RequestKey(asList("/foo"), asList(GET), null, null);
		RequestKey key2 = new RequestKey(asList("foo"), asList(GET), null, null);

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	public void combinePatterns() {
		AntPathMatcher pathMatcher = new AntPathMatcher();

		RequestKey key1 = createKeyFromPatterns("/t1", "/t2");
		RequestKey key2 = createKeyFromPatterns("/m1", "/m2");
		RequestKey key3 = createKeyFromPatterns("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2");
		assertEquals(key3, key1.combine(key2, pathMatcher));

		key1 = createKeyFromPatterns("/t1");
		key2 = createKeyFromPatterns(new String[] {});
		key3 = createKeyFromPatterns("/t1");
		assertEquals(key3, key1.combine(key2, pathMatcher));

		key1 = createKeyFromPatterns(new String[] {});
		key2 = createKeyFromPatterns("/m1");
		key3 = createKeyFromPatterns("/m1");
		assertEquals(key3, key1.combine(key2, pathMatcher));

		key1 = createKeyFromPatterns(new String[] {});
		key2 = createKeyFromPatterns(new String[] {});
		key3 = createKeyFromPatterns("/");
		assertEquals(key3, key1.combine(key2, pathMatcher));
	
	}
	
	@Test
	public void matchPatternsToRequest() {
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		PathMatcher pathMatcher = new AntPathMatcher();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		RequestKey key = new RequestKey(asList("/foo"), null, null, null);
		RequestKey match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull(match);

		request = new MockHttpServletRequest("GET", "/foo/bar");
		key = new RequestKey(asList("/foo/*"), null, null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull("Pattern match", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestKey(asList("/foo"), null, null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull("Implicit match by extension", match);
		assertEquals("Contains matched pattern", "/foo.*", match.getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo/");
		key = new RequestKey(asList("/foo"), null, null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull("Implicit match by trailing slash", match);
		assertEquals("Contains matched pattern", "/foo/", match.getPatterns().iterator().next());

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestKey(asList("/foo.jpg"), null, null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNull("Implicit match ignored if pattern has extension", match);

		request = new MockHttpServletRequest("GET", "/foo.html");
		key = new RequestKey(asList("/foo.jpg"), null, null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNull("Implicit match ignored on pattern with trailing slash", match);
	}

	@Test
	public void matchRequestMethods() {
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		RequestKey key = new RequestKey(asList("/foo"), null, null, null);
		RequestKey match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull("No method matches any method", match);

		key = new RequestKey(asList("/foo"), asList(GET), null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull("Exact match", match);

		key = new RequestKey(asList("/foo"), asList(POST), null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNull("No match", match);
	}

	@Test
	public void testMatchingKeyContent() {
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

		RequestKey key = new RequestKey(asList("/foo*", "/bar"), asList(GET, POST), null, null);
		RequestKey match = key.getMatchingKey(request, pathMatcher, urlPathHelper);
		RequestKey expected = new RequestKey(asList("/foo*"), asList(GET), null, null);

		assertEquals("Matching RequestKey contains matched patterns and methods only", expected, match);

		key = new RequestKey(asList("/**", "/foo*", "/foo"), null, null, null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);
		expected = new RequestKey(asList("/foo", "/foo*", "/**"), null, null, null);

		assertEquals("Matched patterns are sorted with best match at the top", expected, match);

	}

	@Test
	public void testParamConditions() {
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("foo", "bar");

		RequestKey key = new RequestKey(asList("/foo"), null, RequestConditionFactory.parseParams("foo=bar"), null);
		RequestKey match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull(match);

		key = new RequestKey(asList("/foo"), null, RequestConditionFactory.parseParams("foo!=bar"), null);
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNull(match);
	}

	@Test
	public void testHeaderConditions() {
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		PathMatcher pathMatcher = new AntPathMatcher();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.addHeader("foo", "bar");

		RequestKey key = new RequestKey(asList("/foo"), null, null, RequestConditionFactory.parseHeaders("foo=bar"));
		RequestKey match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNotNull(match);

		key = new RequestKey(asList("/foo"), null, null, RequestConditionFactory.parseHeaders("foo!=bar"));
		match = key.getMatchingKey(request, pathMatcher, urlPathHelper);

		assertNull(match);
	}

	@Test
	public void testCreateFromServletRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		RequestKey key = RequestKey.createFromServletRequest(request, new UrlPathHelper());
		assertEquals(new RequestKey(asList("/foo"), asList(RequestMethod.GET), null, null), key);
	}

	private RequestKey createKeyFromPatterns(String... patterns) {
		return new RequestKey(asList(patterns), null, null, null);
	}

}
