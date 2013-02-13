/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

/**
 * Implements the {@code multiply} operator.
 *
 * <p>Conversions and promotions are handled as defined in
 * <a href="http://java.sun.com/docs/books/jls/third_edition/html/conversions.html">Section 5.6.2
 * of the Java Language Specification</a>:
 *
 * <p>If any of the operands is of a reference type, unboxing conversion (Section 5.1.8) is performed. Then:<br>
 * If either operand is of type double, the other is converted to double.<br>
 * Otherwise, if either operand is of type float, the other is converted to float.<br>
 * Otherwise, if either operand is of type long, the other is converted to long.<br>
 * Otherwise, both operands are converted to type int.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 */
public class OpMultiply extends Operator {

	public OpMultiply(int pos, SpelNodeImpl... operands) {
		super("*", pos, operands);
	}

	/**
	 * Implements the {@code multiply} operator directly here for certain types
	 * of supported operands and otherwise delegates to any registered overloader
	 * for types not supported here.
	 * <p>Supported operand types:
	 * <ul>
	 * <li>doubles
	 * <li>longs
	 * <li>integers
	 * <li>String and int ('abc' * 2 == 'abcabc')
	 * </ul>
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object operandOne = getLeftOperand().getValueInternal(state).getValue();
		Object operandTwo = getRightOperand().getValueInternal(state).getValue();
		if (operandOne instanceof Number && operandTwo instanceof Number) {
			Number leftNumber = (Number) operandOne;
			Number rightNumber = (Number) operandTwo;
			if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return new TypedValue(leftNumber.floatValue() * rightNumber.floatValue());
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return new TypedValue(leftNumber.longValue() * rightNumber.longValue());
			}
			else {
				return new TypedValue(leftNumber.intValue() * rightNumber.intValue());
			}
		}
		else if (operandOne instanceof String && operandTwo instanceof Integer) {
			int repeats = (Integer) operandTwo;
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < repeats; i++) {
				result.append(operandOne);
			}
			return new TypedValue(result.toString());
		}
		return state.operate(Operation.MULTIPLY, operandOne, operandTwo);
	}

}
