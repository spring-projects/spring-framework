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
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

// TODO is the operator 'distanceto' any use...?
/**
 * The distanceto operator uses an implementation of the levenshtein distance measurement for determining the 'edit
 * distance' between two strings (the two operands to distanceto). http://en.wikipedia.org/wiki/Levenshtein_distance
 * @author Andy Clement
 * 
 */
public class OperatorDistanceTo extends Operator {

	private final static boolean DEBUG = false;

	public OperatorDistanceTo(Token payload) {
		super(payload);
	}

	@Override
	public String getOperatorName() {
		return "distanceto";
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		try {
			Object left = getLeftOperand().getValue(state);
			Object right = getRightOperand().getValue(state);
			return computeDistanceTo((String) left, (String) right);
		} catch (SpelException ee) {
			throw ee;
		}
	}

	private int computeDistanceTo(String from, String to) {
		if (from.equals(to))
			return 0;
		int[][] d = new int[from.length() + 1][to.length() + 1];

		for (int i = 0; i <= from.length(); i++)
			d[i][0] = i;

		for (int j = 0; j <= to.length(); j++)
			d[0][j] = j;

		for (int i = 1; i <= from.length(); i++) {
			for (int j = 1; j <= to.length(); j++) {
				int cost;
				if (from.charAt(i - 1) == to.charAt(j - 1))
					cost = 0;
				else
					cost = 1;
				d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);// del,ins,subst

			}
		}
		if (DEBUG) {
			// Display the table of values
			System.out.print("    ");
			for (int j = 0; j < from.length(); j++) {
				System.out.print(from.charAt(j) + " ");
			}
			System.out.println();
			for (int j = 0; j < to.length() + 1; j++) {
				System.out.print((j > 0 ? to.charAt(j - 1) : " ") + " ");
				for (int i = 0; i < from.length() + 1; i++) {
					System.out.print(d[i][j]);
					if (i == from.length() && j == to.length())
						System.out.print("<");
					else if (i == from.length() - 1 && j == to.length())
						System.out.print(">");
					else
						System.out.print(" ");
				}
				System.out.println();
			}
		}
		return d[from.length()][to.length()];
	}

	private int min(int i, int j, int k) {
		int min = i;
		if (j < min)
			min = j;
		if (k < min)
			min = k;
		return min;
	}

}
