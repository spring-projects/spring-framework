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

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.CodeFlow;

/**
 * Represents a ternary expression, for example: "someCheck()?true:false".
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class Ternary extends SpelNodeImpl {


	public Ternary(int pos, SpelNodeImpl... args) {
		super(pos,args);
	}

	/**
	 * Evaluate the condition and if true evaluate the first alternative, otherwise
	 * evaluate the second alternative.
	 * @param state the expression state
	 * @throws EvaluationException if the condition does not evaluate correctly to a
	 *         boolean or there is a problem executing the chosen alternative
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Boolean value = this.children[0].getValue(state, Boolean.class);
		if (value == null) {
			throw new SpelEvaluationException(getChild(0).getStartPosition(),
					SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
		TypedValue result = null;
		if (value.booleanValue()) {
			result = this.children[1].getValueInternal(state);
		}
		else {
			result = this.children[2].getValueInternal(state);
		}
		computeExitTypeDescriptor();
		return result;
	}
	
	@Override
	public String toStringAST() {
		return new StringBuilder().append(getChild(0).toStringAST()).append(" ? ").append(getChild(1).toStringAST())
				.append(" : ").append(getChild(2).toStringAST()).toString();
	}

	private void computeExitTypeDescriptor() {
		if (exitTypeDescriptor == null && this.children[1].getExitDescriptor()!=null && this.children[2].getExitDescriptor()!=null) {
			String leftDescriptor = this.children[1].exitTypeDescriptor;
			String rightDescriptor = this.children[2].exitTypeDescriptor;
			if (leftDescriptor.equals(rightDescriptor)) {
				this.exitTypeDescriptor = leftDescriptor;
			}
			else if (leftDescriptor.equals("Ljava/lang/Object") && !CodeFlow.isPrimitive(rightDescriptor)) {
				this.exitTypeDescriptor = rightDescriptor;
			}
			else if (rightDescriptor.equals("Ljava/lang/Object") && !CodeFlow.isPrimitive(leftDescriptor)) {
				this.exitTypeDescriptor = leftDescriptor;
			}
			else {
				// Use the easiest to compute common super type
				this.exitTypeDescriptor = "Ljava/lang/Object";
			}
		}
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl condition = this.children[0];
		SpelNodeImpl left = this.children[1];
		SpelNodeImpl right = this.children[2];
		if (!(condition.isCompilable() && left.isCompilable() && right.isCompilable())) {
			return false;
		}
		return CodeFlow.isBooleanCompatible(condition.exitTypeDescriptor) &&
				left.getExitDescriptor()!=null && 
				right.getExitDescriptor()!=null;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		// May reach here without it computed if all elements are literals
		computeExitTypeDescriptor();
		codeflow.enterCompilationScope();
		this.children[0].generateCode(mv, codeflow);
		codeflow.exitCompilationScope();
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		mv.visitJumpInsn(IFEQ, elseTarget);
		codeflow.enterCompilationScope();
		this.children[1].generateCode(mv, codeflow);
		if (!CodeFlow.isPrimitive(getExitDescriptor())) {
			CodeFlow.insertBoxIfNecessary(mv, codeflow.lastDescriptor().charAt(0));
		}
		codeflow.exitCompilationScope();
		mv.visitJumpInsn(GOTO, endOfIf);
		mv.visitLabel(elseTarget);
		codeflow.enterCompilationScope();
		this.children[2].generateCode(mv, codeflow);
		if (!CodeFlow.isPrimitive(getExitDescriptor())) {
			CodeFlow.insertBoxIfNecessary(mv, codeflow.lastDescriptor().charAt(0));
		}
		codeflow.exitCompilationScope();
		mv.visitLabel(endOfIf);
		codeflow.pushDescriptor(getExitDescriptor());
	}

}
