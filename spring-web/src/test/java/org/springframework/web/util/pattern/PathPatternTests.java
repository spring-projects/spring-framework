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

package org.springframework.web.util.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.PathContainer.Element;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.pattern.PathPattern.PathRemainingMatchInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Exercise matching of {@link PathPattern} objects.
 *
 * @author Andy Clement
 */
public class PathPatternTests {

	@Test
	public void pathContainer() {
		assertThat(elementsToString(toPathContainer("/abc/def").elements())).isEqualTo("[/][abc][/][def]");
		assertThat(elementsToString(toPathContainer("abc/def").elements())).isEqualTo("[abc][/][def]");
		assertThat(elementsToString(toPathContainer("abc/def/").elements())).isEqualTo("[abc][/][def][/]");
		assertThat(elementsToString(toPathContainer("abc//def//").elements())).isEqualTo("[abc][/][/][def][/][/]");
		assertThat(elementsToString(toPathContainer("/").elements())).isEqualTo("[/]");
		assertThat(elementsToString(toPathContainer("///").elements())).isEqualTo("[/][/][/]");
	}

	@Test
	public void hasPatternSyntax() {
		PathPatternParser parser = new PathPatternParser();
		assertThat(parser.parse("/foo/*").hasPatternSyntax()).isTrue();
		assertThat(parser.parse("/foo/**").hasPatternSyntax()).isTrue();
		assertThat(parser.parse("/foo/{*elem}").hasPatternSyntax()).isTrue();
		assertThat(parser.parse("/f?o").hasPatternSyntax()).isTrue();
		assertThat(parser.parse("/f*").hasPatternSyntax()).isTrue();
		assertThat(parser.parse("/foo/{bar}/baz").hasPatternSyntax()).isTrue();
		assertThat(parser.parse("/foo/bar").hasPatternSyntax()).isFalse();
	}

	@Test
	public void matching_LiteralPathElement() {
		checkMatches("foo", "foo");
		checkNoMatch("foo", "bar");
		checkNoMatch("foo", "/foo");
		checkNoMatch("/foo", "foo");
		checkMatches("/f", "/f");
		checkMatches("/foo", "/foo");
		checkNoMatch("/foo", "/food");
		checkNoMatch("/food", "/foo");
		checkMatches("/foo/", "/foo/");
		checkMatches("/foo/bar/woo", "/foo/bar/woo");
		checkMatches("foo/bar/woo", "foo/bar/woo");
	}

	@Test
	public void basicMatching() {
		checkMatches("", "");
		checkMatches("", "/");
		checkMatches("", null);
		checkNoMatch("/abc", "/");
		checkMatches("/", "/");
		checkNoMatch("/", "/a");
		checkMatches("foo/bar/", "foo/bar/");
		checkNoMatch("foo", "foobar");
		checkMatches("/foo/bar", "/foo/bar");
		checkNoMatch("/foo/bar", "/foo/baz");
	}

	private void assertMatches(PathPattern pp, String path) {
		assertThat(pp.matches(toPathContainer(path))).isTrue();
	}

