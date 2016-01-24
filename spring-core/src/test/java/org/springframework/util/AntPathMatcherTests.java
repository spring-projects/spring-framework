/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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
public class AntPathMatcherTests {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void match() {
		// test exact matching
		assertTrue(pathMatcher.match("test", "test"));
		assertTrue(pathMatcher.match("/test", "/test"));
		assertFalse(pathMatcher.match("/test.jpg", "test.jpg"));
		assertFalse(pathMatcher.match("test", "/test"));
		assertFalse(pathMatcher.match("/test", "test"));

		// test matching with ?'s
		assertTrue(pathMatcher.match("t?st", "test"));
		assertTrue(pathMatcher.match("??st", "test"));
		assertTrue(pathMatcher.match("tes?", "test"));
		assertTrue(pathMatcher.match("te??", "test"));
		assertTrue(pathMatcher.match("?es?", "test"));
		assertFalse(pathMatcher.match("tes?", "tes"));
		assertFalse(pathMatcher.match("tes?", "testt"));
		assertFalse(pathMatcher.match("tes?", "tsst"));

		// test matching with *'s
		assertTrue(pathMatcher.match("*", "test"));
		assertTrue(pathMatcher.match("test*", "test"));
		assertTrue(pathMatcher.match("test*", "testTest"));
		assertTrue(pathMatcher.match("test/*", "test/Test"));
		assertTrue(pathMatcher.match("test/*", "test/t"));
		assertTrue(pathMatcher.match("test/*", "test/"));
		assertTrue(pathMatcher.match("*test*", "AnothertestTest"));
		assertTrue(pathMatcher.match("*test", "Anothertest"));
		assertTrue(pathMatcher.match("*.*", "test."));
		assertTrue(pathMatcher.match("*.*", "test.test"));
		assertTrue(pathMatcher.match("*.*", "test.test.test"));
		assertTrue(pathMatcher.match("test*aaa", "testblaaaa"));
		assertFalse(pathMatcher.match("test*", "tst"));
		assertFalse(pathMatcher.match("test*", "tsttest"));
		assertFalse(pathMatcher.match("test*", "test/"));
		assertFalse(pathMatcher.match("test*", "test/t"));
		assertFalse(pathMatcher.match("test/*", "test"));
		assertFalse(pathMatcher.match("*test*", "tsttst"));
		assertFalse(pathMatcher.match("*test", "tsttst"));
		assertFalse(pathMatcher.match("*.*", "tsttst"));
		assertFalse(pathMatcher.match("test*aaa", "test"));
		assertFalse(pathMatcher.match("test*aaa", "testblaaab"));

		// test matching with ?'s and /'s
		assertTrue(pathMatcher.match("/?", "/a"));
		assertTrue(pathMatcher.match("/?/a", "/a/a"));
		assertTrue(pathMatcher.match("/a/?", "/a/b"));
		assertTrue(pathMatcher.match("/??/a", "/aa/a"));
		assertTrue(pathMatcher.match("/a/??", "/a/bb"));
		assertTrue(pathMatcher.match("/?", "/a"));

