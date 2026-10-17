/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CssLinkParser}.
 *
 * <p>Every case is run at chunk sizes 1, 2, 3, 7, 13 and 4096 bytes, asserting
 * identical output at every chunk size: that is what proves the parser is safe
 * across arbitrary buffer boundaries (relevant to the webflux {@code DataBuffer}
 * based caller in particular).
 *
 * @author Brian Clozel
 */
class CssLinkParserTests {

	private static final int[] CHUNK_SIZES = {1, 2, 3, 7, 13, 4096};


	@Test
	void quotedUrlFunctionWithDoubleQuotes() {
		assertLinks("body { background: url(\"foo.png\") }", "foo.png");
	}

	@Test
	void quotedUrlFunctionWithSingleQuotes() {
		assertLinks("body { background: url('foo.png') }", "foo.png");
	}

	@Test
	void unquotedUrlFunction() {
		assertLinks("body { background: url(foo.png) }", "foo.png");
	}

	@Test
	void importWithDoubleQuotedString() {
		assertLinks("@import \"foo.css\";", "foo.css");
	}

	@Test
	void importWithSingleQuotedString() {
		assertLinks("@import 'foo.css';", "foo.css");
	}

	@Test
	void importWithQuotedUrlFunction() {
		assertLinks("@import url(\"foo.css\");", "foo.css");
	}

	@Test
	void importWithUnquotedUrlFunction() {
		assertLinks("@import url(foo.css);", "foo.css");
	}

	@Test
	void whitespaceInsideParenthesesAroundQuotedLink() {
		assertLinks("body { background: url(  \"foo.png\"  ) }", "foo.png");
	}

	@Test
	void whitespaceIncludingNewlinesAndTabsBeforeQuotedLink() {
		assertLinks("body { background: url(\n\t'foo.png'\n) }", "foo.png");
	}

	@Test
	void tabAfterImportKeyword() {
		assertLinks("@import\t\"foo.css\";", "foo.css");
	}

	@Test
	void invalidKeywordIsSkipped() {
		assertLinks("body { background: uurl(foo.png) }");
	}

	@Test
	void urlFunctionAfterNonIdentifierCharacterIsStillAKeyword() {
		assertLinks("body { background: (url(foo.png)) }", "foo.png");
	}

	@Test
	void importWithoutQuoteOrUrlFunctionProducesNoLink() {
		assertLinks("@import foo.css;");
	}

	@Test // https://github.com/spring-projects/spring-framework/issues/22602
	void emptyUrlFunctionProducesNoLink() {
		assertLinks(".fooStyle { background: transparent url() no-repeat left top; }");
	}

	@Test
	void emptyUrlFunctionWithWhitespaceProducesNoLink() {
		assertLinks("body { background: url(   ) }");
	}

	@Test
	void nonAsciiContentAroundLinkPassesThroughUntouched() {
		assertLinks("café { background: url(\"héllo.png\") } 世界", "héllo.png");
	}

	@Test
	void nonAsciiContentWithoutAnyLink() {
		assertLinks("café { color: red; } 世界");
	}

	@Test
	void unterminatedUnquotedLinkIsSkipped() {
		assertLinks("body { background: url(images/missing.png");
	}

	@Test
	void unterminatedDoubleQuotedLinkIsSkipped() {
		assertLinks("body { background: url(\"images/missing.png");
	}

	@Test
	void unterminatedSingleQuotedLinkSpanningIntoNextIsSkipped() {
		assertLinks("a{background:url('x.png}\nb{color:red}");
	}

	@Test
	void endOfFileImmediatelyAfterUrlFunctionKeywordDoesNotThrow() {
		assertLinks("body { background: url(");
	}

	@Test
	void trailingWhitespaceAfterImportAtEndOfFileDoesNotThrow() {
		assertLinks("@import   ");
	}

	@Test
	void runawayUnterminatedLinkIsBoundedAndParsingRecovers() {
		String longUnterminated = "a".repeat(4096);
		assertLinks("body { background: url('" + longUnterminated + " div { background: url('second.png') }",
				"second.png");
	}

	@Test
	void unterminatedQuotedLinkStopsAtNewlineAndNextLinkIsFound() {
		assertLinks("a{background:url('x.png}\nb{background:url('b.png')}", "b.png");
	}

	@Test
	void unterminatedUnquotedLinkStopsAtNewlineAndNextLinkIsFound() {
		assertLinks("a{background:url(x.png\nb{background:url(b.png)}", "b.png");
	}

	@Test
	void unquotedLinkWithTrailingWhitespaceExcludesWhitespace() {
		assertLinks("body { background: url( foo.png \t) }", "foo.png");
	}

	@Test
	void unquotedLinkWithInnerWhitespaceIsInvalid() {
		assertLinks("a{background:url(foo bar.png)} b{background:url(b.png)}", "b.png");
	}

	@Test
	void unquotedLinkWithQuoteIsInvalid() {
		assertLinks("a{background:url(foo'bar.png)} b{background:url(b.png)}", "b.png");
	}

	@Test
	void unquotedLinkWithParenthesisIsInvalid() {
		assertLinks("a{background:url(foo(bar.png)} b{background:url(b.png)}", "b.png");
	}

	@Test
	void unterminatedUnquotedLinkWithTrailingWhitespaceAtEndOfFile() {
		assertLinks("body { background: url(foo.png  ");
	}

	@Test
	void unterminatedLinksIsSkipped() {
		String pathological = "url(x".repeat(200_000);
		assertLinks(pathological);
	}


	private static void assertLinks(String cssContent, String... expectedLinks) {
		byte[] input = cssContent.getBytes(UTF_8);
		for (int chunkSize : CHUNK_SIZES) {
			List<String> links = new ArrayList<>();
			byte[] output = parse(input, chunkSize, links);
			assertThat(output)
					.describedAs("reconstructed output at chunk size " + chunkSize)
					.isEqualTo(input);
			assertThat(links)
					.describedAs("extracted links at chunk size " + chunkSize)
					.containsExactly(expectedLinks);
		}
	}

	private static byte[] parse(byte[] input, int chunkSize, List<String> links) {
		CssLinkParser parser = new CssLinkParser();
		List<CssLinkParser.Token> tokens = new ArrayList<>();
		for (int i = 0; i < input.length; i += chunkSize) {
			int len = Math.min(chunkSize, input.length - i);
			tokens.addAll(parser.feed(input, i, len));
		}
		tokens.addAll(parser.end());
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		for (CssLinkParser.Token token : tokens) {
			output.write(token.bytes(), 0, token.bytes().length);
			if (token.link()) {
				links.add(new String(token.bytes(), UTF_8));
			}
		}
		return output.toByteArray();
	}

}
