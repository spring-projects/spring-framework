/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.util.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PatternParseException.PatternMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Exercise the {@link PathPatternParser}.
 * @author Andy Clement
 */
public class PathPatternParserTests {

	private PathPattern pathPattern;

	@Test
	public void basicPatterns() {
		checkStructure("/");
		checkStructure("/foo");
		checkStructure("foo");
		checkStructure("foo/");
		checkStructure("/foo/");
		checkStructure("");
	}

	@Test
	public void singleCharWildcardPatterns() {
		pathPattern = checkStructure("?");
		assertPathElements(pathPattern, SingleCharWildcardedPathElement.class);
		checkStructure("/?/");
		checkStructure("/?abc?/");
	}

	@Test
	public void multiwildcardPattern() {
		pathPattern = checkStructure("/**");
		assertPathElements(pathPattern, WildcardTheRestPathElement.class);
		// this is not double wildcard, it's / then **acb (an odd, unnecessary use of double *)
		pathPattern = checkStructure("/**acb");
		assertPathElements(pathPattern, SeparatorPathElement.class, RegexPathElement.class);
	}

	@Test
	public void toStringTests() {
		assertEquals("CaptureTheRest(/{*foobar})", checkStructure("/{*foobar}").toChainString());
		assertEquals("CaptureVariable({foobar})", checkStructure("{foobar}").toChainString());
		assertEquals("Literal(abc)", checkStructure("abc").toChainString());
		assertEquals("Regex({a}_*_{b})", checkStructure("{a}_*_{b}").toChainString());
		assertEquals("Separator(/)", checkStructure("/").toChainString());
		assertEquals("SingleCharWildcarded(?a?b?c)", checkStructure("?a?b?c").toChainString());
		assertEquals("Wildcard(*)", checkStructure("*").toChainString());
		assertEquals("WildcardTheRest(/**)", checkStructure("/**").toChainString());
	}

