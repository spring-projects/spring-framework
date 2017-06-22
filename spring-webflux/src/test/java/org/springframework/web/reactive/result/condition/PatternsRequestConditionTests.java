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

import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;

/**
 * Unit tests for {@link PatternsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class PatternsRequestConditionTests {

	@Test
	public void prependSlash() {
		PatternsRequestCondition c = new PatternsRequestCondition("foo");
		assertEquals("/foo", c.getPatterns().iterator().next().getPatternString());
	}

	@Test
	public void prependNonEmptyPatternsOnly() {
		PatternsRequestCondition c = new PatternsRequestCondition("");
		assertEquals("Do not prepend empty patterns (SPR-8255)", "",
				c.getPatterns().iterator().next().getPatternString());
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
		PatternsRequestCondition match = condition.getMatchingCondition(get("/foo").toExchange());

		assertNotNull(match);
	}

	@Test
	public void matchPattern() throws Exception {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(get("/foo/bar").toExchange());

		assertNotNull(match);
	}

	@Test
	public void matchSortPatterns() throws Exception {
		PatternsRequestCondition condition = new PatternsRequestCondition("/*/*", "/foo/bar", "/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(get("/foo/bar").toExchange());
		PatternsRequestCondition expected = new PatternsRequestCondition("/foo/bar", "/foo/*", "/*/*");

		assertEquals(expected, match);
	}

	@Test
	public void matchTrailingSlash() throws Exception {
		MockServerWebExchange exchange = get("/foo/").toExchange();

		PatternsRequestCondition condition = new PatternsRequestCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("Should match by default", "/foo",
				match.getPatterns().iterator().next().getPatternString());

		condition = new PatternsRequestCondition(new String[] {"/foo"}, null);
		match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("Trailing slash should be insensitive to useSuffixPatternMatch settings (SPR-6164, SPR-5636)",
				"/foo", match.getPatterns().iterator().next().getPatternString());

		PathPatternParser parser = new PathPatternParser();
		parser.setMatchOptionalTrailingSlash(false);
		condition = new PatternsRequestCondition(new String[] {"/foo"}, parser);
		match = condition.getMatchingCondition(get("/foo/").toExchange());

		assertNull(match);
	}

	@Test
	public void matchPatternContainsExtension() throws Exception {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo.jpg");
		PatternsRequestCondition match = condition.getMatchingCondition(get("/foo.html").toExchange());

		assertNull(match);
	}

	@Test
	public void compareEqualPatterns() throws Exception {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo*");

		assertEquals(0, c1.compareTo(c2, get("/foo").toExchange()));
	}

	@Test
	public void comparePatternSpecificity() throws Exception {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/fo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo");

		assertEquals(1, c1.compareTo(c2, get("/foo").toExchange()));
	}

	@Test
	public void compareNumberOfMatchingPatterns() throws Exception {
		ServerWebExchange exchange = get("/foo.html").toExchange();

		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo.*", "/foo.jpeg");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo.*", "/foo.html");

		PatternsRequestCondition match1 = c1.getMatchingCondition(exchange);
		PatternsRequestCondition match2 = c2.getMatchingCondition(exchange);

		assertNotNull(match1);
		assertEquals(1, match1.compareTo(match2, exchange));
	}

}
