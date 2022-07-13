/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AntPathMatcher}.
 *
 * @author Alef Arendsen
 * @author Seth Ladd
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class AntPathMatcherTests {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();


	@Test
	void match() {
		// test exact matching
		assertThat(pathMatcher.match("test", "test")).isTrue();
		assertThat(pathMatcher.match("/test", "/test")).isTrue();
		// SPR-14141
		assertThat(pathMatcher.match("https://example.org", "https://example.org")).isTrue();
		assertThat(pathMatcher.match("/test.jpg", "test.jpg")).isFalse();
		assertThat(pathMatcher.match("test", "/test")).isFalse();
		assertThat(pathMatcher.match("/test", "test")).isFalse();

		// test matching with ?'s
		assertThat(pathMatcher.match("t?st", "test")).isTrue();
		assertThat(pathMatcher.match("??st", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "test")).isTrue();
		assertThat(pathMatcher.match("te??", "test")).isTrue();
		assertThat(pathMatcher.match("?es?", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "tes")).isFalse();
		assertThat(pathMatcher.match("tes?", "testt")).isFalse();
		assertThat(pathMatcher.match("tes?", "tsst")).isFalse();

		// test matching with *'s
		assertThat(pathMatcher.match("*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "testTest")).isTrue();
		assertThat(pathMatcher.match("test/*", "test/Test")).isTrue();
		assertThat(pathMatcher.match("test/*", "test/t")).isTrue();
		assertThat(pathMatcher.match("test/*", "test/")).isTrue();
		assertThat(pathMatcher.match("*test*", "AnothertestTest")).isTrue();
		assertThat(pathMatcher.match("*test", "Anothertest")).isTrue();
		assertThat(pathMatcher.match("*.*", "test.")).isTrue();
		assertThat(pathMatcher.match("*.*", "test.test")).isTrue();
		assertThat(pathMatcher.match("*.*", "test.test.test")).isTrue();
		assertThat(pathMatcher.match("test*aaa", "testblaaaa")).isTrue();
		assertThat(pathMatcher.match("test*", "tst")).isFalse();
		assertThat(pathMatcher.match("test*", "tsttest")).isFalse();
		assertThat(pathMatcher.match("test*", "test/")).isFalse();
		assertThat(pathMatcher.match("test*", "test/t")).isFalse();
		assertThat(pathMatcher.match("test/*", "test")).isFalse();
		assertThat(pathMatcher.match("*test*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*test", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*.*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "test")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "testblaaab")).isFalse();

		// test matching with ?'s and /'s
		assertThat(pathMatcher.match("/?", "/a")).isTrue();
		assertThat(pathMatcher.match("/?/a", "/a/a")).isTrue();
		assertThat(pathMatcher.match("/a/?", "/a/b")).isTrue();
		assertThat(pathMatcher.match("/??/a", "/aa/a")).isTrue();
		assertThat(pathMatcher.match("/a/??", "/a/bb")).isTrue();
		assertThat(pathMatcher.match("/?", "/a")).isTrue();

		// test matching with **'s
		assertThat(pathMatcher.match("/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/*/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/**/*", "/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla")).isTrue();
		assertThat(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla/bla")).isTrue();
		assertThat(pathMatcher.match("/**/test", "/bla/bla/test")).isTrue();
		assertThat(pathMatcher.match("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla")).isTrue();
		assertThat(pathMatcher.match("/bla*bla/test", "/blaXXXbla/test")).isTrue();
		assertThat(pathMatcher.match("/*bla/test", "/XXXbla/test")).isTrue();
		assertThat(pathMatcher.match("/bla*bla/test", "/blaXXXbl/test")).isFalse();
		assertThat(pathMatcher.match("/*bla/test", "XXXblab/test")).isFalse();
		assertThat(pathMatcher.match("/*bla/test", "XXXbl/test")).isFalse();

		assertThat(pathMatcher.match("/????", "/bala/bla")).isFalse();
		assertThat(pathMatcher.match("/**/*bla", "/bla/bla/bla/bbb")).isFalse();

		assertThat(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.match("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg")).isTrue();

		assertThat(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing")).isFalse();

		assertThat(pathMatcher.match("/x/x/**/bla", "/x/x/x/")).isFalse();

		assertThat(pathMatcher.match("/foo/bar/**", "/foo/bar")).isTrue();

		assertThat(pathMatcher.match("", "")).isTrue();

		assertThat(pathMatcher.match("/{bla}.*", "/testing.html")).isTrue();
		assertThat(pathMatcher.match("/{bla}", "//x\ny")).isTrue();
		assertThat(pathMatcher.match("/{var:.*}", "/x\ny")).isTrue();
	}

	@Test
	void matchWithNullPath() {
		assertThat(pathMatcher.match("/test", null)).isFalse();
		assertThat(pathMatcher.match("/", null)).isFalse();
		assertThat(pathMatcher.match(null, null)).isFalse();
	}

	// SPR-14247
	@Test
	void matchWithTrimTokensEnabled() throws Exception {
		pathMatcher.setTrimTokens(true);

		assertThat(pathMatcher.match("/foo/bar", "/foo /bar")).isTrue();
	}

	@Test
	void matchStart() {
		// test exact matching
		assertThat(pathMatcher.matchStart("test", "test")).isTrue();
		assertThat(pathMatcher.matchStart("/test", "/test")).isTrue();
		assertThat(pathMatcher.matchStart("/test.jpg", "test.jpg")).isFalse();
		assertThat(pathMatcher.matchStart("test", "/test")).isFalse();
		assertThat(pathMatcher.matchStart("/test", "test")).isFalse();

		// test matching with ?'s
		assertThat(pathMatcher.matchStart("t?st", "test")).isTrue();
		assertThat(pathMatcher.matchStart("??st", "test")).isTrue();
		assertThat(pathMatcher.matchStart("tes?", "test")).isTrue();
		assertThat(pathMatcher.matchStart("te??", "test")).isTrue();
		assertThat(pathMatcher.matchStart("?es?", "test")).isTrue();
		assertThat(pathMatcher.matchStart("tes?", "tes")).isFalse();
		assertThat(pathMatcher.matchStart("tes?", "testt")).isFalse();
		assertThat(pathMatcher.matchStart("tes?", "tsst")).isFalse();

		// test matching with *'s
		assertThat(pathMatcher.matchStart("*", "test")).isTrue();
		assertThat(pathMatcher.matchStart("test*", "test")).isTrue();
		assertThat(pathMatcher.matchStart("test*", "testTest")).isTrue();
		assertThat(pathMatcher.matchStart("test/*", "test/Test")).isTrue();
		assertThat(pathMatcher.matchStart("test/*", "test/t")).isTrue();
		assertThat(pathMatcher.matchStart("test/*", "test/")).isTrue();
		assertThat(pathMatcher.matchStart("*test*", "AnothertestTest")).isTrue();
		assertThat(pathMatcher.matchStart("*test", "Anothertest")).isTrue();
		assertThat(pathMatcher.matchStart("*.*", "test.")).isTrue();
		assertThat(pathMatcher.matchStart("*.*", "test.test")).isTrue();
		assertThat(pathMatcher.matchStart("*.*", "test.test.test")).isTrue();
		assertThat(pathMatcher.matchStart("test*aaa", "testblaaaa")).isTrue();
		assertThat(pathMatcher.matchStart("test*", "tst")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "test/")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "tsttest")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "test/")).isFalse();
		assertThat(pathMatcher.matchStart("test*", "test/t")).isFalse();
		assertThat(pathMatcher.matchStart("test/*", "test")).isTrue();
		assertThat(pathMatcher.matchStart("test/t*.txt", "test")).isTrue();
		assertThat(pathMatcher.matchStart("*test*", "tsttst")).isFalse();
		assertThat(pathMatcher.matchStart("*test", "tsttst")).isFalse();
		assertThat(pathMatcher.matchStart("*.*", "tsttst")).isFalse();
		assertThat(pathMatcher.matchStart("test*aaa", "test")).isFalse();
		assertThat(pathMatcher.matchStart("test*aaa", "testblaaab")).isFalse();

		// test matching with ?'s and /'s
		assertThat(pathMatcher.matchStart("/?", "/a")).isTrue();
		assertThat(pathMatcher.matchStart("/?/a", "/a/a")).isTrue();
		assertThat(pathMatcher.matchStart("/a/?", "/a/b")).isTrue();
		assertThat(pathMatcher.matchStart("/??/a", "/aa/a")).isTrue();
		assertThat(pathMatcher.matchStart("/a/??", "/a/bb")).isTrue();
		assertThat(pathMatcher.matchStart("/?", "/a")).isTrue();

		// test matching with **'s
		assertThat(pathMatcher.matchStart("/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/*/**", "/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/**/*", "/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("test*/**", "test/")).isTrue();
		assertThat(pathMatcher.matchStart("test*/**", "test/t")).isTrue();
		assertThat(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla")).isTrue();
		assertThat(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla/bla")).isTrue();
		assertThat(pathMatcher.matchStart("/**/test", "/bla/bla/test")).isTrue();
		assertThat(pathMatcher.matchStart("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla")).isTrue();
		assertThat(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbla/test")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla/test", "/XXXbla/test")).isTrue();
		assertThat(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbl/test")).isFalse();
		assertThat(pathMatcher.matchStart("/*bla/test", "XXXblab/test")).isFalse();
		assertThat(pathMatcher.matchStart("/*bla/test", "XXXbl/test")).isFalse();

		assertThat(pathMatcher.matchStart("/????", "/bala/bla")).isFalse();
		assertThat(pathMatcher.matchStart("/**/*bla", "/bla/bla/bla/bbb")).isTrue();

		assertThat(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg")).isTrue();

		assertThat(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/")).isTrue();
		assertThat(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing")).isTrue();
		assertThat(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();
		assertThat(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing")).isTrue();

		assertThat(pathMatcher.matchStart("/x/x/**/bla", "/x/x/x/")).isTrue();

		assertThat(pathMatcher.matchStart("", "")).isTrue();
	}

	@Test
	void uniqueDeliminator() {
		pathMatcher.setPathSeparator(".");

		// test exact matching
		assertThat(pathMatcher.match("test", "test")).isTrue();
		assertThat(pathMatcher.match(".test", ".test")).isTrue();
		assertThat(pathMatcher.match(".test/jpg", "test/jpg")).isFalse();
		assertThat(pathMatcher.match("test", ".test")).isFalse();
		assertThat(pathMatcher.match(".test", "test")).isFalse();

		// test matching with ?'s
		assertThat(pathMatcher.match("t?st", "test")).isTrue();
		assertThat(pathMatcher.match("??st", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "test")).isTrue();
		assertThat(pathMatcher.match("te??", "test")).isTrue();
		assertThat(pathMatcher.match("?es?", "test")).isTrue();
		assertThat(pathMatcher.match("tes?", "tes")).isFalse();
		assertThat(pathMatcher.match("tes?", "testt")).isFalse();
		assertThat(pathMatcher.match("tes?", "tsst")).isFalse();

		// test matching with *'s
		assertThat(pathMatcher.match("*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "test")).isTrue();
		assertThat(pathMatcher.match("test*", "testTest")).isTrue();
		assertThat(pathMatcher.match("*test*", "AnothertestTest")).isTrue();
		assertThat(pathMatcher.match("*test", "Anothertest")).isTrue();
		assertThat(pathMatcher.match("*/*", "test/")).isTrue();
		assertThat(pathMatcher.match("*/*", "test/test")).isTrue();
		assertThat(pathMatcher.match("*/*", "test/test/test")).isTrue();
		assertThat(pathMatcher.match("test*aaa", "testblaaaa")).isTrue();
		assertThat(pathMatcher.match("test*", "tst")).isFalse();
		assertThat(pathMatcher.match("test*", "tsttest")).isFalse();
		assertThat(pathMatcher.match("*test*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*test", "tsttst")).isFalse();
		assertThat(pathMatcher.match("*/*", "tsttst")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "test")).isFalse();
		assertThat(pathMatcher.match("test*aaa", "testblaaab")).isFalse();

		// test matching with ?'s and .'s
		assertThat(pathMatcher.match(".?", ".a")).isTrue();
		assertThat(pathMatcher.match(".?.a", ".a.a")).isTrue();
		assertThat(pathMatcher.match(".a.?", ".a.b")).isTrue();
		assertThat(pathMatcher.match(".??.a", ".aa.a")).isTrue();
		assertThat(pathMatcher.match(".a.??", ".a.bb")).isTrue();
		assertThat(pathMatcher.match(".?", ".a")).isTrue();

		// test matching with **'s
		assertThat(pathMatcher.match(".**", ".testing.testing")).isTrue();
		assertThat(pathMatcher.match(".*.**", ".testing.testing")).isTrue();
		assertThat(pathMatcher.match(".**.*", ".testing.testing")).isTrue();
		assertThat(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla")).isTrue();
		assertThat(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla.bla")).isTrue();
		assertThat(pathMatcher.match(".**.test", ".bla.bla.test")).isTrue();
		assertThat(pathMatcher.match(".bla.**.**.bla", ".bla.bla.bla.bla.bla.bla")).isTrue();
		assertThat(pathMatcher.match(".bla*bla.test", ".blaXXXbla.test")).isTrue();
		assertThat(pathMatcher.match(".*bla.test", ".XXXbla.test")).isTrue();
		assertThat(pathMatcher.match(".bla*bla.test", ".blaXXXbl.test")).isFalse();
		assertThat(pathMatcher.match(".*bla.test", "XXXblab.test")).isFalse();
		assertThat(pathMatcher.match(".*bla.test", "XXXbl.test")).isFalse();
	}

	@Test
	void extractPathWithinPattern() throws Exception {
		assertThat(pathMatcher.extractPathWithinPattern("/docs/commit.html", "/docs/commit.html")).isEqualTo("");

		assertThat(pathMatcher.extractPathWithinPattern("/docs/*", "/docs/cvs/commit")).isEqualTo("cvs/commit");
		assertThat(pathMatcher.extractPathWithinPattern("/docs/cvs/*.html", "/docs/cvs/commit.html")).isEqualTo("commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("/docs/**", "/docs/cvs/commit")).isEqualTo("cvs/commit");
		assertThat(pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/cvs/commit.html")).isEqualTo("cvs/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/commit.html")).isEqualTo("commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("/*.html", "/commit.html")).isEqualTo("commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("/*.html", "/docs/commit.html")).isEqualTo("docs/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("*.html", "/commit.html")).isEqualTo("/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("*.html", "/docs/commit.html")).isEqualTo("/docs/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("**/*.*", "/docs/commit.html")).isEqualTo("/docs/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("*", "/docs/commit.html")).isEqualTo("/docs/commit.html");
		// SPR-10515
		assertThat(pathMatcher.extractPathWithinPattern("**/commit.html", "/docs/cvs/other/commit.html")).isEqualTo("/docs/cvs/other/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("/docs/**/commit.html", "/docs/cvs/other/commit.html")).isEqualTo("cvs/other/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("/docs/**/**/**/**", "/docs/cvs/other/commit.html")).isEqualTo("cvs/other/commit.html");

		assertThat(pathMatcher.extractPathWithinPattern("/d?cs/*", "/docs/cvs/commit")).isEqualTo("docs/cvs/commit");
		assertThat(pathMatcher.extractPathWithinPattern("/docs/c?s/*.html", "/docs/cvs/commit.html")).isEqualTo("cvs/commit.html");
		assertThat(pathMatcher.extractPathWithinPattern("/d?cs/**", "/docs/cvs/commit")).isEqualTo("docs/cvs/commit");
		assertThat(pathMatcher.extractPathWithinPattern("/d?cs/**/*.html", "/docs/cvs/commit.html")).isEqualTo("docs/cvs/commit.html");
	}

	@Test
	void extractUriTemplateVariables() throws Exception {
		Map<String, String> result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}", "/hotels/1");
		assertThat(result).isEqualTo(Collections.singletonMap("hotel", "1"));

		result = pathMatcher.extractUriTemplateVariables("/h?tels/{hotel}", "/hotels/1");
		assertThat(result).isEqualTo(Collections.singletonMap("hotel", "1"));

		result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}/bookings/{booking}", "/hotels/1/bookings/2");
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("hotel", "1");
		expected.put("booking", "2");
		assertThat(result).isEqualTo(expected);

		result = pathMatcher.extractUriTemplateVariables("/**/hotels/**/{hotel}", "/foo/hotels/bar/1");
		assertThat(result).isEqualTo(Collections.singletonMap("hotel", "1"));

		result = pathMatcher.extractUriTemplateVariables("/{page}.html", "/42.html");
		assertThat(result).isEqualTo(Collections.singletonMap("page", "42"));

		result = pathMatcher.extractUriTemplateVariables("/{page}.*", "/42.html");
		assertThat(result).isEqualTo(Collections.singletonMap("page", "42"));

		result = pathMatcher.extractUriTemplateVariables("/A-{B}-C", "/A-b-C");
		assertThat(result).isEqualTo(Collections.singletonMap("B", "b"));

		result = pathMatcher.extractUriTemplateVariables("/{name}.{extension}", "/test.html");
		expected = new LinkedHashMap<>();
		expected.put("name", "test");
		expected.put("extension", "html");
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void extractUriTemplateVariablesRegex() {
		Map<String, String> result = pathMatcher
				.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-{version:[\\w\\.]+}.jar",
						"com.example-1.0.0.jar");
		assertThat(result.get("symbolicName")).isEqualTo("com.example");
		assertThat(result.get("version")).isEqualTo("1.0.0");

		result = pathMatcher.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-sources-{version:[\\w\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertThat(result.get("symbolicName")).isEqualTo("com.example");
		assertThat(result.get("version")).isEqualTo("1.0.0");
	}

	/**
	 * SPR-7787
	 */
	@Test
	void extractUriTemplateVarsRegexQualifiers() {
		Map<String, String> result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertThat(result.get("symbolicName")).isEqualTo("com.example");
		assertThat(result.get("version")).isEqualTo("1.0.0");

		result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\w\\.]+}-sources-{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar",
				"com.example-sources-1.0.0-20100220.jar");
		assertThat(result.get("symbolicName")).isEqualTo("com.example");
		assertThat(result.get("version")).isEqualTo("1.0.0");
		assertThat(result.get("year")).isEqualTo("2010");
		assertThat(result.get("month")).isEqualTo("02");
		assertThat(result.get("day")).isEqualTo("20");

		result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar",
				"com.example-sources-1.0.0.{12}.jar");
		assertThat(result.get("symbolicName")).isEqualTo("com.example");
		assertThat(result.get("version")).isEqualTo("1.0.0.{12}");
	}

	/**
	 * SPR-8455
	 */
	@Test
	void extractUriTemplateVarsRegexCapturingGroups() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				pathMatcher.extractUriTemplateVariables("/web/{id:foo(bar)?}", "/web/foobar"))
			.withMessageContaining("The number of capturing groups in the pattern");
	}

	@Test
	void combine() {
		assertThat(pathMatcher.combine(null, null)).isEqualTo("");
		assertThat(pathMatcher.combine("/hotels", null)).isEqualTo("/hotels");
		assertThat(pathMatcher.combine(null, "/hotels")).isEqualTo("/hotels");
		assertThat(pathMatcher.combine("/hotels/*", "booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels/*", "/booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels/**", "booking")).isEqualTo("/hotels/**/booking");
		assertThat(pathMatcher.combine("/hotels/**", "/booking")).isEqualTo("/hotels/**/booking");
		assertThat(pathMatcher.combine("/hotels", "/booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels", "booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels/", "booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels/*", "{hotel}")).isEqualTo("/hotels/{hotel}");
		assertThat(pathMatcher.combine("/hotels/**", "{hotel}")).isEqualTo("/hotels/**/{hotel}");
		assertThat(pathMatcher.combine("/hotels", "{hotel}")).isEqualTo("/hotels/{hotel}");
		assertThat(pathMatcher.combine("/hotels", "{hotel}.*")).isEqualTo("/hotels/{hotel}.*");
		assertThat(pathMatcher.combine("/hotels/*/booking", "{booking}")).isEqualTo("/hotels/*/booking/{booking}");
		assertThat(pathMatcher.combine("/*.html", "/hotel.html")).isEqualTo("/hotel.html");
		assertThat(pathMatcher.combine("/*.html", "/hotel")).isEqualTo("/hotel.html");
		assertThat(pathMatcher.combine("/*.html", "/hotel.*")).isEqualTo("/hotel.html");
		assertThat(pathMatcher.combine("/**", "/*.html")).isEqualTo("/*.html");
		assertThat(pathMatcher.combine("/*", "/*.html")).isEqualTo("/*.html");
		assertThat(pathMatcher.combine("/*.*", "/*.html")).isEqualTo("/*.html");
		// SPR-8858
		assertThat(pathMatcher.combine("/{foo}", "/bar")).isEqualTo("/{foo}/bar");
		// SPR-7970
		assertThat(pathMatcher.combine("/user", "/user")).isEqualTo("/user/user");
		// SPR-10062
		assertThat(pathMatcher.combine("/{foo:.*[^0-9].*}", "/edit/")).isEqualTo("/{foo:.*[^0-9].*}/edit/");
		// SPR-10554
		assertThat(pathMatcher.combine("/1.0", "/foo/test")).isEqualTo("/1.0/foo/test");
		// SPR-12975
		assertThat(pathMatcher.combine("/", "/hotel")).isEqualTo("/hotel");
		// SPR-12975
		assertThat(pathMatcher.combine("/hotel/", "/booking")).isEqualTo("/hotel/booking");
	}

	@Test
	void combineWithTwoFileExtensionPatterns() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				pathMatcher.combine("/*.html", "/*.txt"));
	}

	@Test
	void patternComparator() {
		Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");

		assertThat(comparator.compare(null, null)).isEqualTo(0);
		assertThat(comparator.compare(null, "/hotels/new")).isEqualTo(1);
		assertThat(comparator.compare("/hotels/new", null)).isEqualTo(-1);

		assertThat(comparator.compare("/hotels/new", "/hotels/new")).isEqualTo(0);

		assertThat(comparator.compare("/hotels/new", "/hotels/*")).isEqualTo(-1);
		assertThat(comparator.compare("/hotels/*", "/hotels/new")).isEqualTo(1);
		assertThat(comparator.compare("/hotels/*", "/hotels/*")).isEqualTo(0);

		assertThat(comparator.compare("/hotels/new", "/hotels/{hotel}")).isEqualTo(-1);
		assertThat(comparator.compare("/hotels/{hotel}", "/hotels/new")).isEqualTo(1);
		assertThat(comparator.compare("/hotels/{hotel}", "/hotels/{hotel}")).isEqualTo(0);
		assertThat(comparator.compare("/hotels/{hotel}/booking", "/hotels/{hotel}/bookings/{booking}")).isEqualTo(-1);
		assertThat(comparator.compare("/hotels/{hotel}/bookings/{booking}", "/hotels/{hotel}/booking")).isEqualTo(1);

		// SPR-10550
		assertThat(comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/**")).isEqualTo(-1);
		assertThat(comparator.compare("/**", "/hotels/{hotel}/bookings/{booking}/cutomers/{customer}")).isEqualTo(1);
		assertThat(comparator.compare("/**", "/**")).isEqualTo(0);

		assertThat(comparator.compare("/hotels/{hotel}", "/hotels/*")).isEqualTo(-1);
		assertThat(comparator.compare("/hotels/*", "/hotels/{hotel}")).isEqualTo(1);

		assertThat(comparator.compare("/hotels/*", "/hotels/*/**")).isEqualTo(-1);
		assertThat(comparator.compare("/hotels/*/**", "/hotels/*")).isEqualTo(1);

		assertThat(comparator.compare("/hotels/new", "/hotels/new.*")).isEqualTo(-1);
		assertThat(comparator.compare("/hotels/{hotel}", "/hotels/{hotel}.*")).isEqualTo(2);

		// SPR-6741
		assertThat(comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/hotels/**")).isEqualTo(-1);
		assertThat(comparator.compare("/hotels/**", "/hotels/{hotel}/bookings/{booking}/cutomers/{customer}")).isEqualTo(1);
		assertThat(comparator.compare("/hotels/foo/bar/**", "/hotels/{hotel}")).isEqualTo(1);
		assertThat(comparator.compare("/hotels/{hotel}", "/hotels/foo/bar/**")).isEqualTo(-1);

		// gh-23125
		assertThat(comparator.compare("/hotels/*/bookings/**", "/hotels/**")).isEqualTo(-11);

		// SPR-8683
		assertThat(comparator.compare("/**", "/hotels/{hotel}")).isEqualTo(1);

		// longer is better
		assertThat(comparator.compare("/hotels", "/hotels2")).isEqualTo(1);

		// SPR-13139
		assertThat(comparator.compare("*", "*/**")).isEqualTo(-1);
		assertThat(comparator.compare("*/**", "*")).isEqualTo(1);
	}

	@Test
	void patternComparatorSort() {
		Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");
		List<String> paths = new ArrayList<>(3);

		paths.add(null);
		paths.add("/hotels/new");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isNull();
		paths.clear();

		paths.add("/hotels/new");
		paths.add(null);
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isNull();
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/new");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isEqualTo("/hotels/*");
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/*");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isEqualTo("/hotels/*");
		paths.clear();

		paths.add("/hotels/**");
		paths.add("/hotels/*");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/*");
		assertThat(paths.get(1)).isEqualTo("/hotels/**");
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/**");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/*");
		assertThat(paths.get(1)).isEqualTo("/hotels/**");
		paths.clear();

		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isEqualTo("/hotels/{hotel}");
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/{hotel}");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isEqualTo("/hotels/{hotel}");
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isEqualTo("/hotels/{hotel}");
		assertThat(paths.get(2)).isEqualTo("/hotels/*");
		paths.clear();

		paths.add("/hotels/ne*");
		paths.add("/hotels/n*");
		Collections.shuffle(paths);
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/ne*");
		assertThat(paths.get(1)).isEqualTo("/hotels/n*");
		paths.clear();

		comparator = pathMatcher.getPatternComparator("/hotels/new.html");
		paths.add("/hotels/new.*");
		paths.add("/hotels/{hotel}");
		Collections.shuffle(paths);
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/hotels/new.*");
		assertThat(paths.get(1)).isEqualTo("/hotels/{hotel}");
		paths.clear();

		comparator = pathMatcher.getPatternComparator("/web/endUser/action/login.html");
		paths.add("/**/login.*");
		paths.add("/**/endUser/action/login.*");
		paths.sort(comparator);
		assertThat(paths.get(0)).isEqualTo("/**/endUser/action/login.*");
		assertThat(paths.get(1)).isEqualTo("/**/login.*");
		paths.clear();
	}

	@Test  // SPR-8687
	void trimTokensOff() {
		pathMatcher.setTrimTokens(false);

		assertThat(pathMatcher.match("/group/{groupName}/members", "/group/sales/members")).isTrue();
		assertThat(pathMatcher.match("/group/{groupName}/members", "/group/  sales/members")).isTrue();
		assertThat(pathMatcher.match("/group/{groupName}/members", "/Group/  Sales/Members")).isFalse();
	}

	@Test  // SPR-13286
	void caseInsensitive() {
		pathMatcher.setCaseSensitive(false);

		assertThat(pathMatcher.match("/group/{groupName}/members", "/group/sales/members")).isTrue();
		assertThat(pathMatcher.match("/group/{groupName}/members", "/Group/Sales/Members")).isTrue();
		assertThat(pathMatcher.match("/Group/{groupName}/Members", "/group/Sales/members")).isTrue();
	}

	@Test
	void defaultCacheSetting() {
		match();
		assertThat(pathMatcher.stringMatcherCache.size() > 20).isTrue();

		for (int i = 0; i < 65536; i++) {
			pathMatcher.match("test" + i, "test");
		}
		// Cache turned off because it went beyond the threshold
		assertThat(pathMatcher.stringMatcherCache.isEmpty()).isTrue();
	}

	@Test
	void cachePatternsSetToTrue() {
		pathMatcher.setCachePatterns(true);
		match();
		assertThat(pathMatcher.stringMatcherCache.size() > 20).isTrue();

		for (int i = 0; i < 65536; i++) {
			pathMatcher.match("test" + i, "test" + i);
		}
		// Cache keeps being alive due to the explicit cache setting
		assertThat(pathMatcher.stringMatcherCache.size() > 65536).isTrue();
	}

	@Test
	void preventCreatingStringMatchersIfPathDoesNotStartsWithPatternPrefix() {
		pathMatcher.setCachePatterns(true);
		assertThat(pathMatcher.stringMatcherCache.size()).isEqualTo(0);

		pathMatcher.match("test?", "test");
		assertThat(pathMatcher.stringMatcherCache.size()).isEqualTo(1);

		pathMatcher.match("test?", "best");
		pathMatcher.match("test/*", "view/test.jpg");
		pathMatcher.match("test/**/test.jpg", "view/test.jpg");
		pathMatcher.match("test/{name}.jpg", "view/test.jpg");
		assertThat(pathMatcher.stringMatcherCache.size()).isEqualTo(1);
	}

	@Test
	void creatingStringMatchersIfPatternPrefixCannotDetermineIfPathMatch() {
		pathMatcher.setCachePatterns(true);
		assertThat(pathMatcher.stringMatcherCache.size()).isEqualTo(0);

		pathMatcher.match("test", "testian");
		pathMatcher.match("test?", "testFf");
		pathMatcher.match("test/*", "test/dir/name.jpg");
		pathMatcher.match("test/{name}.jpg", "test/lorem.jpg");
		pathMatcher.match("bla/**/test.jpg", "bla/test.jpg");
		pathMatcher.match("**/{name}.jpg", "test/lorem.jpg");
		pathMatcher.match("/**/{name}.jpg", "/test/lorem.jpg");
		pathMatcher.match("/*/dir/{name}.jpg", "/*/dir/lorem.jpg");

		assertThat(pathMatcher.stringMatcherCache.size()).isEqualTo(7);
	}

	@Test
	void cachePatternsSetToFalse() {
		pathMatcher.setCachePatterns(false);
		match();
		assertThat(pathMatcher.stringMatcherCache.isEmpty()).isTrue();
	}

	@Test
	void extensionMappingWithDotPathSeparator() {
		pathMatcher.setPathSeparator(".");
		assertThat(pathMatcher.combine("/*.html", "hotel.*")).as("Extension mapping should be disabled with \".\" as path separator").isEqualTo("/*.html.hotel.*");
	}

	@Test // gh-22959
	void isPattern() {
		assertThat(pathMatcher.isPattern("/test/*")).isTrue();
		assertThat(pathMatcher.isPattern("/test/**/name")).isTrue();
		assertThat(pathMatcher.isPattern("/test?")).isTrue();
		assertThat(pathMatcher.isPattern("/test/{name}")).isTrue();

		assertThat(pathMatcher.isPattern("/test/name")).isFalse();
		assertThat(pathMatcher.isPattern("/test/foo{bar")).isFalse();
	}

	@Test // gh-23297
	void isPatternWithNullPath() {
		assertThat(pathMatcher.isPattern(null)).isFalse();
	}

}
