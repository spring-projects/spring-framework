/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.web.util.patterns;

import java.text.MessageFormat;

/**
 * The messages that can be included in a {@link PatternParseException} when there is a parse failure.
 * 
 * @author Andy Clement
 */
public enum PatternMessage {
	
	// @formatter:off
	MISSING_CLOSE_CAPTURE("Expected close capture character after variable name '}'"),
	MISSING_OPEN_CAPTURE("Missing preceeding open capture character before variable name'{'"), 
	ILLEGAL_NESTED_CAPTURE("Not allowed to nest variable captures"),
	CANNOT_HAVE_ADJACENT_CAPTURES("Adjacent captures are not allowed"),
	ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR("Character ''{0}'' is not allowed at start of captured variable name"),
	ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR("Character ''{0}'' is not allowed in a captured variable name"),
	NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST("No more pattern data allowed after '{*...}' pattern element"),
	BADLY_FORMED_CAPTURE_THE_REST("Expected form when capturing the rest of the path is simply '{*...}'"),
	MISSING_REGEX_CONSTRAINT("Missing regex constraint on capture"),
	ILLEGAL_DOUBLE_CAPTURE("Not allowed to capture ''{0}'' twice in the same pattern"),
	JDK_PATTERN_SYNTAX_EXCEPTION("Exception occurred in pattern compilation"),
	CAPTURE_ALL_IS_STANDALONE_CONSTRUCT("'{*...}' can only be preceeded by a path separator");
	// @formatter:on

	private final String message;

	private PatternMessage(String message) {
		this.message = message;
	}

	public String formatMessage(Object... inserts) {
		return MessageFormat.format(this.message, inserts);
	}

}
