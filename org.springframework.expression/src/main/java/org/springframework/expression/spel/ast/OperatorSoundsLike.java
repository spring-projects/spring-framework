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
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

public class OperatorSoundsLike extends Operator {

	public OperatorSoundsLike(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValue(state);
		Object right = getRightOperand().getValue(state);
		if (!(left instanceof String)) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.SOUNDSLIKE_NEEDS_STRING_OPERAND, left
					.getClass().getName());
		}
		if (!(right instanceof String)) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.SOUNDSLIKE_NEEDS_STRING_OPERAND, right
					.getClass().getName());
		}
		String leftSoundex = computeSoundex((String) left);
		String rightSoundex = computeSoundex((String) right);
		return state.getTypeComparator().compare(leftSoundex, rightSoundex) == 0;
	}

	// TODO if we keep soundslike, improve upon this basic implementation
	private String computeSoundex(String input) {
		if (input == null || input.length() == 0)
			return "0000";
		input = input.toUpperCase();
		StringBuilder soundex = new StringBuilder();
		soundex.append(input.charAt(0));
		for (int i = 1; i < input.length(); i++) {
			char ch = input.charAt(i);
			if ("HW".indexOf(ch) != -1)
				continue; // remove HWs now
			if ("BFPV".indexOf(ch) != -1) {
				soundex.append("1");
			} else if ("CGJKQSXZ".indexOf(ch) != -1) {
				soundex.append("2");
			} else if ("DT".indexOf(ch) != -1) {
				soundex.append("3");
			} else if ("L".indexOf(ch) != -1) {
				soundex.append("4");
			} else if ("MN".indexOf(ch) != -1) {
				soundex.append("5");
			} else if ("R".indexOf(ch) != -1) {
				soundex.append("6");

			} else {
				soundex.append(ch);
			}
		}
		StringBuilder shorterSoundex = new StringBuilder();
		shorterSoundex.append(soundex.charAt(0));
		for (int i = 1; i < soundex.length(); i++) {
			if ((i + 1) < soundex.length() && soundex.charAt(i) == soundex.charAt(i + 1))
				continue;
			if ("AEIOUY".indexOf(soundex.charAt(i)) != -1)
				continue;
			shorterSoundex.append(soundex.charAt(i));
		}
		shorterSoundex.append("0000");
		return shorterSoundex.substring(0, 4);
	}

	// wikipedia:
	// The Soundex code for a name consists of a letter followed by three numbers: the letter is the first letter of the
	// name, and the numbers encode the remaining consonants. Similar sounding consonants share the same number so, for
	// example, the labial B, F, P and V are all encoded as 1. Vowels can affect the coding, but are never coded
	// directly unless they appear at the start of the name.
	// The exact algorithm is as follows:
	// Retain the first letter of the string
	// Remove all occurrences of the following letters, unless it is the first letter: a, e, h, i, o, u, w, y
	// Assign numbers to the remaining letters (after the first) as follows:
	// b, f, p, v = 1
	// c, g, j, k, q, s, x, z = 2
	// d, t = 3
	// l = 4
	// m, n = 5
	// r = 6
	// If two or more letters with the same number were adjacent in the original name (before step 1), or adjacent
	// except for any intervening h and w (American census only), then omit all but the first.
	// Return the first four characters, right-padding with zeroes if there are fewer than four.
	// Using this algorithm, both "Robert" and "Rupert" return the same string "R163" while "Rubin" yields "R150".

	@Override
	public String getOperatorName() {
		return "soundslike";
	}

}
