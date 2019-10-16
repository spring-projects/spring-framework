/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.expression.spel.standard;

import org.springframework.lang.Nullable;

/**
 * Holder for a kind of token, the associated data and its position in the input data
 * stream (start/end).
 *
 * @author Andy Clement
 * @since 3.0
 */
class Token {

	TokenKind kind;

	@Nullable
	String data;

	int startPos;  // index of first character

	int endPos;  // index of char after the last character


	/**
	 * Constructor for use when there is no particular data for the token
	 * (e.g. TRUE or '+')
	 * @param startPos the exact start
	 * @param endPos the index to the last character
	 */
	Token(TokenKind tokenKind, int startPos, int endPos) {
		this.kind = tokenKind;
		this.startPos = startPos;
		this.endPos = endPos;
	}

	Token(TokenKind tokenKind, char[] tokenData, int startPos, int endPos) {
		this(tokenKind, startPos, endPos);
		this.data = new String(tokenData);
	}


	public TokenKind getKind() {
		return this.kind;
	}

	public boolean isIdentifier() {
		return (this.kind == TokenKind.IDENTIFIER);
	}

	public boolean isNumericRelationalOperator() {
		return (this.kind == TokenKind.GT || this.kind == TokenKind.GE || this.kind == TokenKind.LT ||
				this.kind == TokenKind.LE || this.kind==TokenKind.EQ || this.kind==TokenKind.NE);
	}

	public String stringValue() {
		return (this.data != null ? this.data : "");
	}

	public Token asInstanceOfToken() {
		return new Token(TokenKind.INSTANCEOF, this.startPos, this.endPos);
	}

	public Token asMatchesToken() {
		return new Token(TokenKind.MATCHES, this.startPos, this.endPos);
	}

	public Token asBetweenToken() {
		return new Token(TokenKind.BETWEEN, this.startPos, this.endPos);
	}


	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[").append(this.kind.toString());
		if (this.kind.hasPayload()) {
			s.append(":").append(this.data);
		}
		s.append("]");
		s.append("(").append(this.startPos).append(",").append(this.endPos).append(")");
		return s.toString();
	}

}
