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

package org.springframework.web.reactive.result.condition;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link PatternsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class PatternsRequestConditionTests {

	@Test
	public void prependSlash() {
		PatternsRequestCondition c = new PatternsRequestCondition("foo");
		assertEquals("/foo", c.getPatterns().iterator().next());
	}

	@Test
	public void prependNonEmptyPatternsOnly() {
		PatternsRequestCondition c = new PatternsRequestCondition("");
		assertEquals("Do not prepend empty patterns (SPR-8255)", "", c.getPatterns().iterator().next());
	}

	@Test
	public void combineEmptySets() {
		PatternsRequestCondition c1 = new PatternsRequestCondition();
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertEquals(new PatternsRequestCondition(""), c1.combine(c2));
	}

	@Test
	public void combineOnePatternWithEmptySet() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/type1", "/type2");
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertEquals(new PatternsRequestCondition("/type1", "/type2"), c1.combine(c2));

		c1 = new PatternsRequestCondition();
		c2 = new PatternsRequestCondition("/method1", "/method2");

		assertEquals(new PatternsRequestCondition("/method1", "/method2"), c1.combine(c2));
	}

	@Test
	public void combineMultiplePatterns() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/t1", "/t2");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/m1", "/m2");

		assertEquals(new PatternsRequestCondition("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"), c1.combine(c2));
	}

	@Test
	public void matchDirectPath() throws Exception {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(createExchange("/foo"));

		assertNotNull(match);
	}

	@Test
	public void matchPattern() throws Exception {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(createExchange("/foo/bar"));

		assertNotNull(match);
	}

	@Test
	public void matchSortPatterns() throws Exception {
		PatternsRequestCondition condition = new PatternsRequestCondition("/**", "/foo/bar", "/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(createExchange("/foo/bar"));
		PatternsRequestCondition expected = new PatternsRequestCondition("/foo/bar", "/foo/*", "/**");

		assertEquals(expected, match);
	}

	@Test
	public void matchSuffixPattern() throws Exception {
		ServerWebExchange exchange = createExchange("/foo.html");

		PatternsRequestCondition condition = new PatternsRequestCondition("/{foo}");
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("/{foo}.*", match.getPatterns().iterator().next());

		condition = new PatternsRequestCondition(new String[] {"/{foo}"}, null, null, false, false, null);
		match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("/{foo}", match.getPatterns().iterator().next());
	}

	// SPR-8410

	@Test
	public void matchSuffixPatternUsingFileExtensions() throws Exception {
		String[] patterns = new String[] {"/jobs/{jobName}"};
		Set<String> extensions = Collections.singleton("json");
		PatternsRequestCondition condition = new PatternsRequestCondition(patterns, null, null, true, false, extensions);

		ServerWebExchange exchange = createExchange("/jobs/my.job");
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("/jobs/{jobName}", match.getPatterns().iterator().next());

		exchange = createExchange("/jobs/my.job.json");
		match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("/jobs/{jobName}.json", match.getPatterns().iterator().next());
	}

	@Test
	public void matchSuffixPatternUsingFileExtensions2() throws Exception {
		PatternsRequestCondition condition1 = new PatternsRequestCondition(
				new String[] {"/prefix"}, null, null, true, false, Collections.singleton("json"));

		PatternsRequestCondition condition2 = new PatternsRequestCondition(
				new String[] {"/suffix"}, null, null, true, false, null);

		PatternsRequestCondition combined = condition1.combine(condition2);

		ServerWebExchange exchange = createExchange("/prefix/suffix.json");
		PatternsRequestCondition match = combined.getMatchingCondition(exchange);

		assertNotNull(match);
	}

	@Test
	public void matchTrailingSlash() throws Exception {
		ServerWebExchange exchange = createExchange("/foo/");

		PatternsRequestCondition condition = new PatternsRequestCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("Should match by default", "/foo/", match.getPatterns().iterator().next());

		condition = new PatternsRequestCondition(new String[] {"/foo"}, null, null, false, true, null);
		match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("Trailing slash should be insensitive to useSuffixPatternMatch settings (SPR-6164, SPR-5636)",
				"/foo/", match.getPatterns().iterator().next());

		condition = new PatternsRequestCondition(new String[] {"/foo"}, null, null, false, false, null);
		match = condition.getMatchingCondition(exchange);

		assertNull(match);
	}

	@Test
	public void matchPatternContainsExtension() throws Exception {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo.jpg");
		PatternsRequestCondition match = condition.getMatchingCondition(createExchange("/foo.html"));

		assertNull(match);
	}

	@Test
	public void compareEqualPatterns() throws Exception {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo*");

		assertEquals(0, c1.compareTo(c2, createExchange("/foo")));
	}

	@Test
	public void comparePatternSpecificity() throws Exception {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/fo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo");

		assertEquals(1, c1.compareTo(c2, createExchange("/foo")));
	}

	@Test
	public void compareNumberOfMatchingPatterns() throws Exception {
		ServerWebExchange exchange = createExchange("/foo.html");

		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo", "*.jpeg");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo", "*.html");

		PatternsRequestCondition match1 = c1.getMatchingCondition(exchange);
		PatternsRequestCondition match2 = c2.getMatchingCondition(exchange);

		assertNotNull(match1);
		assertEquals(1, match1.compareTo(match2, exchange));
	}


	private ServerWebExchange createExchange(String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, path);
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}

}