	private void assertNoMatch(PathPattern pp, String path) {
		assertThat(pp.matches(toPathContainer(path))).isFalse();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void optionalTrailingSeparators() {
		PathPattern pp;
		// LiteralPathElement
		pp = parse("/resource");
		assertMatches(pp,"/resource");
		assertMatches(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");
		pp = parse("/resource/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");

		pp = parse("res?urce");
		assertNoMatch(pp,"resource//");
		// SingleCharWildcardPathElement
		pp = parse("/res?urce");
		assertMatches(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");
		pp = parse("/res?urce/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");

		// CaptureVariablePathElement
		pp = parse("/{var}");
		assertMatches(pp,"/resource");
		assertThat(pp.matchAndExtract(toPathContainer("/resource")).getUriVariables()).containsEntry("var", "resource");
		assertMatches(pp,"/resource/");
		assertThat(pp.matchAndExtract(toPathContainer("/resource/")).getUriVariables()).containsEntry(
				"var",
				"resource"
		);
		assertNoMatch(pp,"/resource//");
		pp = parse("/{var}/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertThat(pp.matchAndExtract(toPathContainer("/resource/")).getUriVariables()).containsEntry(
				"var",
				"resource"
		);
		assertNoMatch(pp,"/resource//");

		// CaptureTheRestPathElement
		pp = parse("/{*var}");
		assertMatches(pp,"/resource");
		assertThat(pp.matchAndExtract(toPathContainer("/resource")).getUriVariables()).containsEntry(
				"var",
				"/resource"
		);
		assertMatches(pp,"/resource/");
		assertThat(pp.matchAndExtract(toPathContainer("/resource/")).getUriVariables()).containsEntry(
				"var",
				"/resource/"
		);
		assertMatches(pp,"/resource//");
		assertThat(pp.matchAndExtract(toPathContainer("/resource//")).getUriVariables()).containsEntry(
				"var",
				"/resource//"
		);
		assertMatches(pp,"//resource//");
		assertThat(pp.matchAndExtract(toPathContainer("//resource//")).getUriVariables()).containsEntry(
				"var",
				"//resource//"
		);

		// WildcardTheRestPathElement
		pp = parse("/**");
		assertMatches(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertMatches(pp,"/resource//");
		assertMatches(pp,"//resource//");

		// WildcardPathElement
		pp = parse("/*");
		assertMatches(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");
		pp = parse("/*/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");

		// RegexPathElement
		pp = parse("/{var1}_{var2}");
		assertMatches(pp,"/res1_res2");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2")).getUriVariables()).containsEntry("var1", "res1");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2")).getUriVariables()).containsEntry("var2", "res2");
		assertMatches(pp,"/res1_res2/");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2/")).getUriVariables()).containsEntry("var1", "res1");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2/")).getUriVariables()).containsEntry("var2", "res2");
		assertNoMatch(pp,"/res1_res2//");
		pp = parse("/{var1}_{var2}/");
		assertNoMatch(pp,"/res1_res2");
		assertMatches(pp,"/res1_res2/");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2/")).getUriVariables()).containsEntry("var1", "res1");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2/")).getUriVariables()).containsEntry("var2", "res2");
		assertNoMatch(pp,"/res1_res2//");
		pp = parse("/{var1}*");
		assertMatches(pp,"/a");
		assertMatches(pp,"/a/");
		assertNoMatch(pp,"/"); // no characters for var1
		assertNoMatch(pp,"//"); // no characters for var1

		// Now with trailing matching turned OFF
		PathPatternParser parser = new PathPatternParser();
		parser.setMatchOptionalTrailingSeparator(false);
		// LiteralPathElement
		pp = parser.parse("/resource");
		assertMatches(pp,"/resource");
		assertNoMatch(pp,"/resource/");
		assertNoMatch(pp,"/resource//");
		pp = parser.parse("/resource/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");

		// SingleCharWildcardPathElement
		pp = parser.parse("/res?urce");
		assertMatches(pp,"/resource");
		assertNoMatch(pp,"/resource/");
		assertNoMatch(pp,"/resource//");
		pp = parser.parse("/res?urce/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");

		// CaptureVariablePathElement
		pp = parser.parse("/{var}");
		assertMatches(pp,"/resource");
		assertThat(pp.matchAndExtract(toPathContainer("/resource")).getUriVariables()).containsEntry("var", "resource");
		assertNoMatch(pp,"/resource/");
		assertNoMatch(pp,"/resource//");
		pp = parser.parse("/{var}/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertThat(pp.matchAndExtract(toPathContainer("/resource/")).getUriVariables()).containsEntry(
				"var",
				"resource"
		);
		assertNoMatch(pp,"/resource//");

		// CaptureTheRestPathElement
		pp = parser.parse("/{*var}");
		assertMatches(pp,"/resource");
		assertThat(pp.matchAndExtract(toPathContainer("/resource")).getUriVariables()).containsEntry(
				"var",
				"/resource"
		);
		assertMatches(pp,"/resource/");
		assertThat(pp.matchAndExtract(toPathContainer("/resource/")).getUriVariables()).containsEntry(
				"var",
				"/resource/"
		);
		assertMatches(pp,"/resource//");
		assertThat(pp.matchAndExtract(toPathContainer("/resource//")).getUriVariables()).containsEntry(
				"var",
				"/resource//"
		);
		assertMatches(pp,"//resource//");
		assertThat(pp.matchAndExtract(toPathContainer("//resource//")).getUriVariables()).containsEntry(
				"var",
				"//resource//"
		);

		// WildcardTheRestPathElement
		pp = parser.parse("/**");
		assertMatches(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertMatches(pp,"/resource//");
		assertMatches(pp,"//resource//");

		// WildcardPathElement
		pp = parser.parse("/*");
		assertMatches(pp,"/resource");
		assertNoMatch(pp,"/resource/");
		assertNoMatch(pp,"/resource//");
		pp = parser.parse("/*/");
		assertNoMatch(pp,"/resource");
		assertMatches(pp,"/resource/");
		assertNoMatch(pp,"/resource//");

		// RegexPathElement
		pp = parser.parse("/{var1}_{var2}");
		assertMatches(pp,"/res1_res2");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2")).getUriVariables()).containsEntry("var1", "res1");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2")).getUriVariables()).containsEntry("var2", "res2");
		assertNoMatch(pp,"/res1_res2/");
		assertNoMatch(pp,"/res1_res2//");
		pp = parser.parse("/{var1}_{var2}/");
		assertNoMatch(pp,"/res1_res2");
		assertMatches(pp,"/res1_res2/");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2/")).getUriVariables()).containsEntry("var1", "res1");
		assertThat(pp.matchAndExtract(toPathContainer("/res1_res2/")).getUriVariables()).containsEntry("var2", "res2");
		assertNoMatch(pp,"/res1_res2//");
		pp = parser.parse("/{var1}*");
		assertMatches(pp,"/a");
		assertNoMatch(pp,"/a/");
		assertNoMatch(pp,"/"); // no characters for var1
		assertNoMatch(pp,"//"); // no characters for var1
	}

	@Test
	public void pathRemainderBasicCases_spr15336() {
		// Cover all PathElement kinds
		assertThat(getPathRemaining("/foo", "/foo/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/foo", "/foo/").getPathRemaining().value()).isEqualTo("/");
		assertThat(getPathRemaining("/foo*", "/foo/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/*", "/foo/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/{foo}", "/foo/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/foo","/bar/baz")).isNull();
		assertThat(getPathRemaining("/**", "/foo/bar").getPathRemaining().value()).isEmpty();
		assertThat(getPathRemaining("/{*bar}", "/foo/bar").getPathRemaining().value()).isEmpty();
		assertThat(getPathRemaining("/a?b/d?e", "/aab/dde/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/{abc}abc", "/xyzabc/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/*y*", "/xyzxyz/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/", "/").getPathRemaining().value()).isEmpty();
		assertThat(getPathRemaining("/", "/a").getPathRemaining().value()).isEqualTo("a");
		assertThat(getPathRemaining("/", "/a/").getPathRemaining().value()).isEqualTo("a/");
		assertThat(getPathRemaining("/a{abc}", "/a/bar").getPathRemaining().value()).isEqualTo("/bar");
		assertThat(getPathRemaining("/foo//", "/foo///bar").getPathRemaining().value()).isEqualTo("/bar");
	}

	@Test
	public void encodingAndBoundVariablesCapturePathElement() {
		checkCapture("{var}","f%20o","var","f o");
		checkCapture("{var1}/{var2}","f%20o/f%7Co","var1","f o","var2","f|o");
		checkCapture("{var1}/{var2}","f%20o/f%7co","var1","f o","var2","f|o"); // lower case encoding
		checkCapture("{var:foo}","foo","var","foo");
		checkCapture("{var:f o}","f%20o","var","f o"); // constraint is expressed in non encoded form
		checkCapture("{var:f.o}","f%20o","var","f o");
		checkCapture("{var:f\\|o}","f%7co","var","f|o");
		checkCapture("{var:.*}","x\ny","var","x\ny");
	}

	@Test
	public void encodingAndBoundVariablesCaptureTheRestPathElement() {
		checkCapture("/{*var}","/f%20o","var","/f o");
		checkCapture("{var1}/{*var2}","f%20o/f%7Co","var1","f o","var2","/f|o");
		checkCapture("/{*var}","/foo","var","/foo");
		checkCapture("/{*var}","/f%20o","var","/f o");
		checkCapture("/{*var}","/f%20o","var","/f o");
		checkCapture("/{*var}","/f%7co","var","/f|o");
	}

	@Test
	public void encodingAndBoundVariablesRegexPathElement() {
		checkCapture("/{var1:f o}_ _{var2}","/f%20o_%20_f%7co","var1","f o","var2","f|o");
		checkCapture("/{var1}_{var2}","/f%20o_foo","var1","f o","var2","foo");
		checkCapture("/{var1}_ _{var2}","/f%20o_%20_f%7co","var1","f o","var2","f|o");
		checkCapture("/{var1}_ _{var2:f\\|o}","/f%20o_%20_f%7co","var1","f o","var2","f|o");
		checkCapture("/{var1:f o}_ _{var2:f\\|o}","/f%20o_%20_f%7co","var1","f o","var2","f|o");
		checkCapture("/{var1:f o}_ _{var2:f\\|o}","/f%20o_%20_f%7co","var1","f o","var2","f|o");
		checkCapture("/{var1}_{var2}","/f\noo_foo","var1","f\noo","var2","foo");
	}

	@Test
	public void pathRemainingCornerCases_spr15336() {
		// No match when the literal path element is a longer form of the segment in the pattern
		assertThat((Object) parse("/foo").matchStartOfPath(toPathContainer("/footastic/bar"))).isNull();
		assertThat((Object) parse("/f?o").matchStartOfPath(toPathContainer("/footastic/bar"))).isNull();
		assertThat((Object) parse("/f*o*p").matchStartOfPath(toPathContainer("/flooptastic/bar"))).isNull();
		assertThat((Object) parse("/{abc}abc").matchStartOfPath(toPathContainer("/xyzabcbar/bar"))).isNull();

		// With a /** on the end have to check if there is any more data post
		// 'the match' it starts with a separator
		assertThat(parse("/resource/**").matchStartOfPath(toPathContainer("/resourceX"))).isNull();
		assertThat(parse("/resource/**")
				.matchStartOfPath(toPathContainer("/resource")).getPathRemaining().value()).isEmpty();

		// Similar to above for the capture-the-rest variant
		assertThat(parse("/resource/{*foo}").matchStartOfPath(toPathContainer("/resourceX"))).isNull();
		assertThat(parse("/resource/{*foo}")
				.matchStartOfPath(toPathContainer("/resource")).getPathRemaining().value()).isEmpty();

		PathPattern.PathRemainingMatchInfo pri = parse("/aaa/{bbb}/c?d/e*f/*/g")
				.matchStartOfPath(toPathContainer("/aaa/b/ccd/ef/x/g/i"));
		assertThat(pri).isNotNull();
		assertThat(pri.getPathRemaining().value()).isEqualTo("/i");
		assertThat(pri.getUriVariables()).containsEntry("bbb", "b");

		pri = parse("/aaa/{bbb}/c?d/e*f/*/g/").matchStartOfPath(toPathContainer("/aaa/b/ccd/ef/x/g/i"));
		assertThat(pri).isNotNull();
		assertThat(pri.getPathRemaining().value()).isEqualTo("i");
		assertThat(pri.getUriVariables()).containsEntry("bbb", "b");

		pri = parse("/{aaa}_{bbb}/e*f/{x}/g").matchStartOfPath(toPathContainer("/aa_bb/ef/x/g/i"));
		assertThat(pri).isNotNull();
		assertThat(pri.getPathRemaining().value()).isEqualTo("/i");
		assertThat(pri.getUriVariables()).containsEntry("aaa", "aa");
		assertThat(pri.getUriVariables()).containsEntry("bbb", "bb");
		assertThat(pri.getUriVariables()).containsEntry("x", "x");

		assertThat(parse("/a/b").matchStartOfPath(toPathContainer(""))).isNull();
		assertThat(parse("").matchStartOfPath(toPathContainer("/a/b")).getPathRemaining().value()).isEqualTo("/a/b");
		assertThat(parse("").matchStartOfPath(toPathContainer("")).getPathRemaining().value()).isEmpty();
	}

	@Test
	public void questionMarks() {
		checkNoMatch("a", "ab");
		checkMatches("/f?o/bar", "/foo/bar");
		checkNoMatch("/foo/b2r", "/foo/bar");
		checkNoMatch("?", "te");
		checkMatches("?", "a");
		checkMatches("???", "abc");
		checkNoMatch("tes?", "te");
		checkNoMatch("tes?", "tes");
		checkNoMatch("tes?", "testt");
		checkNoMatch("tes?", "tsst");
		checkMatches(".?.a", ".a.a");
		checkNoMatch(".?.a", ".aba");
		checkMatches("/f?o/bar","/f%20o/bar");
	}

	@Test
	public void captureTheRest() {
		checkMatches("/resource/{*foobar}", "/resource");
		checkNoMatch("/resource/{*foobar}", "/resourceX");
		checkNoMatch("/resource/{*foobar}", "/resourceX/foobar");
		checkMatches("/resource/{*foobar}", "/resource/foobar");
		checkCapture("/resource/{*foobar}", "/resource/foobar", "foobar", "/foobar");
		checkCapture("/customer/{*something}", "/customer/99", "something", "/99");
		checkCapture("/customer/{*something}", "/customer/aa/bb/cc", "something",
				"/aa/bb/cc");
		checkCapture("/customer/{*something}", "/customer/", "something", "/");
		checkCapture("/customer/////{*something}", "/customer/////", "something", "/");
		checkCapture("/customer/////{*something}", "/customer//////", "something", "//");
		checkCapture("/customer//////{*something}", "/customer//////99", "something", "/99");
		checkCapture("/customer//////{*something}", "/customer//////99", "something", "/99");
		checkCapture("/customer/{*something}", "/customer", "something", "");
		checkCapture("/{*something}", "", "something", "");
		checkCapture("/customer/{*something}", "/customer//////99", "something", "//////99");
	}

	@Test
	public void multipleSeparatorsInPattern() {
		PathPattern pp = parse("a//b//c");
		assertThat(pp.toChainString()).isEqualTo("Literal(a) Separator(/) Separator(/) Literal(b) Separator(/) Separator(/) Literal(c)");
		assertMatches(pp,"a//b//c");
		assertThat(parse("a//**").toChainString()).isEqualTo("Literal(a) Separator(/) WildcardTheRest(/**)");
		checkMatches("///abc", "///abc");
		checkNoMatch("///abc", "/abc");
		checkNoMatch("//", "/");
		checkMatches("//", "//");
		checkNoMatch("///abc//d/e", "/abc/d/e");
		checkMatches("///abc//d/e", "///abc//d/e");
		checkNoMatch("///abc//{def}//////xyz", "/abc/foo/xyz");
		checkMatches("///abc//{def}//////xyz", "///abc//p//////xyz");
	}

	@Test
	public void multipleSelectorsInPath() {
		checkNoMatch("/abc", "////abc");
		checkNoMatch("/", "//");
		checkNoMatch("/abc/def/ghi", "/abc//def///ghi");
		checkNoMatch("/abc", "////abc");
		checkMatches("////abc", "////abc");
		checkNoMatch("/", "//");
		checkNoMatch("/abc//def", "/abc/def");
		checkNoMatch("/abc//def///ghi", "/abc/def/ghi");
		checkMatches("/abc//def///ghi", "/abc//def///ghi");
	}

	@Test
	public void multipleSeparatorsInPatternAndPath() {
		checkNoMatch("///one///two///three", "//one/////two///////three");
		checkMatches("//one/////two///////three", "//one/////two///////three");
		checkNoMatch("//one//two//three", "/one/////two/three");
		checkMatches("/one/////two/three", "/one/////two/three");
		checkCapture("///{foo}///bar", "///one///bar", "foo", "one");
	}

	@SuppressWarnings("deprecation")
	@Test
	public void wildcards() {
		checkMatches("/*/bar", "/foo/bar");
		checkNoMatch("/*/bar", "/foo/baz");
		checkNoMatch("/*/bar", "//bar");
		checkMatches("/f*/bar", "/foo/bar");
		checkMatches("/*/bar", "/foo/bar");
		checkMatches("a/*","a/");
		checkMatches("/*","/");
		checkMatches("/*/bar", "/foo/bar");
		checkNoMatch("/*/bar", "/foo/baz");
		checkMatches("/f*/bar", "/foo/bar");
		checkMatches("/*/bar", "/foo/bar");
		checkMatches("/a*b*c*d/bar", "/abcd/bar");
		checkMatches("*a*", "testa");
		checkMatches("a/*", "a/");
		checkNoMatch("a/*", "a//"); // no data for *
		checkMatches("a/*", "a/a/"); // trailing slash, so is allowed
		PathPatternParser ppp = new PathPatternParser();
		ppp.setMatchOptionalTrailingSeparator(false);
		assertThat(ppp.parse("a/*").matches(toPathContainer("a//"))).isFalse();
		checkMatches("a/*", "a/a");
		checkMatches("a/*", "a/a/"); // trailing slash is optional
		checkMatches("/resource/**", "/resource");
		checkNoMatch("/resource/**", "/resourceX");
		checkNoMatch("/resource/**", "/resourceX/foobar");
		checkMatches("/resource/**", "/resource/foobar");
	}

	@Test
	public void constrainedMatches() {
		checkCapture("{foo:[0-9]*}", "123", "foo", "123");
		checkNoMatch("{foo:[0-9]*}", "abc");
		checkNoMatch("/{foo:[0-9]*}", "abc");
		checkCapture("/*/{foo:....}/**", "/foo/barg/foo", "foo", "barg");
		checkCapture("/*/{foo:....}/**", "/foo/barg/abc/def/ghi", "foo", "barg");
		checkNoMatch("{foo:....}", "99");
		checkMatches("{foo:..}", "99");
		checkCapture("/{abc:\\{\\}}", "/{}", "abc", "{}");
		checkCapture("/{abc:\\[\\]}", "/[]", "abc", "[]");
		checkCapture("/{abc:\\\\\\\\}", "/\\\\"); // this is fun...
	}

	@Test
	public void antPathMatcherTests() {
		// test exact matching
		checkMatches("test", "test");
		checkMatches("/test", "/test");
		checkMatches("https://example.org", "https://example.org");
		checkNoMatch("/test.jpg", "test.jpg");
		checkNoMatch("test", "/test");
		checkNoMatch("/test", "test");

		// test matching with ?'s
		checkMatches("t?st", "test");
		checkMatches("??st", "test");
		checkMatches("tes?", "test");
		checkMatches("te??", "test");
		checkMatches("?es?", "test");
		checkNoMatch("tes?", "tes");
		checkNoMatch("tes?", "testt");
		checkNoMatch("tes?", "tsst");

		// test matching with *'s
		checkMatches("*", "test");
		checkMatches("test*", "test");
		checkMatches("test*", "testTest");
		checkMatches("test/*", "test/Test");
		checkMatches("test/*", "test/t");
		checkMatches("test/*", "test/");
		checkMatches("*test*", "AnothertestTest");
		checkMatches("*test", "Anothertest");
		checkMatches("*.*", "test.");
		checkMatches("*.*", "test.test");
		checkMatches("*.*", "test.test.test");
		checkMatches("test*aaa", "testblaaaa");
		checkNoMatch("test*", "tst");
		checkNoMatch("test*", "tsttest");
		checkMatches("test*", "test/"); // trailing slash is optional
		checkMatches("test*", "test"); // trailing slash is optional
		checkNoMatch("test*", "test/t");
		checkNoMatch("test/*", "test");
		checkNoMatch("*test*", "tsttst");
		checkNoMatch("*test", "tsttst");
		checkNoMatch("*.*", "tsttst");
		checkNoMatch("test*aaa", "test");
		checkNoMatch("test*aaa", "testblaaab");

		// test matching with ?'s and /'s
		checkMatches("/?", "/a");
		checkMatches("/?/a", "/a/a");
		checkMatches("/a/?", "/a/b");
		checkMatches("/??/a", "/aa/a");
		checkMatches("/a/??", "/a/bb");
		checkMatches("/?", "/a");

		checkMatches("/**", "");
		checkMatches("/books/**", "/books");
		checkMatches("/**", "/testing/testing");
		checkMatches("/*/**", "/testing/testing");
		checkMatches("/bla*bla/test", "/blaXXXbla/test");
		checkMatches("/*bla/test", "/XXXbla/test");
		checkNoMatch("/bla*bla/test", "/blaXXXbl/test");
		checkNoMatch("/*bla/test", "XXXblab/test");
		checkNoMatch("/*bla/test", "XXXbl/test");
		checkNoMatch("/????", "/bala/bla");
		checkMatches("/foo/bar/**", "/foo/bar/");
		checkMatches("/{bla}.html", "/testing.html");
		checkCapture("/{bla}.*", "/testing.html", "bla", "testing");
	}

	@Test
	public void pathRemainingEnhancements_spr15419() {
		PathPattern pp;
		PathPattern.PathRemainingMatchInfo pri;
		// It would be nice to partially match a path and get any bound variables in one step
		pp = parse("/{this}/{one}/{here}");
		pri = getPathRemaining(pp, "/foo/bar/goo/boo");
		assertThat(pri.getPathRemaining().value()).isEqualTo("/boo");
		assertThat(pri.getPathMatched().value()).isEqualTo("/foo/bar/goo");
		assertThat(pri.getUriVariables()).containsEntry("this", "foo");
		assertThat(pri.getUriVariables()).containsEntry("one", "bar");
		assertThat(pri.getUriVariables()).containsEntry("here", "goo");

		pp = parse("/aaa/{foo}");
		pri = getPathRemaining(pp, "/aaa/bbb");
		assertThat(pri.getPathRemaining().value()).isEmpty();
		assertThat(pri.getPathMatched().value()).isEqualTo("/aaa/bbb");
		assertThat(pri.getUriVariables()).containsEntry("foo", "bbb");

		pp = parse("/aaa/bbb");
		pri = getPathRemaining(pp, "/aaa/bbb");
		assertThat(pri.getPathRemaining().value()).isEmpty();
		assertThat(pri.getPathMatched().value()).isEqualTo("/aaa/bbb");
		assertThat(pri.getUriVariables()).isEmpty();

		pp = parse("/*/{foo}/b*");
		pri = getPathRemaining(pp, "/foo");
		assertThat((Object) pri).isNull();
		pri = getPathRemaining(pp, "/abc/def/bhi");
		assertThat(pri.getPathRemaining().value()).isEmpty();
		assertThat(pri.getUriVariables()).containsEntry("foo", "def");

		pri = getPathRemaining(pp, "/abc/def/bhi/jkl");
		assertThat(pri.getPathRemaining().value()).isEqualTo("/jkl");
		assertThat(pri.getUriVariables()).containsEntry("foo", "def");
	}

	@Test
	public void caseSensitivity() {
		PathPatternParser pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		PathPattern p = pp.parse("abc");
		assertMatches(p,"AbC");
		assertNoMatch(p,"def");
		p = pp.parse("fOo");
		assertMatches(p,"FoO");
		p = pp.parse("/fOo/bAr");
		assertMatches(p,"/FoO/BaR");

		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("abc");
		assertNoMatch(p,"AbC");
		p = pp.parse("fOo");
		assertNoMatch(p,"FoO");
		p = pp.parse("/fOo/bAr");
		assertNoMatch(p,"/FoO/BaR");
		p = pp.parse("/fOO/bAr");
		assertMatches(p,"/fOO/bAr");

		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("{foo:[A-Z]*}");
		assertMatches(p,"abc");
		assertMatches(p,"ABC");

		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("{foo:[A-Z]*}");
		assertNoMatch(p,"abc");
		assertMatches(p,"ABC");

		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("ab?");
		assertMatches(p,"AbC");
		p = pp.parse("fO?");
		assertMatches(p,"FoO");
		p = pp.parse("/fO?/bA?");
		assertMatches(p,"/FoO/BaR");
		assertNoMatch(p,"/bAr/fOo");

		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("ab?");
		assertNoMatch(p,"AbC");
		p = pp.parse("fO?");
		assertNoMatch(p,"FoO");
		p = pp.parse("/fO?/bA?");
		assertNoMatch(p,"/FoO/BaR");
		p = pp.parse("/fO?/bA?");
		assertMatches(p,"/fOO/bAr");

		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("{abc:[A-Z]*}_{def:[A-Z]*}");
		assertMatches(p,"abc_abc");
		assertMatches(p,"ABC_aBc");

		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("{abc:[A-Z]*}_{def:[A-Z]*}");
		assertNoMatch(p,"abc_abc");
		assertMatches(p,"ABC_ABC");

		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("*?a?*");
		assertMatches(p,"bab");
		assertMatches(p,"bAb");

		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("*?A?*");
		assertNoMatch(p,"bab");
		assertMatches(p,"bAb");
	}

	@Test
	public void extractPathWithinPattern_spr15259() {
		checkExtractPathWithinPattern("/**","//","");
		checkExtractPathWithinPattern("/**","/","");
		checkExtractPathWithinPattern("/**","","");
		checkExtractPathWithinPattern("/**","/foobar","foobar");
	}

	@Test
	public void extractPathWithinPattern() throws Exception {
		checkExtractPathWithinPattern("/welcome*/", "/welcome/", "welcome");
		checkExtractPathWithinPattern("/docs/commit.html", "/docs/commit.html", "");
		checkExtractPathWithinPattern("/docs/*", "/docs/cvs/commit", "cvs/commit");
		checkExtractPathWithinPattern("/docs/cvs/*.html", "/docs/cvs/commit.html", "commit.html");
		checkExtractPathWithinPattern("/docs/cvs/file.*.html", "/docs/cvs/file.sha.html", "file.sha.html");
		checkExtractPathWithinPattern("/docs/**", "/docs/cvs/commit", "cvs/commit");
		checkExtractPathWithinPattern("/doo/{*foobar}", "/doo/customer.html", "customer.html");
		checkExtractPathWithinPattern("/doo/{*foobar}", "/doo/daa/customer.html", "daa/customer.html");
		checkExtractPathWithinPattern("/*.html", "/commit.html", "commit.html");
		checkExtractPathWithinPattern("/docs/*/*/*/*", "/docs/cvs/other/commit.html", "cvs/other/commit.html");
		checkExtractPathWithinPattern("/d?cs/**", "/docs/cvs/commit", "docs/cvs/commit");
		checkExtractPathWithinPattern("/*/**", "/docs/cvs/commit///", "docs/cvs/commit");
		checkExtractPathWithinPattern("/*/**", "/docs/cvs/commit/", "docs/cvs/commit");
		checkExtractPathWithinPattern("/aaa/bbb/**", "/aaa///","");
		checkExtractPathWithinPattern("/aaa/bbb/**", "/aaa//","");
		checkExtractPathWithinPattern("/aaa/bbb/**", "/aaa/","");
		checkExtractPathWithinPattern("/docs/**", "/docs/cvs/commit///", "cvs/commit");
		checkExtractPathWithinPattern("/docs/**", "/docs/cvs/commit/", "cvs/commit");
		checkExtractPathWithinPattern("/docs/c?s/*.html", "/docs/cvs/commit.html", "cvs/commit.html");
		checkExtractPathWithinPattern("/d?cs/*/*.html", "/docs/cvs/commit.html", "docs/cvs/commit.html");
		checkExtractPathWithinPattern("/a/b/c*d*/*.html", "/a/b/cod/foo.html", "cod/foo.html");
		checkExtractPathWithinPattern("a/{foo}/b/{bar}", "a/c/b/d", "c/b/d");
		checkExtractPathWithinPattern("a/{foo}_{bar}/d/e", "a/b_c/d/e", "b_c/d/e");
		checkExtractPathWithinPattern("aaa//*///ccc///ddd", "aaa//bbb///ccc///ddd", "bbb/ccc/ddd");
		checkExtractPathWithinPattern("aaa//*///ccc///ddd", "aaa//bbb//ccc/ddd", "bbb/ccc/ddd");
		checkExtractPathWithinPattern("aaa/c*/ddd/", "aaa/ccc///ddd///", "ccc/ddd");
		checkExtractPathWithinPattern("", "", "");
		checkExtractPathWithinPattern("/", "", "");
		checkExtractPathWithinPattern("", "/", "");
		checkExtractPathWithinPattern("//", "", "");
		checkExtractPathWithinPattern("", "//", "");
		checkExtractPathWithinPattern("//", "//", "");
		checkExtractPathWithinPattern("//", "/", "");
		checkExtractPathWithinPattern("/", "//", "");
	}

	@Test
	public void extractPathWithinPatternCustomSeparator() {
		PathPatternParser ppp = new PathPatternParser();
		ppp.setPathOptions(PathContainer.Options.create('.', true));
		PathPattern pp = ppp.parse("test.**");
		PathContainer pathContainer = PathContainer.parsePath(
				"test.projects..spring-framework", PathContainer.Options.create('.', true));
		PathContainer result = pp.extractPathWithinPattern(pathContainer);
		assertThat(result.value()).isEqualTo("projects.spring-framework");
		assertThat(result.elements()).hasSize(3);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void extractUriTemplateVariables_spr15264() {
		PathPattern pp;
		pp = new PathPatternParser().parse("/{foo}");
		assertMatches(pp,"/abc");
		assertNoMatch(pp,"/");
		assertNoMatch(pp,"//");
		checkCapture("/{foo}", "/abc", "foo", "abc");

		pp = new PathPatternParser().parse("/{foo}/{bar}");
		assertMatches(pp,"/abc/def");
		assertNoMatch(pp,"/def");
		assertNoMatch(pp,"/");
		assertNoMatch(pp,"//def");
		assertNoMatch(pp,"//");

		pp = parse("/{foo}/boo");
		assertMatches(pp,"/abc/boo");
		assertMatches(pp,"/a/boo");
		assertNoMatch(pp,"/boo");
		assertNoMatch(pp,"//boo");

		pp = parse("/{foo}*");
		assertMatches(pp,"/abc");
		assertNoMatch(pp,"/");

		checkCapture("/{word:[a-z]*}", "/abc", "word", "abc");
		pp = parse("/{word:[a-z]*}");
		assertNoMatch(pp,"/1");
		assertMatches(pp,"/a");
		assertNoMatch(pp,"/");

		// Two captures mean we use a RegexPathElement
		pp = new PathPatternParser().parse("/{foo}{bar}");
		assertMatches(pp,"/abcdef");
		assertNoMatch(pp,"/");
		assertNoMatch(pp,"//");
		checkCapture("/{foo:[a-z][a-z]}{bar:[a-z]}", "/abc", "foo", "ab", "bar", "c");

		// Only patterns not capturing variables cannot match against just /
		PathPatternParser ppp = new PathPatternParser();
		ppp.setMatchOptionalTrailingSeparator(true);
		pp = ppp.parse("/****");
		assertMatches(pp,"/abcdef");
		assertMatches(pp,"/");
		assertMatches(pp,"/");
		assertMatches(pp,"//");

		// Confirming AntPathMatcher behaviour:
		assertThat(new AntPathMatcher().match("/{foo}", "/")).isFalse();
		assertThat(new AntPathMatcher().match("/{foo}", "/a")).isTrue();
		assertThat(new AntPathMatcher().match("/{foo}{bar}", "/a")).isTrue();
		assertThat(new AntPathMatcher().match("/{foo}*", "/")).isFalse();
		assertThat(new AntPathMatcher().match("/*", "/")).isTrue();
		assertThat(new AntPathMatcher().match("/*{foo}", "/")).isFalse();
		Map<String, String> vars = new AntPathMatcher().extractUriTemplateVariables("/{foo}{bar}", "/a");
		assertThat(vars).containsEntry("foo", "a");
		assertThat(vars.get("bar")).isEmpty();
	}

	@Test
	public void extractUriTemplateVariables() throws Exception {
		assertMatches(parse("{hotel}"),"1");
		assertMatches(parse("/hotels/{hotel}"),"/hotels/1");
		checkCapture("/hotels/{hotel}", "/hotels/1", "hotel", "1");
		checkCapture("/h?tels/{hotel}", "/hotels/1", "hotel", "1");
		checkCapture("/hotels/{hotel}/bookings/{booking}", "/hotels/1/bookings/2", "hotel", "1", "booking", "2");
		checkCapture("/*/hotels/*/{hotel}", "/foo/hotels/bar/1", "hotel", "1");
		checkCapture("/{page}.html", "/42.html", "page", "42");
		checkNoMatch("/{var}","/");
		checkCapture("/{page}.*", "/42.html", "page", "42");
		checkCapture("/A-{B}-C", "/A-b-C", "B", "b");
		checkCapture("/{name}.{extension}", "/test.html", "name", "test", "extension", "html");

		assertThat((Object) checkCapture("/{one}/", "//")).isNull();
		assertThat((Object) checkCapture("", "/abc")).isNull();

		assertThat(checkCapture("", "").getUriVariables()).isEmpty();
		checkCapture("{id}", "99", "id", "99");
		checkCapture("/customer/{customerId}", "/customer/78", "customerId", "78");
		checkCapture("/customer/{customerId}/banana", "/customer/42/banana", "customerId",
				"42");
		checkCapture("{id}/{id2}", "99/98", "id", "99", "id2", "98");
		checkCapture("/foo/{bar}/boo/{baz}", "/foo/plum/boo/apple", "bar", "plum", "baz",
				"apple");
		checkCapture("/{bla}.*", "/testing.html", "bla", "testing");
		PathPattern.PathMatchInfo extracted = checkCapture("/abc", "/abc");
		assertThat(extracted.getUriVariables()).isEmpty();
		checkCapture("/{bla}/foo","/a/foo");
	}

	@Test
	public void extractUriTemplateVariablesRegex() {
		PathPatternParser pp = new PathPatternParser();
		PathPattern p = null;

		p = pp.parse("{symbolicName:[\\w\\.]+}-{version:[\\w\\.]+}.jar");
		PathPattern.PathMatchInfo result = matchAndExtract(p, "com.example-1.0.0.jar");
		assertThat(result.getUriVariables()).containsEntry("symbolicName", "com.example");
		assertThat(result.getUriVariables()).containsEntry("version", "1.0.0");

		p = pp.parse("{symbolicName:[\\w\\.]+}-sources-{version:[\\w\\.]+}.jar");
		result = matchAndExtract(p, "com.example-sources-1.0.0.jar");
		assertThat(result.getUriVariables()).containsEntry("symbolicName", "com.example");
		assertThat(result.getUriVariables()).containsEntry("version", "1.0.0");
	}

	@Test
	public void extractUriTemplateVarsRegexQualifiers() {
		PathPatternParser pp = new PathPatternParser();

		PathPattern p = pp.parse("{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar");
		PathPattern.PathMatchInfo result = p.matchAndExtract(toPathContainer("com.example-sources-1.0.0.jar"));
		assertThat(result.getUriVariables()).containsEntry("symbolicName", "com.example");
		assertThat(result.getUriVariables()).containsEntry("version", "1.0.0");

		p = pp.parse("{symbolicName:[\\w\\.]+}-sources-" +
				"{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar");
		result = matchAndExtract(p,"com.example-sources-1.0.0-20100220.jar");
		assertThat(result.getUriVariables()).containsEntry("symbolicName", "com.example");
		assertThat(result.getUriVariables()).containsEntry("version", "1.0.0");
		assertThat(result.getUriVariables()).containsEntry("year", "2010");
		assertThat(result.getUriVariables()).containsEntry("month", "02");
		assertThat(result.getUriVariables()).containsEntry("day", "20");

		p = pp.parse("{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar");
		result = matchAndExtract(p, "com.example-sources-1.0.0.{12}.jar");
		assertThat(result.getUriVariables()).containsEntry("symbolicName", "com.example");
		assertThat(result.getUriVariables()).containsEntry("version", "1.0.0.{12}");
	}

	@Test
	public void extractUriTemplateVarsRegexCapturingGroups() {
		PathPatternParser ppp = new PathPatternParser();
		PathPattern pathPattern = ppp.parse("/web/{id:foo(bar)?}_{goo}");
		assertThatIllegalArgumentException().isThrownBy(() ->
				matchAndExtract(pathPattern,"/web/foobar_goo"))
			.withMessageContaining("The number of capturing groups in the pattern");
	}

	@Test
	public void combine() {
		TestPathCombiner pathMatcher = new TestPathCombiner();
		assertThat(pathMatcher.combine("", "")).isEmpty();
		assertThat(pathMatcher.combine("/hotels", "")).isEqualTo("/hotels");
		assertThat(pathMatcher.combine("", "/hotels")).isEqualTo("/hotels");
		assertThat(pathMatcher.combine("/hotels/*", "booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels/*", "/booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels", "/booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels", "booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels/", "booking")).isEqualTo("/hotels/booking");
		assertThat(pathMatcher.combine("/hotels/*", "{hotel}")).isEqualTo("/hotels/{hotel}");
		assertThat(pathMatcher.combine("/hotels", "{hotel}")).isEqualTo("/hotels/{hotel}");
		assertThat(pathMatcher.combine("/hotels", "{hotel}.*")).isEqualTo("/hotels/{hotel}.*");
		assertThat(pathMatcher.combine("/hotels/*/booking", "{booking}")).isEqualTo("/hotels/*/booking/{booking}");
		assertThat(pathMatcher.combine("/*.html", "/hotel.html")).isEqualTo("/hotel.html");
		assertThat(pathMatcher.combine("/*.html", "/hotel")).isEqualTo("/hotel.html");
		assertThat(pathMatcher.combine("/*.html", "/hotel.*")).isEqualTo("/hotel.html");
		// TODO this seems rather bogus, should we eagerly show an error?
		assertThat(pathMatcher.combine("/a/b/c/*.html", "/d/e/f/hotel.*")).isEqualTo("/d/e/f/hotel.html");
		assertThat(pathMatcher.combine("/**", "/*.html")).isEqualTo("/*.html");
		assertThat(pathMatcher.combine("/*", "/*.html")).isEqualTo("/*.html");
		assertThat(pathMatcher.combine("/*.*", "/*.html")).isEqualTo("/*.html");
		// SPR-8858
		assertThat(pathMatcher.combine("/{foo}", "/bar")).isEqualTo("/{foo}/bar");
		// SPR-7970
		assertThat(pathMatcher.combine("/user", "/user")).isEqualTo("/user/user");
		// SPR-10062
		assertThat(pathMatcher.combine("/{foo:.*[^0-9].*}", "/edit/")).isEqualTo("/{foo:.*[^0-9].*}/edit/");
		assertThat(pathMatcher.combine("/1.0", "/foo/test")).isEqualTo("/1.0/foo/test");
		// SPR-10554
		// SPR-12975
		assertThat(pathMatcher.combine("/", "/hotel")).isEqualTo("/hotel");
		// SPR-12975
		assertThat(pathMatcher.combine("/hotel/", "/booking")).isEqualTo("/hotel/booking");
		assertThat(pathMatcher.combine("", "/hotel")).isEqualTo("/hotel");
		assertThat(pathMatcher.combine("/hotel", "")).isEqualTo("/hotel");
		// TODO Do we need special handling when patterns contain multiple dots?
	}

	@Test
	public void combineWithTwoFileExtensionPatterns() {
		TestPathCombiner pathMatcher = new TestPathCombiner();
		assertThatIllegalArgumentException().isThrownBy(() ->
				pathMatcher.combine("/*.html", "/*.txt"));
	}

	@Test
	public void patternComparator() {
		Comparator<PathPattern> comparator = PathPattern.SPECIFICITY_COMPARATOR;

		assertThat(comparator.compare(parse("/hotels/new"), parse("/hotels/new"))).isEqualTo(0);

		assertThat(comparator.compare(parse("/hotels/new"), parse("/hotels/*"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("/hotels/*"), parse("/hotels/new"))).isEqualTo(1);
		assertThat(comparator.compare(parse("/hotels/*"), parse("/hotels/*"))).isEqualTo(0);

		assertThat(comparator.compare(parse("/hotels/new"), parse("/hotels/{hotel}"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("/hotels/{hotel}"), parse("/hotels/new"))).isEqualTo(1);
		assertThat(comparator.compare(parse("/hotels/{hotel}"), parse("/hotels/{hotel}"))).isEqualTo(0);
		assertThat(comparator.compare(parse("/hotels/{hotel}/booking"),
				parse("/hotels/{hotel}/bookings/{booking}"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("/hotels/{hotel}/bookings/{booking}"),
				parse("/hotels/{hotel}/booking"))).isEqualTo(1);

		assertThat(comparator.compare(
						parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"),
						parse("/**"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("/**"),
				parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"))).isEqualTo(1);
		assertThat(comparator.compare(parse("/**"), parse("/**"))).isEqualTo(0);

		assertThat(comparator.compare(parse("/hotels/{hotel}"), parse("/hotels/*"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("/hotels/*"), parse("/hotels/{hotel}"))).isEqualTo(1);

		assertThat(comparator.compare(parse("/hotels/*"), parse("/hotels/*/**"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("/hotels/*/**"), parse("/hotels/*"))).isEqualTo(1);

// TODO: shouldn't the wildcard lower the score?
//		assertEquals(-1,
//				comparator.compare(parse("/hotels/new"), parse("/hotels/new.*")));

		// SPR-6741
		assertThat(comparator.compare(
						parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"),
						parse("/hotels/**"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("/hotels/**"),
				parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"))).isEqualTo(1);
		assertThat(comparator.compare(parse("/hotels/foo/bar/**"),
				parse("/hotels/{hotel}"))).isEqualTo(1);
		assertThat(comparator.compare(parse("/hotels/{hotel}"),
				parse("/hotels/foo/bar/**"))).isEqualTo(-1);

		// SPR-8683
		assertThat(comparator.compare(parse("/**"), parse("/hotels/{hotel}"))).isEqualTo(1);

		// longer is better
		assertThat(comparator.compare(parse("/hotels"), parse("/hotels2"))).isEqualTo(1);

		// SPR-13139
		assertThat(comparator.compare(parse("*"), parse("*/**"))).isEqualTo(-1);
		assertThat(comparator.compare(parse("*/**"), parse("*"))).isEqualTo(1);
	}

	@Test
	public void compare_spr15597() {
		PathPatternParser parser = new PathPatternParser();
		PathPattern p1 = parser.parse("/{foo}");
		PathPattern p2 = parser.parse("/{foo}.*");
		PathPattern.PathMatchInfo r1 = matchAndExtract(p1, "/file.txt");
		PathPattern.PathMatchInfo r2 = matchAndExtract(p2, "/file.txt");

		// works fine
		assertThat(r1.getUriVariables()).containsEntry("foo", "file.txt");
		assertThat(r2.getUriVariables()).containsEntry("foo", "file");

		// This produces 2 (see comments in https://jira.spring.io/browse/SPR-14544 )
		// Comparator<String> patternComparator = new AntPathMatcher().getPatternComparator("");
		// System.out.println(patternComparator.compare("/{foo}","/{foo}.*"));

		assertThat(p1.compareTo(p2)).isGreaterThan(0);
	}

	@Test
	public void patternCompareWithNull() {
		assertThat(PathPattern.SPECIFICITY_COMPARATOR.compare(null, null)).isEqualTo(0);
		assertThat(PathPattern.SPECIFICITY_COMPARATOR.compare(parse("/abc"), null)).isLessThan(0);
		assertThat(PathPattern.SPECIFICITY_COMPARATOR.compare(null, parse("/abc"))).isGreaterThan(0);
	}

	@Test
	public void patternComparatorSort() {
		Comparator<PathPattern> comparator = PathPattern.SPECIFICITY_COMPARATOR;

		List<PathPattern> paths = new ArrayList<>(3);
		PathPatternParser pp = new PathPatternParser();
		paths.add(null);
		paths.add(null);
		paths.sort(comparator);
		assertThat((Object) paths.get(0)).isNull();
		assertThat((Object) paths.get(1)).isNull();
		paths.clear();

		paths.add(null);
		paths.add(pp.parse("/hotels/new"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/new");
		assertThat(paths.get(1)).isNull();
		paths.clear();

		paths.add(pp.parse("/hotels/*"));
		paths.add(pp.parse("/hotels/new"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/new");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/*");
		paths.clear();

		paths.add(pp.parse("/hotels/new"));
		paths.add(pp.parse("/hotels/*"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/new");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/*");
		paths.clear();

		paths.add(pp.parse("/hotels/**"));
		paths.add(pp.parse("/hotels/*"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/*");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/**");
		paths.clear();

		paths.add(pp.parse("/hotels/*"));
		paths.add(pp.parse("/hotels/**"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/*");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/**");
		paths.clear();

		paths.add(pp.parse("/hotels/{hotel}"));
		paths.add(pp.parse("/hotels/new"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/new");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/{hotel}");
		paths.clear();

		paths.add(pp.parse("/hotels/new"));
		paths.add(pp.parse("/hotels/{hotel}"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/new");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/{hotel}");
		paths.clear();

		paths.add(pp.parse("/hotels/*"));
		paths.add(pp.parse("/hotels/{hotel}"));
		paths.add(pp.parse("/hotels/new"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/new");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/{hotel}");
		assertThat(paths.get(2).getPatternString()).isEqualTo("/hotels/*");
		paths.clear();

		paths.add(pp.parse("/hotels/ne*"));
		paths.add(pp.parse("/hotels/n*"));
		Collections.shuffle(paths);
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/hotels/ne*");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/hotels/n*");
		paths.clear();

		// comparator = new PatternComparatorConsideringPath("/hotels/new.html");
		// paths.add(pp.parse("/hotels/new.*"));
		// paths.add(pp.parse("/hotels/{hotel}"));
		// Collections.shuffle(paths);
		// Collections.sort(paths, comparator);
		// assertEquals("/hotels/new.*", paths.get(0).toPatternString());
		// assertEquals("/hotels/{hotel}", paths.get(1).toPatternString());
		// paths.clear();

		comparator = (p1, p2) -> {
			int index = p1.compareTo(p2);
			return (index != 0 ? index : p1.getPatternString().compareTo(p2.getPatternString()));
		};
		paths.add(pp.parse("/*/login.*"));
		paths.add(pp.parse("/*/endUser/action/login.*"));
		paths.sort(comparator);
		assertThat(paths.get(0).getPatternString()).isEqualTo("/*/endUser/action/login.*");
		assertThat(paths.get(1).getPatternString()).isEqualTo("/*/login.*");
		paths.clear();
	}

	@Test  // SPR-13286
	public void caseInsensitive() {
		PathPatternParser pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		PathPattern p = pp.parse("/group/{groupName}/members");
		assertMatches(p,"/group/sales/members");
		assertMatches(p,"/Group/Sales/Members");
		assertMatches(p,"/group/Sales/members");
	}

	@Test
	public void parameters() {
		// CaptureVariablePathElement
		PathPattern.PathMatchInfo result = matchAndExtract("/abc/{var}","/abc/one;two=three;four=five");
		assertThat(result.getUriVariables()).containsEntry("var", "one");
		assertThat(result.getMatrixVariables().get("var").getFirst("two")).isEqualTo("three");
		assertThat(result.getMatrixVariables().get("var").getFirst("four")).isEqualTo("five");
		// RegexPathElement
		result = matchAndExtract("/abc/{var1}_{var2}","/abc/123_456;a=b;c=d");
		assertThat(result.getUriVariables()).containsEntry("var1", "123");
		assertThat(result.getUriVariables()).containsEntry("var2", "456");
		// vars associated with second variable
		assertThat(result.getMatrixVariables()).doesNotContainKey("var1");
		assertThat(result.getMatrixVariables()).doesNotContainKey("var1");
		assertThat(result.getMatrixVariables().get("var2").getFirst("a")).isEqualTo("b");
		assertThat(result.getMatrixVariables().get("var2").getFirst("c")).isEqualTo("d");
		// CaptureTheRestPathElement
		result = matchAndExtract("/{*var}","/abc/123_456;a=b;c=d");
		assertThat(result.getUriVariables()).containsEntry("var", "/abc/123_456");
		assertThat(result.getMatrixVariables().get("var").getFirst("a")).isEqualTo("b");
		assertThat(result.getMatrixVariables().get("var").getFirst("c")).isEqualTo("d");
		result = matchAndExtract("/{*var}","/abc/123_456;a=b;c=d/789;a=e;f=g");
		assertThat(result.getUriVariables()).containsEntry("var", "/abc/123_456/789");
		assertThat(result.getMatrixVariables().get("var").get("a").toString()).isEqualTo("[b, e]");
		assertThat(result.getMatrixVariables().get("var").getFirst("c")).isEqualTo("d");
		assertThat(result.getMatrixVariables().get("var").getFirst("f")).isEqualTo("g");

		result = matchAndExtract("/abc/{var}","/abc/one");
		assertThat(result.getUriVariables()).containsEntry("var", "one");
		assertThat(result.getMatrixVariables()).doesNotContainKey("var");

		result = matchAndExtract("","");
		assertThat(result).isNotNull();
		result = matchAndExtract("","/");
		assertThat(result).isNotNull();
	}

	private PathPattern.PathMatchInfo matchAndExtract(String pattern, String path) {
		return parse(pattern).matchAndExtract(PathPatternTests.toPathContainer(path));
	}

	@SuppressWarnings("deprecation")
	private PathPattern parse(String path) {
		PathPatternParser pp = new PathPatternParser();
		pp.setMatchOptionalTrailingSeparator(true);
		return pp.parse(path);
	}

	public static PathContainer toPathContainer(String path) {
		if (path == null) {
			return null;
		}
		return PathContainer.parsePath(path);
	}

	@SuppressWarnings("deprecation")
	private void checkMatches(String uriTemplate, String path) {
		PathPatternParser parser = new PathPatternParser();
		parser.setMatchOptionalTrailingSeparator(true);
		PathPattern p = parser.parse(uriTemplate);
		PathContainer pc = toPathContainer(path);
		assertThat(p.matches(pc)).isTrue();
	}

	private void checkNoMatch(String uriTemplate, String path) {
		PathPatternParser p = new PathPatternParser();
		PathPattern pattern = p.parse(uriTemplate);
		PathContainer PathContainer = toPathContainer(path);
		assertThat(pattern.matches(PathContainer)).isFalse();
	}

	private PathPattern.PathMatchInfo checkCapture(String uriTemplate, String path, String... keyValues) {
		PathPatternParser parser = new PathPatternParser();
		PathPattern pattern = parser.parse(uriTemplate);
		PathPattern.PathMatchInfo matchResult = pattern.matchAndExtract(toPathContainer(path));
		Map<String, String> expectedKeyValues = new HashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			expectedKeyValues.put(keyValues[i], keyValues[i + 1]);
		}
		for (Map.Entry<String, String> me : expectedKeyValues.entrySet()) {
			String value = matchResult.getUriVariables().get(me.getKey());
			assertThat(value).as("value for " + me.getKey()).isEqualTo(me.getValue());
		}
		return matchResult;
	}

	private void checkExtractPathWithinPattern(String pattern, String path, String expected) {
		PathPatternParser ppp = new PathPatternParser();
		PathPattern pp = ppp.parse(pattern);
		String s = pp.extractPathWithinPattern(toPathContainer(path)).value();
		assertThat(s).isEqualTo(expected);
	}

	private PathRemainingMatchInfo getPathRemaining(String pattern, String path) {
		return parse(pattern).matchStartOfPath(toPathContainer(path));
	}

	private PathRemainingMatchInfo getPathRemaining(PathPattern pattern, String path) {
		return pattern.matchStartOfPath(toPathContainer(path));
	}

	private PathPattern.PathMatchInfo matchAndExtract(PathPattern p, String path) {
		return p.matchAndExtract(toPathContainer(path));
	}

	private String elementsToString(List<Element> elements) {
		StringBuilder s = new StringBuilder();
		for (Element element: elements) {
			s.append('[').append(element.value()).append(']');
		}
		return s.toString();
	}


	static class TestPathCombiner {

		PathPatternParser pp = new PathPatternParser();

		public String combine(String string1, String string2) {
			PathPattern pattern1 = pp.parse(string1);
			PathPattern pattern2 = pp.parse(string2);
			return pattern1.combine(pattern2).getPatternString();
		}

	}

}
