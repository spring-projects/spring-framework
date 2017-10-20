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

import java.text.MessageFormat;

/**
 * Exception that is thrown when there is a problem with the pattern being parsed.
 *
 * @author Andy Clement
 * @since 5.0
 */
@SuppressWarnings("serial")
public class PatternParseException extends IllegalArgumentException {

	private final int position;

	private final char[] pattern;

	private final PatternMessage messageType;

	private final Object[] inserts;


	PatternParseException(int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
		super(messageType.formatMessage(inserts));
		this.position = pos;
		this.pattern = pattern;
		this.messageType = messageType;
		this.inserts = inserts;
	}

	PatternParseException(Throwable cause, int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
		super(messageType.formatMessage(inserts), cause);
		this.position = pos;
		this.pattern = pattern;
		this.messageType = messageType;
		this.inserts = inserts;
	}


	/**
	 * Return a formatted message with inserts applied.
	 */
	@Override
	public String getMessage() {
		return this.messageType.formatMessage(this.inserts);
	}

	/**
	 * Return a detailed message that includes the original pattern text
	 * with a pointer to the error position, as well as the error message.
	 */
	public String toDetailedString() {
		StringBuilder buf = new StringBuilder();
		buf.append(this.pattern).append('\n');
		for (int i = 0; i < this.position; i++) {
			buf.append(' ');
		}
		buf.append("^\n");
		buf.append(getMessage());
		return buf.toString();
	}

	public int getPosition() {
		return this.position;
	}

	public PatternMessage getMessageType() {
		return this.messageType;
	}

	public Object[] getInserts() {
		return this.inserts;
	}


	/**
	 * The messages that can be included in a {@link PatternParseException} when there is a parse failure.
	 */
	public enum PatternMessage {

		MISSING_CLOSE_CAPTURE("Expected close capture character after variable name '}'"),
		MISSING_OPEN_CAPTURE("Missing preceeding open capture character before variable name'{'"),
		ILLEGAL_NESTED_CAPTURE("Not allowed to nest variable captures"),
		CANNOT_HAVE_ADJACENT_CAPTURES("Adjacent captures are not allowed"),
		ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR("Char ''{0}'' not allowed at start of captured variable name"),
		ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR("Char ''{0}'' is not allowed in a captured variable name"),
		NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST("No more pattern data allowed after '{*...}' pattern element"),
		BADLY_FORMED_CAPTURE_THE_REST("Expected form when capturing the rest of the path is simply '{*...}'"),
		MISSING_REGEX_CONSTRAINT("Missing regex constraint on capture"),
		ILLEGAL_DOUBLE_CAPTURE("Not allowed to capture ''{0}'' twice in the same pattern"),
		REGEX_PATTERN_SYNTAX_EXCEPTION("Exception occurred in regex pattern compilation"),
		CAPTURE_ALL_IS_STANDALONE_CONSTRUCT("'{*...}' can only be preceeded by a path separator");

		private final String message;

		PatternMessage(String message) {
			this.message = message;
		}

		public String formatMessage(Object... inserts) {
			return MessageFormat.format(this.message, inserts);
		}
	}

}
