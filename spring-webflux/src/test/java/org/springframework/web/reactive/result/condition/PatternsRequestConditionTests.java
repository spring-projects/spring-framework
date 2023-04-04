/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

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
		assertThat(c.getPatterns().iterator().next().getPatternString())
				.as("Do not prepend empty patterns (SPR-8255)").isEmpty();
	}

	@Test
	public void combineEmptySets() {
		PatternsRequestCondition c1 = new PatternsRequestCondition();
		PatternsRequestCondition c2 = new PatternsRequestCondition();
		PatternsRequestCondition c3 = c1.combine(c2);

		assertThat(c1.getPatterns()).isSameAs(c2.getPatterns()).containsExactly(this.parser.parse(""));
		assertThat(c3.toString()).isEqualTo("[/ || ]");
	}

	@Test
	public void combineOnePatternWithEmptySet() {
		PatternsRequestCondition c1 = createPatternsCondition("/type1", "/type2");
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertThat(c1.combine(c2)).isEqualTo(createPatternsCondition("/type1", "/type2"));

		c1 = new PatternsRequestCondition();
		c2 = createPatternsCondition("/method1", "/method2");

		assertThat(c1.combine(c2)).isEqualTo(createPatternsCondition("/method1", "/method2"));
	}

	@Test
	public void combineMultiplePatterns() {
		PatternsRequestCondition c1 = createPatternsCondition("/t1", "/t2");
		PatternsRequestCondition c2 = createPatternsCondition("/m1", "/m2");

		assertThat(c1.combine(c2)).isEqualTo(createPatternsCondition("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"));
	}

	@Test
	public void matchDirectPath() {
		PatternsRequestCondition condition = createPatternsCondition("/foo");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertThat(match).isNotNull();
	}

	@Test
	public void matchPattern() {
		PatternsRequestCondition condition = createPatternsCondition("/foo/*");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo/bar"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertThat(match).isNotNull();
	}

	@Test
	public void matchSortPatterns() {
		PatternsRequestCondition condition = createPatternsCondition("/*/*", "/foo/bar", "/foo/*");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo/bar"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);
		PatternsRequestCondition expected = createPatternsCondition("/foo/bar", "/foo/*", "/*/*");

		assertThat(match).isEqualTo(expected);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void matchTrailingSlash() {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo/"));

		PathPatternParser patternParser = new PathPatternParser();
		patternParser.setMatchOptionalTrailingSeparator(true);

		PatternsRequestCondition condition = new PatternsRequestCondition(patternParser.parse("/foo"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next().getPatternString())
				.as("Should match by default")
				.isEqualTo("/foo");

		condition = new PatternsRequestCondition(patternParser.parse("/foo"));
		match = condition.getMatchingCondition(exchange);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next().getPatternString())
				.as("Trailing slash should be insensitive to useSuffixPatternMatch settings (SPR-6164, SPR-5636)")
				.isEqualTo("/foo");

		PathPatternParser parser = new PathPatternParser();
		parser.setMatchOptionalTrailingSeparator(false);
		condition = new PatternsRequestCondition(parser.parse("/foo"));
		match = condition.getMatchingCondition(MockServerWebExchange.from(get("/foo/")));

		assertThat(match).isNull();
	}

	@Test
	public void matchPatternContainsExtension() {
		PatternsRequestCondition condition = createPatternsCondition("/foo.jpg");
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/foo.html"));
		PatternsRequestCondition match = condition.getMatchingCondition(exchange);

		assertThat(match).isNull();
	}

	@Test // gh-22543
	public void matchWithEmptyPatterns() {
		PatternsRequestCondition condition = new PatternsRequestCondition();
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("")))).isNotNull();
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/anything")))).isNull();

		condition = condition.combine(new PatternsRequestCondition());
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("")))).isNotNull();
		assertThat(condition.getMatchingCondition(MockServerWebExchange.from(get("/anything")))).isNull();
	}

	@Test
	public void compareToConsistentWithEquals() {
		PatternsRequestCondition c1 = createPatternsCondition("/foo*");
		PatternsRequestCondition c2 = createPatternsCondition("/foo*");

		assertThat(c1.compareTo(c2, MockServerWebExchange.from(get("/foo")))).isEqualTo(0);
	}

	@Test
	public void equallyMatchingPatternsAreBothPresent() {
		PatternsRequestCondition c = createPatternsCondition("/a", "/b");
		assertThat(c.getPatterns()).hasSize(2);
		Iterator<PathPattern> itr = c.getPatterns().iterator();
		assertThat(itr.next().getPatternString()).isEqualTo("/a");
		assertThat(itr.next().getPatternString()).isEqualTo("/b");
	}

	@Test
	public void comparePatternSpecificity() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo"));

		PatternsRequestCondition c1 = createPatternsCondition("/fo*");
		PatternsRequestCondition c2 = createPatternsCondition("/foo");

		assertThat(c1.compareTo(c2, exchange)).isEqualTo(1);

		c1 = createPatternsCondition("/fo*");
		c2 = createPatternsCondition("/*oo");

		assertThat(c1.compareTo(c2, exchange))
				.as("Patterns are equally specific even if not the same")
				.isEqualTo(0);
	}

	@Test
	public void compareNumberOfMatchingPatterns() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo.html"));

		PatternsRequestCondition c1 = createPatternsCondition("/foo.*", "/foo.jpeg");
		PatternsRequestCondition c2 = createPatternsCondition("/foo.*", "/foo.html");

		PatternsRequestCondition match1 = c1.getMatchingCondition(exchange);
		PatternsRequestCondition match2 = c2.getMatchingCondition(exchange);

		assertThat(match1).isNotNull();
		assertThat(match1.compareTo(match2, exchange)).isEqualTo(1);
	}

	private PatternsRequestCondition createPatternsCondition(String... patterns) {
		return new PatternsRequestCondition(Arrays
				.stream(patterns)
				.map(this.parser::parse)
				.toList());
	}

}
