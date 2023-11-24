/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.expression.spel.standard;

import java.util.function.Consumer;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.expression.spel.SpelMessage.MISSING_CONSTRUCTOR_ARGS;
import static org.springframework.expression.spel.SpelMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING;
import static org.springframework.expression.spel.SpelMessage.NON_TERMINATING_QUOTED_STRING;
import static org.springframework.expression.spel.SpelMessage.NOT_AN_INTEGER;
import static org.springframework.expression.spel.SpelMessage.NOT_A_LONG;
import static org.springframework.expression.spel.SpelMessage.OOD;
import static org.springframework.expression.spel.SpelMessage.REAL_CANNOT_BE_LONG;
import static org.springframework.expression.spel.SpelMessage.RIGHT_OPERAND_PROBLEM;
import static org.springframework.expression.spel.SpelMessage.RUN_OUT_OF_ARGUMENTS;
import static org.springframework.expression.spel.SpelMessage.UNEXPECTED_DATA_AFTER_DOT;
import static org.springframework.expression.spel.SpelMessage.UNEXPECTED_ESCAPE_CHAR;

/**
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class SpelParserTests {

	private final SpelExpressionParser parser = new SpelExpressionParser();


	@Test
	void nullExpressionIsRejected() {
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression(null));
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw(null));
	}

	@Test
	void emptyExpressionIsRejected() {
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression(""));
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw(""));
	}

	@Test
	void blankExpressionIsRejected() {
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression("     "));
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseExpression("\t\n"));
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw("     "));
		assertNullOrEmptyExpressionIsRejected(() -> parser.parseRaw("\t\n"));
	}

	private static void assertNullOrEmptyExpressionIsRejected(ThrowingCallable throwingCallable) {
		assertThatIllegalArgumentException()
				.isThrownBy(throwingCallable)
				.withMessage("'expressionString' must not be null or blank");
	}

	@Test
	void theMostBasic() {
		SpelExpression expr = parser.parseRaw("2");
		assertThat(expr).isNotNull();
		assertThat(expr.getAST()).isNotNull();
		assertThat(expr.getValue()).isEqualTo(2);
		assertThat(expr.getValueType()).isEqualTo(Integer.class);
		assertThat(expr.getAST().getValue(null)).isEqualTo(2);
	}

	@Test
	void valueType() {
		EvaluationContext ctx = new StandardEvaluationContext();
		Class<?> c = parser.parseRaw("2").getValueType();
		assertThat(c).isEqualTo(Integer.class);
		c = parser.parseRaw("12").getValueType(ctx);
		assertThat(c).isEqualTo(Integer.class);
		c = parser.parseRaw("null").getValueType();
		assertThat(c).isNull();
		c = parser.parseRaw("null").getValueType(ctx);
		assertThat(c).isNull();
		Object o = parser.parseRaw("null").getValue(ctx, Integer.class);
		assertThat(o).isNull();
	}

	@Test
	void whitespace() {
		SpelExpression expr = parser.parseRaw("2      +    3");
		assertThat(expr.getValue()).isEqualTo(5);
		expr = parser.parseRaw("2	+	3");
		assertThat(expr.getValue()).isEqualTo(5);
		expr = parser.parseRaw("2\n+\t3");
		assertThat(expr.getValue()).isEqualTo(5);
		expr = parser.parseRaw("2\r\n+\t3");
		assertThat(expr.getValue()).isEqualTo(5);
	}

	@Test
	void arithmeticPlus1() {
		SpelExpression expr = parser.parseRaw("2+2");
		assertThat(expr).isNotNull();
		assertThat(expr.getAST()).isNotNull();
		assertThat(expr.getValue()).isEqualTo(4);
	}

	@Test
	void arithmeticPlus2() {
		SpelExpression expr = parser.parseRaw("37+41");
		assertThat(expr.getValue()).isEqualTo(78);
	}

	@Test
	void arithmeticMultiply1() {
		SpelExpression expr = parser.parseRaw("2*3");
		assertThat(expr).isNotNull();
		assertThat(expr.getAST()).isNotNull();
		assertThat(expr.getValue()).isEqualTo(6);
	}

	@Test
	void arithmeticPrecedence1() {
		SpelExpression expr = parser.parseRaw("2*3+5");
		assertThat(expr.getValue()).isEqualTo(11);
	}

	@Test
	void parseExceptions() {
		assertParseException(() -> parser.parseRaw("new String"), MISSING_CONSTRUCTOR_ARGS, 10);
		assertParseException(() -> parser.parseRaw("new String(3,"), RUN_OUT_OF_ARGUMENTS, 10);
		assertParseException(() -> parser.parseRaw("new String(3"), RUN_OUT_OF_ARGUMENTS, 10);
		assertParseException(() -> parser.parseRaw("new String("), RUN_OUT_OF_ARGUMENTS, 10);
		assertParseException(() -> parser.parseRaw("\"abc"), NON_TERMINATING_DOUBLE_QUOTED_STRING, 0);
		assertParseException(() -> parser.parseRaw("abc\""), NON_TERMINATING_DOUBLE_QUOTED_STRING, 3);
		assertParseException(() -> parser.parseRaw("'abc"), NON_TERMINATING_QUOTED_STRING, 0);
		assertParseException(() -> parser.parseRaw("abc'"), NON_TERMINATING_QUOTED_STRING, 3);
		assertParseException(() -> parser.parseRaw("("), OOD, 0);
		assertParseException(() -> parser.parseRaw(")"), OOD, 0);
		assertParseException(() -> parser.parseRaw("+"), OOD, 0);
		assertParseException(() -> parser.parseRaw("1+"), RIGHT_OPERAND_PROBLEM, 1);
	}

	@Test
	void arithmeticPrecedence2() {
		SpelExpression expr = parser.parseRaw("2+3*5");
		assertThat(expr.getValue()).isEqualTo(17);
	}

	@Test
	void arithmeticPrecedence3() {
		SpelExpression expr = parser.parseRaw("3+10/2");
		assertThat(expr.getValue()).isEqualTo(8);
	}

	@Test
	void arithmeticPrecedence4() {
		SpelExpression expr = parser.parseRaw("10/2+3");
		assertThat(expr.getValue()).isEqualTo(8);
	}

	@Test
	void arithmeticPrecedence5() {
		SpelExpression expr = parser.parseRaw("(4+10)/2");
		assertThat(expr.getValue()).isEqualTo(7);
	}

	@Test
	void arithmeticPrecedence6() {
		SpelExpression expr = parser.parseRaw("(3+2)*2");
		assertThat(expr.getValue()).isEqualTo(10);
	}

	@Test
	void booleanOperators() {
		SpelExpression expr = parser.parseRaw("true");
		assertThat(expr.getValue(Boolean.class)).isTrue();
		expr = parser.parseRaw("false");
		assertThat(expr.getValue(Boolean.class)).isFalse();
		expr = parser.parseRaw("false and false");
		assertThat(expr.getValue(Boolean.class)).isFalse();
		expr = parser.parseRaw("true and (true or false)");
		assertThat(expr.getValue(Boolean.class)).isTrue();
		expr = parser.parseRaw("true and true or false");
		assertThat(expr.getValue(Boolean.class)).isTrue();
		expr = parser.parseRaw("!true");
		assertThat(expr.getValue(Boolean.class)).isFalse();
		expr = parser.parseRaw("!(false or true)");
		assertThat(expr.getValue(Boolean.class)).isFalse();
	}

	@Test
	void booleanOperators_symbolic_spr9614() {
		SpelExpression expr = parser.parseRaw("true");
		assertThat(expr.getValue(Boolean.class)).isTrue();
		expr = parser.parseRaw("false");
		assertThat(expr.getValue(Boolean.class)).isFalse();
		expr = parser.parseRaw("false && false");
		assertThat(expr.getValue(Boolean.class)).isFalse();
		expr = parser.parseRaw("true && (true || false)");
		assertThat(expr.getValue(Boolean.class)).isTrue();
		expr = parser.parseRaw("true && true || false");
		assertThat(expr.getValue(Boolean.class)).isTrue();
		expr = parser.parseRaw("!true");
		assertThat(expr.getValue(Boolean.class)).isFalse();
		expr = parser.parseRaw("!(false || true)");
		assertThat(expr.getValue(Boolean.class)).isFalse();
	}

	@Test
	void stringLiterals() {
		SpelExpression expr = parser.parseRaw("'howdy'");
		assertThat(expr.getValue()).isEqualTo("howdy");
		expr = parser.parseRaw("'hello '' world'");
		assertThat(expr.getValue()).isEqualTo("hello ' world");
	}

	@Test
	void stringLiterals2() {
		SpelExpression expr = parser.parseRaw("'howdy'.substring(0,2)");
		assertThat(expr.getValue()).isEqualTo("ho");
	}

	@Test
	void testStringLiterals_DoubleQuotes_spr9620() {
		SpelExpression expr = parser.parseRaw("\"double quote: \"\".\"");
		assertThat(expr.getValue()).isEqualTo("double quote: \".");
		expr = parser.parseRaw("\"hello \"\" world\"");
		assertThat(expr.getValue()).isEqualTo("hello \" world");
	}

	@Test
	void testStringLiterals_DoubleQuotes_spr9620_2() {
		assertParseExceptionThrownBy(() -> parser.parseRaw("\"double quote: \\\"\\\".\""))
			.satisfies(ex -> {
				assertThat(ex.getPosition()).isEqualTo(17);
				assertThat(ex.getMessageCode()).isEqualTo(UNEXPECTED_ESCAPE_CHAR);
			});
	}

	@Test
	void positionalInformation() {
		SpelExpression expr = parser.parseRaw("true and true or false");
		SpelNode rootAst = expr.getAST();
		OpOr operatorOr = (OpOr) rootAst;
		OpAnd operatorAnd = (OpAnd) operatorOr.getLeftOperand();
		SpelNode rightOrOperand = operatorOr.getRightOperand();

		// check position for final 'false'
		assertThat(rightOrOperand.getStartPosition()).isEqualTo(17);
		assertThat(rightOrOperand.getEndPosition()).isEqualTo(22);

		// check position for first 'true'
		assertThat(operatorAnd.getLeftOperand().getStartPosition()).isEqualTo(0);
		assertThat(operatorAnd.getLeftOperand().getEndPosition()).isEqualTo(4);

		// check position for second 'true'
		assertThat(operatorAnd.getRightOperand().getStartPosition()).isEqualTo(9);
		assertThat(operatorAnd.getRightOperand().getEndPosition()).isEqualTo(13);

		// check position for OperatorAnd
		assertThat(operatorAnd.getStartPosition()).isEqualTo(5);
		assertThat(operatorAnd.getEndPosition()).isEqualTo(8);

		// check position for OperatorOr
		assertThat(operatorOr.getStartPosition()).isEqualTo(14);
		assertThat(operatorOr.getEndPosition()).isEqualTo(16);
	}

	@Test
	void tokenKind() {
		TokenKind tk = TokenKind.NOT;
		assertThat(tk.hasPayload()).isFalse();
		assertThat(tk.toString()).isEqualTo("NOT(!)");

		tk = TokenKind.MINUS;
		assertThat(tk.hasPayload()).isFalse();
		assertThat(tk.toString()).isEqualTo("MINUS(-)");

		tk = TokenKind.LITERAL_STRING;
		assertThat(tk.toString()).isEqualTo("LITERAL_STRING");
		assertThat(tk.hasPayload()).isTrue();
	}

	@Test
	void token() {
		Token token = new Token(TokenKind.NOT, 0, 3);
		assertThat(token.kind).isEqualTo(TokenKind.NOT);
		assertThat(token.startPos).isEqualTo(0);
		assertThat(token.endPos).isEqualTo(3);
		assertThat(token.toString()).isEqualTo("[NOT(!)](0,3)");

		token = new Token(TokenKind.LITERAL_STRING, "abc".toCharArray(), 0, 3);
		assertThat(token.kind).isEqualTo(TokenKind.LITERAL_STRING);
		assertThat(token.startPos).isEqualTo(0);
		assertThat(token.endPos).isEqualTo(3);
		assertThat(token.toString()).isEqualTo("[LITERAL_STRING:abc](0,3)");
	}

	@Test
	void exceptions() {
		ExpressionException exprEx = new ExpressionException("test");
		assertThat(exprEx.getSimpleMessage()).isEqualTo("test");
		assertThat(exprEx.toDetailedString()).isEqualTo("test");
		assertThat(exprEx.getMessage()).isEqualTo("test");

		exprEx = new ExpressionException("wibble", "test");
		assertThat(exprEx.getSimpleMessage()).isEqualTo("test");
		assertThat(exprEx.toDetailedString()).isEqualTo("Expression [wibble]: test");
		assertThat(exprEx.getMessage()).isEqualTo("Expression [wibble]: test");

		exprEx = new ExpressionException("wibble", 3, "test");
		assertThat(exprEx.getSimpleMessage()).isEqualTo("test");
		assertThat(exprEx.toDetailedString()).isEqualTo("Expression [wibble] @3: test");
		assertThat(exprEx.getMessage()).isEqualTo("Expression [wibble] @3: test");
	}

	@Test
	void parseMethodsOnNumbers() {
		checkNumber("3.14.toString()", "3.14", String.class);
		checkNumber("3.toString()", "3", String.class);
	}

	@Test
	void numerics() {
		checkNumber("2", 2, Integer.class);
		checkNumber("22", 22, Integer.class);
		checkNumber("+22", 22, Integer.class);
		checkNumber("-22", -22, Integer.class);
		checkNumber("2L", 2L, Long.class);
		checkNumber("22l", 22L, Long.class);

		checkNumber("0x1", 1, Integer.class);
		checkNumber("0x1L", 1L, Long.class);
		checkNumber("0xa", 10, Integer.class);
		checkNumber("0xAL", 10L, Long.class);

		checkNumberError("0x", NOT_AN_INTEGER);
		checkNumberError("0xL", NOT_A_LONG);
		checkNumberError(".324", UNEXPECTED_DATA_AFTER_DOT);
		checkNumberError("3.4L", REAL_CANNOT_BE_LONG);

		checkNumber("3.5f", 3.5f, Float.class);
		checkNumber("1.2e3", 1.2e3d, Double.class);
		checkNumber("1.2e+3", 1.2e3d, Double.class);
		checkNumber("1.2e-3", 1.2e-3d, Double.class);
		checkNumber("1.2e3", 1.2e3d, Double.class);
		checkNumber("1e+3", 1e3d, Double.class);
	}


	private void checkNumber(String expression, Object value, Class<?> type) {
		try {
			SpelExpression expr = parser.parseRaw(expression);
			Object exprVal = expr.getValue();
			assertThat(exprVal).isEqualTo(value);
			assertThat(exprVal.getClass()).isEqualTo(type);
		}
		catch (Exception ex) {
			throw new AssertionError(ex.getMessage(), ex);
		}
	}

	private void checkNumberError(String expression, SpelMessage expectedMessage) {
		assertParseExceptionThrownBy(() -> parser.parseRaw(expression))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(expectedMessage));
	}

	private static ThrowableAssertAlternative<SpelParseException> assertParseExceptionThrownBy(ThrowingCallable throwingCallable) {
		return assertThatExceptionOfType(SpelParseException.class).isThrownBy(throwingCallable);
	}

	private static void assertParseException(ThrowingCallable throwingCallable, SpelMessage expectedMessage, int expectedPosition) {
		assertParseExceptionThrownBy(throwingCallable)
				.satisfies(parseExceptionRequirements(expectedMessage, expectedPosition));
	}

	private static <E extends SpelParseException> Consumer<E> parseExceptionRequirements(
			SpelMessage expectedMessage, int expectedPosition) {

		return ex -> {
			assertThat(ex.getMessageCode()).isEqualTo(expectedMessage);
			assertThat(ex.getPosition()).isEqualTo(expectedPosition);
			if (ex.getExpressionString() != null) {
				assertThat(ex.getMessage()).contains(ex.getExpressionString());
			}
		};
	}

}
