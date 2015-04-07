/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;


/**
 * The operator 'instanceof' checks if an object is of the class specified in the right
 * hand operand, in the same way that {@code instanceof} does in Java.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorInstanceof extends Operator {

	private Class<?> type;
	
	public OperatorInstanceof(int pos, SpelNodeImpl... operands) {
		super("instanceof", pos, operands);
	}


	/**
	 * Compare the left operand to see it is an instance of the type specified as the
	 * right operand. The right operand must be a class.
	 * @param state the expression state
	 * @return true if the left operand is an instanceof of the right operand, otherwise
	 *         false
	 * @throws EvaluationException if there is a problem evaluating the expression
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue left = getLeftOperand().getValueInternal(state);
		TypedValue right = getRightOperand().getValueInternal(state);
		Object leftValue = left.getValue();
		Object rightValue = right.getValue();
		BooleanTypedValue result = null;
		if (rightValue == null || !(rightValue instanceof Class<?>)) {
			throw new SpelEvaluationException(getRightOperand().getStartPosition(),
					SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND,
					(rightValue == null ? "null" : rightValue.getClass().getName()));
		}
		Class<?> rightClass = (Class<?>) rightValue;
		if (leftValue == null) {
			result = BooleanTypedValue.FALSE;  // null is not an instanceof anything
		}
		else {
			result = BooleanTypedValue.forValue(rightClass.isAssignableFrom(leftValue.getClass()));
		}
		this.type = rightClass;
		this.exitTypeDescriptor = "Z";
		return result;
	}

	@Override
	public boolean isCompilable() {
		return (this.exitTypeDescriptor != null && getLeftOperand().isCompilable());
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		getLeftOperand().generateCode(mv, cf);
		mv.visitTypeInsn(INSTANCEOF,Type.getInternalName(this.type));
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
