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

package org.springframework.web.util.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PatternParseException.PatternMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Exercise the {@link PathPatternParser}.
 *
 * @author Andy Clement
 * @author Sam Brannen
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
		assertThat(checkStructure("/{*foobar}").toChainString()).isEqualTo("CaptureTheRest(/{*foobar})");
		assertThat(checkStructure("{foobar}").toChainString()).isEqualTo("CaptureVariable({foobar})");
		assertThat(checkStructure("abc").toChainString()).isEqualTo("Literal(abc)");
		assertThat(checkStructure("{a}_*_{b}").toChainString()).isEqualTo("Regex({a}_*_{b})");
		assertThat(checkStructure("/").toChainString()).isEqualTo("Separator(/)");
		assertThat(checkStructure("?a?b?c").toChainString()).isEqualTo("SingleCharWildcarded(?a?b?c)");
		assertThat(checkStructure("*").toChainString()).isEqualTo("Wildcard(*)");
		assertThat(checkStructure("/**").toChainString()).isEqualTo("WildcardTheRest(/**)");
	}

	@Test
	public void captureTheRestPatterns() {
		pathPattern = parse("{*foobar}");
		assertThat(pathPattern.computePatternString()).isEqualTo("/{*foobar}");
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
		assertThat(pp2).isEqualTo(pp1);
		assertThat(pp2.hashCode()).isEqualTo(pp1.hashCode());
		assertThat(pp3).isNotEqualTo(pp1);
		assertThat(pp1.equals("abc")).isFalse();

		pp1 = caseInsensitiveParser.parse("/abc");
		pp2 = caseSensitiveParser.parse("/abc");
		assertThat(pp1.equals(pp2)).isFalse();
		assertThat(pp2.hashCode()).isNotEqualTo((long) pp1.hashCode());
	}

	@Test
	public void regexPathElementPatterns() {
		checkError("/{var:[^/]*}", 8, PatternMessage.MISSING_CLOSE_CAPTURE);
		checkError("/{var:abc", 8, PatternMessage.MISSING_CLOSE_CAPTURE);

		// Do not check the expected position due a change in RegEx parsing in JDK 13.
		// See https://github.com/spring-projects/spring-framework/issues/23669
		checkError("/{var:a{{1,2}}}", PatternMessage.REGEX_PATTERN_SYNTAX_EXCEPTION);

		pathPattern = checkStructure("/{var:\\\\}");
		PathElement next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());
		assertMatches(pathPattern,"/\\");

		pathPattern = checkStructure("/{var:\\/}");
		next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());
		assertNoMatch(pathPattern,"/aaa");

		pathPattern = checkStructure("/{var:a{1,2}}");
		next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());

		pathPattern = checkStructure("/{var:[^\\/]*}");
		next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());
		PathPattern.PathMatchInfo result = matchAndExtract(pathPattern,"/foo");
		assertThat(result.getUriVariables().get("var")).isEqualTo("foo");

		pathPattern = checkStructure("/{var:\\[*}");
		next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());
		result = matchAndExtract(pathPattern,"/[[[");
		assertThat(result.getUriVariables().get("var")).isEqualTo("[[[");

		pathPattern = checkStructure("/{var:[\\{]*}");
		next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());
		result = matchAndExtract(pathPattern,"/{{{");
		assertThat(result.getUriVariables().get("var")).isEqualTo("{{{");

		pathPattern = checkStructure("/{var:[\\}]*}");
		next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());
		result = matchAndExtract(pathPattern,"/}}}");
		assertThat(result.getUriVariables().get("var")).isEqualTo("}}}");

		pathPattern = checkStructure("*");
		assertThat(pathPattern.getHeadSection().getClass().getName()).isEqualTo(WildcardPathElement.class.getName());
		checkStructure("/*");
		checkStructure("/*/");
		checkStructure("*/");
		checkStructure("/*/");
		pathPattern = checkStructure("/*a*/");
		next = pathPattern.getHeadSection().next;
		assertThat(next.getClass().getName()).isEqualTo(RegexPathElement.class.getName());
		pathPattern = checkStructure("*/");
		assertThat(pathPattern.getHeadSection().getClass().getName()).isEqualTo(WildcardPathElement.class.getName());
		checkError("{foo}_{foo}", 0, PatternMessage.ILLEGAL_DOUBLE_CAPTURE, "foo");
		checkError("/{bar}/{bar}", 7, PatternMessage.ILLEGAL_DOUBLE_CAPTURE, "bar");
		checkError("/{bar}/{bar}_{foo}", 7, PatternMessage.ILLEGAL_DOUBLE_CAPTURE, "bar");

		pathPattern = checkStructure("{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar");
		assertThat(pathPattern.getHeadSection().getClass().getName()).isEqualTo(RegexPathElement.class.getName());
	}

	@Test
	public void completeCapturingPatterns() {
		pathPattern = checkStructure("{foo}");
		assertThat(pathPattern.getHeadSection().getClass().getName()).isEqualTo(CaptureVariablePathElement.class.getName());
		checkStructure("/{foo}");
		checkStructure("/{f}/");
		checkStructure("/{foo}/{bar}/{wibble}");
		checkStructure("/{mobile-number}"); // gh-23101
	}

	@Test
	public void noEncoding() {
		// Check no encoding of expressions or constraints
		PathPattern pp = parse("/{var:f o}");
		assertThat(pp.toChainString()).isEqualTo("Separator(/) CaptureVariable({var:f o})");

		pp = parse("/{var:f o}_");
		assertThat(pp.toChainString()).isEqualTo("Separator(/) Regex({var:f o}_)");

		pp = parse("{foo:f o}_ _{bar:b\\|o}");
		assertThat(pp.toChainString()).isEqualTo("Regex({foo:f o}_ _{bar:b\\|o})");
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
		assertThat(pathPattern.getHeadSection().getClass().getName()).isEqualTo(RegexPathElement.class.getName());
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
		assertThatIllegalArgumentException().isThrownBy(() ->
				pp.matchAndExtract(toPSC("/foo")))
			.withMessage("No capture groups allowed in the constraint regex: foo(bar)");
		assertThatIllegalArgumentException().isThrownBy(() ->
				pp.matchAndExtract(toPSC("/foobar")))
			.withMessage("No capture groups allowed in the constraint regex: foo(bar)");
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
		assertThat(parse("{foo}").getCapturedVariableCount()).isEqualTo(1);
		assertThat(parse("foo").getCapturedVariableCount()).isEqualTo(0);
		assertThat(parse("{*foobar}").getCapturedVariableCount()).isEqualTo(1);
		assertThat(parse("/{*foobar}").getCapturedVariableCount()).isEqualTo(1);
		assertThat(parse("/**").getCapturedVariableCount()).isEqualTo(0);
		assertThat(parse("{abc}asdf").getCapturedVariableCount()).isEqualTo(1);
		assertThat(parse("{abc}_*").getCapturedVariableCount()).isEqualTo(1);
		assertThat(parse("{abc}_{def}").getCapturedVariableCount()).isEqualTo(2);
		assertThat(parse("/").getCapturedVariableCount()).isEqualTo(0);
		assertThat(parse("a?b").getCapturedVariableCount()).isEqualTo(0);
		assertThat(parse("*").getCapturedVariableCount()).isEqualTo(0);

		// Test on full templates
		assertThat(parse("/foo/bar").getCapturedVariableCount()).isEqualTo(0);
		assertThat(parse("/{foo}").getCapturedVariableCount()).isEqualTo(1);
		assertThat(parse("/{foo}/{bar}").getCapturedVariableCount()).isEqualTo(2);
		assertThat(parse("/{foo}/{bar}_{goo}_{wibble}/abc/bar").getCapturedVariableCount()).isEqualTo(4);
	}

	@Test
	public void patternPropertyGetWildcardCountTests() {
		// Test all basic section types
		assertThat(parse("{foo}").getScore()).isEqualTo(computeScore(1, 0));
		assertThat(parse("foo").getScore()).isEqualTo(computeScore(0, 0));
		assertThat(parse("{*foobar}").getScore()).isEqualTo(computeScore(0, 0));
		//		assertEquals(1,parse("/**").getScore());
		assertThat(parse("{abc}asdf").getScore()).isEqualTo(computeScore(1, 0));
		assertThat(parse("{abc}_*").getScore()).isEqualTo(computeScore(1, 1));
		assertThat(parse("{abc}_{def}").getScore()).isEqualTo(computeScore(2, 0));
		assertThat(parse("/").getScore()).isEqualTo(computeScore(0, 0));
		// currently deliberate
		assertThat(parse("a?b").getScore()).isEqualTo(computeScore(0, 0));
		assertThat(parse("*").getScore()).isEqualTo(computeScore(0, 1));

		// Test on full templates
		assertThat(parse("/foo/bar").getScore()).isEqualTo(computeScore(0, 0));
		assertThat(parse("/{foo}").getScore()).isEqualTo(computeScore(1, 0));
		assertThat(parse("/{foo}/{bar}").getScore()).isEqualTo(computeScore(2, 0));
		assertThat(parse("/{foo}/{bar}_{goo}_{wibble}/abc/bar").getScore()).isEqualTo(computeScore(4, 0));
		assertThat(parse("/{foo}/*/*_*/{bar}_{goo}_{wibble}/abc/bar").getScore()).isEqualTo(computeScore(4, 3));
	}

	@Test
	public void multipleSeparatorPatterns() {
		pathPattern = checkStructure("///aaa");
		assertThat(pathPattern.getNormalizedLength()).isEqualTo(6);
		assertPathElements(pathPattern, SeparatorPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, LiteralPathElement.class);
		pathPattern = checkStructure("///aaa////aaa/b");
		assertThat(pathPattern.getNormalizedLength()).isEqualTo(15);
		assertPathElements(pathPattern, SeparatorPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, LiteralPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, SeparatorPathElement.class, SeparatorPathElement.class,
				LiteralPathElement.class, SeparatorPathElement.class, LiteralPathElement.class);
		pathPattern = checkStructure("/////**");
		assertThat(pathPattern.getNormalizedLength()).isEqualTo(5);
		assertPathElements(pathPattern, SeparatorPathElement.class, SeparatorPathElement.class,
				SeparatorPathElement.class, SeparatorPathElement.class, WildcardTheRestPathElement.class);
	}

	@Test
	public void patternPropertyGetLengthTests() {
		// Test all basic section types
		assertThat(parse("{foo}").getNormalizedLength()).isEqualTo(1);
		assertThat(parse("foo").getNormalizedLength()).isEqualTo(3);
		assertThat(parse("{*foobar}").getNormalizedLength()).isEqualTo(1);
		assertThat(parse("/{*foobar}").getNormalizedLength()).isEqualTo(1);
		assertThat(parse("/**").getNormalizedLength()).isEqualTo(1);
		assertThat(parse("{abc}asdf").getNormalizedLength()).isEqualTo(5);
		assertThat(parse("{abc}_*").getNormalizedLength()).isEqualTo(3);
		assertThat(parse("{abc}_{def}").getNormalizedLength()).isEqualTo(3);
		assertThat(parse("/").getNormalizedLength()).isEqualTo(1);
		assertThat(parse("a?b").getNormalizedLength()).isEqualTo(3);
		assertThat(parse("*").getNormalizedLength()).isEqualTo(1);

		// Test on full templates
		assertThat(parse("/foo/bar").getNormalizedLength()).isEqualTo(8);
		assertThat(parse("/{foo}").getNormalizedLength()).isEqualTo(2);
		assertThat(parse("/{foo}/{bar}").getNormalizedLength()).isEqualTo(4);
		assertThat(parse("/{foo}/{bar}_{goo}_{wibble}/abc/bar").getNormalizedLength()).isEqualTo(16);
	}

	@Test
	public void compareTests() {
		PathPattern p1, p2, p3;

		// Based purely on number of captures
		p1 = parse("{a}");
		p2 = parse("{a}/{b}");
		p3 = parse("{a}/{b}/{c}");
		// Based on number of captures
		assertThat(p1.compareTo(p2)).isEqualTo(-1);
		List<PathPattern> patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertThat(patterns.get(0)).isEqualTo(p1);

		// Based purely on length
		p1 = parse("/a/b/c");
		p2 = parse("/a/boo/c/doo");
		p3 = parse("/asdjflaksjdfjasdf");
		assertThat(p1.compareTo(p2)).isEqualTo(1);
		patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertThat(patterns.get(0)).isEqualTo(p3);

		// Based purely on 'wildness'
		p1 = parse("/*");
		p2 = parse("/*/*");
		p3 = parse("/*/*/*_*");
		assertThat(p1.compareTo(p2)).isEqualTo(-1);
		patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertThat(patterns.get(0)).isEqualTo(p1);

		// Based purely on catchAll
		p1 = parse("{*foobar}");
		p2 = parse("{*goo}");
		assertThat(p1.compareTo(p2) != 0).isTrue();

		p1 = parse("/{*foobar}");
		p2 = parse("/abc/{*ww}");
		assertThat(p1.compareTo(p2)).isEqualTo(+1);
		assertThat(p2.compareTo(p1)).isEqualTo(-1);

		p3 = parse("/this/that/theother");
		assertThat(p1.isCatchAll()).isTrue();
		assertThat(p2.isCatchAll()).isTrue();
		assertThat(p3.isCatchAll()).isFalse();
		patterns = new ArrayList<>();
		patterns.add(p2);
		patterns.add(p3);
		patterns.add(p1);
		Collections.sort(patterns);
		assertThat(patterns.get(0)).isEqualTo(p3);
		assertThat(patterns.get(1)).isEqualTo(p2);
	}

	@Test // Should be updated with gh-24952
	public void doubleWildcardWithinPatternNotSupported() {
		PathPatternParser parser = new PathPatternParser();
		PathPattern pattern = parser.parse("/resources/**/details");
		assertThat(pattern.matches(PathContainer.parsePath("/resources/test/details"))).isTrue();
		assertThat(pattern.matches(PathContainer.parsePath("/resources/projects/spring/details"))).isFalse();
	}

	@Test
	public void separatorTests() {
		PathPatternParser parser = new PathPatternParser();
		parser.setPathOptions(PathContainer.Options.create('.', false));
		String rawPattern = "first.second.{last}";
		PathPattern pattern = parser.parse(rawPattern);
		assertThat(pattern.computePatternString()).isEqualTo(rawPattern);
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
		assertThat(pp.computePatternString()).isEqualTo(pattern);
		return pp;
	}

	/**
	 * Delegates to {@link #checkError(String, int, PatternMessage, String...)},
	 * passing {@code -1} as the {@code expectedPos}.
	 * @since 5.2
	 */
	private void checkError(String pattern, PatternMessage expectedMessage, String... expectedInserts) {
		checkError(pattern, -1, expectedMessage, expectedInserts);
	}

	/**
	 * @param expectedPos the expected position, or {@code -1} if the position should not be checked
	 */
	private void checkError(String pattern, int expectedPos, PatternMessage expectedMessage,
			String... expectedInserts) {

		assertThatExceptionOfType(PatternParseException.class)
			.isThrownBy(() -> pathPattern = parse(pattern))
			.satisfies(ex -> {
				if (expectedPos >= 0) {
					assertThat(ex.getPosition()).as(ex.toDetailedString()).isEqualTo(expectedPos);
				}
				assertThat(ex.getMessageType()).as(ex.toDetailedString()).isEqualTo(expectedMessage);
				if (expectedInserts.length != 0) {
					assertThat(ex.getInserts()).isEqualTo(expectedInserts);
				}
			});
	}

	@SafeVarargs
	private final void assertPathElements(PathPattern p, Class<? extends PathElement>... sectionClasses) {
		PathElement head = p.getHeadSection();
		for (Class<? extends PathElement> sectionClass : sectionClasses) {
			if (head == null) {
				fail("Ran out of data in parsed pattern. Pattern is: " + p.toChainString());
			}
			assertThat(head.getClass().getSimpleName()).as("Not expected section type. Pattern is: " + p.toChainString()).isEqualTo(sectionClass.getSimpleName());
			head = head.next;
		}
	}

	// Mirrors the score computation logic in PathPattern
	private int computeScore(int capturedVariableCount, int wildcardCount) {
		return capturedVariableCount + wildcardCount * 100;
	}

	private void assertMatches(PathPattern pp, String path) {
		assertThat(pp.matches(PathPatternTests.toPathContainer(path))).isTrue();
	}

	private void assertNoMatch(PathPattern pp, String path) {
		assertThat(pp.matches(PathPatternTests.toPathContainer(path))).isFalse();
	}

	private PathPattern.PathMatchInfo matchAndExtract(PathPattern pp, String path) {
		return pp.matchAndExtract(PathPatternTests.toPathContainer(path));
	}

	private PathContainer toPSC(String path) {
		return PathPatternTests.toPathContainer(path);
	}

}
