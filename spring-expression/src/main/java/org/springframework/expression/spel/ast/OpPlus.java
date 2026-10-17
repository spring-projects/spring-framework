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

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * The plus operator will:
 * <ul>
 * <li>add numbers
 * <li>concatenate strings
 * </ul>
 *
 * <p>It can also be used as a unary operator for numbers.
 *
 * <p>The standard promotions are performed when the operand types vary
 * (double + int = double). For other operand types, it delegates to the
 * registered {@link org.springframework.expression.OperatorOverloader}.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Ivo Smid
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 * @since 3.0
 */
public class OpPlus extends Operator {

	/**
	 * Maximum number of characters permitted in a concatenated string.
	 * @since 5.2.24
	 */
	private static final int MAX_CONCATENATED_STRING_LENGTH = 100_000;

	/**
	 * The non-{@code String} operand of the most recent {@code String + non-String}
	 * (or {@code non-String + String}) concatenation, or {@code null} if the most
	 * recent evaluation was not such a concatenation.
	 * @since 7.1
	 */
	private volatile @Nullable SpelNodeImpl nonStringOperand;

	/**
	 * The type of the most recent non-{@code null} value of a non-{@code String}
	 * operand that has been converted to a {@code String} for a
	 * {@code String + non-String} (or {@code non-String + String}) concatenation,
	 * or {@code null} if no such value has been converted.
	 * <p>Unless the operand's exit type descriptor guarantees that a non-{@code null}
	 * value is of this type, the compiled form verifies the type at runtime.
	 * @since 7.1
	 */
	private volatile @Nullable Class<?> nonStringOperandType;

	/**
	 * The type of the {@link TypeConverter} that most recently converted a
	 * non-{@code null} value of a non-{@code String} operand to a {@code String}
	 * for a {@code String + non-String} (or {@code non-String + String})
	 * concatenation, or {@code null} if no such value has been converted.
	 * <p>The type is tracked instead of the instance in order not to retain the
	 * {@code TypeConverter} and since a {@code StandardEvaluationContext} creates
	 * a new {@code StandardTypeConverter} by default. Consequently, switching
	 * between {@code TypeConverter} instances of the same type which convert values
	 * differently is only detected if the expression may be compiled automatically.
	 * @since 7.1
	 */
	private volatile @Nullable Class<?> nonStringOperandTypeConverterType;

	/**
	 * Tracks whether the registered {@link TypeConverter} produced a result for a
	 * {@code String + non-String} (or {@code non-String + String}) concatenation that
	 * differs from {@link String#valueOf(Object)} or converted an operand whose type
	 * descriptor carries annotations, since annotation-driven formatting (for
	 * example, via {@code @NumberFormat}) typically depends on the value.
	 * <p>When {@code true}, this node cannot be compiled, since the compiled form
	 * uses {@code StringBuilder.append(T)} which is equivalent to {@code String.valueOf(T)}.
	 * <p>This flag is a one-way latch: once set to {@code true} it is never reset.
	 * @since 7.1
	 */
	private volatile boolean typeConversionDiffersFromToString;


	public OpPlus(int startPos, int endPos, SpelNodeImpl... operands) {
		super("+", startPos, endPos, operands);
		Assert.notEmpty(operands, "Operands must not be empty");
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();

		if (this.children.length < 2) {  // if only one operand, then this is unary plus
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
				state.trackOperation();
				if (operandOne instanceof Double) {
					this.exitTypeDescriptor = "D";
				}
				else if (operandOne instanceof Float) {
					this.exitTypeDescriptor = "F";
				}
				else if (operandOne instanceof Long) {
					this.exitTypeDescriptor = "J";
				}
				else if (operandOne instanceof Integer) {
					this.exitTypeDescriptor = "I";
				}
				return new TypedValue(operandOne);
			}
			return state.operate(Operation.ADD, operandOne, null);
		}

		TypedValue operandOneValue = leftOp.getValueInternal(state);
		Object leftOperand = operandOneValue.getValue();
		TypedValue operandTwoValue = getRightOperand().getValueInternal(state);
		Object rightOperand = operandTwoValue.getValue();

