/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.util;

import java.net.IDN;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of the URL parser from the Living URL standard.
 *
 * <p>All comments in this class refer to parts of the
 * <a href="https://url.spec.whatwg.org/#url-parsing">parsing algorithm</a>.
 * This implementation differs from the one defined in the specification in
 * these areas:
 * <ul>
 * <li>Support for URI templates has been added, through the
 * {@link State#URL_TEMPLATE} state</li>
 * <li>Consequentially, the {@linkplain UrlRecord#port() URL port} has been
 * changed from an integer to a string,</li>
 * <li>To ensure that trailing slashes are significant, this implementation
 * prepends a '/' to each segment.</li>
 * </ul>
 * All of these modifications have been indicated through comments that start
 * with {@code EXTRA}.
 *
 * @author Arjen Poutsma
 * @since 6.2
 * @see <a href="https://url.spec.whatwg.org/#url-parsing">URL parsing</a>
 */
final class UrlParser {

	private static final int EOF = -1;

	private static final int MAX_PORT = 65535;

	private static final Log logger = LogFactory.getLog(UrlParser.class);


	private final StringBuilder input;

	@Nullable
	private final UrlRecord base;

	@Nullable
	private Charset encoding;

	@Nullable
	private final Consumer<String> validationErrorHandler;

	private int pointer;

	private final StringBuilder buffer;

	@Nullable
	private State state;

	@Nullable
	private State previousState;

	@Nullable
	private State stateOverride;

	private boolean atSignSeen;

	private boolean passwordTokenSeen;

	private boolean insideBrackets;

	private boolean stopMainLoop = false;


	private UrlParser(String input, @Nullable UrlRecord base, @Nullable Charset encoding, @Nullable Consumer<String> validationErrorHandler) {
		this.input = new StringBuilder(input);
		this.base = base;
		this.encoding = encoding;
		this.validationErrorHandler = validationErrorHandler;
		this.buffer = new StringBuilder(this.input.length() / 2);
	}


	/**
	 * Parse the given input into a URL record.
	 * @param input the scalar value string
	 * @param base the optional base URL to resolve relative URLs against. If
	 * {@code null}, relative URLs cannot be parsed.
	 * @param encoding the optional encoding to use. If {@code null}, no
	 * encoding is performed.
	 * @param validationErrorHandler optional consumer for non-fatal URL
	 * validation messages
	 * @return a URL record, as defined in the
	 * <a href="https://url.spec.whatwg.org/#concept-url">living URL
	 * specification</a>
	 * @throws InvalidUrlException if the {@code input} does not contain a
	 * parsable URL
	 */
	public static UrlRecord parse(String input, @Nullable UrlRecord base,
			@Nullable Charset encoding, @Nullable Consumer<String> validationErrorHandler)
			throws InvalidUrlException {

		Assert.notNull(input, "Input must not be null");

		UrlParser parser = new UrlParser(input, base, encoding, validationErrorHandler);
		return parser.basicUrlParser(null, null);
	}

	/**
	 * The basic URL parser takes a scalar value string input, with an optional
	 * null or base URL base (default null), an optional encoding
	 * {@code encoding}
	 * (default UTF-8), an optional URL {@code url}, and an optional state
	 * override {@code state override}.
	 */
	private UrlRecord basicUrlParser(@Nullable UrlRecord url, @Nullable State stateOverride) {
		// If url is not given:
		if (url == null) {
			// Set url to a new URL.
			url = new UrlRecord();
			sanitizeInput(true);
		}
		else {
			sanitizeInput(false);
		}

		// Let state be state override if given, or scheme start state otherwise.
		this.state = stateOverride != null ? stateOverride : State.SCHEME_START;
		this.stateOverride = stateOverride;

		// Keep running the following state machine by switching on state.
		// If after a run pointer points to the EOF code point, go to the next step.
		// Otherwise, increase pointer by 1 and continue with the state machine.
		while (!this.stopMainLoop && this.pointer <= this.input.length()) {
			int c;
			if (this.pointer < this.input.length()) {
				c = this.input.codePointAt(this.pointer);
			}
			else {
				c = EOF;
			}
			if (logger.isTraceEnabled()) {
				String cStr = c != EOF ? Character.toString(c) : "EOF";
				logger.trace("current: " + cStr + " ptr: " + this.pointer + " Buffer: " + this.buffer + " State: " + this.state);
			}
			this.state.handle(c, url, this);
			this.pointer++;
		}
		return url;
	}

	void sanitizeInput(boolean removeC0ControlOrSpace) {
		boolean strip = true;
		for (int i = 0; i < this.input.length(); i++) {
			int c = this.input.codePointAt(i);
			boolean isSpaceOrC0 = c == ' ' || isC0Control(c);
			boolean isTabOrNL = c == '\t' || isNewline(c);
			if ((strip && isSpaceOrC0) || isTabOrNL) {
				if (validate()) {
					// If input contains any leading (or trailing) C0 control or space, invalid-URL-unit validation error.
					// If input contains any ASCII tab or newline, invalid-URL-unit validation error.
					validationError("Code point \"" + c + "\" is not a URL unit.");
				}
				// Remove any leading C0 control or space from input.
				if (removeC0ControlOrSpace && isSpaceOrC0) {
					this.input.deleteCharAt(i);
				}
				else if (isTabOrNL) {
					// Remove all ASCII tab or newline from input.
					this.input.deleteCharAt(i);
				}
				i--;
			}
			else {
				strip = false;
			}
		}
		if (removeC0ControlOrSpace) {
			for (int i = this.input.length() - 1; i >= 0; i--) {
				int c = this.input.codePointAt(i);
				if (c == ' ' || isC0Control(c)) {
					if (validate()) {
						// If input contains any (leading or) trailing C0 control or space, invalid-URL-unit validation error.
						validationError("Code point \"" + c + "\" is not a URL unit.");
					}
					// Remove any trailing C0 control or space from input.
					this.input.deleteCharAt(i);
				}
				else {
					break;
				}
			}
		}
	}

	private void setState(State newState) {
		if (logger.isTraceEnabled()) {
			String c;
			if (this.pointer < this.input.length()) {
				c = Character.toString(this.input.codePointAt(this.pointer));
			}
			else {
				c = "EOF";
			}
			logger.trace("Changing state from " + this.state + " to " + newState + " (cur: " + c + " prev: " + this.previousState + ")");
		}
		// EXTRA: we keep the previous state, to ensure that the parser can escape from malformed URI templates
		this.previousState = this.state;
		this.state = newState;
	}

	private static LinkedList<String> strictSplit(String input, int delimiter) {
		// Let position be a position variable for input, initially pointing at the start of input.
		int position = 0;
		// Let tokens be a list of strings, initially empty.
		LinkedList<String> tokens = new LinkedList<>();
		// Let token be the result of collecting a sequence of code points that are not equal to delimiter from input, given position.
		int delIdx = input.indexOf(delimiter, position);
		String token = (delIdx != EOF) ? input.substring(position, delIdx) : input.substring(position);
		position = delIdx;
		// Append token to tokens.
		tokens.add(token);
		// While position is not past the end of input:
		while (position != EOF) {
			// Assert: the code point at position within input is delimiter.
			Assert.state(input.codePointAt(position) == delimiter, "Codepoint is not a delimiter");
			// Advance position by 1.
			position++;
			delIdx = input.indexOf(delimiter, position);
			// Let token be the result of collecting a sequence of code points that are not equal to delimiter from input, given position.
			token = (delIdx != EOF) ? input.substring(position, delIdx) : input.substring(position);
			position = delIdx;
			// Append token to tokens.
			tokens.add(token);
		}
		return tokens;
	}

	private static String domainToAscii(String domain, boolean beStrict) {
		// If beStrict is false, domain is an ASCII string, and strictly splitting domain on U+002E (.) does not produce any item that starts with an ASCII case-insensitive match for "xn--", this step is equivalent to ASCII lowercasing domain.
		if (!beStrict && containsOnlyAscii(domain)) {
			int dotIdx = domain.indexOf('.');
			boolean onlyLowerCase = true;
			while (dotIdx != -1) {
				if (domain.length() - dotIdx > 4) {
					// ASCII case-insensitive match for "xn--"
					int ch0 = domain.codePointAt(dotIdx + 1);
					int ch1 = domain.codePointAt(dotIdx + 2);
					int ch2 = domain.codePointAt(dotIdx + 3);
					int ch3 = domain.codePointAt(dotIdx + 4);
					if ((ch0 == 'x' || ch0 == 'X') &&
							(ch1 == 'n' || ch1 == 'N') &&
							ch2 == '-' && ch3 == '_') {
						onlyLowerCase = false;
						break;
					}
				}
				dotIdx = domain.indexOf('.', dotIdx + 1);
			}
			if (onlyLowerCase) {
				return domain.toLowerCase(Locale.ENGLISH);
			}
		}
		// Let result be the result of running Unicode ToASCII (https://www.unicode.org/reports/tr46/#ToASCII) with domain_name set to domain, UseSTD3ASCIIRules set to beStrict, CheckHyphens set to false, CheckBidi set to true, CheckJoiners set to true, Transitional_Processing set to false, and VerifyDnsLength set to beStrict. [UTS46]
		int flag = 0;
		if (beStrict) {
			flag |= IDN.USE_STD3_ASCII_RULES;
		}
		// Implementation note: implementing Unicode ToASCII is beyond the scope of this parser, we use java.net.IDN.toASCII
		try {
			return IDN.toASCII(domain, flag);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidUrlException("Could not convert \"" + domain + "\" to ASCII: " + ex.getMessage(), ex);
		}
	}

	private boolean validate() {
		return this.validationErrorHandler != null;
	}

	private void validationError(@Nullable String additionalInfo) {
		if (this.validationErrorHandler != null) {
			StringBuilder message = new StringBuilder("URL validation error for URL [");
			message.append(this.input);
			message.append("]@");
			message.append(this.pointer);
			if (additionalInfo != null) {
				message.append(". ");
				message.append(additionalInfo);
			}
			this.validationErrorHandler.accept(message.toString());
		}
	}


	private void failure(@Nullable String additionalInfo) {
		StringBuilder message = new StringBuilder("URL parsing failure for URL [");
		message.append(this.input);
		message.append("] @ ");
		message.append(this.pointer);
		if (additionalInfo != null) {
			message.append(". ");
			message.append(additionalInfo);
		}
		throw new InvalidUrlException(message.toString());
	}

	/**
	 * The C0 control percent-encode set are the C0 controls and all code points greater than U+007E (~).
	 */
	private static boolean c0ControlPercentEncodeSet(int ch) {
		return isC0Control(ch) || Integer.compareUnsigned(ch, '~') > 0;
	}

	/**
	 * The fragment percent-encode set is the C0 control percent-encode set and U+0020 SPACE, U+0022 ("), U+003C (<), U+003E (>), and U+0060 (`).
	 */
	private static boolean fragmentPercentEncodeSet(int ch) {
		return c0ControlPercentEncodeSet(ch) || ch == ' ' || ch == '"' || ch == '<' || ch == '>' || ch == '`';
	}

	/**
	 * The query percent-encode set is the C0 control percent-encode set and U+0020 SPACE, U+0022 ("), U+0023 (#), U+003C (<), and U+003E (>).
	 */
	private static boolean queryPercentEncodeSet(int ch) {
		return c0ControlPercentEncodeSet(ch) || ch == ' ' || ch == '"' || ch == '#' || ch == '<' || ch == '>';
	}

	/**
	 * The special-query percent-encode set is the query percent-encode set and U+0027 (').
	 */
	private static boolean specialQueryPercentEncodeSet(int ch) {
		return queryPercentEncodeSet(ch) || ch == '\'';
	}


	/**
	 * The path percent-encode set is the query percent-encode set and U+003F (?), U+0060 (`), U+007B ({), and U+007D (}).
	 */
	private static boolean pathPercentEncodeSet(int ch) {
		return queryPercentEncodeSet(ch) || ch == '?' || ch == '`' || ch == '{' || ch == '}';
	}

	/**
	 * The userinfo percent-encode set is the path percent-encode set and U+002F (/), U+003A (:), U+003B (;), U+003D (=), U+0040 (@), U+005B ([) to U+005E (^), inclusive, and U+007C (|).
	 */
	private static boolean userinfoPercentEncodeSet(int ch) {
		return pathPercentEncodeSet(ch) || ch == '/' || ch == ':' || ch == ';' || ch == '=' || ch == '@' ||
				(Integer.compareUnsigned(ch, '[') >= 0 && Integer.compareUnsigned(ch, '^') <= 0) || ch == '|';
	}

	private static boolean isC0Control(int ch) {
		return ch >= 0 && ch <= 0x1F;
	}

	private static boolean isNewline(int ch) {
		return ch == '\r' || ch == '\n';
	}

	private static boolean isAsciiAlpha(int ch) {
		return (ch >= 'A' && ch <= 'Z') ||
				(ch >= 'a' && ch <= 'z');
	}

	private static boolean containsOnlyAsciiDigits(CharSequence string) {
		for (int i=0; i< string.length(); i++ ) {
			int ch = codePointAt(string, i);
			if (!isAsciiDigit(ch)) {
				return false;
			}
		}
		return true;
	}

	private static boolean containsOnlyAscii(String string) {
		for (int i = 0; i < string.length(); i++) {
			int ch = string.codePointAt(i);
			if (!isAsciiCodePoint(ch)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isAsciiCodePoint(int ch) {
		// An ASCII code point is a code point in the range U+0000 NULL to U+007F DELETE, inclusive.
		return Integer.compareUnsigned(ch, 0) >= 0 && Integer.compareUnsigned(ch, 127) <= 0;
	}

	private static boolean isAsciiDigit(int ch) {
		return (ch >= '0' && ch <= '9');
	}

	private static boolean isAsciiAlphaNumeric(int ch) {
		return isAsciiAlpha(ch) || isAsciiDigit(ch);
	}

	private static boolean isAsciiHexDigit(int ch) {
		return isAsciiDigit(ch) ||
				(ch >= 'A' && ch <= 'F') ||
				(ch >= 'a' && ch <= 'f');
	}

	private static boolean isForbiddenDomain(int ch) {
		return isForbiddenHost(ch) || isC0Control(ch) || ch == '%' || ch == 0x7F;
	}

	private static boolean isForbiddenHost(int ch) {
		return ch == 0x00 || ch == '\t' || isNewline(ch) || ch == ' ' || ch == '#' || ch == '/' || ch == ':' ||
				ch == '<' || ch == '>' || ch == '?' || ch == '@' || ch == '[' || ch == '\\' || ch == ']' || ch == '^' ||
				ch == '|';
	}

	private static boolean isNonCharacter(int ch) {
		return (ch >= 0xFDD0 && ch <= 0xFDEF) || ch == 0xFFFE || ch == 0xFFFF || ch == 0x1FFFE || ch == 0x1FFFF ||
				ch == 0x2FFFE || ch == 0x2FFFF || ch == 0x3FFFE || ch == 0x3FFFF || ch == 0x4FFFE || ch == 0x4FFFF ||
				ch == 0x5FFFE || ch == 0x5FFFF || ch == 0x6FFFE || ch == 0x6FFFF || ch == 0x7FFFE || ch == 0x7FFFF ||
				ch == 0x8FFFE || ch == 0x8FFFF || ch == 0x9FFFE || ch == 0x9FFFF || ch == 0xAFFFE || ch == 0xAFFFF ||
				ch == 0xBFFFE || ch == 0xBFFFF || ch == 0xCFFFE || ch == 0xCFFFF || ch == 0xDFFFE || ch == 0xDFFFF ||
				ch == 0xEFFFE || ch == 0xEFFFF || ch == 0xFFFFE || ch == 0xFFFFF || ch == 0x10FFFE || ch == 0x10FFFF;
	}

	private static boolean isUrlCodePoint(int ch) {
		return isAsciiAlphaNumeric(ch) ||
				ch == '!' || ch == '$' || ch == '&' || ch == '\'' || ch == '(' || ch == ')' || ch == '*' || ch == '+'
				|| ch == ',' || ch == '-' || ch == '.' || ch == '/' || ch == ':' || ch == ';' || ch == '=' || ch == '?'
				|| ch == '@' || ch == '_' || ch == '~' ||
				(ch >= 0x00A0 && ch <= 0x10FFFD && !Character.isSurrogate((char) ch) && !isNonCharacter(ch));
	}

	private static boolean isSpecialScheme(String scheme) {
		return "ftp".equals(scheme) ||
				"file".equals(scheme) ||
				"http".equals(scheme) ||
				"https".equals(scheme) ||
				"ws".equals(scheme) ||
				"wss".equals(scheme);
	}


	private static int defaultPort(@Nullable String scheme) {
		if (scheme != null) {
			return switch (scheme) {
				case "ftp" -> 21;
				case "http" -> 80;
				case "https" -> 443;
				case "ws" -> 80;
				case "wss" -> 443;
				default -> -1;
			};
		}
		else {
			return -1;
		}
	}

	private void append(String s) {
		this.buffer.append(s);
	}

	private void append(char ch) {
		this.buffer.append(ch);
	}

	private void append(int ch) {
		this.buffer.appendCodePoint(ch);
	}

	private void prepend(String s) {
		this.buffer.insert(0, s);
	}

	private void emptyBuffer() {
		this.buffer.setLength(0);
	}

	private int remaining(int deltaPos) {
		int pos = this.pointer + deltaPos + 1;
		if (pos < this.input.length()) {
			return this.input.codePointAt(pos);
		}
		else {
			return EOF;
		}
	}

	private static String percentDecode(String input) {
		try {
			return UriUtils.decode(input, StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidUrlException("Could not decode \"" + input + "\": " + ex.getMessage(), ex);
		}
	}

	@Nullable
	private String percentEncode(int c, IntPredicate percentEncodeSet) {
		if (this.encoding == null) {
			return null;
		}
		else {
			return percentEncode(Character.toString(c), percentEncodeSet);
		}
	}

	private String percentEncode(String input, IntPredicate percentEncodeSet) {
		if (this.encoding == null) {
			return input;
		}
		else {
			byte[] bytes = input.getBytes(this.encoding);
			boolean original = true;
			for (byte b : bytes) {
				if (percentEncodeSet.test(b)) {
					original = false;
					break;
				}
			}
			if (original) {
				return input;
			}
			StringBuilder output = new StringBuilder();
			for (byte b : bytes) {
				if (!percentEncodeSet.test(b)) {
					output.append((char)b);
				}
				else {
					output.append('%');
					char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
					char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
					output.append(hex1);
					output.append(hex2);
				}
			}
			return output.toString();
		}
	}

	/**
	 * A single-dot URL path segment is a URL path segment that is "[/]." or an ASCII case-insensitive match for "[/]%2e".
	 */
	private static boolean isSingleDotPathSegment(StringBuilder b) {
		int len = b.length();
		switch (len) {
			case 1 -> {
				int ch0 = b.codePointAt(0);
				return ch0 == '.';
			}
			case 2 -> {
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				return ch0 == '/' && ch1 == '.';
			}
			case 3 -> {
				//  ASCII case-insensitive match for "%2e".
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				int ch2 = b.codePointAt(2);
				return ch0 == '%' && ch1 == '2' && (ch2 == 'e' || ch2 == 'E');
			}
			case 4 -> {
				//  ASCII case-insensitive match for "/%2e".
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				int ch2 = b.codePointAt(2);
				int ch3 = b.codePointAt(3);
				return ch0 == '/' && ch1 == '%' && ch2 == '2' && (ch3 == 'e' || ch3 == 'E');
			}
			default -> {
				return false;
			}
		}
	}

	/**
	 * A double-dot URL path segment is a URL path segment that is "[/].." or an ASCII case-insensitive match for "/.%2e", "/%2e.", or "/%2e%2e".
	 */
	private static boolean isDoubleDotPathSegment(StringBuilder b) {
		int len = b.length();
		switch (len) {
			case 2 -> {
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				return ch0 == '.' && ch1 == '.';
			}
			case 3 -> {
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				int ch2 = b.codePointAt(2);
				return ch0 == '/' && ch1 == '.' && ch2 == '.';
			}
			case 4 -> {
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				int ch2 = b.codePointAt(2);
				int ch3 = b.codePointAt(3);
				// case-insensitive match for ".%2e" or "%2e."
				return (ch0 == '.' && ch1 == '%' && ch2 == '2' && (ch3 == 'e' || ch3 == 'E') ||
						(ch0 == '%' && ch1 == '2' && (ch2 == 'e' || ch2 == 'E') && ch3 == '.'));
			}
			case 5 -> {
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				int ch2 = b.codePointAt(2);
				int ch3 = b.codePointAt(3);
				int ch4 = b.codePointAt(4);
				// case-insensitive match for "/.%2e" or "/%2e."
				return ch0 == '/' &&
						(ch1 == '.' && ch2 == '%' && ch3 == '2' && (ch4 == 'e' || ch4 == 'E')
								|| (ch1 == '%' && ch2 == '2' && (ch3 == 'e' || ch3 == 'E') && ch4 == '.'));
			}
			case 6 -> {
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				int ch2 = b.codePointAt(2);
				int ch3 = b.codePointAt(3);
				int ch4 = b.codePointAt(4);
				int ch5 = b.codePointAt(5);
				// case-insensitive match for "%2e%2e".
				return ch0 == '%' && ch1 == '2' && (ch2 == 'e' || ch2 == 'E')
						&& ch3 == '%' && ch4 == '2' && (ch5 == 'e' || ch5 == 'E');
			}
			case 7 -> {
				int ch0 = b.codePointAt(0);
				int ch1 = b.codePointAt(1);
				int ch2 = b.codePointAt(2);
				int ch3 = b.codePointAt(3);
				int ch4 = b.codePointAt(4);
				int ch5 = b.codePointAt(5);
				int ch6 = b.codePointAt(6);
				// case-insensitive match for "/%2e%2e".
				return ch0 == '/' && ch1 == '%' && ch2 == '2' && (ch3 == 'e' || ch3 == 'E')
						&& ch4 == '%' && ch5 == '2' && (ch6 == 'e' || ch6 == 'E');
			}
			default -> {
				return false;
			}
		}
	}


	/**
	 * A Windows drive letter is two code points, of which the first is an ASCII alpha and the second is either U+003A (:) or U+007C (|).
	 *
	 * A normalized Windows drive letter is a Windows drive letter of which the second code point is U+003A (:).
	 */
	private static boolean isWindowsDriveLetter(CharSequence input, boolean normalized) {
		if (input.length() != 2) {
			return false;
		}
		return isWindowsDriveLetterInternal(input, normalized);
	}

	/**
	 * A string starts with a Windows drive letter if all of the following are true:
	 *
	 * its length is greater than or equal to 2
	 * its first two code points are a Windows drive letter
	 * its length is 2 or its third code point is U+002F (/), U+005C (\), U+003F (?), or U+0023 (#).
	 */
	private static boolean startsWithWindowsDriveLetter(String input) {
		int len = input.length();
		if (len < 2) {
			return false;
		}
		if (!isWindowsDriveLetterInternal(input, false)) {
			return false;
		}
		if (len == 2) {
			return true;
		}
		else {
			int ch2 = input.codePointAt(2);
			return ch2 == '/' || ch2 == '\\' || ch2 == '?' || ch2 == '#';
		}
	}

	private static boolean isWindowsDriveLetterInternal(CharSequence s, boolean normalized) {
		int ch0 = codePointAt(s, 0);
		if (!isAsciiAlpha(ch0)) {
			return false;
		}
		else {
			int ch1 = codePointAt(s, 1);
			if (normalized) {
				return ch1 == ':';
			}
			else {
				return ch1 == ':' || ch1 == '|';
			}
		}
	}

	private static int codePointAt(CharSequence s, int index) {
		if (s instanceof String string) {
			return string.codePointAt(index);
		}
		else if (s instanceof StringBuilder builder) {
			return builder.codePointAt(index);
		}
		else {
			throw new IllegalStateException();
		}
	}


	private enum State {

		SCHEME_START {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is an ASCII alpha, append c, lowercased, to buffer, and set state to scheme state.
				if (isAsciiAlpha(c)) {
					p.append(Character.toLowerCase((char) c));
					p.setState(SCHEME);
				}
				// EXTRA: if c is '{', then append c to buffer, set previous state to scheme state, and state to url template state.
				//
				else if (p.previousState != URL_TEMPLATE && c == '{') {
					p.append(c);
					p.previousState = SCHEME;
					p.state = URL_TEMPLATE;
				}
				// Otherwise, if state override is not given, set state to no scheme state and decrease pointer by 1.
				else if (p.stateOverride == null) {
					p.setState(NO_SCHEME);
					p.pointer--;
				}
				// Otherwise, return failure.
				else {
					p.failure(null);
				}
			}
		},
		SCHEME {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is an ASCII alphanumeric, U+002B (+), U+002D (-), or U+002E (.), append c, lowercased, to buffer.
				if (isAsciiAlphaNumeric(c) || (c == '+' || c == '-' || c == '.')) {
					p.append(Character.toLowerCase((char) c));
				}
				// EXTRA: if c is '{', then append c to buffer, set state to url template state.
				else if (p.previousState != URL_TEMPLATE && c == '{') {
					p.append(c);
					p.setState(URL_TEMPLATE);
				}
				// Otherwise, if c is U+003A (:), then:
				else if (c == ':') {
					// If state override is given, then:
					if (p.stateOverride != null) {
						boolean urlSpecialScheme = url.isSpecial();
						String bufferString = p.buffer.toString();
						boolean bufferSpecialScheme = isSpecialScheme(bufferString);
						// If url’s scheme is a special scheme and buffer is not a special scheme, then return.
						if (urlSpecialScheme && !bufferSpecialScheme) {
							return;
						}
						// If url’s scheme is not a special scheme and buffer is a special scheme, then return.
						if (!urlSpecialScheme && bufferSpecialScheme) {
							return;
						}
						// If url includes credentials or has a non-null port, and buffer is "file", then return.
						if ((url.includesCredentials() || url.port() != null) && "file".equals(bufferString)) {
							return;
						}
						// If url’s scheme is "file" and its host is an empty host, then return.
						if ("file".equals(url.scheme()) && (url.host() == null || url.host() == EmptyHost.INSTANCE)) {
							return;
						}
					}
					// Set url’s scheme to buffer.
					url.scheme = p.buffer.toString();
					// If state override is given, then:
					if (p.stateOverride != null) {
						// If url’s port is url’s scheme’s default port, then set url’s port to null.
						if (url.port instanceof IntPort intPort &&
								intPort.value() == defaultPort(url.scheme)) {
							url.port = null;
							// Return.
							p.stopMainLoop = true;
							return;
						}
					}
					// Set buffer to the empty string.
					p.emptyBuffer();
					// If url’s scheme is "file", then:
					if (url.scheme.equals("file")) {
						// If remaining does not start with "//", special-scheme-missing-following-solidus validation error.
						if (p.validate() && (p.remaining(0) != '/' || p.remaining(1) != '/')) {
							p.validationError("\"file\" scheme not followed by \"//\".");
						}
						// Set state to file state.
						p.setState(FILE);
					}
					// Otherwise, if url is special, base is non-null, and base’s scheme is url’s scheme:
					else if (url.isSpecial() && p.base != null && p.base.scheme().equals(url.scheme)) {
						// Assert: base is special (and therefore does not have an opaque path).
						Assert.state(!p.base.path().isOpaque(), "Opaque path not expected");
						// Set state to special relative or authority state.
						p.setState(SPECIAL_RELATIVE_OR_AUTHORITY);
					}
					// Otherwise, if url is special, set state to special authority slashes state.
					else if (url.isSpecial()) {
						p.setState(SPECIAL_AUTHORITY_SLASHES);
					}
					// Otherwise, if remaining starts with an U+002F (/), set state to path or authority state and increase pointer by 1.
					else if (p.remaining(0) == '/') {
						p.setState(PATH_OR_AUTHORITY);
						p.pointer++;
					}
					// Otherwise, set url’s path to the empty string and set state to opaque path state.
					else {
						url.path = new PathSegment("");
						p.setState(OPAQUE_PATH);
					}
				}
				// Otherwise, if state override is not given, set buffer to the empty string, state to no scheme state, and start over (from the first code point in input).
				else if (p.stateOverride == null) {
					p.emptyBuffer();
					p.setState(NO_SCHEME);
					p.pointer = -1;
				}
				// Otherwise, return failure.
				else {
					p.failure(null);
				}

			}
		},
		NO_SCHEME {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If base is null, or base has an opaque path and c is not U+0023 (#), missing-scheme-non-relative-URL
				// validation error, return failure.
				if (p.base == null || p.base.path().isOpaque() && c != '#') {
					p.failure("The input is missing a scheme, because it does not begin with an ASCII alpha \"" +
							(c != EOF ? Character.toString(c) : "") + "\", and no base URL was provided.");
				}
				// Otherwise, if base has an opaque path and c is U+0023 (#), set url’s scheme to base’s scheme, url’s
				// path to base’s path, url’s query to base’s query, url’s fragment to the empty string, and set state to fragment state.
				else if (p.base.path().isOpaque() && c == '#') {
					url.scheme = p.base.scheme();
					url.path = p.base.path();
					url.query = p.base.query;
					url.fragment = new StringBuilder();
					p.setState(FRAGMENT);
				}
				// Otherwise, if base’s scheme is not "file", set state to relative state and decrease pointer by 1.
				else if (!"file".equals(p.base.scheme())) {
					p.setState(RELATIVE);
					p.pointer--;
				}
				// Otherwise, set state to file state and decrease pointer by 1.
				else {
					p.setState(FILE);
					p.pointer--;
				}
			}
		},
		SPECIAL_RELATIVE_OR_AUTHORITY {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is U+002F (/) and remaining starts with U+002F (/), then set state to special authority ignore slashes state and increase pointer by 1.
				if (c == '/' && p.remaining(0) == '/') {
					p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
					p.pointer++;
				}
				// Otherwise, special-scheme-missing-following-solidus validation error, set state to relative state and decrease pointer by 1.
				else {
					if (p.validate()) {
						p.validationError("The input’s scheme is not followed by \"//\".");
					}
					p.setState(RELATIVE);
					p.pointer--;
				}
			}
		},
		PATH_OR_AUTHORITY {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is U+002F (/), then set state to authority state.
				if (c == '/') {
					p.setState(AUTHORITY);
				}
				// Otherwise, set state to path state, and decrease pointer by 1.
				else {
					p.setState(PATH);
					p.pointer--;
				}
			}
		},
		RELATIVE {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// Assert: base’s scheme is not "file".
				Assert.state(p.base != null && !"file".equals(p.base.scheme()), "Base scheme not provided or supported");
				// Set url’s scheme to base’s scheme.
				url.scheme = p.base.scheme;
				// If c is U+002F (/), then set state to relative slash state.
				if (c == '/') {
					// EXTRA : append '/' to let the path segment start with /
					p.append('/');
					p.setState(RELATIVE_SLASH);
				}
				// Otherwise, if url is special and c is U+005C (\), invalid-reverse-solidus validation error, set state to relative slash state.
				else if (url.isSpecial() && c == '\\') {
					if (p.validate()) {
						p.validationError("URL uses \\ instead of /.");
					}
					// EXTRA : append '/' to let the path segment start with /
					p.append('/');
					p.setState(RELATIVE_SLASH);
				}
				// Otherwise
				else {
					// Set url’s username to base’s username, url’s password to base’s password, url’s host to base’s host,
					// url’s port to base’s port, url’s path to a clone of base’s path, and url’s query to base’s query.
					url.username = (p.base.username != null) ? new StringBuilder(p.base.username) : null;
					url.password = (p.base.password != null) ? new StringBuilder(p.base.password) : null;
					url.host = p.base.host();
					url.port = p.base.port();
					url.path = p.base.path().clone();
					url.query = p.base.query;
					// If c is U+003F (?), then set url’s query to the empty string, and state to query state.
					if (c == '?') {
						url.query = new StringBuilder();
						p.setState(QUERY);
					}
					// Otherwise, if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
					else if (c == '#') {
						url.fragment = new StringBuilder();
						p.setState(FRAGMENT);
					}
					// Otherwise, if c is not the EOF code point:
					else if (c != EOF) {
						// Set url’s query to null.
						url.query = null;
						// Shorten url’s path.
						url.shortenPath();
						// Set state to path state and decrease pointer by 1.
						p.setState(PATH);
						p.pointer--;
					}
				}
			}
		},
		RELATIVE_SLASH {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If url is special and c is U+002F (/) or U+005C (\), then:
				if (url.isSpecial() && (c == '/' || c == '\\')) {
					// If c is U+005C (\), invalid-reverse-solidus validation error.
					if (p.validate() && c == '\\') {
						p.validationError("URL uses \\ instead of /.");
					}
					// Set state to special authority ignore slashes state.
					p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
				}
				// Otherwise, if c is U+002F (/), then set state to authority state.
				else if (c == '/') {
					// EXTRA: empty buffer to remove appended slash, since this is not a path
					p.emptyBuffer();
					p.setState(AUTHORITY);
				}
				// Otherwise, set url’s username to base’s username, url’s password to base’s password, url’s host
				// to base’s host, url’s port to base’s port, state to path state, and then, decrease pointer by 1.
				else {
					Assert.state(p.base != null, "No base URL available");
					url.username = (p.base.username != null) ? new StringBuilder(p.base.username) : null;
					url.password = (p.base.password != null) ? new StringBuilder(p.base.password) : null;
					url.host = p.base.host();
					url.port = p.base.port();
					p.setState(PATH);
					p.pointer--;
				}

			}
		},
		SPECIAL_AUTHORITY_SLASHES {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is U+002F (/) and remaining starts with U+002F (/), then set state to special authority ignore slashes state and increase pointer by 1.
				if (c == '/' && p.remaining(0) == '/') {
					p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
					p.pointer++;
				}
				// Otherwise, special-scheme-missing-following-solidus validation error, set state to special authority ignore slashes state and decrease pointer by 1.
				else {
					if (p.validate()) {
						p.validationError("Scheme \"" + url.scheme + "\" not followed by \"//\".");
					}
					p.setState(SPECIAL_AUTHORITY_IGNORE_SLASHES);
					p.pointer--;
				}
			}
		},
		SPECIAL_AUTHORITY_IGNORE_SLASHES {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is neither U+002F (/) nor U+005C (\), then set state to authority state and decrease pointer by 1.
				if (c != '/' && c != '\\') {
					p.setState(AUTHORITY);
					p.pointer--;
				}
				// Otherwise, special-scheme-missing-following-solidus validation error.
				else {
					if (p.validate()) {
						p.validationError("Scheme \"" + url.scheme + "\" not followed by \"//\".");
					}
				}
			}
		},
		AUTHORITY {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is U+0040 (@), then:
				if (c == '@') {
					// Invalid-credentials validation error.
					if (p.validate()) {
						p.validationError("Invalid credentials");
					}
					// If atSignSeen is true, then prepend "%40" to buffer.
					if (p.atSignSeen) {
						p.prepend("%40");
					}
					// Set atSignSeen to true.
					p.atSignSeen = true;

					int bufferLen = p.buffer.length();
					// For each codePoint in buffer:
					for (int i = 0; i < bufferLen; i++) {
						int codePoint = p.buffer.codePointAt(i);
						// If codePoint is U+003A (:) and passwordTokenSeen is false, then set passwordTokenSeen to true and continue.
						if (codePoint == ':' && !p.passwordTokenSeen) {
							p.passwordTokenSeen = true;
							continue;
						}
						// Let encodedCodePoints be the result of running UTF-8 percent-encode codePoint using the userinfo percent-encode set.
						String encodedCodePoints = p.percentEncode(codePoint, UrlParser::userinfoPercentEncodeSet);
						// If passwordTokenSeen is true, then append encodedCodePoints to url’s password.
						if (p.passwordTokenSeen) {
							if (encodedCodePoints != null) {
								url.appendToPassword(encodedCodePoints);
							}
							else {
								url.appendToPassword(codePoint);
							}
						}
						// Otherwise, append encodedCodePoints to url’s username.
						else {
							if (encodedCodePoints != null) {
								url.appendToUsername(encodedCodePoints);
							}
							else {
								url.appendToUsername(codePoint);
							}
						}
					}
					// Set buffer to the empty string.
					p.emptyBuffer();
				}
				// Otherwise, if one of the following is true:
				// - c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
				// - url is special and c is U+005C (\)
				else if ((c == EOF || c == '/' || c == '?' || c == '#') ||
						(url.isSpecial() && c == '\\')) {
					// If atSignSeen is true and buffer is the empty string, host-missing validation error, return failure.
					if (p.atSignSeen && p.buffer.isEmpty()) {
						p.failure("Missing host.");
					}
					// Decrease pointer by buffer’s code point length + 1, set buffer to the empty string, and set state to host state.
					p.pointer -= p.buffer.length() + 1;
					p.emptyBuffer();
					p.setState(HOST);
				}
				// Otherwise, append c to buffer.
				else {
					p.append(c);
				}
			}
		},
		HOST {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If state override is given and url’s scheme is "file", then decrease pointer by 1 and set state to file host state.
				if (p.stateOverride != null && "file".equals(url.scheme())) {
					p.pointer--;
					p.setState(FILE_HOST);
				}
				// Otherwise, if c is U+003A (:) and insideBrackets is false, then:
				else if (c == ':' && !p.insideBrackets) {
					// If buffer is the empty string, host-missing validation error, return failure.
					if (p.buffer.isEmpty()) {
						p.failure("Missing host.");
					}
					// If state override is given and state override is hostname state, then return.
					if (p.stateOverride == HOST) {
						p.stopMainLoop = true;
						return;
					}
					// Let host be the result of host parsing buffer with url is not special.
					Host host = Host.parse(p.buffer.toString(), !url.isSpecial(), p);
					// Set url’s host to host, buffer to the empty string, and state to port state.
					url.host = host;
					p.emptyBuffer();
					p.setState(PORT);
				}
				// Otherwise, if one of the following is true:
				// - c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
				// - url is special and c is U+005C (\)
				else if ( (c == EOF || c == '/' || c == '?' || c == '#') ||
						(url.isSpecial() && c == '\\')) {
					// then decrease pointer by 1, and then:
					p.pointer--;
					// If url is special and buffer is the empty string, host-missing validation error, return failure.
					if (url.isSpecial() && p.buffer.isEmpty()) {
						p.failure("The input has a special scheme, but does not contain a host.");
					}
					// Otherwise, if state override is given, buffer is the empty string, and either url includes credentials or url’s port is non-null, return.
					else if (p.stateOverride != null && p.buffer.isEmpty() &&
							(url.includesCredentials() || url.port() != null )) {
						p.stopMainLoop = true;
						return;
					}
					// EXTRA: if buffer is not empty
					if (!p.buffer.isEmpty()) {
						// Let host be the result of host parsing buffer with url is not special.
						Host host = Host.parse(p.buffer.toString(), !url.isSpecial(), p);
						// Set url’s host to host, buffer to the empty string, and state to path start state.
						url.host = host;
					}
					else {
						url.host = EmptyHost.INSTANCE;
					}
					p.emptyBuffer();
					p.setState(PATH_START);
					// If state override is given, then return.
					if (p.stateOverride != null) {
						p.stopMainLoop = true;
						return;
					}
				}
				// Otherwise:
				else {
					// If c is U+005B ([), then set insideBrackets to true.
					if (c == '[') {
						p.insideBrackets = true;
					}
					// If c is U+005D (]), then set insideBrackets to false.
					else if (c == ']') {
						p.insideBrackets = false;
					}
					// Append c to buffer.
					p.append(c);
				}
			}
		},
		PORT {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is an ASCII digit, append c to buffer.
				if (isAsciiDigit(c)) {
					p.append(c);
				}
				// EXTRA: if c is '{', then append c to buffer, set state to url template state.
				else if (p.previousState != URL_TEMPLATE && c == '{') {
					p.append(c);
					p.setState(URL_TEMPLATE);
				}
				// Otherwise, if one of the following is true:
				// - c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
				// - url is special and c is U+005C (\)
				// - state override is given
				else if (c == EOF || c == '/' || c == '?' || c == '#' ||
						(url.isSpecial() && c == '\\') ||
						(p.stateOverride != null)) {
					// If buffer is not the empty string, then:
					if (!p.buffer.isEmpty()) {
						// EXTRA: if buffer contains only ASCII digits, then
						if (containsOnlyAsciiDigits(p.buffer)) {
							try {
								// Let port be the mathematical integer value that is represented by buffer in radix-10 using ASCII digits for digits with values 0 through 9.
								int port = Integer.parseInt(p.buffer, 0, p.buffer.length(), 10);
								// If port is greater than 2^16 − 1, port-out-of-range validation error, return failure.
								if (port > MAX_PORT) {
									p.failure("Port \"" + port + "\" is out of range");
								}
								int defaultPort = defaultPort(url.scheme);
								// Set url’s port to null, if port is url’s scheme’s default port; otherwise to port.
								if (defaultPort != -1 && port == defaultPort) {
									url.port = null;
								}
								else {
									url.port = new IntPort(port);
								}
							}
							catch (NumberFormatException ex) {
								p.failure(ex.getMessage());
							}
						}
						// EXTRA: otherwise, set url's port to buffer
						else {
							url.port = new StringPort(p.buffer.toString());
						}
						// Set buffer to the empty string.
						p.emptyBuffer();
					}
					// If state override is given, then return.
					if (p.stateOverride != null) {
						p.stopMainLoop = true;
						return;
					}
					// Set state to path start state and decrease pointer by 1.
					p.setState(PATH_START);
					p.pointer--;
				}
				// Otherwise, port-invalid validation error, return failure.
				else {
					p.failure("Invalid port: \"" + Character.toString(c) + "\"");
				}
			}
		},
		FILE {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// Set url’s scheme to "file".
				url.scheme = "file";
				// Set url’s host to the empty string.
				url.host = EmptyHost.INSTANCE;
				// If c is U+002F (/) or U+005C (\), then:
				if (c == '/' || c == '\\') {
					// If c is U+005C (\), invalid-reverse-solidus validation error.
					if (p.validate() && c == '\\') {
						p.validationError("URL uses \\ instead of /.");
					}
					// Set state to file slash state.
					p.setState(FILE_SLASH);
				}
				// Otherwise, if base is non-null and base’s scheme is "file":
				else if (p.base != null && p.base.scheme().equals("file")) {
					// Set url’s host to base’s host, url’s path to a clone of base’s path, and url’s query to base’s query.
					url.host = p.base.host;
					url.path = p.base.path().clone();
					url.query = p.base.query;
					// If c is U+003F (?), then set url’s query to the empty string and state to query state.
					if (c == '?') {
						url.query = new StringBuilder();
						p.setState(QUERY);
					}
					// Otherwise, if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
					else if (c == '#') {
						url.fragment = new StringBuilder();
						p.setState(FRAGMENT);
					}
					// Otherwise, if c is not the EOF code point:
					else if (c != EOF) {
						// Set url’s query to null.
						url.query = null;
						// If the code point substring from pointer to the end of input does not start with a Windows drive letter, then shorten url’s path.
						String substring = p.input.substring(p.pointer);
						if (!startsWithWindowsDriveLetter(substring)) {
							url.shortenPath();
						}
						// Otherwise:
						else {
							// File-invalid-Windows-drive-letter validation error.
							if (p.validate()) {
								p.validationError("The input is a relative-URL string that starts with a Windows " +
										"drive letter and the base URL’s scheme is \"file\".");
							}
							// Set url’s path to « ».
							url.path = new PathSegments();
						}
						// Set state to path state and decrease pointer by 1.
						p.setState(PATH);
						p.pointer--;
					}
				}
				// Otherwise, set state to path state, and decrease pointer by 1.
				else {
					p.setState(PATH);
					p.pointer--;
				}
			}
		},
		FILE_SLASH {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is U+002F (/) or U+005C (\), then:
				if (c == '/' || c == '\\') {
					// If c is U+005C (\), invalid-reverse-solidus validation error.
					if (p.validate() && c == '\\') {
						p.validationError("URL uses \\ instead of /.");
					}
					// Set state to file host state.
					p.setState(FILE_HOST);
				}
				// Otherwise:
				else {
					// If base is non-null and base’s scheme is "file", then:
					if (p.base != null && p.base.scheme.equals("file")) {
						// Set url’s host to base’s host.
						url.host = p.base.host;
						// If the code point substring from pointer to the end of input does not start with a Windows drive letter and base’s path[0] is a normalized Windows drive letter, then append base’s path[0] to url’s path.
						String substring = p.input.substring(p.pointer);
						if (!startsWithWindowsDriveLetter(substring) &&
								p.base.path instanceof PathSegments basePath &&
								!basePath.isEmpty() &&
								isWindowsDriveLetter(basePath.get(0), true)) {
							url.path.append(basePath.get(0));
						}
					}
					// Set state to path state, and decrease pointer by 1.
					p.setState(PATH);
					p.pointer--;
				}
			}
		},
		FILE_HOST {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is the EOF code point, U+002F (/), U+005C (\), U+003F (?), or U+0023 (#), then decrease pointer by 1 and then:
				if (c == EOF || c == '/' || c == '\\' || c == '?' || c == '#') {
					p.pointer--;
					// If state override is not given and buffer is a Windows drive letter, file-invalid-Windows-drive-letter-host validation error, set state to path state.
					if (p.stateOverride == null && isWindowsDriveLetter(p.buffer, false)) {
						p.validationError("A file: URL’s host is a Windows drive letter.");
						p.setState(PATH);
					}
					// Otherwise, if buffer is the empty string, then:
					else if (p.buffer.isEmpty()) {
						// Set url’s host to the empty string.
						url.host = EmptyHost.INSTANCE;
						// If state override is given, then return.
						if (p.stateOverride != null) {
							p.stopMainLoop = true;
							return;
						}
						// Set state to path start state.
						p.setState(PATH_START);
					}
					// Otherwise, run these steps:
					else {
						// Let host be the result of host parsing buffer with url is not special.
						Host host = Host.parse(p.buffer.toString(), !url.isSpecial(), p);
						// If host is "localhost", then set host to the empty string.
						if (host instanceof Domain domain && domain.domain().equals("localhost")) {
							host = EmptyHost.INSTANCE;
						}
						// Set url’s host to host.
						url.host = host;
						// If state override is given, then return.
						if (p.stateOverride != null) {
							p.stopMainLoop = true;
							return;
						}
						// Set buffer to the empty string and state to path start state.
						p.emptyBuffer();
						p.setState(PATH_START);
					}
				}
				// Otherwise, append c to buffer.
				else {
					p.append(c);
				}
			}
		},
		PATH_START {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If url is special, then:
				if (url.isSpecial()) {
					// If c is U+005C (\), invalid-reverse-solidus validation error.
					if (p.validate() && c == '\\') {
						p.validationError("URL uses \"\\\" instead of \"/\"");
					}
					// Set state to path state.
					p.setState(PATH);
					// If c is neither U+002F (/) nor U+005C (\), then decrease pointer by 1.
					if (c != '/' && c != '\\') {
						p.pointer--;
					}
					else {
						p.append('/');
					}
				}
				// Otherwise, if state override is not given and if c is U+003F (?), set url’s query to the empty string and state to query state.
				else if (p.stateOverride == null && c == '?') {
					url.query = new StringBuilder();
					p.setState(QUERY);
				}
				// Otherwise, if state override is not given and if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
				else if (p.stateOverride == null && c =='#') {
					url.fragment = new StringBuilder();
					p.setState(FRAGMENT);
				}
				// Otherwise, if c is not the EOF code point:
				else if (c != EOF) {
					// Set state to path state.
					p.setState(PATH);
					// If c is not U+002F (/), then decrease pointer by 1.
					if (c != '/') {
						p.pointer--;
					}
					// EXTRA: otherwise append '/' to let the path segment start with /
					else {
						p.append('/');
					}
				}
				// Otherwise, if state override is given and url’s host is null, append the empty string to url’s path.
				else if (p.stateOverride != null && url.host() == null) {
					url.path().append("");
				}
			}
		},
		PATH {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If one of the following is true:
				// - c is the EOF code point or U+002F (/)
				// - url is special and c is U+005C (\)
				// - state override is not given and c is U+003F (?) or U+0023 (#)
				// then:
				if (c == EOF || c == '/' ||
						(url.isSpecial() && c == '\\') ||
						(p.stateOverride == null && (c == '?' || c == '#'))) {
					// If url is special and c is U+005C (\), invalid-reverse-solidus validation error.
					if (p.validate() && url.isSpecial() && c == '\\') {
						p.validationError("URL uses \"\\\" instead of \"/\"");
					}
					// If buffer is a double-dot URL path segment, then:
					if (isDoubleDotPathSegment(p.buffer)) {
						// Shorten url’s path.
						url.shortenPath();
						// If neither c is U+002F (/), nor url is special and c is U+005C (\), append the empty string to url’s path.
						if (c != '/' && !(url.isSpecial() && c == '\\')) {
							url.path.append("");
						}
					}
					else {
						boolean singlePathSegment = isSingleDotPathSegment(p.buffer);
						// Otherwise, if buffer is a single-dot URL path segment and if neither c is U+002F (/), nor url is special and c is U+005C (\), append the empty string to url’s path.
						if (singlePathSegment && c != '/' && !(url.isSpecial() && c == '\\')) {
							url.path.append("");
						}
						// Otherwise, if buffer is not a single-dot URL path segment, then:
						else if (!singlePathSegment) {
							// If url’s scheme is "file", url’s path is empty, and buffer is a Windows drive letter, then replace the second code point in buffer with U+003A (:).
							if ("file".equals(url.scheme) && url.path.isEmpty() && isWindowsDriveLetter(p.buffer, false)) {
								p.buffer.setCharAt(1, ':');
							}
							// Append buffer to url’s path.
							url.path.append(p.buffer.toString());
						}
					}
					// Set buffer to the empty string.
					p.emptyBuffer();
					if ( c == '/' || url.isSpecial() && c == '\\') {
						p.append('/');
					}
					// If c is U+003F (?), then set url’s query to the empty string and state to query state.
					if (c == '?') {
						url.query = new StringBuilder();
						p.setState(QUERY);
					}
					// If c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
					if (c == '#') {
						url.fragment = new StringBuilder();
						p.setState(FRAGMENT);
					}
				}
				// EXTRA: Otherwise, if c is '{', then append c to buffer, set state to url template state.
				else if (p.previousState != URL_TEMPLATE && c == '{') {
					p.append(c);
					p.setState(URL_TEMPLATE);
				}
				// Otherwise, run these steps:
				else {
					if (p.validate()) {
						// If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
						if (!isUrlCodePoint(c) && c != '%') {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
						// If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
						else if (c == '%' &&
								(p.pointer >= p.input.length() - 2 ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 1)) ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 2)))) {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
					}
					// UTF-8 percent-encode c using the path percent-encode set and append the result to buffer.
					String encoded = p.percentEncode(c, UrlParser::pathPercentEncodeSet);
					if (encoded != null) {
						p.append(encoded);
					}
					else {
						p.append(c);
					}
				}
			}
		},
		OPAQUE_PATH {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// EXTRA: if previous state is URL Template and the buffer is empty, append buffer to url's path and empty the buffer
				if (p.previousState == URL_TEMPLATE && !p.buffer.isEmpty()) {
					url.path.append(p.buffer.toString());
					p.emptyBuffer();
				}
				// If c is U+003F (?), then set url’s query to the empty string and state to query state.
				if (c == '?') {
					url.query = new StringBuilder();
					p.setState(QUERY);
				}
				// Otherwise, if c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
				else if (c == '#') {
					url.fragment = new StringBuilder();
					p.setState(FRAGMENT);
				}
				// EXTRA: Otherwise, if c is '{', then append c to buffer, set state to url template state.
				else if (p.previousState != URL_TEMPLATE && c == '{') {
					p.append(c);
					p.setState(URL_TEMPLATE);
				}
				// Otherwise:
				else {
					if (p.validate()) {
						// If c is not the EOF code point, not a URL code point, and not U+0025 (%), invalid-URL-unit validation error.
						if (c != EOF && !isUrlCodePoint(c) && c != '%') {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
						// If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
						else if (c == '%' &&
								(p.pointer >= p.input.length() - 2 ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 1)) ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 2)))) {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
					}
					// If c is not the EOF code point, UTF-8 percent-encode c using the C0 control percent-encode set and append the result to url’s path.
					if (c != EOF) {
						String encoded = p.percentEncode(c, UrlParser::c0ControlPercentEncodeSet);
						if (encoded != null) {
							url.path.append(encoded);
						}
						else {
							url.path.append(c);
						}
					}
				}
			}
		},
		QUERY {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If encoding is not UTF-8 and one of the following is true:
				// - url is not special
				// - url’s scheme is "ws" or "wss"
				//  then set encoding to UTF-8.
				if (p.encoding != null &&
						!StandardCharsets.UTF_8.equals(p.encoding) &&
						(!url.isSpecial() || "ws".equals(url.scheme) || "wss".equals(url.scheme))) {
					p.encoding = StandardCharsets.UTF_8;
				}
				// If one of the following is true:
				// - state override is not given and c is U+0023 (#)
				// - c is the EOF code point
				if ( (p.stateOverride == null && c == '#') || c == EOF) {
					// Let queryPercentEncodeSet be the special-query percent-encode set if url is special; otherwise the query percent-encode set.
					IntPredicate queryPercentEncodeSet = url.isSpecial() ? UrlParser::specialQueryPercentEncodeSet : UrlParser::queryPercentEncodeSet;
					// Percent-encode after encoding, with encoding, buffer, and queryPercentEncodeSet, and append the result to url’s query.
					String encoded = p.percentEncode(p.buffer.toString(), queryPercentEncodeSet);
					Assert.state(url.query != null, "Url's query should not be null");
					url.query.append(encoded);
					// Set buffer to the empty string.
					p.emptyBuffer();
					// If c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
					if (c == '#') {
						url.fragment = new StringBuilder();
						p.setState(FRAGMENT);
					}
				}
				// EXTRA: Otherwise, if c is '{', then append c to buffer, set state to url template state.
				else if (p.previousState != URL_TEMPLATE && c == '{') {
					p.append(c);
					p.setState(URL_TEMPLATE);
				}
				// Otherwise, if c is not the EOF code point:
				else if (c != EOF) {
					if (p.validate()) {
						// If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
						if (!isUrlCodePoint(c) && c != '%') {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
						// If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
						else if (c == '%' &&
								(p.pointer >= p.input.length() - 2 ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 1)) ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 2)))) {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
					}
					// Append c to buffer.
					p.append(c);
				}
			}
		},
		FRAGMENT {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				// If c is not the EOF code point, then:
				if (c != EOF) {
					if (p.validate()) {
						// If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
						if (!isUrlCodePoint(c) && c != '%') {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
						// If c is U+0025 (%) and remaining does not start with two ASCII hex digits, invalid-URL-unit validation error.
						else if (c == '%' &&
								(p.pointer >= p.input.length() - 2 ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 1)) ||
										!isAsciiHexDigit(p.input.codePointAt(p.pointer + 2)))) {
							p.validationError("Invalid URL Unit: \"" + (char) c + "\"");
						}
					}
					// UTF-8 percent-encode c using the fragment percent-encode set and append the result to url’s fragment.
					String encoded = p.percentEncode(c, UrlParser::fragmentPercentEncodeSet);
					Assert.state(url.fragment != null, "Url's fragment should not be null");
					if (encoded != null) {
						url.fragment.append(encoded);
					}
					else {
						url.fragment.appendCodePoint(c);
					}
				}
			}
		},
		URL_TEMPLATE {
			@Override
			public void handle(int c, UrlRecord url, UrlParser p) {
				Assert.state(p.previousState != null, "No previous state set");
				if (c == '}') {
					p.append(c);
					p.setState(p.previousState);
				}
				else if (c == EOF) {
					p.pointer -= p.buffer.length() + 1;
					p.emptyBuffer();
					p.setState(p.previousState);
				}
				else {
					p.append(c);
				}
			}
		};

		public abstract void handle(int c, UrlRecord url, UrlParser p);


	}


	/**
	 * A URL is a struct that represents a universal identifier. To disambiguate from a valid URL string it can also be
	 * referred to as a
	 * <em>URL record</em>.
	 */
	static final class UrlRecord {

		private String scheme = "";

		@Nullable
		private StringBuilder username = null;

		@Nullable
		private StringBuilder password = null;

		@Nullable
		private Host host = null;

		@Nullable
		private Port port = null;

		private Path path = new PathSegments();

		@Nullable
		private StringBuilder query = null;

		@Nullable
		private StringBuilder fragment = null;

		public UrlRecord() {
		}


		/**
		 * A URL is special if its scheme is a special scheme. A URL is not special if its scheme is not a special scheme.
		 */
		public boolean isSpecial() {
			return isSpecialScheme(this.scheme);
		}


		/**
		 * A URL includes credentials if its username or password is not the empty string.
		 */
		public boolean includesCredentials() {
			return this.username != null && !this.username.isEmpty() || this.password != null && !this.password.isEmpty();
		}

		/**
		 * A URL has an opaque path if its path is a URL path segment.
		 */
		public boolean hasOpaquePath() {
			return path().isOpaque();
		}


		/**
		 * The serialization of an origin is the string obtained by applying the following algorithm to the given origin origin:
		 * If origin is an opaque origin, then return "null".
		 * Otherwise, let result be origin's scheme.
		 * Append "://" to result.
		 * Append origin's host, serialized, to result.
		 * If origin's port is non-null, append a U+003A COLON character (:), and origin's port, serialized, to result.
		 * Return result.
		 */
		public String origin() {
			String scheme = scheme();
			if (scheme.equals("ftp") || scheme.equals("http") || scheme.equals("https") || scheme.equals("ws") || scheme.equals("wss")) {
				StringBuilder builder = new StringBuilder(scheme);
				builder.append("://");
				builder.append(host());
				Port port = port();
				if (port != null) {
					builder.append(':');
					builder.append(port);
				}
				return builder.toString();
			}
			else {
				return "null";
			}
		}

		/**
		 * A URL’s scheme is an ASCII string that identifies the type of URL and can be used to dispatch a URL for
		 * further processing after parsing. It is initially the empty string.
		 */
		public String scheme() {
			return this.scheme;
		}

		/**
		 * The protocol getter steps are to return this’s URL’s scheme, followed by U+003A (:).
		 */
		public String protocol() {
			return scheme() + ":";
		}

		/**
		 * A URL’s username is an ASCII string identifying a username. It is initially the empty string.
		 */
		public String username() {
			if (this.username != null) {
				return this.username.toString();
			}
			else {
				return "";
			}
		}

		void appendToUsername(int codePoint) {
			if (this.username == null) {
				this.username = new StringBuilder(2);
			}
			this.username.appendCodePoint(codePoint);
		}

		public void appendToUsername(String s) {
			if (this.username == null) {
				this.username = new StringBuilder(s);
			}
			else {
				this.username.append(s);
			}
		}

		/**
		 * A URL’s password is an ASCII string identifying a password. It is initially the empty string.
		 */
		public String password() {
			if (this.password != null) {
				return this.password.toString();
			}
			else {
				return "";
			}
		}

		void appendToPassword(int codePoint) {
			if (this.password == null) {
				this.password = new StringBuilder(2);
			}
			this.password.appendCodePoint(codePoint);
		}

		void appendToPassword(String s) {
			if (this.password == null) {
				this.password = new StringBuilder(s);
			}
			else {
				this.password.append(s);
			}
		}

		/**
		 * A URL’s host is {@code null} or a {@linkplain Host host}. It is initially {@code null}.
		 */
		@Nullable
		public Host host() {
			return this.host;
		}

		/**
		 *The host getter steps are:
		 * Let url be this’s URL.
		 * If url’s host is null, then return the empty string.
		 * If url’s port is null, return url’s host, serialized.
		 * Return url’s host, serialized, followed by U+003A (:) and url’s port, serialized.
		 */
		public String hostString() {
			if (host() == null) {
				return "";
			}
			StringBuilder builder = new StringBuilder(hostname());
			Port port = port();
			if (port != null) {
				builder.append(':');
				builder.append(port);
			}
			return builder.toString();
		}

		public String hostname() {
			Host host = host();
			if (host == null) {
				return "";
			}
			else {
				return host.toString();
			}
		}

		/**
		 * A URL’s port is either null, a string representing a 16-bit unsigned integer  that identifies a networking
		 * port, or a string containing a uri template . It is initially {@code null}.
		 */
		@Nullable
		public Port port() {
			return this.port;
		}

		public String portString() {
			if (port() == null) {
				return "";
			}
			else {
				return port().toString();
			}
		}

		/**
		 * A URL’s path is a URL {@linkplain Path path}, usually identifying a location. It is initially {@code « »}.
		 */
		public Path path() {
			return this.path;
		}

		public String pathname() {
			return path().name();
		}

		/**
		 * To shorten a url’s path:
		 * <ol>
	 	 * <li>Assert: url does not have an opaque path.</li>
		 * <li>Let path be url’s path.</li>
		 * <li>If url’s scheme is "file", path’s size is 1, and path[0] is a
		 * normalized Windows drive letter, then return.</li>
		 * <li>Remove path’s last item, if any.</li>
		 * </ol>
		 */
		public void shortenPath() {
			this.path.shorten(this.scheme);
		}

		/**
		 * A URL’s query is either {@code null} or an ASCII string. It is initially {@code null}.
		 */
		@Nullable
		public String query() {
			if (this.query == null) {
				return null;
			}
			else {
				return this.query.toString();
			}
		}

		/**
		 * The search getter steps are:
		 * If this’s URL’s query is either null or the empty string, then return the empty string.
		 * Return U+003F (?), followed by this’s URL’s query.
		 */
		public String search() {
			String query = query();
			if (query == null) {
				return "";
			}
			else {
				return "?" + query;
			}
		}

		/**
		 * A URL’s fragment is either {@code null}  or an ASCII string that can be used for further processing on the
		 * resource the URL’s other components identify. It is initially {@code null}.
		 */
		@Nullable
		public String fragment() {
			if (this.fragment == null) {
				return null;
			}
			else {
				return this.fragment.toString();
			}
		}

		/**
		 * The hash getter steps are:
		 * If this’s URL’s fragment is either null or the empty string, then return the empty string.
		 * Return U+0023 (#), followed by this’s URL’s fragment.
		 */
		public String hash() {
			String fragment = fragment();
			if (fragment == null || fragment.isEmpty()) {
				return "";
			}
			else {
				return "#" + fragment;
			}
		}

		public String href() {
			// Let output be url’s scheme and U+003A (:) concatenated.
			StringBuilder output = new StringBuilder(scheme());
			output.append(':');
			Host host = host();
			// If url’s host is non-null:
			if (host != null) {
				// Append "//" to output.
				output.append("//");
				// If url includes credentials, then:
				if (includesCredentials()) {
					// Append url’s username to output.
					output.append(username());
					String password = password();
					// If url’s password is not the empty string, then append U+003A (:), followed by url’s password, to output.
					if (!password.isEmpty()) {
						output.append(':');
						output.append(password);
					}
					// Append U+0040 (@) to output.
					output.append('@');
				}
				// Append url’s host, serialized, to output.
				output.append(hostname());
				Port port = port();
				// If url’s port is non-null, append U+003A (:) followed by url’s port, serialized, to output.
				if (port != null) {
					output.append(':');
					output.append(port());
				}
			}
			// If url’s host is null, url does not have an opaque path, url’s path’s size is greater than 1, and url’s path[0] is the empty string, then append U+002F (/) followed by U+002E (.) to output.
			else if (!hasOpaquePath() &&
					path() instanceof PathSegments pathSegments &&
					pathSegments.size() > 1 &&
					pathSegments.get(0).isEmpty()) {
				output.append("/.");
			}
			// Append the result of URL path serializing url to output.
			output.append(pathname());
			// If url’s query is non-null, append U+003F (?), followed by url’s query, to output.
			String query = query();
			if (query != null) {
				output.append('?');
				output.append(query);
			}
			// If exclude fragment is false and url’s fragment is non-null, then append U+0023 (#), followed by url’s fragment, to output.
			String fragment = fragment();
			if (fragment != null) {
				output.append('#');
				output.append(fragment);
			}
			// Return output.
			return output.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			UrlRecord that = (UrlRecord) obj;
			return Objects.equals(this.scheme(), that.scheme()) &&
					Objects.equals(this.username(), that.username()) &&
					Objects.equals(this.password(), that.password()) &&
					Objects.equals(this.host(), that.host()) &&
					Objects.equals(this.port(), that.port()) &&
					Objects.equals(this.path(), that.path()) &&
					Objects.equals(this.query(), that.query()) &&
					Objects.equals(this.fragment(), that.fragment());
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.scheme, this.username, this.password, this.host, this.port, this.path, this.query, this.fragment);
		}

		@Override
		public String toString() {
			return "UrlRecord[" +
					"scheme=" + this.scheme + ", " +
					"username=" + this.username + ", " +
					"password=" + this.password + ", " +
					"host=" + this.host + ", " +
					"port=" + this.port + ", " +
					"path=" + this.path + ", " +
					"query=" + this.query + ", " +
					"fragment=" + this.fragment + ']';
		}


	}

	/**
	 * A host is a domain, an IP address, an opaque host, or an empty host.
	 * Typically a host serves as a network address, but it is sometimes used as
	 * opaque identifier in URLs where a network address is not necessary.
	 */
	sealed interface Host permits Domain, EmptyHost, IpAddressHost, OpaqueHost {


		/**
		 * The host parser takes a scalar value string input with an optional
		 * boolean isOpaque (default false), and then runs these steps. They return failure or a host.
		 */
		static Host parse(String input, boolean isOpaque, UrlParser p) {
			// If input starts with U+005B ([), then:
			if (!input.isEmpty() && input.codePointAt(0) == '[') {
				int last = input.length() - 1;
				// If input does not end with U+005D (]), IPv6-unclosed validation error, return failure.
				if (input.codePointAt(last) != ']') {
					throw new InvalidUrlException("IPv6 address is missing the closing \"]\").");
				}
				// Return the result of IPv6 parsing input with its leading U+005B ([) and trailing U+005D (]) removed.
				String ipv6Host = input.substring(1, last);
				return new IpAddressHost(Ipv6Address.parse(ipv6Host));
			}
			// If isOpaque is true, then return the result of opaque-host parsing input.
			if (isOpaque) {
				return OpaqueHost.parse(input, p);
			}
			// Assert: input is not the empty string.
			Assert.state(!input.isEmpty(), "Input should not be empty");

			// Let domain be the result of running UTF-8 decode without BOM on the percent-decoding of input.
			String domain = percentDecode(input);
			// Let asciiDomain be the result of running domain to ASCII with domain and false.
			String asciiDomain = domainToAscii(domain, false);

			for (int i=0; i < asciiDomain.length(); i++) {
				int ch = asciiDomain.codePointAt(i);
				// If asciiDomain contains a forbidden domain code point, domain-invalid-code-point validation error, return failure.
				if (isForbiddenDomain(ch)) {
					throw new InvalidUrlException("Invalid character \"" + ch + "\" in domain \"" + input + "\"");
				}
			}
			// If asciiDomain ends in a number, then return the result of IPv4 parsing asciiDomain.
			if (endsInNumber(asciiDomain)) {
				Ipv4Address address = Ipv4Address.parse(asciiDomain, p);
				return new IpAddressHost(address);
			}
			// Return asciiDomain.
			else {
				return new Domain(asciiDomain);
			}
		}

		private static boolean endsInNumber(String input) {
			// Let parts be the result of strictly splitting input on U+002E (.).
			LinkedList<String> parts = strictSplit(input, '.');
			if (parts.isEmpty()) {
				return false;
			}
			// If the last item in parts is the empty string, then:
			if (parts.getLast().isEmpty()) {
				// If parts’s size is 1, then return false.
				if (parts.size() == 1) {
					return false;
				}
				// Remove the last item from parts.
				parts.removeLast();
			}
			// Let last be the last item in parts.
			String last = parts.getLast();
			// If last is non-empty and contains only ASCII digits, then return true.
			if (!last.isEmpty() && containsOnlyAsciiDigits(last)) {
				return true;
			}
			// If parsing last as an IPv4 number does not return failure, then return true.
			ParseIpv4NumberResult result = Ipv4Address.parseIpv4Number(last);
			return result != ParseIpv4NumberFailure.INSTANCE;
		}
	}

	/**
	 * A domain is a non-empty ASCII string that identifies a realm within a
	 * network. [RFC1034].
	 */
	static final class Domain implements Host {

		private final String domain;

		Domain(String domain) {
			this.domain = domain;
		}

		public String domain() {
			return this.domain;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o instanceof Domain other) {
				return this.domain.equals(other.domain);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return this.domain.hashCode();
		}

		@Override
		public String toString() {
			return this.domain;
		}

	}

	static final class IpAddressHost implements Host {

		private final IpAddress address;

		private final String addressString;

		IpAddressHost(IpAddress address) {
			this.address = address;
			if (address instanceof Ipv6Address) {
				this.addressString = "[" + address + "]";
			}
			else {
				this.addressString = address.toString();
			}
		}

		public IpAddress address() {
			return this.address;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			else if (obj instanceof IpAddressHost other) {
				return this.address.equals(other.address);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return this.address.hashCode();
		}

		@Override
		public String toString() {
			return this.addressString;
		}
	}

	static final class OpaqueHost implements Host {

		private final String host;

		private OpaqueHost(String host) {
			this.host = host;
		}

		/**
		 * The opaque-host parser takes a scalar value string input, and then runs these steps. They return failure or
		 * an opaque host.
		 */
		public static OpaqueHost parse(String input, UrlParser p) {
			for (int i = 0; i < input.length(); i++) {
				int ch = input.codePointAt(i);
				// If input contains a forbidden host code point, host-invalid-code-point validation error, return failure.
				if (isForbiddenHost(ch)) {
					throw new InvalidUrlException("An opaque host contains a forbidden host code point.");
				}
				// If input contains a code point that is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
				if (p.validate() && !isUrlCodePoint(ch) && ch != '%') {
					p.validationError("Code point \"" + ch + "\" is not a URL unit.");
				}
				//If input contains a U+0025 (%) and the two code points following it are not ASCII hex digits, invalid-URL-unit validation error.
				if (p.validate() && ch == '%' && (input.length() - i < 2 || !isAsciiDigit(input.codePointAt(i + 1)) || !isAsciiDigit(input.codePointAt(i + 2)))) {
					p.validationError("Code point \"" + ch + "\" is not a URL unit.");
				}
			}
			//Return the result of running UTF-8 percent-encode on input using the C0 control percent-encode set.
			String encoded = p.percentEncode(input, UrlParser::c0ControlPercentEncodeSet);
			return new OpaqueHost(encoded);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			else if (obj instanceof OpaqueHost other) {
				return this.host.equals(other.host);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return this.host.hashCode();
		}

		@Override
		public String toString() {
			return this.host;
		}

	}

	static final class EmptyHost implements Host {

		static final EmptyHost INSTANCE = new EmptyHost();

		private EmptyHost() {
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj != null && obj.getClass() == this.getClass();
		}

		@Override
		public int hashCode() {
			return 1;
		}

		@Override
		public String toString() {
			return "";
		}

	}

	sealed interface IpAddress permits Ipv4Address, Ipv6Address {

	}

	static final class Ipv4Address implements IpAddress {

		private final int address;

		private final String string;

		Ipv4Address(int address) {
			this.address = address;
			this.string = serialize(address);
		}

		/**
		 * The IPv4 serializer takes an IPv4 address {@code address} and then runs these steps. They return an ASCII string.
		 */
		private static String serialize(int address) {
			//Let output be the empty string.
			StringBuilder output = new StringBuilder();
			//Let n be the value of address.
			int n = address;
			//For each i in the range 1 to 4, inclusive:
			for (int i = 1; i <= 4; i++) {
				// Prepend n % 256, serialized, to output.
				output.insert(0, Integer.toUnsignedString(Integer.remainderUnsigned(n, 256)));
				//If i is not 4, then prepend U+002E (.) to output.
				if (i != 4) {
					output.insert(0, '.');
				}
				//Set n to floor(n / 256).
				n = Math.floorDiv(n, 256);
			}
			//Return output.
			return output.toString();
		}

		public static Ipv4Address parse(String input, UrlParser p) {
			// Let parts be the result of strictly splitting input on U+002E (.).
			List<String> parts = strictSplit(input, '.');
			int partsSize = parts.size();
			// If the last item in parts is the empty string, then:
			if (parts.get(partsSize - 1).isEmpty()) {
				// IPv4-empty-part validation error.
				p.validationError("IPv4 address ends with \".\"");
				// If parts’s size is greater than 1, then remove the last item from parts.
				if (partsSize > 1) {
					parts.remove(partsSize - 1);
					partsSize--;
				}
			}
			// If parts’s size is greater than 4, IPv4-too-many-parts validation error, return failure.
			if (partsSize > 4) {
				throw new InvalidUrlException("IPv4 address does not consist of exactly 4 parts.");
			}
			// Let numbers be an empty list.
			List<Integer> numbers = new ArrayList<>(partsSize);
			// For each part of parts:
			for (int i = 0; i < partsSize; i++) {
				String part = parts.get(i);
				// Let result be the result of parsing part.
				ParseIpv4NumberResult result = parseIpv4Number(part);
				// If result is failure, IPv4-non-numeric-part validation error, return failure.
				if (result == ParseIpv4NumberFailure.INSTANCE) {
					p.failure("An IPv4 address part is not numeric.");
				}
				else {
					ParseIpv4NumberSuccess success = (ParseIpv4NumberSuccess) result;
					if (p.validate() && success.validationError()) {
						p.validationError("The IPv4 address contains numbers expressed using hexadecimal or octal digits.");
					}
					// Append result to numbers.
					numbers.add(success.number());
				}
			}
			for (Iterator<Integer> iterator = numbers.iterator(); iterator.hasNext(); ) {
				Integer number = iterator.next();
				// If any item in numbers is greater than 255, IPv4-out-of-range-part validation error.
				if (p.validate() && number > 255) {
					p.validationError("An IPv4 address part exceeds 255.");
				}
				if (iterator.hasNext()) {
					// If any but the last item in numbers is greater than 255, then return failure.
					if (number > 255) {
						throw new InvalidUrlException("An IPv4 address part exceeds 255.");
					}
				}
				else {
					// If the last item in numbers is greater than or equal to 256^(5 − numbers’s size), then return failure.
					double limit = Math.pow(256, (5 - numbers.size()));
					if (number >= limit) {
						throw new InvalidUrlException("IPv4 address part " + number + " exceeds " + limit + ".'");
					}
				}
			}
			// Let ipv4 be the last item in numbers.
			int ipv4 = numbers.get(numbers.size() - 1);
			// Remove the last item from numbers.
			numbers.remove(numbers.size() - 1);
			// Let counter be 0.
			int counter = 0;
			// For each n of numbers:
			for (Integer n : numbers) {
				// Increment ipv4 by n × 256^(3 − counter).
				int increment = n * (int) Math.pow(256, 3 - counter);
				ipv4 += increment;
				// Increment counter by 1.
				counter++;
			}
			// Return ipv4.
			return new Ipv4Address(ipv4);
		}

		/**
		 * The IPv4 number parser takes an ASCII string input and then runs these steps. They return failure or a tuple of a number and a boolean.
		 */
		private static ParseIpv4NumberResult parseIpv4Number(String input) {
			// If input is the empty string, then return failure.
			if (input.isEmpty()) {
				return ParseIpv4NumberFailure.INSTANCE;
			}
			// Let validationError be false.
			boolean validationError = false;
			// Let R be 10.
			int r = 10;
			int len = input.length();
			// If input contains at least two code points and the first two code points are either "0X" or "0x", then:
			if (len >= 2) {
				int ch0 = input.codePointAt(0);
				int ch1 = input.codePointAt(1);
				if (ch0 == '0' && (ch1 == 'X' || ch1 == 'x')) {
					// Set validationError to true.
					validationError = true;
					// Remove the first two code points from input.
					input = input.substring(2);
					// Set R to 16.
					r = 16;
				}
				// Otherwise, if input contains at least two code points and the first code point is U+0030 (0), then:
				else if (ch0 == '0') {
					// Set validationError to true.
					validationError = true;
					// Remove the first code point from input.
					input = input.substring(1);
					// Set R to 8.
					r = 8;
				}
			}
			// If input is the empty string, then return (0, true).
			if (input.isEmpty()) {
				return new ParseIpv4NumberSuccess(0, true);
			}
			// If input contains a code point that is not a radix-R digit, then return failure.
			for (int i = 0; i < input.length(); i++) {
				int c = input.codePointAt(i);
				int digit = Character.digit(c, r);
				if (digit == -1) {
					return ParseIpv4NumberFailure.INSTANCE;
				}
			}
			try {
				// Let output be the mathematical integer value that is represented by input in radix-R notation, using ASCII hex digits for digits with values 0 through 15.
				int output = Integer.parseInt(input, r);
				// Return (output, validationError).
				return new ParseIpv4NumberSuccess(output, validationError);
			}
			catch (NumberFormatException ex) {
				return ParseIpv4NumberFailure.INSTANCE;
			}
		}


		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o instanceof Ipv4Address other) {
				return this.address == other.address;
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return this.address;
		}

		@Override
		public String toString() {
			return this.string;
		}
	}

	static final class Ipv6Address implements IpAddress {

		private final int[] pieces;

		private final String string;

		private Ipv6Address(int[] pieces) {
			Assert.state(pieces.length == 8, "Invalid amount of IPv6 pieces");
			this.pieces = pieces;
			this.string = serialize(pieces);
		}

		/**
		 * The IPv6 parser takes a scalar value string input and then runs these steps. They return failure or an IPv6 address.
		 */
		public static Ipv6Address parse(String input) {
			// Let address be a new IPv6 address whose IPv6 pieces are all 0.
			int[] address = new int[8];
			// Let pieceIndex be 0.
			int pieceIndex = 0;
			// Let compress be null.
			Integer compress = null;
			// Let pointer be a pointer for input.
			int pointer = 0;
			int inputLength = input.length();
			int c = (inputLength > 0) ? input.codePointAt(0) : EOF;
			// If c is U+003A (:), then:
			if (c == ':') {
				// If remaining does not start with U+003A (:), IPv6-invalid-compression validation error, return failure.
				if (inputLength > 1 && input.codePointAt(1) != ':') {
					throw new InvalidUrlException("IPv6 address begins with improper compression.");
				}
				// Increase pointer by 2.
				pointer += 2;
				// Increase pieceIndex by 1 and then set compress to pieceIndex.
				pieceIndex++;
				compress = pieceIndex;
			}
			c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
			// While c is not the EOF code point:
			while (c != EOF) {
				// If pieceIndex is 8, IPv6-too-many-pieces validation error, return failure.
				if (pieceIndex == 8) {
					throw new InvalidUrlException("IPv6 address contains more than 8 pieces.");
				}
				// If c is U+003A (:), then:
				if (c == ':') {
					// If compress is non-null, IPv6-multiple-compression validation error, return failure.
					if (compress != null) {
						throw new InvalidUrlException("IPv6 address is compressed in more than one spot.");
					}
					// Increase pointer and pieceIndex by 1, set compress to pieceIndex, and then continue.
					pointer++;
					pieceIndex++;
					compress = pieceIndex;
					c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
					continue;
				}
				// Let value and length be 0.
				int value = 0;
				int length = 0;
				// While length is less than 4 and c is an ASCII hex digit, set value to value × 0x10 + c interpreted as hexadecimal number, and increase pointer and length by 1.
				while (length < 4 && isAsciiHexDigit(c)) {
					int cHex = Character.digit(c, 16);
					value = (value * 0x10) + cHex;
					pointer++;
					length++;
					c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
				}
				// If c is U+002E (.), then:
				if (c == '.') {
					// If length is 0, IPv4-in-IPv6-invalid-code-point validation error, return failure.
					if (length == 0) {
						throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part is empty.");
					}
					// Decrease pointer by length.
					pointer -= length;
					// If pieceIndex is greater than 6, IPv4-in-IPv6-too-many-pieces validation error, return failure.
					if (pieceIndex > 6) {
						throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv6 address has more than 6 pieces.");
					}
					// Let numbersSeen be 0.
					int numbersSeen = 0;
					c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
					// While c is not the EOF code point:
					while (c != EOF) {
						// Let ipv4Piece be null.
						Integer ipv4Piece = null;
						// If numbersSeen is greater than 0, then:
						if (numbersSeen > 0) {
							// If c is a U+002E (.) and numbersSeen is less than 4, then increase pointer by 1.
							if (c =='.' && numbersSeen < 4) {
								pointer++;
								c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
							}
							// Otherwise, IPv4-in-IPv6-invalid-code-point validation error, return failure.
							else {
								throw new InvalidUrlException("IPv6 address with IPv4 address syntax: " +
										"IPv4 part is empty or contains a non-ASCII digit.");
							}
						}
						// If c is not an ASCII digit, IPv4-in-IPv6-invalid-code-point validation error, return failure.
						if (!isAsciiDigit(c)) {
							throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part contains a non-ASCII digit.");
						}
						// While c is an ASCII digit:
						while (isAsciiDigit(c)) {
							// Let number be c interpreted as decimal number.
							int number = Character.digit(c, 10);
							// If ipv4Piece is null, then set ipv4Piece to number.
							if (ipv4Piece == null) {
								ipv4Piece = number;
							}
							// Otherwise, if ipv4Piece is 0, IPv4-in-IPv6-invalid-code-point validation error, return failure.
							else if (ipv4Piece == 0) {
								throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part contains a non-ASCII digit.");
							}
							// Otherwise, set ipv4Piece to ipv4Piece × 10 + number.
							else {
								ipv4Piece = ipv4Piece * 10 + number;
							}
							// If ipv4Piece is greater than 255, IPv4-in-IPv6-out-of-range-part validation error, return failure.
							if (ipv4Piece > 255) {
								throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 part exceeds 255.");
							}
							// Increase pointer by 1.
							pointer++;
							c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
						}
						// Set address[pieceIndex] to address[pieceIndex] × 0x100 + ipv4Piece.
						address[pieceIndex] = address[pieceIndex] * 0x100 + (ipv4Piece != null ? ipv4Piece : 0);
						// Increase numbersSeen by 1.
						numbersSeen++;
						// If numbersSeen is 2 or 4, then increase pieceIndex by 1.
						if (numbersSeen == 2 || numbersSeen == 4) {
							pieceIndex++;
						}
						c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
					}
					// If numbersSeen is not 4, IPv4-in-IPv6-too-few-parts validation error, return failure.
					if (numbersSeen != 4) {
						throw new InvalidUrlException("IPv6 address with IPv4 address syntax: IPv4 address contains too few parts.");
					}
					// Break.
					break;
				}
				// Otherwise, if c is U+003A (:):
				else if (c == ':') {
					// Increase pointer by 1.
					pointer++;
					c = (pointer < inputLength) ? input.codePointAt(pointer) : EOF;
					// If c is the EOF code point, IPv6-invalid-code-point validation error, return failure.
					if (c == EOF) {
						throw new InvalidUrlException("IPv6 address unexpectedly ends.");
					}
				}
				// Otherwise, if c is not the EOF code point, IPv6-invalid-code-point validation error, return failure.
				else if (c != EOF) {
					throw new InvalidUrlException("IPv6 address contains \"" + Character.toString(c) + "\", which is neither an ASCII hex digit nor a ':'.");
				}
				// Set address[pieceIndex] to value.
				address[pieceIndex] = value;
				// Increase pieceIndex by 1.
				pieceIndex++;
			}
			// If compress is non-null, then:
			if (compress != null) {
				// Let swaps be pieceIndex − compress.
				int swaps = pieceIndex - compress;
				// Set pieceIndex to 7.
				pieceIndex = 7;
				// While pieceIndex is not 0 and swaps is greater than 0, swap address[pieceIndex] with address[compress + swaps − 1], and then decrease both pieceIndex and swaps by 1.
				while (pieceIndex != 0 && swaps > 0) {
					int tmp = address[pieceIndex];
					address[pieceIndex] = address[compress + swaps - 1];
					address[compress + swaps - 1] = tmp;
					pieceIndex--;
					swaps--;
				}
			}
			// Otherwise, if compress is null and pieceIndex is not 8, IPv6-too-few-pieces validation error, return failure.
			else if (compress == null && pieceIndex != 8) {
				throw new InvalidUrlException("An uncompressed IPv6 address contains fewer than 8 pieces.");
			}
			// Return address.
			return new Ipv6Address(address);
		}


		/**
		 * The IPv6 serializer takes an IPv6 address {@code address} and then runs these steps. They return an ASCII string.
		 */
		private static String serialize(int[] address) {
			// Let output be the empty string.
			StringBuilder output = new StringBuilder();
			// Let compress be an index to the first IPv6 piece in the first longest sequences of address’s IPv6 pieces that are 0.
			int compress = longestSequenceOf0Pieces(address);
			// Let ignore0 be false.
			boolean ignore0 = false;
			// For each pieceIndex in the range 0 to 7, inclusive:
			for (int pieceIndex = 0; pieceIndex <= 7; pieceIndex++) {
				// If ignore0 is true and address[pieceIndex] is 0, then continue.
				if (ignore0 && address[pieceIndex] == 0) {
					continue;
				}
				// Otherwise, if ignore0 is true, set ignore0 to false.
				else if (ignore0) {
					ignore0 = false;
				}
				// If compress is pieceIndex, then:
				if (compress == pieceIndex) {
					// Let separator be "::" if pieceIndex is 0, and U+003A (:) otherwise.
					String separator = (pieceIndex == 0) ? "::" : ":";
					// Append separator to output.
					output.append(separator);
					// Set ignore0 to true and continue.
					ignore0 = true;
					continue;
				}
				// Append address[pieceIndex], represented as the shortest possible lowercase hexadecimal number, to output.
				output.append(Integer.toHexString(address[pieceIndex]));
				// If pieceIndex is not 7, then append U+003A (:) to output.
				if (pieceIndex != 7) {
					output.append(':');
				}
			}
			// Return output.
			return output.toString();
		}

		private static int longestSequenceOf0Pieces(int[] pieces) {
			int longestStart = -1;
			int longestLength = -1;
			int start = -1;
			for (int i = 0; i < pieces.length + 1; i++) {
				if (i < pieces.length && pieces[i] == 0) {
					if (start < 0) {
						start = i;
					}
				}
				else if (start >= 0) {
					int length = i - start;
					if (length > longestLength) {
						longestStart = start;
						longestLength = length;
					}
					start = -1;
				}
			}
			// If there is no sequence of address’s IPv6 pieces that are 0 that is longer than 1, then set compress to null.
			if (longestLength > 1) {
				return longestStart;
			}
			else {
				return -1;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			else if (obj instanceof Ipv6Address other) {
				return Arrays.equals(this.pieces, other.pieces);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.pieces);
		}

		@Override
		public String toString() {
			return this.string;
		}
	}

	sealed interface Port permits StringPort, IntPort {

	}

	static final class StringPort implements Port {

		private final String port;

		public StringPort(String port) {
			this.port = port;
		}

		public String value() {
			return this.port;
		}

		@Override
		public String toString() {
			return this.port;
		}
	}

	static final class IntPort implements Port {

		private final int port;

		public IntPort(int port) {
			this.port = port;
		}

		public int value() {
			return this.port;
		}

		@Override
		public String toString() {
			return Integer.toString(this.port);
		}

	}

	sealed interface Path permits PathSegment, PathSegments {

		void append(int codePoint);

		void append(String s);

		boolean isEmpty();

		void shorten(String scheme);

		boolean isOpaque();

		Path clone();

		String name();
	}

	static final class PathSegment implements Path {

		@Nullable
		private StringBuilder builder = null;

		@Nullable
		String segment;

		PathSegment(String segment) {
			this.segment = segment;
		}

		PathSegment(int codePoint) {
			append(codePoint);
		}

		public String segment() {
			String result = this.segment;
			if (result == null) {
				Assert.state(this.builder != null, "String nor StringBuilder available");
				result = this.builder.toString();
				this.segment = result;
			}
			return result;
		}

		@Override
		public void append(int codePoint) {
			this.segment = null;
			if (this.builder == null) {
				this.builder = new StringBuilder(2);
			}
			this.builder.appendCodePoint(codePoint);
		}

		@Override
		public void append(String s) {
			this.segment = null;
			if (this.builder == null) {
				this.builder = new StringBuilder(s);
			}
			else {
				this.builder.append(s);
			}
		}

		@Override
		public String name() {
			String name = segment();
			if (name.startsWith("/")) {
				name = name.substring(1);
			}
			return name;
		}

		@Override
		public boolean isEmpty() {
			if (this.segment != null) {
				return this.segment.isEmpty();
			}
			else {
				Assert.state(this.builder != null, "String nor StringBuilder available");
				return this.builder.isEmpty();
			}
		}

		@Override
		public void shorten(String scheme) {
			throw new IllegalStateException("Opaque path not expected");
		}

		@Override
		public boolean isOpaque() {
			return true;
		}

		@Override
		public Path clone() {
			return new PathSegment(segment());
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o instanceof PathSegment other) {
				return segment().equals(other.segment());
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return segment().hashCode();
		}

		@Override
		public String toString() {
			return segment();
		}
	}

	static final class PathSegments implements Path {

		private final List<PathSegment> segments;

		public PathSegments() {
			this.segments = new ArrayList<>();
		}

		public PathSegments(List<PathSegment> segments) {
			this.segments = new ArrayList<>(segments);
		}


		@Override
		public void append(int codePoint) {
			this.segments.add(new PathSegment(codePoint));
		}

		@Override
		public void append(String segment) {
			this.segments.add(new PathSegment(segment));
		}

		public int size() {
			return this.segments.size();
		}

		public String get(int i) {
			return this.segments.get(i).segment();
		}

		@Override
		public boolean isEmpty() {
			return this.segments.isEmpty();
		}

		@Override
		public void shorten(String scheme) {
			int size = size();
			if ("file".equals(scheme) &&
					size == 1 &&
					isWindowsDriveLetter(get(0), true)) {
				return;
			}
			if (!isEmpty()) {
				this.segments.remove(size - 1);
			}
		}

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public Path clone() {
			return new PathSegments(this.segments);
		}

		@Override
		public String name() {
			StringBuilder output = new StringBuilder();
			for (PathSegment segment : this.segments) {
				output.append('/');
				output.append(segment.name());
			}
			return output.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o instanceof PathSegments other) {
				return this.segments.equals(other.segments);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return this.segments.hashCode();
		}

		@Override
		public String toString() {
			StringBuilder output = new StringBuilder();
			for (PathSegment segment : this.segments) {
				output.append(segment);
			}
			return output.toString();
		}

	}

	private sealed interface ParseIpv4NumberResult permits ParseIpv4NumberFailure, ParseIpv4NumberSuccess {
	}

	private record ParseIpv4NumberSuccess(int number, boolean validationError) implements ParseIpv4NumberResult {
	}

	private static final class ParseIpv4NumberFailure implements ParseIpv4NumberResult {

		public static final ParseIpv4NumberFailure INSTANCE = new ParseIpv4NumberFailure();

		private ParseIpv4NumberFailure() {
		}

	}



}
