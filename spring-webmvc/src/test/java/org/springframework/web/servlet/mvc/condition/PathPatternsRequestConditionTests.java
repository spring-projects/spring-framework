/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

/**
 * Tests for {@link PathPatternsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
class PathPatternsRequestConditionTests {

	private static final PathPatternParser parser = new PathPatternParser();


	@Test
	void prependSlash() {
		assertThat(createCondition("foo").getPatternValues()).containsExactly("/foo");
	}

	@Test
	void prependNonEmptyPatternsOnly() {
		assertThat(createCondition("").getPatternValues()).first(STRING)
				.as("Do not prepend empty patterns (SPR-8255)").isEmpty();
	}

	@Test
	void getDirectUrls() {
		PathPatternsRequestCondition condition = createCondition("/something", "/else/**");
		assertThat(condition.getDirectPaths()).containsExactly("/something");
	}

	@Test
	void combineEmptySets() {
		PathPatternsRequestCondition c1 = createCondition();
		PathPatternsRequestCondition c2 = createCondition();
		PathPatternsRequestCondition c3 = c1.combine(c2);

		assertThat(c1.getPatternValues()).isSameAs(c2.getPatternValues()).containsExactly("");
		assertThat(c3.getPatternValues()).containsExactly("", "/");
	}

	@Test
	void combineOnePatternWithEmptySet() {
		PathPatternsRequestCondition c1 = createCondition("/type1", "/type2");
		PathPatternsRequestCondition c2 = createCondition();

		assertThat(c1.combine(c2)).isEqualTo(createCondition("/type1", "/type2"));

		c1 = createCondition();
		c2 = createCondition("/method1", "/method2");

		assertThat(c1.combine(c2)).isEqualTo(createCondition("/method1", "/method2"));
	}

	@Test
	void combineMultiplePatterns() {
		PathPatternsRequestCondition c1 = createCondition("/t1", "/t2");
		PathPatternsRequestCondition c2 = createCondition("/m1", "/m2");

		assertThat(c1.combine(c2)).isEqualTo(createCondition("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"));
	}

	@Test
	void matchDirectPath() {
		MockHttpServletRequest request = createRequest("/foo");

		PathPatternsRequestCondition condition = createCondition("/foo");
		PathPatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
	}

	@Test
	void matchPattern() {
		MockHttpServletRequest request = createRequest("/foo/bar");


		PathPatternsRequestCondition condition = createCondition("/foo/*");
		PathPatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
	}

	@Test
	void matchPatternWithContextPath() {
		MockHttpServletRequest request = createRequest("/app", "/app/foo/bar");

		PathPatternsRequestCondition condition = createCondition("/foo/*");
		PathPatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
	}

	@Test
	void matchSortPatterns() {
		MockHttpServletRequest request = createRequest("/foo/bar");

		PathPatternsRequestCondition condition = createCondition("/**", "/foo/bar", "/foo/*");
		PathPatternsRequestCondition match = condition.getMatchingCondition(request);
		PathPatternsRequestCondition expected = createCondition("/foo/bar", "/foo/*", "/**");

		assertThat(match).isEqualTo(expected);
	}

	@Test
	@SuppressWarnings("deprecation")
	void matchTrailingSlash() {
		MockHttpServletRequest request = createRequest("/foo/");

		PathPatternParser patternParser = new PathPatternParser();
		patternParser.setMatchOptionalTrailingSeparator(true);

		PathPatternsRequestCondition condition = new PathPatternsRequestCondition(patternParser, "/foo");
		PathPatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatternValues()).containsExactly("/foo");

		PathPatternParser strictParser = new PathPatternParser();
		strictParser.setMatchOptionalTrailingSeparator(false);

		condition = new PathPatternsRequestCondition(strictParser, "/foo");
		match = condition.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	void matchPatternContainsExtension() {
		MockHttpServletRequest request = createRequest("/foo.html");
		PathPatternsRequestCondition match = createCondition("/foo.jpg").getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test // gh-22543
	void matchWithEmptyPatterns() {
		PathPatternsRequestCondition condition = createCondition();
		assertThat(condition.getMatchingCondition(createRequest(""))).isNotNull();
		assertThat(condition.getMatchingCondition(createRequest("/anything"))).isNull();

		condition = condition.combine(createCondition());
		assertThat(condition.getMatchingCondition(createRequest(""))).isNotNull();
		assertThat(condition.getMatchingCondition(createRequest("/anything"))).isNull();
	}

	@Test
	void compareEqualPatterns() {
		PathPatternsRequestCondition c1 = createCondition("/foo*");
		PathPatternsRequestCondition c2 = createCondition("/foo*");

		assertThat(c1.compareTo(c2, createRequest("/foo"))).isEqualTo(0);
	}

	@Test
	void comparePatternSpecificity() {
		PathPatternsRequestCondition c1 = createCondition("/fo*");
		PathPatternsRequestCondition c2 = createCondition("/foo");

		assertThat(c1.compareTo(c2, createRequest("/foo"))).isEqualTo(1);
	}

	@Test
	void compareNumberOfMatchingPatterns() {
		HttpServletRequest request = createRequest("/foo");

		PathPatternsRequestCondition c1 = createCondition("/foo", "/bar");
		PathPatternsRequestCondition c2 = createCondition("/foo", "/f*");

		PathPatternsRequestCondition match1 = c1.getMatchingCondition(request);
		PathPatternsRequestCondition match2 = c2.getMatchingCondition(request);

		assertThat(match1.compareTo(match2, request)).isEqualTo(1);
	}


	private MockHttpServletRequest createRequest(String requestURI) {
		return createRequest("", requestURI);
	}

	private MockHttpServletRequest createRequest(String contextPath, String requestURI) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
		request.setContextPath(contextPath);
		ServletRequestPathUtils.parseAndCache(request);
		return request;
	}

	private PathPatternsRequestCondition createCondition(String... patterns) {
		return new PathPatternsRequestCondition(parser, patterns);
	}
}
