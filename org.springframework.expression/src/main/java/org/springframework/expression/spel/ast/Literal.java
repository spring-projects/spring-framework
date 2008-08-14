/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.ast;

import org.antlr.runtime.Token;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.internal.InternalELException;

/**
 * Common superclass for nodes representing literals (boolean, string, number, etc).
 * 
 * @author Andy Clement
 * 
 */
public abstract class Literal extends SpelNode {

	public Literal(Token payload) {
		super(payload);
	}

	public abstract Object getLiteralValue();

	@Override
	public final Object getValue(ExpressionState state) throws SpelException {
		return getLiteralValue();
	}

	@Override
	public String toString() {
		return getLiteralValue().toString();
	}

	@Override
	public String toStringAST() {
		return toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

	/**
	 * Process the string form of a number, using the specified base if supplied and return an appropriate literal to
	 * hold it. Any suffix to indicate a long will be taken into account (either 'l' or 'L' is supported).
	 * 
	 * @param numberToken the token holding the number as its payload (eg. 1234 or 0xCAFE)
	 * @param radix the base of number
	 * @return a subtype of Literal that can represent it
	 */
	public static Literal getIntLiteral(Token numberToken, int radix) {
		String numberString = numberToken.getText();

		boolean isLong = false;
		boolean isHex = (radix == 16);

		if (numberString.length() > 0) {
			isLong = numberString.endsWith("L") || numberString.endsWith("l");
		}

		if (isLong || isHex) { // needs to be chopped up a little
			int len = numberString.length();
			// assert: if hex then startsWith 0x or 0X
			numberString = numberString.substring((isHex ? 2 : 0), isLong ? len - 1 : len);
		}

		if (isLong) {
			try {
				long value = Long.parseLong(numberString, radix);
				return new LongLiteral(numberToken, value);
			} catch (NumberFormatException nfe) {
				throw new InternalELException(new SpelException(numberToken.getCharPositionInLine(), nfe,
						SpelMessages.NOT_A_LONG, numberToken.getText()));
			}
		} else {
			try {
				int value = Integer.parseInt(numberString, radix);
				return new IntLiteral(numberToken, value);
			} catch (NumberFormatException nfe) {
				throw new InternalELException(new SpelException(numberToken.getCharPositionInLine(), nfe,
						SpelMessages.NOT_AN_INTEGER, numberToken.getText()));
			}
		}
	}

}
