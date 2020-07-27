/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.InternalParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.lang.Nullable;

/**
 * Common superclass for nodes representing literals (boolean, string, number, etc).
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public abstract class Literal extends SpelNodeImpl {

	@Nullable
	private final String originalValue;


	public Literal(@Nullable String originalValue, int startPos, int endPos) {
		super(startPos, endPos);
		this.originalValue = originalValue;
	}


	@Nullable
	public final String getOriginalValue() {
		return this.originalValue;
	}

	@Override
	public final TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
		return getLiteralValue();
	}

	@Override
	public String toString() {
		return String.valueOf(getLiteralValue().getValue());
	}

	@Override
	public String toStringAST() {
		return toString();
	}


	public abstract TypedValue getLiteralValue();


	/**
	 * Process the string form of a number, using the specified base if supplied
	 * and return an appropriate literal to hold it. Any suffix to indicate a
	 * long will be taken into account (either 'l' or 'L' is supported).
	 * @param numberToken the token holding the number as its payload (eg. 1234 or 0xCAFE)
	 * @param radix the base of number
	 * @return a subtype of Literal that can represent it
	 */
	public static Literal getIntLiteral(String numberToken, int startPos, int endPos, int radix) {
		try {
			int value = Integer.parseInt(numberToken, radix);
			return new IntLiteral(numberToken, startPos, endPos, value);
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_AN_INTEGER, numberToken));
		}
	}

	public static Literal getLongLiteral(String numberToken, int startPos, int endPos, int radix) {
		try {
			long value = Long.parseLong(numberToken, radix);
			return new LongLiteral(numberToken, startPos, endPos, value);
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_LONG, numberToken));
		}
	}

	public static Literal getRealLiteral(String numberToken, int startPos, int endPos, boolean isFloat) {
		try {
			if (isFloat) {
				float value = Float.parseFloat(numberToken);
				return new FloatLiteral(numberToken, startPos, endPos, value);
			}
			else {
				double value = Double.parseDouble(numberToken);
				return new RealLiteral(numberToken, startPos, endPos, value);
			}
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_REAL, numberToken));
		}
	}

}
