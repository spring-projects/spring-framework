/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PatternsRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
class PatternsRequestConditionTests {

	@Test
	void prependSlash() {
		assertThat(new PatternsRequestCondition("foo").getPatterns().iterator().next())
				.isEqualTo("/foo");
	}

	@Test
	void prependNonEmptyPatternsOnly() {
		assertThat(new PatternsRequestCondition("").getPatterns().iterator().next())
				.as("Do not prepend empty patterns (SPR-8255)")
				.isEqualTo("");
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

		assertThat(c3).isSameAs(c1);
		assertThat(c1.getPatterns()).isSameAs(c2.getPatterns()).containsExactly("");
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
	@SuppressWarnings("deprecation")
	void matchSuffixPattern() {
		MockHttpServletRequest request = initRequest("/foo.html");

		boolean useSuffixPatternMatch = true;
		PatternsRequestCondition condition =
				new PatternsRequestCondition(new String[] {"/{foo}"}, null, null, useSuffixPatternMatch, true);
		PatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/{foo}.*");

		useSuffixPatternMatch = false;
		condition = new PatternsRequestCondition(
				new String[] {"/{foo}"}, null, null, useSuffixPatternMatch, false);
		match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/{foo}");
	}

	@Test // SPR-8410
	@SuppressWarnings("deprecation")
	void matchSuffixPatternUsingFileExtensions() {
		PatternsRequestCondition condition = new PatternsRequestCondition(
				new String[] {"/jobs/{jobName}"}, null, null, true, false, Collections.singletonList("json"));

		MockHttpServletRequest request = initRequest("/jobs/my.job");
		PatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/jobs/{jobName}");

		request = initRequest("/jobs/my.job.json");
		match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/jobs/{jobName}.json");
	}

	@Test
	@SuppressWarnings("deprecation")
	void matchSuffixPatternUsingFileExtensions2() {
		PatternsRequestCondition condition1 = new PatternsRequestCondition(
				new String[] {"/prefix"}, null, null, true, false, Collections.singletonList("json"));

		PatternsRequestCondition condition2 = new PatternsRequestCondition(
				new String[] {"/suffix"}, null, null, true, false, null);

		PatternsRequestCondition combined = condition1.combine(condition2);

		MockHttpServletRequest request = initRequest("/prefix/suffix.json");
		PatternsRequestCondition match = combined.getMatchingCondition(request);

		assertThat(match).isNotNull();
	}

	@Test
	void matchTrailingSlash() {
		MockHttpServletRequest request = initRequest("/foo/");

		PatternsRequestCondition condition = new PatternsRequestCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).as("Should match by default").isEqualTo("/foo/");

		condition = new PatternsRequestCondition(new String[] {"/foo"}, true, null);
		match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next())
				.as("Trailing slash should be insensitive to useSuffixPatternMatch settings (SPR-6164, SPR-5636)")
				.isEqualTo("/foo/");

		condition = new PatternsRequestCondition(new String[] {"/foo"}, false, null);
		match = condition.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	void matchPatternContainsExtension() {
		MockHttpServletRequest request = initRequest("/foo.html");
		PatternsRequestCondition match = new PatternsRequestCondition("/foo.jpg").getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test // gh-22543
	void matchWithEmptyPatterns() {
		PatternsRequestCondition condition = new PatternsRequestCondition();
		assertThat(condition.getMatchingCondition(initRequest(""))).isNotNull();
		assertThat(condition.getMatchingCondition(initRequest("/anything"))).isNull();

		condition = condition.combine(new PatternsRequestCondition());
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
