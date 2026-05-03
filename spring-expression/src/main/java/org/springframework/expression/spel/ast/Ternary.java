/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Objects;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;

/**
 * Represents a ternary expression, for example: "someCheck()?true:false".
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class Ternary extends SpelNodeImpl {

	public Ternary(int startPos, int endPos, SpelNodeImpl... args) {
		super(startPos, endPos, args);
	}


	/**
	 * Evaluate the condition and if true evaluate the first alternative, otherwise
	 * evaluate the second alternative.
	 * @param state the expression state
	 * @throws EvaluationException if the condition does not evaluate correctly to
	 * a boolean or there is a problem executing the chosen alternative
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Boolean condition = this.children[0].getValue(state, Boolean.class);
		if (condition == null) {
			throw new SpelEvaluationException(getChild(0).getStartPosition(),
					SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
		TypedValue result = this.children[condition ? 1 : 2].getValueInternal(state);
		computeExitTypeDescriptor();
		return result;
	}

	@Override
	public String toStringAST() {
		return "(" + getChild(0).toStringAST() + " ? " + getChild(1).toStringAST() + " : " + getChild(2).toStringAST() + ")";
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl condition = this.children[0];
		SpelNodeImpl left = this.children[1];
		SpelNodeImpl right = this.children[2];
		String conditionDescriptor = condition.exitTypeDescriptor;
		String leftDescriptor = left.exitTypeDescriptor;
		String rightDescriptor = right.exitTypeDescriptor;

		return (condition.isCompilable() && left.isCompilable() && right.isCompilable() &&
				CodeFlow.isBooleanCompatible(conditionDescriptor) &&
				leftDescriptor != null && rightDescriptor != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		// If all elements are literals and the expression was not previously
		// evaluated in interpreted mode, we may get here without the exit descriptor
		// having been computed, so we must ensure the exit descriptor has been
		// computed before proceeding.
		computeExitTypeDescriptor();

		cf.enterCompilationScope();
		this.children[0].generateCode(mv, cf);
		String lastDesc = cf.lastDescriptor();
		Assert.state(lastDesc != null, "No last descriptor");
		if (!CodeFlow.isPrimitive(lastDesc)) {
			CodeFlow.insertUnboxInsns(mv, 'Z', lastDesc);
		}
		cf.exitCompilationScope();
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		mv.visitJumpInsn(IFEQ, elseTarget);
		cf.enterCompilationScope();
		this.children[1].generateCode(mv, cf);
		if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
			lastDesc = cf.lastDescriptor();
			Assert.state(lastDesc != null, "No last descriptor");
			CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
		}
		cf.exitCompilationScope();
		mv.visitJumpInsn(GOTO, endOfIf);
		mv.visitLabel(elseTarget);
		cf.enterCompilationScope();
		this.children[2].generateCode(mv, cf);
		if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
			lastDesc = cf.lastDescriptor();
			Assert.state(lastDesc != null, "No last descriptor");
			CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
		}
		cf.exitCompilationScope();
		mv.visitLabel(endOfIf);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

	private void computeExitTypeDescriptor() {
		String leftDescriptor = this.children[1].exitTypeDescriptor;
		String rightDescriptor = this.children[2].exitTypeDescriptor;
		if (this.exitTypeDescriptor == null && leftDescriptor != null && rightDescriptor != null) {
			if (Objects.equals(leftDescriptor, rightDescriptor)) {
				this.exitTypeDescriptor = leftDescriptor;
			}
			else {
				// Use the easiest to compute common supertype
				this.exitTypeDescriptor = "Ljava/lang/Object";
			}
		}
	}

}
