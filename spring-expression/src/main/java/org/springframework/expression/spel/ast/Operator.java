/*
 * Copyright 2002-2021 the original author or authors.
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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.lang.Nullable;
import org.springframework.util.NumberUtils;
import org.springframework.util.ObjectUtils;

/**
 * Common supertype for operators that operate on either one or two operands.
 * In the case of multiply or divide there would be two operands, but for
 * unary plus or minus, there is only one.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 * @since 3.0
 */
public abstract class Operator extends SpelNodeImpl {

	private final String operatorName;

	// The descriptors of the runtime operand values are used if the discovered declared
	// descriptors are not providing enough information (for example a generic type
	// whose accessors seem to only be returning 'Object' - the actual descriptors may
	// indicate 'int')

	@Nullable
	protected String leftActualDescriptor;

	@Nullable
	protected String rightActualDescriptor;


	public Operator(String payload, int startPos, int endPos, SpelNodeImpl... operands) {
		super(startPos, endPos, operands);
		this.operatorName = payload;
	}


	public SpelNodeImpl getLeftOperand() {
		return this.children[0];
	}

	public SpelNodeImpl getRightOperand() {
		return this.children[1];
	}

	public final String getOperatorName() {
		return this.operatorName;
	}

	/**
	 * String format for all operators is the same
	 * {@code '(' [operand] [operator] [operand] ')'}.
	 */
	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("(");
		sb.append(getChild(0).toStringAST());
		for (int i = 1; i < getChildCount(); i++) {
			sb.append(' ').append(getOperatorName()).append(' ');
			sb.append(getChild(i).toStringAST());
		}
		sb.append(')');
		return sb.toString();
	}


	protected boolean isCompilableOperatorUsingNumerics() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right = getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}

		// Supported operand types for equals (at the moment)
		String leftDesc = left.exitTypeDescriptor;
		String rightDesc = right.exitTypeDescriptor;
		DescriptorComparison dc = DescriptorComparison.checkNumericCompatibility(
				leftDesc, rightDesc, this.leftActualDescriptor, this.rightActualDescriptor);
		return (dc.areNumbers && dc.areCompatible);
	}

	/**
	 * Numeric comparison operators share very similar generated code, only differing in
	 * two comparison instructions.
	 */
	protected void generateComparisonCode(MethodVisitor mv, CodeFlow cf, int compInstruction1, int compInstruction2) {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right = getRightOperand();
		String leftDesc = left.exitTypeDescriptor;
		String rightDesc = right.exitTypeDescriptor;
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		boolean unboxLeft = !CodeFlow.isPrimitive(leftDesc);
		boolean unboxRight = !CodeFlow.isPrimitive(rightDesc);
		DescriptorComparison dc = DescriptorComparison.checkNumericCompatibility(
				leftDesc, rightDesc, this.leftActualDescriptor, this.rightActualDescriptor);
		char targetType = dc.compatibleType;  // CodeFlow.toPrimitiveTargetDesc(leftDesc);

		cf.enterCompilationScope();
		left.generateCode(mv, cf);
		cf.exitCompilationScope();
		if (CodeFlow.isPrimitive(leftDesc)) {
			CodeFlow.insertBoxIfNecessary(mv, leftDesc);
			unboxLeft = true;
		}

		cf.enterCompilationScope();
		right.generateCode(mv, cf);
		cf.exitCompilationScope();
		if (CodeFlow.isPrimitive(rightDesc)) {
			CodeFlow.insertBoxIfNecessary(mv, rightDesc);
			unboxRight = true;
		}

		// This code block checks whether the left or right operand is null and handles
		// those cases before letting the original code (that only handled actual numbers) run
		Label rightIsNonNull = new Label();
		mv.visitInsn(DUP);  // stack: left/right/right
		mv.visitJumpInsn(IFNONNULL, rightIsNonNull);  // stack: left/right
		// here: RIGHT==null LEFT==unknown
		mv.visitInsn(SWAP);  // right/left
		Label leftNotNullRightIsNull = new Label();
		mv.visitJumpInsn(IFNONNULL, leftNotNullRightIsNull);  // stack: right
		// here: RIGHT==null LEFT==null
		mv.visitInsn(POP);  // stack: <nothing>
		// load 0 or 1 depending on comparison instruction
		switch (compInstruction1) {
		case IFGE: // OpLT
		case IFLE: // OpGT
			mv.visitInsn(ICONST_0);  // false - null is not < or > null
			break;
		case IFGT: // OpLE
		case IFLT: // OpGE
			mv.visitInsn(ICONST_1);  // true - null is <= or >= null
			break;
		default:
			throw new IllegalStateException("Unsupported: " + compInstruction1);
		}
		mv.visitJumpInsn(GOTO, endOfIf);
		mv.visitLabel(leftNotNullRightIsNull);  // stack: right
		// RIGHT==null LEFT!=null
		mv.visitInsn(POP);  // stack: <nothing>
		// load 0 or 1 depending on comparison instruction
		switch (compInstruction1) {
		case IFGE: // OpLT
		case IFGT: // OpLE
			mv.visitInsn(ICONST_0);  // false - something is not < or <= null
			break;
		case IFLE: // OpGT
		case IFLT: // OpGE
			mv.visitInsn(ICONST_1);  // true - something is > or >= null
			break;
		default:
			throw new IllegalStateException("Unsupported: " + compInstruction1);
		}
		mv.visitJumpInsn(GOTO, endOfIf);

		mv.visitLabel(rightIsNonNull);  // stack: left/right
		// here: RIGHT!=null LEFT==unknown
		mv.visitInsn(SWAP);  // stack: right/left
		mv.visitInsn(DUP);  // stack: right/left/left
		Label neitherRightNorLeftAreNull = new Label();
		mv.visitJumpInsn(IFNONNULL, neitherRightNorLeftAreNull);  // stack: right/left
		// here: RIGHT!=null LEFT==null
		mv.visitInsn(POP2);  // stack: <nothing>
		switch (compInstruction1) {
		case IFGE: // OpLT
		case IFGT: // OpLE
			mv.visitInsn(ICONST_1);  // true - null is < or <= something
			break;
		case IFLE: // OpGT
		case IFLT: // OpGE
			mv.visitInsn(ICONST_0);  // false - null is not > or >= something
			break;
		default:
			throw new IllegalStateException("Unsupported: " + compInstruction1);
		}
		mv.visitJumpInsn(GOTO, endOfIf);
		mv.visitLabel(neitherRightNorLeftAreNull);  // stack: right/left
		// neither were null so unbox and proceed with numeric comparison
		if (unboxLeft) {
			CodeFlow.insertUnboxInsns(mv, targetType, leftDesc);
		}
		// What we just unboxed might be a double slot item (long/double)
		// so can't just use SWAP
		// stack: right/left(1or2slots)
		if (targetType == 'D' || targetType == 'J') {
			mv.visitInsn(DUP2_X1);
			mv.visitInsn(POP2);
		}
		else {
			mv.visitInsn(SWAP);
		}
		// stack: left(1or2)/right
		if (unboxRight) {
			CodeFlow.insertUnboxInsns(mv, targetType, rightDesc);
		}

		// assert: SpelCompiler.boxingCompatible(leftDesc, rightDesc)
		if (targetType == 'D') {
			mv.visitInsn(DCMPG);
			mv.visitJumpInsn(compInstruction1, elseTarget);
		}
		else if (targetType == 'F') {
			mv.visitInsn(FCMPG);
			mv.visitJumpInsn(compInstruction1, elseTarget);
		}
		else if (targetType == 'J') {
			mv.visitInsn(LCMP);
			mv.visitJumpInsn(compInstruction1, elseTarget);
		}
		else if (targetType == 'I') {
			mv.visitJumpInsn(compInstruction2, elseTarget);
		}
		else {
			throw new IllegalStateException("Unexpected descriptor " + leftDesc);
		}

		// Other numbers are not yet supported (isCompilable will not have returned true)
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(endOfIf);
		cf.pushDescriptor("Z");
	}


	/**
	 * Perform an equality check for the given operand values.
	 * <p>This method is not just used for reflective comparisons in subclasses
	 * but also from compiled expression code, which is why it needs to be
	 * declared as {@code public static} here.
	 * @param context the current evaluation context
	 * @param left the left-hand operand value
	 * @param right the right-hand operand value
	 */
	public static boolean equalityCheck(EvaluationContext context, @Nullable Object left, @Nullable Object right) {
		if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return (leftBigDecimal.compareTo(rightBigDecimal) == 0);
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return (leftNumber.doubleValue() == rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return (leftNumber.floatValue() == rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return (leftBigInteger.compareTo(rightBigInteger) == 0);
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return (leftNumber.longValue() == rightNumber.longValue());
			}
			else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
				return (leftNumber.intValue() == rightNumber.intValue());
			}
			else if (leftNumber instanceof Short || rightNumber instanceof Short) {
				return (leftNumber.shortValue() == rightNumber.shortValue());
			}
			else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
				return (leftNumber.byteValue() == rightNumber.byteValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double comparison
				return (leftNumber.doubleValue() == rightNumber.doubleValue());
			}
		}

		if (left instanceof CharSequence && right instanceof CharSequence) {
			return left.toString().equals(right.toString());
		}

		if (left instanceof Boolean && right instanceof Boolean) {
			return left.equals(right);
		}

		if (ObjectUtils.nullSafeEquals(left, right)) {
			return true;
		}

		if (context.getTypeComparator().canCompare(left, right)) {
			return context.getTypeComparator().compare(left, right) == 0;
		}

		return false;
	}


	/**
	 * A descriptor comparison encapsulates the result of comparing descriptor
	 * for two operands and describes at what level they are compatible.
	 */
	protected static final class DescriptorComparison {

		static final DescriptorComparison NOT_NUMBERS = new DescriptorComparison(false, false, ' ');

		static final DescriptorComparison INCOMPATIBLE_NUMBERS = new DescriptorComparison(true, false, ' ');

		final boolean areNumbers;  // Were the two compared descriptor both for numbers?

		final boolean areCompatible;  // If they were numbers, were they compatible?

		final char compatibleType;  // When compatible, what is the descriptor of the common type

		private DescriptorComparison(boolean areNumbers, boolean areCompatible, char compatibleType) {
			this.areNumbers = areNumbers;
			this.areCompatible = areCompatible;
			this.compatibleType = compatibleType;
		}

		/**
		 * Return an object that indicates whether the input descriptors are compatible.
		 * <p>A declared descriptor is what could statically be determined (e.g. from looking
		 * at the return value of a property accessor method) whilst an actual descriptor
		 * is the type of an actual object that was returned, which may differ.
		 * <p>For generic types with unbound type variables, the declared descriptor
		 * discovered may be 'Object' but from the actual descriptor it is possible to
		 * observe that the objects are really numeric values (e.g. ints).
		 * @param leftDeclaredDescriptor the statically determinable left descriptor
		 * @param rightDeclaredDescriptor the statically determinable right descriptor
		 * @param leftActualDescriptor the dynamic/runtime left object descriptor
		 * @param rightActualDescriptor the dynamic/runtime right object descriptor
		 * @return a DescriptorComparison object indicating the type of compatibility, if any
		 */
		@SuppressWarnings("NullAway")
		public static DescriptorComparison checkNumericCompatibility(
				@Nullable String leftDeclaredDescriptor, @Nullable String rightDeclaredDescriptor,
				@Nullable String leftActualDescriptor, @Nullable String rightActualDescriptor) {

			String ld = leftDeclaredDescriptor;
			String rd = rightDeclaredDescriptor;

			boolean leftNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(ld);
			boolean rightNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(rd);

			// If the declared descriptors aren't providing the information, try the actual descriptors
			if (!leftNumeric && !ObjectUtils.nullSafeEquals(ld, leftActualDescriptor)) {
				ld = leftActualDescriptor;
				leftNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(ld);
			}
			if (!rightNumeric && !ObjectUtils.nullSafeEquals(rd, rightActualDescriptor)) {
				rd = rightActualDescriptor;
				rightNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(rd);
			}

			if (leftNumeric && rightNumeric) {
				if (CodeFlow.areBoxingCompatible(ld, rd)) {
					return new DescriptorComparison(true, true, CodeFlow.toPrimitiveTargetDesc(ld));
				}
				else {
					return DescriptorComparison.INCOMPATIBLE_NUMBERS;
				}
			}
			else {
				return DescriptorComparison.NOT_NUMBERS;
			}
		}
	}

}
