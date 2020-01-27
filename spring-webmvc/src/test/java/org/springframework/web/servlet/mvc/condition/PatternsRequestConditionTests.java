/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class PatternsRequestConditionTests {

	@Test
	public void prependSlash() {
		PatternsRequestCondition c = new PatternsRequestCondition("foo");
		assertThat(c.getPatterns().iterator().next()).isEqualTo("/foo");
	}

	@Test
	public void prependNonEmptyPatternsOnly() {
		PatternsRequestCondition c = new PatternsRequestCondition("");
		assertThat(c.getPatterns().iterator().next()).as("Do not prepend empty patterns (SPR-8255)").isEqualTo("");
	}

	@Test
	public void combineEmptySets() {
		PatternsRequestCondition c1 = new PatternsRequestCondition();
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertThat(c1.combine(c2)).isEqualTo(new PatternsRequestCondition(""));
	}

	@Test
	public void combineOnePatternWithEmptySet() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/type1", "/type2");
		PatternsRequestCondition c2 = new PatternsRequestCondition();

		assertThat(c1.combine(c2)).isEqualTo(new PatternsRequestCondition("/type1", "/type2"));

		c1 = new PatternsRequestCondition();
		c2 = new PatternsRequestCondition("/method1", "/method2");

		assertThat(c1.combine(c2)).isEqualTo(new PatternsRequestCondition("/method1", "/method2"));
	}

	@Test
	public void combineMultiplePatterns() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/t1", "/t2");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/m1", "/m2");

		assertThat(c1.combine(c2)).isEqualTo(new PatternsRequestCondition("/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"));
	}

	@Test
	public void matchDirectPath() {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(new MockHttpServletRequest("GET", "/foo"));

		assertThat(match).isNotNull();
	}

	@Test
	public void matchPattern() {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(new MockHttpServletRequest("GET", "/foo/bar"));

		assertThat(match).isNotNull();
	}

	@Test
	public void matchSortPatterns() {
		PatternsRequestCondition condition = new PatternsRequestCondition("/**", "/foo/bar", "/foo/*");
		PatternsRequestCondition match = condition.getMatchingCondition(new MockHttpServletRequest("GET", "/foo/bar"));
		PatternsRequestCondition expected = new PatternsRequestCondition("/foo/bar", "/foo/*", "/**");

		assertThat(match).isEqualTo(expected);
	}

	@Test
	public void matchSuffixPattern() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.html");

		PatternsRequestCondition condition = new PatternsRequestCondition("/{foo}");
		PatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/{foo}.*");

		boolean useSuffixPatternMatch = false;
		condition = new PatternsRequestCondition(new String[] {"/{foo}"}, null, null, useSuffixPatternMatch, false);
		match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/{foo}");
	}

	@Test // SPR-8410
	public void matchSuffixPatternUsingFileExtensions() {
		String[] patterns = new String[] {"/jobs/{jobName}"};
		List<String> extensions = Arrays.asList("json");
		PatternsRequestCondition condition = new PatternsRequestCondition(patterns, null, null, true, false, extensions);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs/my.job");
		PatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/jobs/{jobName}");

		request = new MockHttpServletRequest("GET", "/jobs/my.job.json");
		match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).isEqualTo("/jobs/{jobName}.json");
	}

	@Test
	public void matchSuffixPatternUsingFileExtensions2() {
		PatternsRequestCondition condition1 = new PatternsRequestCondition(
				new String[] {"/prefix"}, null, null, true, false, Arrays.asList("json"));

		PatternsRequestCondition condition2 = new PatternsRequestCondition(
				new String[] {"/suffix"}, null, null, true, false, null);

		PatternsRequestCondition combined = condition1.combine(condition2);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/prefix/suffix.json");
		PatternsRequestCondition match = combined.getMatchingCondition(request);

		assertThat(match).isNotNull();
	}

	@Test
	public void matchTrailingSlash() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/");

		PatternsRequestCondition condition = new PatternsRequestCondition("/foo");
		PatternsRequestCondition match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).as("Should match by default").isEqualTo("/foo/");

		condition = new PatternsRequestCondition(new String[] {"/foo"}, null, null, false, true);
		match = condition.getMatchingCondition(request);

		assertThat(match).isNotNull();
		assertThat(match.getPatterns().iterator().next()).as("Trailing slash should be insensitive to useSuffixPatternMatch settings (SPR-6164, SPR-5636)").isEqualTo("/foo/");

		condition = new PatternsRequestCondition(new String[] {"/foo"}, null, null, false, false);
		match = condition.getMatchingCondition(request);

		assertThat(match).isNull();
	}

	@Test
	public void matchPatternContainsExtension() {
		PatternsRequestCondition condition = new PatternsRequestCondition("/foo.jpg");
		PatternsRequestCondition match = condition.getMatchingCondition(new MockHttpServletRequest("GET", "/foo.html"));

		assertThat(match).isNull();
	}

	@Test // gh-22543
	public void matchWithEmptyPatterns() {
		PatternsRequestCondition condition = new PatternsRequestCondition();
		assertThat(condition).isEqualTo(new PatternsRequestCondition(""));
		assertThat(condition.getMatchingCondition(new MockHttpServletRequest("GET", ""))).isNotNull();
		assertThat(condition.getMatchingCondition(new MockHttpServletRequest("GET", "/anything"))).isNull();

		condition = condition.combine(new PatternsRequestCondition());
		assertThat(condition).isEqualTo(new PatternsRequestCondition(""));
		assertThat(condition.getMatchingCondition(new MockHttpServletRequest("GET", ""))).isNotNull();
		assertThat(condition.getMatchingCondition(new MockHttpServletRequest("GET", "/anything"))).isNull();
	}

	@Test
	public void compareEqualPatterns() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo*");

		assertThat(c1.compareTo(c2, new MockHttpServletRequest("GET", "/foo"))).isEqualTo(0);
	}

	@Test
	public void comparePatternSpecificity() {
		PatternsRequestCondition c1 = new PatternsRequestCondition("/fo*");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo");

		assertThat(c1.compareTo(c2, new MockHttpServletRequest("GET", "/foo"))).isEqualTo(1);
	}

	@Test
	public void compareNumberOfMatchingPatterns() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/foo.html");

		PatternsRequestCondition c1 = new PatternsRequestCondition("/foo", "*.jpeg");
		PatternsRequestCondition c2 = new PatternsRequestCondition("/foo", "*.html");

		PatternsRequestCondition match1 = c1.getMatchingCondition(request);
		PatternsRequestCondition match2 = c2.getMatchingCondition(request);

		assertThat(match1.compareTo(match2, request)).isEqualTo(1);
	}

}
