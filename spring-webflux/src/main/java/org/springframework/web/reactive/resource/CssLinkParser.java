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

package org.springframework.web.reactive.resource;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.buffer.DataBuffer;

/**
 * A lightweight, forward-only parser that recognizes CSS {@code url()}
 * and {@code @import} link tokens.
 *
 * <p>This parser is used by {@link CssLinkResourceTransformer} to rewrite
 * CSS links on-the-fly, if needed. Other tokens are handed back as is
 * to be written to the output stream. Invalid links (unterminated, containing
 * characters not allowed by the CSS syntax, or exceeding {@link #MAX_LINK_LENGTH})
 * are skipped and written as-is, and parsing resumes right after them.
 *
 * @author Brian Clozel
 * @since 7.1
 */
final class CssLinkParser {

	private static final byte[] URL_KEYWORD = {'u', 'r', 'l', '('};

	private static final byte[] IMPORT_KEYWORD = {'@', 'i', 'm', 'p', 'o', 'r', 't'};

	private static final int MAX_LINK_LENGTH = 2048;


	private State state = State.CONTENT;

	private byte[] keyword = URL_KEYWORD;

	private int matched;

	private byte terminator;

	/** Length of an unquoted link, excluding trailing whitespace, or -1 if none seen yet. */
	private int linkEnd = -1;

	/** Whether the previous byte was an identifier character. */
	private boolean previousWasIdentifierChar;

	private boolean hasLinks;

	private final ByteArrayOutputStream literal = new ByteArrayOutputStream();

	private final ByteArrayOutputStream link = new ByteArrayOutputStream();

	private List<Token> tokens = new ArrayList<>();


	/**
	 * Feed the next buffer of input to the parser, reading directly from it
	 * rather than copying it into a {@code byte[]} first. Does not affect the
	 * buffer's read position, and does not release it; that remains the
	 * caller's responsibility.
	 * @return the tokens produced by this buffer
	 */
	List<Token> feed(DataBuffer buffer) {
		buffer.forEachByte(buffer.readPosition(), buffer.readableByteCount(), b -> {
			processByte(b);
			return true;
		});
		return flushTokens();
	}

	/**
	 * Signal the end of the input.
	 * @return the remaining tokens
	 */
	List<Token> end() {
		abortLink();
		return flushTokens();
	}

	/**
	 * Whether at least one link token was produced so far.
	 */
	boolean hasLinks() {
		return this.hasLinks;
	}

	private void processByte(byte b) {
		boolean reexamine;
		do {
			reexamine = this.state.process(b, this);
		}
		while (reexamine);
		this.previousWasIdentifierChar = State.isIdentifierChar(b);
	}

	private void appendToLink(byte b) {
		this.link.write(b);
		// ignore links larger than the maximum length
		if (this.link.size() > MAX_LINK_LENGTH) {
			abortLink();
		}
	}

	/**
	 * Emit the first {@code length} bytes of the current link as a link token,
	 * followed by any remaining bytes and the terminator as literal content.
	 */
	private void completeLink(int length, byte terminator) {
		flushLiteral();
		byte[] bytes = this.link.toByteArray();
		if (length > 0) {
			this.tokens.add(new Token((length < bytes.length ? Arrays.copyOf(bytes, length) : bytes), true));
			this.hasLinks = true;
		}
		this.literal.write(bytes, length, bytes.length - length);
		this.literal.write(terminator);
		this.link.reset();
		this.state = State.CONTENT;
	}

	/**
	 * Write the current link back as literal content and resume parsing.
	 */
	private void abortLink() {
		this.literal.write(this.link.toByteArray(), 0, this.link.size());
		this.link.reset();
		this.state = State.CONTENT;
	}

	private void flushLiteral() {
		if (this.literal.size() > 0) {
			this.tokens.add(new Token(this.literal.toByteArray(), false));
			this.literal.reset();
		}
	}

	private List<Token> flushTokens() {
		flushLiteral();
		List<Token> result = this.tokens;
		this.tokens = new ArrayList<>();
		return result;
	}


