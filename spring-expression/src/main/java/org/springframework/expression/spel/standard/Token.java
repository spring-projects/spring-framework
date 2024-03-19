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

package org.springframework.expression.spel.standard;

import org.springframework.lang.Nullable;

/**
 * Holder for a kind of token, the associated data, and its position in the input
 * data stream (start/end).
 *
 * @author Andy Clement
 * @since 3.0
 */
class Token {

	final TokenKind kind;

	@Nullable
	final String data;

	final int startPos;

	final int endPos;


	/**
	 * Constructor for use when there is no particular data for the token
	 * (e.g. TRUE or '+').
	 * @param tokenKind the kind of token
	 * @param startPos the exact start position
	 * @param endPos the index of the last character
	 */
	Token(TokenKind tokenKind, int startPos, int endPos) {
		this(tokenKind, null, startPos, endPos);
	}

	/**
	 * Constructor for use when there is data for the token.
	 * @param tokenKind the kind of token
	 * @param tokenData the data for the token
	 * @param startPos the exact start position
	 * @param endPos the index of the last character
	 */
	Token(TokenKind tokenKind, @Nullable char[] tokenData, int startPos, int endPos) {
		this.kind = tokenKind;
		this.data = (tokenData != null ? new String(tokenData) : null);
		this.startPos = startPos;
		this.endPos = endPos;
	}


	public TokenKind getKind() {
		return this.kind;
	}

	public boolean isIdentifier() {
		return (this.kind == TokenKind.IDENTIFIER);
	}

	public boolean isNumericRelationalOperator() {
		return (this.kind == TokenKind.GT || this.kind == TokenKind.GE || this.kind == TokenKind.LT ||
				this.kind == TokenKind.LE || this.kind == TokenKind.EQ || this.kind == TokenKind.NE);
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
		StringBuilder sb = new StringBuilder();
		sb.append('[').append(this.kind);
		if (this.kind.hasPayload()) {
			sb.append(':').append(this.data);
		}
		sb.append(']');
		sb.append('(').append(this.startPos).append(',').append(this.endPos).append(')');
		return sb.toString();
	}

}
