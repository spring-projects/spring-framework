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
import org.springframework.expression.Operation;
import org.springframework.expression.spel.ExpressionState;

/**
 * Implements the multiply operator. Conversions and promotions:
 * http://java.sun.com/docs/books/jls/third_edition/html/conversions.html Section 5.6.2:
 * 
 * If any of the operands is of a reference type, unboxing conversion (¤5.1.8) is performed. Then:<br>
 * If either operand is of type double, the other is converted to double.<br>
 * Otherwise, if either operand is of type float, the other is converted to float.<br>
 * Otherwise, if either operand is of type long, the other is converted to long.<br>
 * Otherwise, both operands are converted to type int.
 * 
 * @author Andy Clement
 */
public class OperatorMultiply extends Operator {

	public OperatorMultiply(Token payload) {
		super(payload);
	}

	/**
	 * Implements multiply directly here for some types of operand, otherwise delegates to any registered overloader for
	 * types it does not recognize. Supported types here are:
	 * <ul>
	 * <li>integers
	 * <li>doubles
	 * <li>string and int ('abc' * 2 == 'abcabc')
	 * </ul>
	 */
	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object operandOne = getLeftOperand().getValue(state);
		Object operandTwo = getRightOperand().getValue(state);
		if (operandOne instanceof Number && operandTwo instanceof Number) {
			Number op1 = (Number) operandOne;
			Number op2 = (Number) operandTwo;
			if (op1 instanceof Double || op2 instanceof Double) {
				Double result = op1.doubleValue() * op2.doubleValue();
				return result;
			} else if (op1 instanceof Float || op2 instanceof Float) {
				Float result = op1.floatValue() * op2.floatValue();
				return result;
			} else if (op1 instanceof Long || op2 instanceof Long) {
				Long result = op1.longValue() * op2.longValue();
				return result;
			} else { // promote to int
				Integer result = op1.intValue() * op2.intValue();
				return result;
			}
		} else if (operandOne instanceof String && operandTwo instanceof Integer) {
			int repeats = ((Integer) operandTwo).intValue();
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < repeats; i++) {
				result.append(operandOne);
			}
			return result.toString();
		}
		return state.operate(Operation.MULTIPLY, operandOne, operandTwo);
	}

	@Override
	public String getOperatorName() {
		return "*";
	}

}