	/**
	 * Represents the internal state of the {@link CssLinkParser}, which processes
	 * the input one byte at a time. The flow is shown below:
	 * <p><pre>
	 *                       no match
	 *               +-----------------------+
	 *               v                       |
	 *   +------> CONTENT --"u" or "@"--> KEYWORD
	 *   ^           ^                       |
	 *   |           |                       | "url(" or "@import"
	 *   |           |  other byte after     v
	 *   |           +----"@import"---- BEFORE_LINK <--+
	 *   |                               |   |   |     | whitespace
	 *   |                               |   |   +-----+
	 *   |              quote            |   |
	 *   |      +------------------------+   +-----------------+
	 *   |      |                              other byte      |
	 *   |      v                              after "url("    v
	 *   +<-QUOTED_LINK                                  UNQUOTED_LINK
	 *   ^  closing quote                                      |
	 *   |  or invalid link                ")" or invalid link |
	 *   +<----------------------------------------------------+
	 * </pre>
	 * Links are invalid if they contain an unescaped newline (quoted links),
	 * a quote, a parenthesis or inner whitespace (unquoted links), or if they
	 * exceed {@link CssLinkParser#MAX_LINK_LENGTH}. Invalid links are written back
	 * as literal content, and the byte that invalidated them is re-examined as
	 * {@link #CONTENT}.
	 */
	private enum State {

		CONTENT {
			@Override
			boolean process(byte b, CssLinkParser parser) {
				parser.literal.write(b);
				// "url(" keyword, unless part of a longer identifier
				if (b == URL_KEYWORD[0] && !parser.previousWasIdentifierChar) {
					beginKeyword(parser, URL_KEYWORD);
				}
				// "@import" keyword
				else if (b == IMPORT_KEYWORD[0]) {
					beginKeyword(parser, IMPORT_KEYWORD);
				}
				return false;
			}

			private void beginKeyword(CssLinkParser parser, byte[] keyword) {
				parser.keyword = keyword;
				parser.matched = 1;
				parser.state = State.KEYWORD;
			}
		},

		KEYWORD {
			@Override
			boolean process(byte b, CssLinkParser parser) {
				// current byte matching the keyword
				if (parser.matched < parser.keyword.length && b == parser.keyword[parser.matched]) {
					parser.literal.write(b);
					parser.matched++;
					if (parser.keyword == URL_KEYWORD && parser.matched == URL_KEYWORD.length) {
						parser.state = State.BEFORE_LINK;
					}
					return false;
				}
				// "@import" must not be part of a longer identifier;
				// other bytes are re-examined as content.
				boolean complete = (parser.matched == parser.keyword.length && !isIdentifierChar(b));
				parser.state = (complete ? State.BEFORE_LINK : State.CONTENT);
				return true;
			}
		},

		BEFORE_LINK {
			@Override
			boolean process(byte b, CssLinkParser parser) {
				if (isCssWhitespace(b)) {
					parser.literal.write(b);
					return false;
				}
				if (b == '\'' || b == '"') {
					parser.literal.write(b);
					parser.terminator = b;
					parser.state = State.QUOTED_LINK;
					return false;
				}
				if (parser.keyword == URL_KEYWORD) {
					parser.linkEnd = -1;
					parser.state = State.UNQUOTED_LINK;
					return true;
				}
				// @import with url(...) following.
				parser.state = State.CONTENT;
				return true;
			}
		},

		QUOTED_LINK {
			@Override
			boolean process(byte b, CssLinkParser parser) {
				if (b == parser.terminator) {
					parser.completeLink(parser.link.size(), b);
					return false;
				}
				// unescaped newlines are not allowed in CSS strings
				if (b == '\n' || b == '\r' || b == '\f') {
					parser.abortLink();
					return true;
				}
				parser.appendToLink(b);
				return false;
			}
		},

		UNQUOTED_LINK {
			@Override
			boolean process(byte b, CssLinkParser parser) {
				if (b == ')') {
					parser.completeLink((parser.linkEnd != -1 ? parser.linkEnd : parser.link.size()), b);
					return false;
				}
				if (isCssWhitespace(b)) {
					if (parser.linkEnd == -1) {
						parser.linkEnd = parser.link.size();
					}
					parser.appendToLink(b);
					return false;
				}
				// only whitespace is allowed before the closing parenthesis,
				// and quotes and parentheses are not allowed in unquoted urls
				if (parser.linkEnd != -1 || b == '\'' || b == '"' || b == '(') {
					parser.abortLink();
					return true;
				}
				parser.appendToLink(b);
				return false;
			}
		};

		abstract boolean process(byte b, CssLinkParser parser);

		private static boolean isIdentifierChar(byte b) {
			return ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') ||
					b == '-' || b == '_' || b < 0);
		}

		private static boolean isCssWhitespace(byte b) {
			return (b == ' ' || b == '\t' || b == '\n' || b == '\r' || b == '\f');
		}
	}


	/**
	 * A span of bytes emitted by the parser: either literal content to write
	 * through unchanged, or the contents of a link (without its surrounding
	 * keyword, whitespace, quotes or parenthesis).
	 * @param bytes the span of bytes
	 * @param link whether {@code bytes} is a link, as opposed to literal content
	 */
	record Token(byte[] bytes, boolean link) {
	}

}
