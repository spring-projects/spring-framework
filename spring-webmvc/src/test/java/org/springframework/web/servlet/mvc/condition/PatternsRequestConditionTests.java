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
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

/**
 * Tests for {@link PatternsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("removal")
class PatternsRequestConditionTests {

	@Test
	void prependSlash() {
		assertThat(new PatternsRequestCondition("foo").getPatterns()).containsExactly("/foo");
	}

	@Test
	void prependNonEmptyPatternsOnly() {
		assertThat(new PatternsRequestCondition("").getPatterns()).first(STRING)
				.as("Do not prepend empty patterns (SPR-8255)").isEmpty();
	}

	@Test
	void getDirectUrls() {
		PatternsRequestCondition condition = new PatternsRequestCondition("/something", "/else/**");
		assertThat(condition.getDirectPaths()).containsExactly("/something");
	}

	@Test
	void combineEmptySets() {
		PatternsRequestCondition c1 = new PatternsRequestCondition();
		PatternsRequestCondition c2 = new PatternsRequestCondition();
		PatternsRequestCondition c3 = c1.combine(c2);

		assertThat(c1.getPatterns()).isSameAs(c2.getPatterns()).containsExactly("");
		assertThat(c3.getPatterns()).containsExactly("", "/");
	}

	@Test
	void combineOnePatternWithEmptySet() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/type1", "/type2");
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertThat(c1.combine(c2)).isEqualTo(new PatternsRequestCondition("/type1", "/type2"));

		c1 = new PatternsRequestCondition();
		c2 = new PatternsRequestCondition("/method1", "/method2");

		assertThat(c1.combine(c2)).isEqualTo(new PatternsRequestCondition("/method1", "/method2"));
	}

	@Test
	void combineMultiplePatterns() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/t1", "/t2");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/m1", "/m2");

		assertThat(c1.combine(c2)).isEqualTo(new PatternsRequestCondition("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"));
	}

	@Test
	void matchDirectPath() {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(initRequest("/foo"));

		assertThat(match).isNotNull();
	}

	@Test
	void matchPattern() {
		MockHttpServletRequest request = initRequest("/foo/bar");

		PatternsRequestCondition condition = new PatternsRequestCondition("/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
	}

	@Test
	void matchSortPatterns() {
		MockHttpServletRequest request = initRequest("/foo/bar");

		PatternsRequestCondition condition = new PatternsRequestCondition("/**", "/foo/bar", "/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(request);
		PatternsRequestCondition expected = new PatternsRequestCondition("/foo/bar", "/foo/*", "/**");

		assertThat(match).isEqualTo(expected);
	}

	@Test
	void matchPatternContainsExtension() {
		MockHttpServletRequest request = initRequest("/foo.html");
		PatternsRequestCondition match = new PatternsRequestCondition("/foo.jpg").getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test // gh-22543
	void matchWithEmptyPatterns() {
		PatternsRequestCondition condition = new PatternsRequestCondition().combine(new PatternsRequestCondition());
		assertThat(condition.getMatchingCondition(initRequest(""))).isNotNull();
		assertThat(condition.getMatchingCondition(initRequest("/anything"))).isNull();
	}

	@Test
	void compareEqualPatterns() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo*");

		assertThat(c1.compareTo(c2, initRequest("/foo"))).isEqualTo(0);
	}

	@Test
	void comparePatternSpecificity() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/fo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo");

		assertThat(c1.compareTo(c2, initRequest("/foo"))).isEqualTo(1);
	}

	@Test
	void compareNumberOfMatchingPatterns() {
		HttpServletRequest request = initRequest("/foo.html");

		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo.html", "*.jpeg");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo.html", "*.html");

		PatternsRequestCondition match1 = c1.getMatchingCondition(request);
		PatternsRequestCondition match2 = c2.getMatchingCondition(request);

		assertThat(match1.compareTo(match2, request)).isEqualTo(1);
	}


	private MockHttpServletRequest initRequest(String requestUri) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		UrlPathHelper.defaultInstance.resolveAndCacheLookupPath(request);
		return request;
	}

}