		if (leftOperand instanceof Number leftNumber && rightOperand instanceof Number rightNumber) {
			state.trackOperation();
			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.add(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() + rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.add(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() + rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() + rightNumber.intValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double addition
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
		}

		if (leftOperand instanceof String leftString && rightOperand instanceof String rightString) {
			this.exitTypeDescriptor = "Ljava/lang/String";
			// Avoid an unnecessary volatile write if the non-String operand has not changed.
			if (this.nonStringOperand != null) {
				this.nonStringOperand = null;
			}
			checkStringLength(leftString);
			checkStringLength(rightString);
			return concatenate(state, leftString, rightString);
		}

		if (leftOperand instanceof String leftString) {
			checkStringLength(leftString);
			String rightString = convertNonStringOperandToString(operandTwoValue, state);
			checkStringLength(rightString);
			this.exitTypeDescriptor = "Ljava/lang/String";
			SpelNodeImpl rightOp = getRightOperand();
			// Avoid an unnecessary volatile write if the non-String operand has not changed.
			if (this.nonStringOperand != rightOp) {
				this.nonStringOperand = rightOp;
			}
			return concatenate(state, leftString, rightString);
		}

		if (rightOperand instanceof String rightString) {
			checkStringLength(rightString);
			String leftString = convertNonStringOperandToString(operandOneValue, state);
			checkStringLength(leftString);
			this.exitTypeDescriptor = "Ljava/lang/String";
			// Avoid an unnecessary volatile write if the non-String operand has not changed.
			if (this.nonStringOperand != leftOp) {
				this.nonStringOperand = leftOp;
			}
			return concatenate(state, leftString, rightString);
		}

		return state.operate(Operation.ADD, leftOperand, rightOperand);
	}

	private void checkStringLength(String string) {
		checkStringLength(string.length());
	}

	private void checkStringLength(int stringLength) {
		if (stringLength > MAX_CONCATENATED_STRING_LENGTH) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, MAX_CONCATENATED_STRING_LENGTH);
		}
	}

	private TypedValue concatenate(ExpressionState state, String leftString, String rightString) {
		checkStringLength(leftString.length() + rightString.length());
		state.trackOperation();
		return new TypedValue(leftString + rightString);
	}

	/**
	 * Convert the value of a non-{@code String} operand to a {@code String} for use
	 * in a {@code String + non-String} (or {@code non-String + String}) concatenation,
	 * using the registered {@link TypeConverter} if it can convert the value and
	 * {@link String#valueOf(Object)} otherwise.
	 * <p>In order to determine whether this node can be compiled, this method also
	 * tracks the type of the value and whether the {@code TypeConverter} produced a
	 * result that differs from {@code String.valueOf()}.
	 * <p>If the expression may be compiled automatically, the result is compared
	 * for every evaluation. Otherwise, the result is only compared if the type of
	 * the value or the type of the {@code TypeConverter} changes, in order to avoid
	 * converting the value to a {@code String} twice for every evaluation in
	 * interpreted mode, while still supporting explicit compilation.
	 * @since 7.1
	 */
	private String convertNonStringOperandToString(TypedValue typedValue, ExpressionState state) {
		Object value = typedValue.getValue();
		if (value == null) {
			return "null";
		}

		Class<?> type = value.getClass();
		TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
		Class<?> typeConverterType = typeConverter.getClass();
		boolean compare = (!this.typeConversionDiffersFromToString &&
				(isCompilationEnabled(state) || type != this.nonStringOperandType ||
						typeConverterType != this.nonStringOperandTypeConverterType));
		String result;
		TypeDescriptor sourceType = typedValue.getTypeDescriptor();
		TypeDescriptor targetType = TypeDescriptor.valueOf(String.class);
		if (typeConverter.canConvert(sourceType, targetType)) {
			result = String.valueOf(typeConverter.convertValue(value, sourceType, targetType));
			if (compare && ((sourceType != null && sourceType.getAnnotations().length > 0) ||
					!result.equals(String.valueOf(value)))) {
				this.typeConversionDiffersFromToString = true;
			}
		}
		else {
			result = String.valueOf(value);
		}

		// Avoid unnecessary volatile writes if the tracked types have not changed.
		if (type != this.nonStringOperandType) {
			this.nonStringOperandType = type;
		}
		if (typeConverterType != this.nonStringOperandTypeConverterType) {
			this.nonStringOperandTypeConverterType = typeConverterType;
		}

		return result;
	}

	/**
	 * Determine if the expression may be compiled automatically, based on the
	 * configured {@link SpelCompilerMode} and the {@code EvaluationContext}.
	 * @since 7.1
	 */
	private static boolean isCompilationEnabled(ExpressionState state) {
		return (state.getConfiguration().getCompilerMode() != SpelCompilerMode.OFF &&
				state.getEvaluationContext().isCompilationSupported());
	}

	@Override
	public String toStringAST() {
		if (this.children.length < 2) {  // unary plus
			return "+" + getLeftOperand().toStringAST();
		}
		return super.toStringAST();
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		if (this.children.length < 2) {
			throw new IllegalStateException("No right operand");
		}
		return this.children[1];
	}

	@Override
	public boolean isCompilable() {
		if (!getLeftOperand().isCompilable()) {
			return false;
		}
		if (this.children.length > 1) {
			if (!getRightOperand().isCompilable()) {
				return false;
			}
		}
		String exitDesc = this.exitTypeDescriptor;
		if (exitDesc == null) {
			return false;
		}

		SpelNodeImpl nonStringOperand = this.nonStringOperand;
		if ("Ljava/lang/String".equals(exitDesc) && nonStringOperand != null) {
			// The compiled form converts the non-String operand via String.valueOf(), which
			// is only equivalent to the conversion performed in interpreted mode if a value
			// has been converted without the TypeConverter producing a different result.
			// In addition, the compiled form must be able to reference the type of that
			// value in order to verify that subsequent values are of the same type.
			String descriptor = nonStringOperand.exitTypeDescriptor;
			Class<?> type = this.nonStringOperandType;
			return (!this.typeConversionDiffersFromToString && type != null &&
					descriptor != null && !"V".equals(descriptor) &&
					(CodeFlow.isPrimitive(descriptor) || isAccessible(type)));
		}

		return true;
	}

	/**
	 * Determine if the supplied type is accessible from a compiled expression,
	 * which requires the type to be {@code public} and its package to be exported
	 * unconditionally by its module.
	 * @since 7.1
	 */
	private static boolean isAccessible(Class<?> type) {
		return (Modifier.isPublic(type.getModifiers()) &&
				type.getModule().isExported(type.getPackageName()));
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		if ("Ljava/lang/String".equals(this.exitTypeDescriptor)) {
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
			walk(mv, cf, this, getLeftOperand());
			walk(mv, cf, this, getRightOperand());
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
		}
		else {
			this.children[0].generateCode(mv, cf);
			String leftDesc = this.children[0].exitTypeDescriptor;
			String exitDesc = this.exitTypeDescriptor;
			Assert.state(exitDesc != null, "No exit type descriptor");
			char targetDesc = exitDesc.charAt(0);
			CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
			if (this.children.length > 1) {
				cf.enterCompilationScope();
				this.children[1].generateCode(mv, cf);
				String rightDesc = this.children[1].exitTypeDescriptor;
				cf.exitCompilationScope();
				CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
				switch (targetDesc) {
					case 'I' -> mv.visitInsn(IADD);
					case 'J' -> mv.visitInsn(LADD);
					case 'F' -> mv.visitInsn(FADD);
					case 'D' -> mv.visitInsn(DADD);
					default -> throw new IllegalStateException(
							"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
				}
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

	/**
	 * Walk through a possible tree of nodes that combine strings and append
	 * them all to the same (on stack) StringBuilder.
	 * @param concatenation the {@code OpPlus} node to which the operand belongs
	 * @param operand the operand to append
	 */
	private static void walk(MethodVisitor mv, CodeFlow cf, OpPlus concatenation,
			@Nullable SpelNodeImpl operand) {

		if (operand instanceof OpPlus plus && "Ljava/lang/String".equals(plus.exitTypeDescriptor)) {
			// Only flatten nested String concatenations: numeric additions (and unary
			// plus) must be evaluated as an operand in their own right.
			walk(mv, cf, plus, plus.getLeftOperand());
			walk(mv, cf, plus, plus.getRightOperand());
		}
		else if (operand != null) {
			cf.enterCompilationScope();
			operand.generateCode(mv, cf);
			String descriptor = cf.lastDescriptor();
			cf.exitCompilationScope();
			if (operand == concatenation.nonStringOperand) {
				concatenation.appendNonStringOperand(mv, descriptor);
			}
			else {
				appendStringOperand(mv, descriptor);
			}
		}
	}

	/**
	 * Emit the {@code StringBuilder.append(String)} call for an operand that
	 * evaluated to a {@code String}, casting it to {@code String} if necessary.
	 * @since 7.1
	 */
	private static void appendStringOperand(MethodVisitor mv, @Nullable String descriptor) {
		if (!"Ljava/lang/String".equals(descriptor)) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/String");
		}
		// StringBuilder.append(String):
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
	}

	/**
	 * Emit the appropriate {@code StringBuilder.append(T)} call for the
	 * non-{@code String} operand, whose type is described by the supplied descriptor.
	 * <p>Primitive types use the dedicated {@code append} overloads directly.
	 * Reference types use {@code append(Object)}, which delegates to
	 * {@link String#valueOf(Object)} and therefore handles {@code null} safely
	 * (producing {@code "null"}).
	 * <p>Unless the descriptor denotes a {@code final} type which is identical to
	 * the {@linkplain #nonStringOperandType tracked type}, the emitted code verifies
	 * that a non-{@code null} value is an instance of exactly the tracked type and
	 * throws an {@link IllegalStateException} otherwise, which allows a compiled
	 * expression to revert to interpreted mode in {@code SpelCompilerMode.MIXED}.
	 * @since 7.1
	 */
	private void appendNonStringOperand(MethodVisitor mv, @Nullable String descriptor) {
		if (CodeFlow.isPrimitive(descriptor)) {
			// byte (B) and short (S) are widened to int on the JVM operand stack
			String appendTypeDesc = switch (descriptor.charAt(0)) {
				case 'B', 'S', 'I' -> "I";
				case 'J' -> "J";
				case 'F' -> "F";
				case 'D' -> "D";
				case 'Z' -> "Z";
				case 'C' -> "C";
				default -> throw new IllegalStateException(
						"Unexpected primitive descriptor '" + descriptor + "'");
			};
			// StringBuilder.append(<primitive>):
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
					"(" + appendTypeDesc + ")Ljava/lang/StringBuilder;", false);
			return;
		}

		Class<?> type = this.nonStringOperandType;
		Assert.state(type != null, "No type available for non-String operand");
		if (!Modifier.isFinal(type.getModifiers()) || !CodeFlow.toDescriptor(type).equals(descriptor)) {
			// Equivalent to the following, where a null value is appended as "null".
			// if (value != null && value.getClass() != type) { throw new IllegalStateException(...); }
			// The stack contains sb/value both before and after the type check, where sb is a StringBuilder.
			Label typeVerified = new Label();
			mv.visitInsn(DUP);                                         // stack: sb/value/value
			mv.visitJumpInsn(IFNULL, typeVerified);                    // stack: sb/value
			mv.visitInsn(DUP);                                         // stack: sb/value/value
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass",
					"()Ljava/lang/Class;", false);                     // stack: sb/value/class
			mv.visitLdcInsn(Type.getType(type));                       // stack: sb/value/class/type
			// IF_ACMPEQ: "if address (reference) compare equal" (i.e., class == type)
			mv.visitJumpInsn(IF_ACMPEQ, typeVerified);                 // stack: sb/value
			mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");  // stack: sb/value/ex
			mv.visitInsn(DUP);                                         // stack: sb/value/ex/ex
			mv.visitLdcInsn("Operand of compiled String concatenation is not of type " + type.getName());
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>",
					"(Ljava/lang/String;)V", false);                   // stack: sb/value/ex
			mv.visitInsn(ATHROW);
			mv.visitLabel(typeVerified);                               // stack: sb/value
		}

		// StringBuilder.append(Object):
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
	}

}
