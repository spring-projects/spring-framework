/*
 * Copyright 2002-2007 the original author or authors.
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

import junit.framework.TestCase;

/**
 * @author Alef Arendsen
 * @author Seth Ladd
 * @author Juergen Hoeller
 */
public class PathMatcherTests extends TestCase {

	public void testAntPathMatcher() {
		PathMatcher pathMatcher = new AntPathMatcher();

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

		// test matchin with *'s
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
	}

	public void testAntPathMatcherWithMatchStart() {
		PathMatcher pathMatcher = new AntPathMatcher();

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

		// test matchin with *'s
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

	public void testAntPathMatcherWithUniqueDeliminator() {
		AntPathMatcher pathMatcher = new AntPathMatcher();
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

		// test matchin with *'s
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

	public void testAntPathMatcherExtractPathWithinPattern() throws Exception {
		PathMatcher pathMatcher = new AntPathMatcher();

		assertEquals("", pathMatcher.extractPathWithinPattern("/docs/commit.html", "/docs/commit.html"));

		assertEquals("cvs/commit", pathMatcher.extractPathWithinPattern("/docs/*", "/docs/cvs/commit"));
		assertEquals("commit.html", pathMatcher.extractPathWithinPattern("/docs/cvs/*.html", "/docs/cvs/commit.html"));
		assertEquals("cvs/commit", pathMatcher.extractPathWithinPattern("/docs/**", "/docs/cvs/commit"));
		assertEquals("cvs/commit.html", pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/cvs/commit.html"));
		assertEquals("commit.html", pathMatcher.extractPathWithinPattern("/docs/**/*.html", "/docs/commit.html"));
		assertEquals("commit.html", pathMatcher.extractPathWithinPattern("/*.html", "/commit.html"));
		assertEquals("docs/commit.html", pathMatcher.extractPathWithinPattern("/*.html", "/docs/commit.html"));
		assertEquals("/commit.html", pathMatcher.extractPathWithinPattern("*.html", "/commit.html"));
		assertEquals("/docs/commit.html", pathMatcher.extractPathWithinPattern("*.html", "/docs/commit.html"));
		assertEquals("/docs/commit.html", pathMatcher.extractPathWithinPattern("**/*.*", "/docs/commit.html"));
		assertEquals("/docs/commit.html", pathMatcher.extractPathWithinPattern("*", "/docs/commit.html"));

		assertEquals("docs/cvs/commit", pathMatcher.extractPathWithinPattern("/d?cs/*", "/docs/cvs/commit"));
		assertEquals("cvs/commit.html", pathMatcher.extractPathWithinPattern("/docs/c?s/*.html", "/docs/cvs/commit.html"));
		assertEquals("docs/cvs/commit", pathMatcher.extractPathWithinPattern("/d?cs/**", "/docs/cvs/commit"));
		assertEquals("docs/cvs/commit.html", pathMatcher.extractPathWithinPattern("/d?cs/**/*.html", "/docs/cvs/commit.html"));
	}

}
