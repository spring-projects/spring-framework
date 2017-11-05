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

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.junit.Test;

import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
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

	private final PathPatternParser parser = new PathPatternParser();

	@Test
	public void prependNonEmptyPatternsOnly() {
		PatternsRequestCondition c = createPatternsCondition("");
		assertEquals("Do not prepend empty patterns (SPR-8255)", "",
				c.getPatterns().iterator().next().getPatternString());
	}

	@Test
	public void combineEmptySets() {
		PatternsRequestCondition c1 = new PatternsRequestCondition();
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertEquals(createPatternsCondition(), c1.combine(c2));
	}

	@Test
	public void combineOnePatternWithEmptySet() {
		PatternsRequestCondition c1 = createPatternsCondition("/type1", "/type2");
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertEquals(createPatternsCondition("/type1", "/type2"), c1.combine(c2));

		c1 = new PatternsRequestCondition();
		c2 = createPatternsCondition("/method1", "/method2");

		assertEquals(createPatternsCondition("/method1", "/method2"), c1.combine(c2));
	}

	@Test
	public void combineMultiplePatterns() {
		PatternsRequestCondition c1 = createPatternsCondition("/t1", "/t2");
		PatternsRequestCondition c2 = createPatternsCondition("/m1", "/m2");

		assertEquals(createPatternsCondition("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"), c1.combine(c2));
	}

	@Test
	public void matchDirectPath() throws Exception {
		PatternsRequestCondition condition = createPatternsCondition("/foo");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
	}

	@Test
	public void matchPattern() throws Exception {
		PatternsRequestCondition condition = createPatternsCondition("/foo/*");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo/bar"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
	}

	@Test
	public void matchSortPatterns() throws Exception {
		PatternsRequestCondition condition = createPatternsCondition("/*/*", "/foo/bar", "/foo/*");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo/bar"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);
		PatternsRequestCondition expected = createPatternsCondition("/foo/bar", "/foo/*", "/*/*");

		assertEquals(expected, match);
	}

	@Test
	public void matchTrailingSlash() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo/"));

		PatternsRequestCondition condition = createPatternsCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("Should match by default", "/foo",
				match.getPatterns().iterator().next().getPatternString());

		condition = createPatternsCondition("/foo");
		match = condition.getMatchingCondition(exchange);

		assertNotNull(match);
		assertEquals("Trailing slash should be insensitive to useSuffixPatternMatch settings (SPR-6164, SPR-5636)",
				"/foo", match.getPatterns().iterator().next().getPatternString());

		PathPatternParser parser = new PathPatternParser();
		parser.setMatchOptionalTrailingSeparator(false);
		condition = new PatternsRequestCondition(parser.parse("/foo"));
		match = condition.getMatchingCondition(MockServerWebExchange.from(get("/foo/")));

		assertNull(match);
	}

	@Test
	public void matchPatternContainsExtension() throws Exception {
		PatternsRequestCondition condition = createPatternsCondition("/foo.jpg");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo.html"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertNull(match);
	}

	@Test
	public void compareToConsistentWithEquals() throws Exception {
		PatternsRequestCondition c1 = createPatternsCondition("/foo*");
		PatternsRequestCondition c2 = createPatternsCondition("/foo*");

		assertEquals(0, c1.compareTo(c2, MockServerWebExchange.from(get("/foo"))));
	}

	@Test
	public void equallyMatchingPatternsAreBothPresent() throws Exception {
		PatternsRequestCondition c = createPatternsCondition("/a", "/b");
		assertEquals(2, c.getPatterns().size());
		Iterator<PathPattern> itr = c.getPatterns().iterator();
		assertEquals("/a", itr.next().getPatternString());
		assertEquals("/b", itr.next().getPatternString());
	}

	@Test
	public void comparePatternSpecificity() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo"));

		PatternsRequestCondition c1 = createPatternsCondition("/fo*");
		PatternsRequestCondition c2 = createPatternsCondition("/foo");

		assertEquals(1, c1.compareTo(c2, exchange));

		c1 = createPatternsCondition("/fo*");
		c2 = createPatternsCondition("/*oo");

		assertEquals("Patterns are equally specific even if not the same", 0, c1.compareTo(c2, exchange));
	}

	@Test
	public void compareNumberOfMatchingPatterns() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo.html"));

		PatternsRequestCondition c1 = createPatternsCondition("/foo.*", "/foo.jpeg");
		PatternsRequestCondition c2 = createPatternsCondition("/foo.*", "/foo.html");

		PatternsRequestCondition match1 = c1.getMatchingCondition(exchange);
		PatternsRequestCondition match2 = c2.getMatchingCondition(exchange);

		assertNotNull(match1);
		assertEquals(1, match1.compareTo(match2, exchange));
	}

	private PatternsRequestCondition createPatternsCondition(String... patterns) {
		return new PatternsRequestCondition(Arrays
				.stream(patterns)
				.map(rawPattern -> this.parser.parse(rawPattern))
				.collect(Collectors.toList()));
	}

}
