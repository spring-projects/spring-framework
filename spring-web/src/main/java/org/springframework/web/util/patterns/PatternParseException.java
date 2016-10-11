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

/**
 * Exception that is thrown when there is a problem with the pattern being parsed.
 * 
 * @author Andy Clement
 */
public class PatternParseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int pos;

	private char[] patternText;

	private final PatternMessage message;

	private final Object[] inserts;

	public PatternParseException(int pos, char[] patternText, PatternMessage message, Object... inserts) {
		super(message.formatMessage(inserts));
		this.pos = pos;
		this.patternText = patternText;
		this.message = message;
		this.inserts = inserts;
	}

	public PatternParseException(Throwable cause, int pos, char[] patternText, PatternMessage message, Object... inserts) {
		super(message.formatMessage(inserts),cause);
		this.pos = pos;
		this.patternText = patternText;
		this.message = message;
		this.inserts = inserts;
	}

	/**
	 * @return a formatted message with inserts applied
	 */
	@Override
	public String getMessage() {
		return this.message.formatMessage(this.inserts);
	}

	/**
	 * @return a detailed message that includes the original pattern text with a pointer to the error position,
	 * as well as the error message.
	 */
	public String toDetailedString() {
		StringBuilder buf = new StringBuilder();
		buf.append(patternText).append('\n');
		for (int i = 0; i < pos; i++) {
			buf.append(' ');
		}
		buf.append("^\n");
		buf.append(getMessage());
		return buf.toString();
	}

	public Object[] getInserts() {
		return this.inserts;
	}

	public int getPosition() {
		return pos;
	}

	public PatternMessage getMessageType() {
		return message;
	}

}