	@Test
	public void captureTheRestPatterns() {
		pathPattern = parse("{*foobar}");
		assertEquals("/{*foobar}", pathPattern.computePatternString());
		assertPathElements(pathPattern, CaptureTheRestPathElement.class);
		pathPattern = checkStructure("/{*foobar}");
		assertPathElements(pathPattern, CaptureTheRestPathElement.class);
		checkError("/{*foobar}/", 10, PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
		checkError("/{*foobar}abc", 10, PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
		checkError("/{*f%obar}", 4, PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR);
		checkError("/{*foobar}abc", 10, PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
		checkError("/{f*oobar}", 3, PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR);
		checkError("/{*foobar}/abc", 10, PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
		checkError("/{*foobar:.*}/abc", 9, PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR);
		checkError("/{abc}{*foobar}", 1, PatternMessage.CAPTURE_ALL_IS_STANDALONE_CONSTRUCT);
		checkError("/{abc}{*foobar}{foo}", 15, PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
	}

	@Test
	public void equalsAndHashcode() {
		PathPatternParser caseInsensitiveParser = new PathPatternParser();
		caseInsensitiveParser.setCaseSensitive(false);
		PathPatternParser caseSensitiveParser = new PathPatternParser();
		PathPattern pp1 = caseInsensitiveParser.parse("/abc");
		PathPattern pp2 = caseInsensitiveParser.parse("/abc");
		PathPattern pp3 = caseInsensitiveParser.parse("/def");
		assertEquals(pp1, pp2);
		assertEquals(pp1.hashCode(), pp2.hashCode());
		assertNotEquals(pp1, pp3);
		assertFalse(pp1.equals("abc"));

		pp1 = caseInsensitiveParser.parse("/abc");
		pp2 = caseSensitiveParser.parse("/abc");
		assertFalse(pp1.equals(pp2));
		assertNotEquals(pp1.hashCode(), pp2.hashCode());
	}

	@Test
	public void regexPathElementPatterns() {
		checkError("/{var:[^/]*}", 8, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("/{var:abc", 8, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("/{var:a{{1,2}}}", 6, PatternMessage.REGEX_PATTERN_SYNTAX_EXCEPTION);

		pathPattern = checkStructure("/{var:\\\\}");
		PathElement next = pathPattern.getHeadSection().next;
		assertEquals(CaptureVariablePathElement.class.getName(), next.getClass().getName());
		assertMatches(pathPattern,"/\\");

		pathPattern = checkStructure("/{var:\\/}");
		next = pathPattern.getHeadSection().next;
		assertEquals(CaptureVariablePathElement.class.getName(), next.getClass().getName());
		assertNoMatch(pathPattern,"/aaa");

		pathPattern = checkStructure("/{var:a{1,2}}");
		next = pathPattern.getHeadSection().next;
		assertEquals(CaptureVariablePathElement.class.getName(), next.getClass().getName());

		pathPattern = checkStructure("/{var:[^\\/]*}");
		next = pathPattern.getHeadSection().next;
		assertEquals(CaptureVariablePathElement.class.getName(), next.getClass().getName());
		PathPattern.PathMatchInfo result = matchAndExtract(pathPattern,"/foo");
		assertEquals("foo", result.getUriVariables().get("var"));

		pathPattern = checkStructure("/{var:\\[*}");
		next = pathPattern.getHeadSection().next;
		assertEquals(CaptureVariablePathElement.class.getName(), next.getClass().getName());
		result = matchAndExtract(pathPattern,"/[[[");
		assertEquals("[[[", result.getUriVariables().get("var"));

		pathPattern = checkStructure("/{var:[\\{]*}");
		next = pathPattern.getHeadSection().next;
		assertEquals(CaptureVariablePathElement.class.getName(), next.getClass().getName());
		result = matchAndExtract(pathPattern,"/{{{");
		assertEquals("{{{", result.getUriVariables().get("var"));

		pathPattern = checkStructure("/{var:[\\}]*}");
		next = pathPattern.getHeadSection().next;
		assertEquals(CaptureVariablePathElement.class.getName(), next.getClass().getName());
		result = matchAndExtract(pathPattern,"/}}}");
		assertEquals("}}}", result.getUriVariables().get("var"));

		pathPattern = checkStructure("*");
		assertEquals(WildcardPathElement.class.getName(), pathPattern.getHeadSection().getClass().getName());
		checkStructure("/*");
		checkStructure("/*/");
		checkStructure("*/");
		checkStructure("/*/");
		pathPattern = checkStructure("/*a*/");
		next = pathPattern.getHeadSection().next;
		assertEquals(RegexPathElement.class.getName(), next.getClass().getName());
		pathPattern = checkStructure("*/");
		assertEquals(WildcardPathElement.class.getName(), pathPattern.getHeadSection().getClass().getName());
		checkError("{foo}_{foo}", 0, PatternMessage.ILLEGAL_DOUBLE_CAPTURE, "foo");
		checkError("/{bar}/{bar}", 7, PatternMessage.ILLEGAL_DOUBLE_CAPTURE, "bar");
		checkError("/{bar}/{bar}_{foo}", 7, PatternMessage.ILLEGAL_DOUBLE_CAPTURE, "bar");

		pathPattern = checkStructure("{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar");
		assertEquals(RegexPathElement.class.getName(), pathPattern.getHeadSection().getClass().getName());
	}

	@Test
	public void completeCapturingPatterns() {
		pathPattern = checkStructure("{foo}");
		assertEquals(CaptureVariablePathElement.class.getName(), pathPattern.getHeadSection().getClass().getName());
		checkStructure("/{foo}");
		checkStructure("/{f}/");
		checkStructure("/{foo}/{bar}/{wibble}");
	}
	
	@Test
	public void noEncoding() {
		// Check no encoding of expressions or constraints
		PathPattern pp = parse("/{var:f o}");
		assertEquals("Separator(/) CaptureVariable({var:f o})",pp.toChainString());

		pp = parse("/{var:f o}_");
		assertEquals("Separator(/) Regex({var:f o}_)",pp.toChainString());
		
		pp = parse("{foo:f o}_ _{bar:b\\|o}");
		assertEquals("Regex({foo:f o}_ _{bar:b\\|o})",pp.toChainString());
	}

	@Test
	public void completeCaptureWithConstraints() {
		pathPattern = checkStructure("{foo:...}");
		assertPathElements(pathPattern, CaptureVariablePathElement.class);
		pathPattern = checkStructure("{foo:[0-9]*}");
		assertPathElements(pathPattern, CaptureVariablePathElement.class);
		checkError("{foo:}", 5, PatternMessage.MISSING_REGEX_CONSTRAINT);
	}

	@Test
	public void partialCapturingPatterns() {
		pathPattern = checkStructure("{foo}abc");
		assertEquals(RegexPathElement.class.getName(), pathPattern.getHeadSection().getClass().getName());
		checkStructure("abc{foo}");
		checkStructure("/abc{foo}");
		checkStructure("{foo}def/");
		checkStructure("/abc{foo}def/");
		checkStructure("{foo}abc{bar}");
		checkStructure("{foo}abc{bar}/");
		checkStructure("/{foo}abc{bar}/");
	}

	@Test
	public void illegalCapturePatterns() {
		checkError("{abc/", 4, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("{abc:}/", 5, PatternMessage.MISSING_REGEX_CONSTRAINT);
		checkError("{", 1, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("{abc", 4, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("{/}", 1, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("/{", 2, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("}", 0, PatternMessage.MISSING_OPEN_CAPTURE);
		checkError("/}", 1, PatternMessage.MISSING_OPEN_CAPTURE);
		checkError("def}", 3, PatternMessage.MISSING_OPEN_CAPTURE);
		checkError("/{/}", 2, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("/{{/}", 2, PatternMessage.ILLEGAL_NESTED_CAPTURE);
		checkError("/{abc{/}", 5, PatternMessage.ILLEGAL_NESTED_CAPTURE);
		checkError("/{0abc}/abc", 2, PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR);
		checkError("/{a?bc}/abc", 3, PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR);
		checkError("/{abc}_{abc}", 1, PatternMessage.ILLEGAL_DOUBLE_CAPTURE);
		checkError("/foobar/{abc}_{abc}", 8, PatternMessage.ILLEGAL_DOUBLE_CAPTURE);
		checkError("/foobar/{abc:..}_{abc:..}", 8, PatternMessage.ILLEGAL_DOUBLE_CAPTURE);
		PathPattern pp = parse("/{abc:foo(bar)}");
		try {
			pp.matchAndExtract(toPSC("/foo"));
			fail("Should have raised exception");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("No capture groups allowed in the constraint regex: foo(bar)", iae.getMessage());
		}
		try {
			pp.matchAndExtract(toPSC("/foobar"));
			fail("Should have raised exception");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("No capture groups allowed in the constraint regex: foo(bar)", iae.getMessage());
		}
	}

	@Test
	public void badPatterns() {
//		checkError("/{foo}{bar}/",6,PatternMessage.CANNOT_HAVE_ADJACENT_CAPTURES);
		checkError("/{?}/", 2, PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR, "?");
		checkError("/{a?b}/", 3, PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR, "?");
		checkError("/{%%$}", 2, PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR, "%");
		checkError("/{ }", 2, PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR, " ");
		checkError("/{%:[0-9]*}", 2, PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR, "%");
	}

	@Test
	public void patternPropertyGetCaptureCountTests() {
		// Test all basic section types
		assertEquals(1, parse("{foo}").getCapturedVariableCount());
		assertEquals(0, parse("foo").getCapturedVariableCount());
		assertEquals(1, parse("{*foobar}").getCapturedVariableCount());
		assertEquals(1, parse("/{*foobar}").getCapturedVariableCount());
		assertEquals(0, parse("/**").getCapturedVariableCount());
		assertEquals(1, parse("{abc}asdf").getCapturedVariableCount());
		assertEquals(1, parse("{abc}_*").getCapturedVariableCount());
		assertEquals(2, parse("{abc}_{def}").getCapturedVariableCount());
		assertEquals(0, parse("/").getCapturedVariableCount());
		assertEquals(0, parse("a?b").getCapturedVariableCount());
		assertEquals(0, parse("*").getCapturedVariableCount());

		// Test on full templates
		assertEquals(0, parse("/foo/bar").getCapturedVariableCount());
		assertEquals(1, parse("/{foo}").getCapturedVariableCount());
		assertEquals(2, parse("/{foo}/{bar}").getCapturedVariableCount());
		assertEquals(4, parse("/{foo}/{bar}_{goo}_{wibble}/abc/bar").getCapturedVariableCount());
	}

	@Test
	public void patternPropertyGetWildcardCountTests() {
		// Test all basic section types
		assertEquals(computeScore(1, 0), parse("{foo}").getScore());
		assertEquals(computeScore(0, 0), parse("foo").getScore());
		assertEquals(computeScore(0, 0), parse("{*foobar}").getScore());
//		assertEquals(1,parse("/**").getScore());
		assertEquals(computeScore(1, 0), parse("{abc}asdf").getScore());
		assertEquals(computeScore(1, 1), parse("{abc}_*").getScore());
		assertEquals(computeScore(2, 0), parse("{abc}_{def}").getScore());
		assertEquals(computeScore(0, 0), parse("/").getScore());
		assertEquals(computeScore(0, 0), parse("a?b").getScore()); // currently deliberate
		assertEquals(computeScore(0, 1), parse("*").getScore());

		// Test on full templates
		assertEquals(computeScore(0, 0), parse("/foo/bar").getScore());
		assertEquals(computeScore(1, 0), parse("/{foo}").getScore());
		assertEquals(computeScore(2, 0), parse("/{foo}/{bar}").getScore());
		assertEquals(computeScore(4, 0), parse("/{foo}/{bar}_{goo}_{wibble}/abc/bar").getScore());
		assertEquals(computeScore(4, 3), parse("/{foo}/*/*_*/{bar}_{goo}_{wibble}/abc/bar").getScore());
	}

	@Test
	public void multipleSeparatorPatterns() {
		pathPattern = checkStructure("///aaa");
		assertEquals(6, pathPattern.getNormalizedLength());
		assertPathElements(pathPattern, SeparatorPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, LiteralPathElement.class);
		pathPattern = checkStructure("///aaa////aaa/b");
		assertEquals(15, pathPattern.getNormalizedLength());
		assertPathElements(pathPattern, SeparatorPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, LiteralPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, SeparatorPathElement.class, SeparatorPathElement.class,
				LiteralPathElement.class, SeparatorPathElement.class, LiteralPathElement.class);
		pathPattern = checkStructure("/////**");
		assertEquals(5, pathPattern.getNormalizedLength());
		assertPathElements(pathPattern, SeparatorPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, SeparatorPathElement.class, WildcardTheRestPathElement.class);
	}

	@Test
	public void patternPropertyGetLengthTests() {
		// Test all basic section types
		assertEquals(1, parse("{foo}").getNormalizedLength());
		assertEquals(3, parse("foo").getNormalizedLength());
		assertEquals(1, parse("{*foobar}").getNormalizedLength());
		assertEquals(1, parse("/{*foobar}").getNormalizedLength());
		assertEquals(1, parse("/**").getNormalizedLength());
		assertEquals(5, parse("{abc}asdf").getNormalizedLength());
		assertEquals(3, parse("{abc}_*").getNormalizedLength());
		assertEquals(3, parse("{abc}_{def}").getNormalizedLength());
		assertEquals(1, parse("/").getNormalizedLength());
		assertEquals(3, parse("a?b").getNormalizedLength());
		assertEquals(1, parse("*").getNormalizedLength());

		// Test on full templates
		assertEquals(8, parse("/foo/bar").getNormalizedLength());
		assertEquals(2, parse("/{foo}").getNormalizedLength());
		assertEquals(4, parse("/{foo}/{bar}").getNormalizedLength());
		assertEquals(16, parse("/{foo}/{bar}_{goo}_{wibble}/abc/bar").getNormalizedLength());
	}

	@Test
	public void compareTests() {
		PathPattern p1, p2, p3;

		// Based purely on number of captures
		p1 = parse("{a}");
		p2 = parse("{a}/{b}");
		p3 = parse("{a}/{b}/{c}");
		assertEquals(-1, p1.compareTo(p2)); // Based on number of captures
		List<PathPattern> patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertEquals(p1, patterns.get(0));

		// Based purely on length
		p1 = parse("/a/b/c");
		p2 = parse("/a/boo/c/doo");
		p3 = parse("/asdjflaksjdfjasdf");
		assertEquals(1, p1.compareTo(p2));
		patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertEquals(p3, patterns.get(0));

		// Based purely on 'wildness'
		p1 = parse("/*");
		p2 = parse("/*/*");
		p3 = parse("/*/*/*_*");
		assertEquals(-1, p1.compareTo(p2));
		patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertEquals(p1, patterns.get(0));

		// Based purely on catchAll
		p1 = parse("{*foobar}");
		p2 = parse("{*goo}");
		assertTrue(p1.compareTo(p2) != 0);

		p1 = parse("/{*foobar}");
		p2 = parse("/abc/{*ww}");
		assertEquals(+1, p1.compareTo(p2));
		assertEquals(-1, p2.compareTo(p1));

		p3 = parse("/this/that/theother");
		assertTrue(p1.isCatchAll());
		assertTrue(p2.isCatchAll());
		assertFalse(p3.isCatchAll());
		patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertEquals(p3, patterns.get(0));
		assertEquals(p2, patterns.get(1));
	}

	private PathPattern parse(String pattern) {
		PathPatternParser patternParser = new PathPatternParser();
		return patternParser.parse(pattern);
	}

	/**
	 * Verify the pattern string computed for a parsed pattern matches the original pattern text
	 */
	private PathPattern checkStructure(String pattern) {
		PathPattern pp = parse(pattern);
		assertEquals(pattern, pp.computePatternString());
		return pp;
	}

	private void checkError(String pattern, int expectedPos, PatternMessage expectedMessage,
			String... expectedInserts) {

		try {
			pathPattern = parse(pattern);
			fail("Expected to fail");
		}
		catch (PatternParseException ppe) {
			assertEquals(ppe.toDetailedString(), expectedPos, ppe.getPosition());
			assertEquals(ppe.toDetailedString(), expectedMessage, ppe.getMessageType());
			if (expectedInserts.length != 0) {
				assertEquals(ppe.getInserts().length, expectedInserts.length);
				for (int i = 0; i < expectedInserts.length; i++) {
					assertEquals("Insert at position " + i + " is wrong", expectedInserts[i], ppe.getInserts()[i]);
				}
			}
		}
	}

	@SafeVarargs
	private final void assertPathElements(PathPattern p, Class<? extends PathElement>... sectionClasses) {
		PathElement head = p.getHeadSection();
		for (Class<? extends PathElement> sectionClass : sectionClasses) {
			if (head == null) {
				fail("Ran out of data in parsed pattern. Pattern is: " + p.toChainString());
			}
			assertEquals("Not expected section type. Pattern is: " + p.toChainString(),
					sectionClass.getSimpleName(), head.getClass().getSimpleName());
			head = head.next;
		}
	}

	// Mirrors the score computation logic in PathPattern
	private int computeScore(int capturedVariableCount, int wildcardCount) {
		return capturedVariableCount + wildcardCount * 100;
	}

	private void assertMatches(PathPattern pp, String path) {
		assertTrue(pp.matches(PathPatternTests.toPathContainer(path)));
	}

	private void assertNoMatch(PathPattern pp, String path) {
		assertFalse(pp.matches(PathPatternTests.toPathContainer(path)));
	}
	
	private PathPattern.PathMatchInfo matchAndExtract(PathPattern pp, String path) {
		 return pp.matchAndExtract(PathPatternTests.toPathContainer(path));
	}

	private PathContainer toPSC(String path) {
		return PathPatternTests.toPathContainer(path);
	}

}
