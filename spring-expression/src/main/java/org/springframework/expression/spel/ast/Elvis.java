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
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.Assert;

/**
 * Represents the Elvis operator {@code ?:}.
 *
 * <p>For the expression "{@code A ?: B}", if {@code A} is neither {@code null},
 * an empty {@link Optional}, nor an empty {@link String}, the value of the
 * expression is {@code A}, or {@code A.get()} for an {@code Optional}. If
 * {@code A} is {@code null}, an empty {@code Optional}, or an
 * empty {@code String}, the value of the expression is {@code B}.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class Elvis extends SpelNodeImpl {

	/**
	 * Tracks the descriptor for the value contained in an {@link Optional} that
	 * was unwrapped in {@link #getValueInternal(ExpressionState)} using the
	 * null-safe operator.
	 * @since 7.1
	 */
	private volatile @Nullable String unwrappedOptionalDescriptor;


	public Elvis(int startPos, int endPos, SpelNodeImpl... args) {
		super(startPos, endPos, args);
	}


	/**
	 * If the left-hand operand is neither neither {@code null}, an empty
	 * {@link Optional}, nor an empty {@link String}, return its value, or the
	 * value contained in the {@code Optional}. If the left-hand operand is
	 * {@code null}, an empty {@code Optional}, or an empty {@code String},
	 * return the other value.
	 * @param state the expression state
	 * @throws EvaluationException if the null/empty check does not evaluate correctly
	 * or there is a problem evaluating the alternative
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue result;
		TypedValue leftHandTypedValue = this.children[0].getValueInternal(state);
		Object leftHandValue = leftHandTypedValue.getValue();

		if (leftHandValue instanceof Optional<?> optional) {
			if (optional.isPresent()) {
				Object value = optional.get();
				this.unwrappedOptionalDescriptor = CodeFlow.toDescriptor(value.getClass());
				result = new TypedValue(value);
			}
			else {
				result = this.children[1].getValueInternal(state);
			}
		}
		// If this check is changed, the generateCode() method will need changing too
		else if (leftHandValue != null && !"".equals(leftHandValue)) {
			result = leftHandTypedValue;
		}
		else {
			result = this.children[1].getValueInternal(state);
		}
		computeExitTypeDescriptor();
		return result;
	}

	@Override
	public String toStringAST() {
		return "(" + getChild(0).toStringAST() + " ?: " + getChild(1).toStringAST() + ")";
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl condition = this.children[0];
		SpelNodeImpl ifNullValue = this.children[1];
		String conditionDescriptor = (this.unwrappedOptionalDescriptor != null ?
				this.unwrappedOptionalDescriptor : condition.exitTypeDescriptor);
		String ifNullValueDescriptor = ifNullValue.exitTypeDescriptor;

		return (condition.isCompilable() && ifNullValue.isCompilable() &&
				conditionDescriptor != null && ifNullValueDescriptor != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		// If both elements are literals and the expression was not previously
		// evaluated in interpreted mode, we may get here without the exit descriptor
		// having been computed, so we must ensure the exit descriptor has been
		// computed before proceeding.
		computeExitTypeDescriptor();

		Label elseTarget = new Label();
		Label endOfIf = new Label();

		cf.enterCompilationScope();
		this.children[0].generateCode(mv, cf);
		String lastDesc = cf.lastDescriptor();
		Assert.state(lastDesc != null, "No last descriptor");
		if ("Ljava/util/Optional".equals(lastDesc)) {
			CodeFlow.insertOptionalUnwrapIfNecessary(mv, lastDesc);
		}
		else {
			CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
		}
		cf.exitCompilationScope();

		mv.visitInsn(DUP);
		mv.visitJumpInsn(IFNULL, elseTarget);
		// Also check if empty string, as per the code in the interpreted version
		mv.visitInsn(DUP);
		mv.visitLdcInsn("");
		mv.visitInsn(SWAP);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",false);
		mv.visitJumpInsn(IFEQ, endOfIf);  // if not empty, drop through to elseTarget

		mv.visitLabel(elseTarget);
		mv.visitInsn(POP);
		cf.enterCompilationScope();
		this.children[1].generateCode(mv, cf);
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
		SpelNodeImpl condition = this.children[0];
		SpelNodeImpl ifNullValue = this.children[1];
		String conditionDescriptor = (this.unwrappedOptionalDescriptor != null ?
				this.unwrappedOptionalDescriptor : condition.exitTypeDescriptor);
		String ifNullValueDescriptor = ifNullValue.exitTypeDescriptor;

		if (this.exitTypeDescriptor == null && conditionDescriptor != null && ifNullValueDescriptor != null) {
			if (Objects.equals(conditionDescriptor, ifNullValueDescriptor)) {
				this.exitTypeDescriptor = conditionDescriptor;
			}
			else {
				// Use the easiest to compute common supertype
				this.exitTypeDescriptor = "Ljava/lang/Object";
			}
		}
	}

}