		// test matching with **'s
		assertTrue(pathMatcher.match("/**", "/testing/testing"));
		assertTrue(pathMatcher.match("/*/**", "/testing/testing"));
		assertTrue(pathMatcher.match("/**/*", "/testing/testing"));
		assertTrue(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla"));
		assertTrue(pathMatcher.match("/bla/**/bla", "/bla/testing/testing/bla/bla"));
		assertTrue(pathMatcher.match("/**/test", "/bla/bla/test"));
		assertTrue(pathMatcher.match("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla"));
		assertTrue(pathMatcher.match("/bla*bla/test", "/blaXXXbla/test"));
		assertTrue(pathMatcher.match("/*bla/test", "/XXXbla/test"));
		assertFalse(pathMatcher.match("/bla*bla/test", "/blaXXXbl/test"));
		assertFalse(pathMatcher.match("/*bla/test", "XXXblab/test"));
		assertFalse(pathMatcher.match("/*bla/test", "XXXbl/test"));

		assertFalse(pathMatcher.match("/????", "/bala/bla"));
		assertFalse(pathMatcher.match("/**/*bla", "/bla/bla/bla/bbb"));

		assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(pathMatcher.match("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertTrue(pathMatcher.match("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

		assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(pathMatcher.match("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertFalse(pathMatcher.match("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

		assertFalse(pathMatcher.match("/x/x/**/bla", "/x/x/x/"));

		assertTrue(pathMatcher.match("", ""));

		assertTrue(pathMatcher.match("/{bla}.*", "/testing.html"));
	}

	@Test
	public void isPattern() {
		assertTrue(pathMatcher.isPattern("/tes?"));
		assertTrue(pathMatcher.isPattern("/bla/*"));
		assertTrue(pathMatcher.isPattern("/bla/**"));
		assertTrue(pathMatcher.isPattern("/{bla}.*"));
		assertTrue(pathMatcher.isPattern("/{bla}.jpg"));

		assertFalse(pathMatcher.isPattern("/test"));
		assertFalse(pathMatcher.isPattern("test"));
		assertFalse(pathMatcher.isPattern("test.jpg"));
		assertFalse(pathMatcher.isPattern("bla/test.jpg"));
		assertFalse(pathMatcher.isPattern("/bla/test.jpg"));
	}


	@Test
	public void withMatchStart() {
		// test exact matching
		assertTrue(pathMatcher.matchStart("test", "test"));
		assertTrue(pathMatcher.matchStart("/test", "/test"));
		assertFalse(pathMatcher.matchStart("/test.jpg", "test.jpg"));
		assertFalse(pathMatcher.matchStart("test", "/test"));
		assertFalse(pathMatcher.matchStart("/test", "test"));

		// test matching with ?'s
		assertTrue(pathMatcher.matchStart("t?st", "test"));
		assertTrue(pathMatcher.matchStart("??st", "test"));
		assertTrue(pathMatcher.matchStart("tes?", "test"));
		assertTrue(pathMatcher.matchStart("te??", "test"));
		assertTrue(pathMatcher.matchStart("?es?", "test"));
		assertFalse(pathMatcher.matchStart("tes?", "tes"));
		assertFalse(pathMatcher.matchStart("tes?", "testt"));
		assertFalse(pathMatcher.matchStart("tes?", "tsst"));

		// test matching with *'s
		assertTrue(pathMatcher.matchStart("*", "test"));
		assertTrue(pathMatcher.matchStart("test*", "test"));
		assertTrue(pathMatcher.matchStart("test*", "testTest"));
		assertTrue(pathMatcher.matchStart("test/*", "test/Test"));
		assertTrue(pathMatcher.matchStart("test/*", "test/t"));
		assertTrue(pathMatcher.matchStart("test/*", "test/"));
		assertTrue(pathMatcher.matchStart("*test*", "AnothertestTest"));
		assertTrue(pathMatcher.matchStart("*test", "Anothertest"));
		assertTrue(pathMatcher.matchStart("*.*", "test."));
		assertTrue(pathMatcher.matchStart("*.*", "test.test"));
		assertTrue(pathMatcher.matchStart("*.*", "test.test.test"));
		assertTrue(pathMatcher.matchStart("test*aaa", "testblaaaa"));
		assertFalse(pathMatcher.matchStart("test*", "tst"));
		assertFalse(pathMatcher.matchStart("test*", "test/"));
		assertFalse(pathMatcher.matchStart("test*", "tsttest"));
		assertFalse(pathMatcher.matchStart("test*", "test/"));
		assertFalse(pathMatcher.matchStart("test*", "test/t"));
		assertTrue(pathMatcher.matchStart("test/*", "test"));
		assertTrue(pathMatcher.matchStart("test/t*.txt", "test"));
		assertFalse(pathMatcher.matchStart("*test*", "tsttst"));
		assertFalse(pathMatcher.matchStart("*test", "tsttst"));
		assertFalse(pathMatcher.matchStart("*.*", "tsttst"));
		assertFalse(pathMatcher.matchStart("test*aaa", "test"));
		assertFalse(pathMatcher.matchStart("test*aaa", "testblaaab"));

		// test matching with ?'s and /'s
		assertTrue(pathMatcher.matchStart("/?", "/a"));
		assertTrue(pathMatcher.matchStart("/?/a", "/a/a"));
		assertTrue(pathMatcher.matchStart("/a/?", "/a/b"));
		assertTrue(pathMatcher.matchStart("/??/a", "/aa/a"));
		assertTrue(pathMatcher.matchStart("/a/??", "/a/bb"));
		assertTrue(pathMatcher.matchStart("/?", "/a"));

		// test matching with **'s
		assertTrue(pathMatcher.matchStart("/**", "/testing/testing"));
		assertTrue(pathMatcher.matchStart("/*/**", "/testing/testing"));
		assertTrue(pathMatcher.matchStart("/**/*", "/testing/testing"));
		assertTrue(pathMatcher.matchStart("test*/**", "test/"));
		assertTrue(pathMatcher.matchStart("test*/**", "test/t"));
		assertTrue(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla"));
		assertTrue(pathMatcher.matchStart("/bla/**/bla", "/bla/testing/testing/bla/bla"));
		assertTrue(pathMatcher.matchStart("/**/test", "/bla/bla/test"));
		assertTrue(pathMatcher.matchStart("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla"));
		assertTrue(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbla/test"));
		assertTrue(pathMatcher.matchStart("/*bla/test", "/XXXbla/test"));
		assertFalse(pathMatcher.matchStart("/bla*bla/test", "/blaXXXbl/test"));
		assertFalse(pathMatcher.matchStart("/*bla/test", "XXXblab/test"));
		assertFalse(pathMatcher.matchStart("/*bla/test", "XXXbl/test"));

		assertFalse(pathMatcher.matchStart("/????", "/bala/bla"));
		assertTrue(pathMatcher.matchStart("/**/*bla", "/bla/bla/bla/bbb"));

		assertTrue(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(pathMatcher.matchStart("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertTrue(pathMatcher.matchStart("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg"));

		assertTrue(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/"));
		assertTrue(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing"));
		assertTrue(pathMatcher.matchStart("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing"));
		assertTrue(pathMatcher.matchStart("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing"));

		assertTrue(pathMatcher.matchStart("/x/x/**/bla", "/x/x/x/"));

		assertTrue(pathMatcher.matchStart("", ""));
	}

	@Test
	public void uniqueDeliminator() {
		pathMatcher.setPathSeparator(".");

		// test exact matching
		assertTrue(pathMatcher.match("test", "test"));
		assertTrue(pathMatcher.match(".test", ".test"));
		assertFalse(pathMatcher.match(".test/jpg", "test/jpg"));
		assertFalse(pathMatcher.match("test", ".test"));
		assertFalse(pathMatcher.match(".test", "test"));

		// test matching with ?'s
		assertTrue(pathMatcher.match("t?st", "test"));
		assertTrue(pathMatcher.match("??st", "test"));
		assertTrue(pathMatcher.match("tes?", "test"));
		assertTrue(pathMatcher.match("te??", "test"));
		assertTrue(pathMatcher.match("?es?", "test"));
		assertFalse(pathMatcher.match("tes?", "tes"));
		assertFalse(pathMatcher.match("tes?", "testt"));
		assertFalse(pathMatcher.match("tes?", "tsst"));

		// test matching with *'s
		assertTrue(pathMatcher.match("*", "test"));
		assertTrue(pathMatcher.match("test*", "test"));
		assertTrue(pathMatcher.match("test*", "testTest"));
		assertTrue(pathMatcher.match("*test*", "AnothertestTest"));
		assertTrue(pathMatcher.match("*test", "Anothertest"));
		assertTrue(pathMatcher.match("*/*", "test/"));
		assertTrue(pathMatcher.match("*/*", "test/test"));
		assertTrue(pathMatcher.match("*/*", "test/test/test"));
		assertTrue(pathMatcher.match("test*aaa", "testblaaaa"));
		assertFalse(pathMatcher.match("test*", "tst"));
		assertFalse(pathMatcher.match("test*", "tsttest"));
		assertFalse(pathMatcher.match("*test*", "tsttst"));
		assertFalse(pathMatcher.match("*test", "tsttst"));
		assertFalse(pathMatcher.match("*/*", "tsttst"));
		assertFalse(pathMatcher.match("test*aaa", "test"));
		assertFalse(pathMatcher.match("test*aaa", "testblaaab"));

		// test matching with ?'s and .'s
		assertTrue(pathMatcher.match(".?", ".a"));
		assertTrue(pathMatcher.match(".?.a", ".a.a"));
		assertTrue(pathMatcher.match(".a.?", ".a.b"));
		assertTrue(pathMatcher.match(".??.a", ".aa.a"));
		assertTrue(pathMatcher.match(".a.??", ".a.bb"));
		assertTrue(pathMatcher.match(".?", ".a"));

		// test matching with **'s
		assertTrue(pathMatcher.match(".**", ".testing.testing"));
		assertTrue(pathMatcher.match(".*.**", ".testing.testing"));
		assertTrue(pathMatcher.match(".**.*", ".testing.testing"));
		assertTrue(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla"));
		assertTrue(pathMatcher.match(".bla.**.bla", ".bla.testing.testing.bla.bla"));
		assertTrue(pathMatcher.match(".**.test", ".bla.bla.test"));
		assertTrue(pathMatcher.match(".bla.**.**.bla", ".bla.bla.bla.bla.bla.bla"));
		assertTrue(pathMatcher.match(".bla*bla.test", ".blaXXXbla.test"));
		assertTrue(pathMatcher.match(".*bla.test", ".XXXbla.test"));
		assertFalse(pathMatcher.match(".bla*bla.test", ".blaXXXbl.test"));
		assertFalse(pathMatcher.match(".*bla.test", "XXXblab.test"));
		assertFalse(pathMatcher.match(".*bla.test", "XXXbl.test"));
	}

	@Test
	public void extractPathWithinPattern() throws Exception {
		assertEquals("", pathMatcher.extractPathWithinPattern("/docs/commit.html", "/docs/commit.html"));

		assertEquals("cvs/commit", pathMatcher.extractPathWithinPattern("/docs/*", "/docs/cvs/commit"));
		assertEquals("commit.html", pathMatcher.extractPathWithinPattern("/docs/cvs/*.html", "/docs/cvs/commit.html"));
		assertEquals("cvs/commit", pathMatcher.extractPathWithinPattern("/docs/**", "/docs/cvs/commit"));
		assertEquals("cvs/commit.html",
				pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/cvs/commit.html"));
		assertEquals("commit.html", pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/commit.html"));
		assertEquals("commit.html", pathMatcher.extractPathWithinPattern("/*.html", "/commit.html"));
		assertEquals("docs/commit.html", pathMatcher.extractPathWithinPattern("/*.html", "/docs/commit.html"));
		assertEquals("/commit.html", pathMatcher.extractPathWithinPattern("*.html", "/commit.html"));
		assertEquals("/docs/commit.html", pathMatcher.extractPathWithinPattern("*.html", "/docs/commit.html"));
		assertEquals("/docs/commit.html", pathMatcher.extractPathWithinPattern("**/*.*", "/docs/commit.html"));
		assertEquals("/docs/commit.html", pathMatcher.extractPathWithinPattern("*", "/docs/commit.html"));
		// SPR-10515
		assertEquals("/docs/cvs/other/commit.html", pathMatcher.extractPathWithinPattern("**/commit.html", "/docs/cvs/other/commit.html"));
		assertEquals("cvs/other/commit.html", pathMatcher.extractPathWithinPattern("/docs/**/commit.html", "/docs/cvs/other/commit.html"));
		assertEquals("cvs/other/commit.html", pathMatcher.extractPathWithinPattern("/docs/**/**/**/**", "/docs/cvs/other/commit.html"));

		assertEquals("docs/cvs/commit", pathMatcher.extractPathWithinPattern("/d?cs/*", "/docs/cvs/commit"));
		assertEquals("cvs/commit.html",
				pathMatcher.extractPathWithinPattern("/docs/c?s/*.html", "/docs/cvs/commit.html"));
		assertEquals("docs/cvs/commit", pathMatcher.extractPathWithinPattern("/d?cs/**", "/docs/cvs/commit"));
		assertEquals("docs/cvs/commit.html",
				pathMatcher.extractPathWithinPattern("/d?cs/**/*.html", "/docs/cvs/commit.html"));
	}

	@Test
	public void extractUriTemplateVariables() throws Exception {
		Map<String, String> result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}", "/hotels/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		result = pathMatcher.extractUriTemplateVariables("/h?tels/{hotel}", "/hotels/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		result = pathMatcher.extractUriTemplateVariables("/hotels/{hotel}/bookings/{booking}", "/hotels/1/bookings/2");
		Map<String, String> expected = new LinkedHashMap<String, String>();
		expected.put("hotel", "1");
		expected.put("booking", "2");
		assertEquals(expected, result);

		result = pathMatcher.extractUriTemplateVariables("/**/hotels/**/{hotel}", "/foo/hotels/bar/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		result = pathMatcher.extractUriTemplateVariables("/{page}.html", "/42.html");
		assertEquals(Collections.singletonMap("page", "42"), result);

		result = pathMatcher.extractUriTemplateVariables("/{page}.*", "/42.html");
		assertEquals(Collections.singletonMap("page", "42"), result);

		result = pathMatcher.extractUriTemplateVariables("/A-{B}-C", "/A-b-C");
		assertEquals(Collections.singletonMap("B", "b"), result);

		result = pathMatcher.extractUriTemplateVariables("/{name}.{extension}", "/test.html");
		expected = new LinkedHashMap<String, String>();
		expected.put("name", "test");
		expected.put("extension", "html");
		assertEquals(expected, result);
	}

	@Test
	public void extractUriTemplateVariablesRegex() {
		Map<String, String> result = pathMatcher
				.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-{version:[\\w\\.]+}.jar",
						"com.example-1.0.0.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));

		result = pathMatcher.extractUriTemplateVariables("{symbolicName:[\\w\\.]+}-sources-{version:[\\w\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));
	}

	/**
	 * SPR-7787
	 */
	@Test
	public void extractUriTemplateVarsRegexQualifiers() {
		Map<String, String> result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar",
				"com.example-sources-1.0.0.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));

		result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\w\\.]+}-sources-{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar",
				"com.example-sources-1.0.0-20100220.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0", result.get("version"));
		assertEquals("2010", result.get("year"));
		assertEquals("02", result.get("month"));
		assertEquals("20", result.get("day"));

		result = pathMatcher.extractUriTemplateVariables(
				"{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar",
				"com.example-sources-1.0.0.{12}.jar");
		assertEquals("com.example", result.get("symbolicName"));
		assertEquals("1.0.0.{12}", result.get("version"));
	}

	/**
	 * SPR-8455
	 */
	@Test
	public void extractUriTemplateVarsRegexCapturingGroups() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(containsString("The number of capturing groups in the pattern"));
		pathMatcher.extractUriTemplateVariables("/web/{id:foo(bar)?}", "/web/foobar");
	}

	@Test
	public void combine() {
		assertEquals("", pathMatcher.combine(null, null));
		assertEquals("/hotels", pathMatcher.combine("/hotels", null));
		assertEquals("/hotels", pathMatcher.combine(null, "/hotels"));
		assertEquals("/hotels/booking", pathMatcher.combine("/hotels/*", "booking"));
		assertEquals("/hotels/booking", pathMatcher.combine("/hotels/*", "/booking"));
		assertEquals("/hotels/**/booking", pathMatcher.combine("/hotels/**", "booking"));
		assertEquals("/hotels/**/booking", pathMatcher.combine("/hotels/**", "/booking"));
		assertEquals("/hotels/booking", pathMatcher.combine("/hotels", "/booking"));
		assertEquals("/hotels/booking", pathMatcher.combine("/hotels", "booking"));
		assertEquals("/hotels/booking", pathMatcher.combine("/hotels/", "booking"));
		assertEquals("/hotels/{hotel}", pathMatcher.combine("/hotels/*", "{hotel}"));
		assertEquals("/hotels/**/{hotel}", pathMatcher.combine("/hotels/**", "{hotel}"));
		assertEquals("/hotels/{hotel}", pathMatcher.combine("/hotels", "{hotel}"));
		assertEquals("/hotels/{hotel}.*", pathMatcher.combine("/hotels", "{hotel}.*"));
		assertEquals("/hotels/*/booking/{booking}", pathMatcher.combine("/hotels/*/booking", "{booking}"));
		assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel.html"));
		assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel"));
		assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel.*"));
		assertEquals("/*.html", pathMatcher.combine("/**", "/*.html"));
		assertEquals("/*.html", pathMatcher.combine("/*", "/*.html"));
		assertEquals("/*.html", pathMatcher.combine("/*.*", "/*.html"));
		assertEquals("/{foo}/bar", pathMatcher.combine("/{foo}", "/bar"));	// SPR-8858
		assertEquals("/user/user", pathMatcher.combine("/user", "/user"));	// SPR-7970
		assertEquals("/{foo:.*[^0-9].*}/edit/", pathMatcher.combine("/{foo:.*[^0-9].*}", "/edit/")); // SPR-10062
		assertEquals("/1.0/foo/test", pathMatcher.combine("/1.0", "/foo/test")); // SPR-10554
		assertEquals("/hotel", pathMatcher.combine("/", "/hotel")); // SPR-12975
		assertEquals("/hotel/booking", pathMatcher.combine("/hotel/", "/booking")); // SPR-12975
	}

	@Test
	public void combineWithTwoFileExtensionPatterns() {
		exception.expect(IllegalArgumentException.class);
		pathMatcher.combine("/*.html", "/*.txt");
	}

	@Test
	public void patternComparator() {
		Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");

		assertEquals(0, comparator.compare(null, null));
		assertEquals(1, comparator.compare(null, "/hotels/new"));
		assertEquals(-1, comparator.compare("/hotels/new", null));

		assertEquals(0, comparator.compare("/hotels/new", "/hotels/new"));

		assertEquals(-1, comparator.compare("/hotels/new", "/hotels/*"));
		assertEquals(1, comparator.compare("/hotels/*", "/hotels/new"));
		assertEquals(0, comparator.compare("/hotels/*", "/hotels/*"));

		assertEquals(-1, comparator.compare("/hotels/new", "/hotels/{hotel}"));
		assertEquals(1, comparator.compare("/hotels/{hotel}", "/hotels/new"));
		assertEquals(0, comparator.compare("/hotels/{hotel}", "/hotels/{hotel}"));
		assertEquals(-1, comparator.compare("/hotels/{hotel}/booking", "/hotels/{hotel}/bookings/{booking}"));
		assertEquals(1, comparator.compare("/hotels/{hotel}/bookings/{booking}", "/hotels/{hotel}/booking"));

		// SPR-10550
		assertEquals(-1, comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/**"));
		assertEquals(1, comparator.compare("/**","/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"));
		assertEquals(0, comparator.compare("/**","/**"));

		assertEquals(-1, comparator.compare("/hotels/{hotel}", "/hotels/*"));
		assertEquals(1, comparator.compare("/hotels/*", "/hotels/{hotel}"));

		assertEquals(-1, comparator.compare("/hotels/*", "/hotels/*/**"));
		assertEquals(1, comparator.compare("/hotels/*/**", "/hotels/*"));

		assertEquals(-1, comparator.compare("/hotels/new", "/hotels/new.*"));
		assertEquals(2, comparator.compare("/hotels/{hotel}", "/hotels/{hotel}.*"));

		// SPR-6741
		assertEquals(-1, comparator.compare("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}", "/hotels/**"));
		assertEquals(1, comparator.compare("/hotels/**", "/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"));
		assertEquals(1, comparator.compare("/hotels/foo/bar/**", "/hotels/{hotel}"));
		assertEquals(-1, comparator.compare("/hotels/{hotel}", "/hotels/foo/bar/**"));
		assertEquals(2, comparator.compare("/hotels/**/bookings/**", "/hotels/**"));
		assertEquals(-2, comparator.compare("/hotels/**", "/hotels/**/bookings/**"));

		// SPR-8683
		assertEquals(1, comparator.compare("/**", "/hotels/{hotel}"));

		// longer is better
		assertEquals(1, comparator.compare("/hotels", "/hotels2"));

		// SPR-13139
		assertEquals(-1, comparator.compare("*", "*/**"));
		assertEquals(1, comparator.compare("*/**", "*"));
	}

	@Test
	public void patternComparatorSort() {
		Comparator<String> comparator = pathMatcher.getPatternComparator("/hotels/new");
		List<String> paths = new ArrayList<String>(3);

		paths.add(null);
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertNull(paths.get(1));
		paths.clear();

		paths.add("/hotels/new");
		paths.add(null);
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertNull(paths.get(1));
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/*", paths.get(1));
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/*");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/*", paths.get(1));
		paths.clear();

		paths.add("/hotels/**");
		paths.add("/hotels/*");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/*", paths.get(0));
		assertEquals("/hotels/**", paths.get(1));
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/**");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/*", paths.get(0));
		assertEquals("/hotels/**", paths.get(1));
		paths.clear();

		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		paths.clear();

		paths.add("/hotels/new");
		paths.add("/hotels/{hotel}");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		paths.clear();

		paths.add("/hotels/*");
		paths.add("/hotels/{hotel}");
		paths.add("/hotels/new");
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		assertEquals("/hotels/*", paths.get(2));
		paths.clear();

		paths.add("/hotels/ne*");
		paths.add("/hotels/n*");
		Collections.shuffle(paths);
		Collections.sort(paths, comparator);
		assertEquals("/hotels/ne*", paths.get(0));
		assertEquals("/hotels/n*", paths.get(1));
		paths.clear();

		comparator = pathMatcher.getPatternComparator("/hotels/new.html");
		paths.add("/hotels/new.*");
		paths.add("/hotels/{hotel}");
		Collections.shuffle(paths);
		Collections.sort(paths, comparator);
		assertEquals("/hotels/new.*", paths.get(0));
		assertEquals("/hotels/{hotel}", paths.get(1));
		paths.clear();

		comparator = pathMatcher.getPatternComparator("/web/endUser/action/login.html");
		paths.add("/**/login.*");
		paths.add("/**/endUser/action/login.*");
		Collections.sort(paths, comparator);
		assertEquals("/**/endUser/action/login.*", paths.get(0));
		assertEquals("/**/login.*", paths.get(1));
		paths.clear();
	}

	@Test  // SPR-8687
	public void trimTokensOff() {
		pathMatcher.setTrimTokens(false);

		assertTrue(pathMatcher.match("/group/{groupName}/members", "/group/sales/members"));
		assertTrue(pathMatcher.match("/group/{groupName}/members", "/group/  sales/members"));
		assertFalse(pathMatcher.match("/group/{groupName}/members", "/Group/  Sales/Members"));
	}

	@Test  // SPR-13286
	public void caseInsensitive() {
		pathMatcher.setCaseSensitive(false);

		assertTrue(pathMatcher.match("/group/{groupName}/members", "/group/sales/members"));
		assertTrue(pathMatcher.match("/group/{groupName}/members", "/Group/Sales/Members"));
		assertTrue(pathMatcher.match("/Group/{groupName}/Members", "/group/Sales/members"));
	}

	@Test
	public void defaultCacheSetting() {
		match();
		assertTrue(pathMatcher.stringMatcherCache.size() > 20);

		for (int i = 0; i < 65536; i++) {
			pathMatcher.match("test" + i, "test");
		}
		// Cache turned off because it went beyond the threshold
		assertTrue(pathMatcher.stringMatcherCache.isEmpty());
	}

	@Test
	public void cachePatternsSetToTrue() {
		pathMatcher.setCachePatterns(true);
		match();
		assertTrue(pathMatcher.stringMatcherCache.size() > 20);

		for (int i = 0; i < 65536; i++) {
			pathMatcher.match("test" + i, "test");
		}
		// Cache keeps being alive due to the explicit cache setting
		assertTrue(pathMatcher.stringMatcherCache.size() > 65536);
	}

	@Test
	public void cachePatternsSetToFalse() {
		pathMatcher.setCachePatterns(false);
		match();
		assertTrue(pathMatcher.stringMatcherCache.isEmpty());
	}

	@Test
	public void extensionMappingWithDotPathSeparator() {
		pathMatcher.setPathSeparator(".");
		assertEquals("Extension mapping should be disabled with \".\" as path separator",
				"/*.html.hotel.*", pathMatcher.combine("/*.html", "hotel.*"));
	}

}
