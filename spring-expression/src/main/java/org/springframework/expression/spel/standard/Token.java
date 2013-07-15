/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.standard;

/**
 * Holder for a kind of token, the associated data and its position in the input data
 * stream (start/end).
 *
 * @author Andy Clement
 * @since 3.0
 */
class Token {

	TokenKind kind;

	String data;

	int startpos; // index of first character

	int endpos;   // index of char after the last character


	/**
	 * Constructor for use when there is no particular data for the token (eg. TRUE or
	 * '+')
	 * @param startpos the exact start
	 * @param endpos the index to the last character
	 */
	Token(TokenKind tokenKind, int startpos, int endpos) {
		this.kind = tokenKind;
		this.startpos = startpos;
		this.endpos = endpos;
	}

	Token(TokenKind tokenKind, char[] tokenData, int pos, int endpos) {
		this(tokenKind,pos,endpos);
		this.data = new String(tokenData);
	}


	public TokenKind getKind() {
		return this.kind;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[").append(this.kind.toString());
		if (this.kind.hasPayload()) {
			s.append(":").append(this.data);
		}
		s.append("]");
		s.append("(").append(this.startpos).append(",").append(this.endpos).append(")");
		return s.toString();
	}

	public boolean isIdentifier() {
		return this.kind==TokenKind.IDENTIFIER;
	}

	public boolean isNumericRelationalOperator() {
		return this.kind==TokenKind.GT || this.kind==TokenKind.GE || this.kind==TokenKind.LT || this.kind==TokenKind.LE || this.kind==TokenKind.EQ || this.kind==TokenKind.NE;
	}

	public String stringValue() {
		return this.data;
	}

	public Token asInstanceOfToken() {
		return new Token(TokenKind.INSTANCEOF,this.startpos,this.endpos);
	}

	public Token asMatchesToken() {
		return new Token(TokenKind.MATCHES,this.startpos,this.endpos);
	}

	public Token asBetweenToken() {
		return new Token(TokenKind.BETWEEN,this.startpos,this.endpos);
	}
}
