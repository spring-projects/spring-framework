/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.standard;

import org.jspecify.annotations.Nullable;

/**
 * Holder for a kind of token, the associated data, and its position in the input
 * data stream (start/end).
 *
 * @author Andy Clement
 * @since 3.0
 */
class Token {

	final TokenKind kind;

	final @Nullable String data;

	final int startPos;

	final int endPos;


	/**
	 * Constructor for use when there is no particular data for the token
	 * (for example, TRUE or '+').
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
	Token(TokenKind tokenKind, char @Nullable [] tokenData, int startPos, int endPos) {
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
