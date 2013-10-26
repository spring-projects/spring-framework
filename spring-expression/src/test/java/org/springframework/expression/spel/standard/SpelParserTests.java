/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.standard;

import static org.junit.Assert.*;

import org.junit.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author Andy Clement
 */
public class SpelParserTests {

	@Test
	public void theMostBasic() throws EvaluationException, ParseException {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2");
		assertNotNull(expr);
		assertNotNull(expr.getAST());
		assertEquals(2, expr.getValue());
		assertEquals(Integer.class, expr.getValueType());
		assertEquals(2, expr.getAST().getValue(null));
	}

	@Test
	public void valueType() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		EvaluationContext ctx = new StandardEvaluationContext();
		Class<?> c = parser.parseRaw("2").getValueType();
		assertEquals(Integer.class, c);
		c = parser.parseRaw("12").getValueType(ctx);
		assertEquals(Integer.class, c);
		c = parser.parseRaw("null").getValueType();
		assertNull(c);
		c = parser.parseRaw("null").getValueType(ctx);
		assertNull(c);
		Object o = parser.parseRaw("null").getValue(ctx, Integer.class);
		assertNull(o);
	}

	@Test
	public void whitespace() throws EvaluationException, ParseException {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2      +    3");
		assertEquals(5, expr.getValue());
		expr = parser.parseRaw("2	+	3");
		assertEquals(5, expr.getValue());
		expr = parser.parseRaw("2\n+\t3");
		assertEquals(5, expr.getValue());
		expr = parser.parseRaw("2\r\n+\t3");
		assertEquals(5, expr.getValue());
	}

	@Test
	public void arithmeticPlus1() throws EvaluationException, ParseException {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2+2");
		assertNotNull(expr);
		assertNotNull(expr.getAST());
		assertEquals(4, expr.getValue());
	}

	@Test
	public void arithmeticPlus2() throws EvaluationException, ParseException {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("37+41");
		assertEquals(78, expr.getValue());
	}

	@Test
	public void arithmeticMultiply1() throws EvaluationException, ParseException {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2*3");
		assertNotNull(expr);
		assertNotNull(expr.getAST());
		// printAst(expr.getAST(),0);
		assertEquals(6, expr.getValue());
	}

	@Test
	public void arithmeticPrecedence1() throws EvaluationException, ParseException {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2*3+5");
		assertEquals(11, expr.getValue());
	}

	@Test
	public void generalExpressions() throws Exception {

		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String");
			fail();
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.MISSING_CONSTRUCTOR_ARGS, spe.getMessageCode());
			assertEquals(10, spe.getPosition());
		}

		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String(3,");
			fail();
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.RUN_OUT_OF_ARGUMENTS, spe.getMessageCode());
			assertEquals(10, spe.getPosition());
		}

		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String(3");
			fail();
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.RUN_OUT_OF_ARGUMENTS, spe.getMessageCode());
			assertEquals(10, spe.getPosition());
		}

		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("new String(");
			fail();
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.RUN_OUT_OF_ARGUMENTS, spe.getMessageCode());
			assertEquals(10, spe.getPosition());
		}

		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("\"abc");
			fail();
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING, spe.getMessageCode());
			assertEquals(0, spe.getPosition());
		}

		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw("'abc");
			fail();
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.NON_TERMINATING_QUOTED_STRING, spe.getMessageCode());
			assertEquals(0, spe.getPosition());
		}

	}

	@Test
	public void arithmeticPrecedence2() throws EvaluationException, ParseException {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expr = parser.parseRaw("2+3*5");
		assertEquals(17, expr.getValue());
	}

	@Test
	public void arithmeticPrecedence3() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("3+10/2");
		assertEquals(8, expr.getValue());
	}

	@Test
	public void arithmeticPrecedence4() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("10/2+3");
		assertEquals(8, expr.getValue());
	}

	@Test
	public void arithmeticPrecedence5() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("(4+10)/2");
		assertEquals(7, expr.getValue());
	}

	@Test
	public void arithmeticPrecedence6() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("(3+2)*2");
		assertEquals(10, expr.getValue());
	}

	@Test
	public void booleanOperators() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("true");
		assertEquals(Boolean.TRUE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("false");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("false and false");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("true and (true or false)");
		assertEquals(Boolean.TRUE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("true and true or false");
		assertEquals(Boolean.TRUE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("!true");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("!(false or true)");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
	}

	@Test
	public void booleanOperators_symbolic_spr9614() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("true");
		assertEquals(Boolean.TRUE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("false");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("false && false");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("true && (true || false)");
		assertEquals(Boolean.TRUE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("true && true || false");
		assertEquals(Boolean.TRUE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("!true");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
		expr = new SpelExpressionParser().parseRaw("!(false || true)");
		assertEquals(Boolean.FALSE, expr.getValue(Boolean.class));
	}

	@Test
	public void stringLiterals() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("'howdy'");
		assertEquals("howdy", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("'hello '' world'");
		assertEquals("hello ' world", expr.getValue());
	}

	@Test
	public void stringLiterals2() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("'howdy'.substring(0,2)");
		assertEquals("ho", expr.getValue());
	}

	@Test
	public void testStringLiterals_DoubleQuotes_spr9620() throws Exception {
		SpelExpression expr = new SpelExpressionParser().parseRaw("\"double quote: \"\".\"");
		assertEquals("double quote: \".", expr.getValue());
		expr = new SpelExpressionParser().parseRaw("\"hello \"\" world\"");
		assertEquals("hello \" world", expr.getValue());
	}

	@Test
	public void testStringLiterals_DoubleQuotes_spr9620_2() throws Exception {
		try {
			new SpelExpressionParser().parseRaw("\"double quote: \\\"\\\".\"");
			fail("Should have failed");
		} catch (SpelParseException spe) {
			assertEquals(17, spe.getPosition());
			assertEquals(SpelMessage.UNEXPECTED_ESCAPE_CHAR, spe.getMessageCode());
		}
	}

	@Test
	public void positionalInformation() throws EvaluationException, ParseException {
		SpelExpression expr = new SpelExpressionParser().parseRaw("true and true or false");
		SpelNode rootAst = expr.getAST();
		OpOr operatorOr = (OpOr) rootAst;
		OpAnd operatorAnd = (OpAnd) operatorOr.getLeftOperand();
		SpelNode rightOrOperand = operatorOr.getRightOperand();

		// check position for final 'false'
		assertEquals(17, rightOrOperand.getStartPosition());
		assertEquals(22, rightOrOperand.getEndPosition());

		// check position for first 'true'
		assertEquals(0, operatorAnd.getLeftOperand().getStartPosition());
		assertEquals(4, operatorAnd.getLeftOperand().getEndPosition());

		// check position for second 'true'
		assertEquals(9, operatorAnd.getRightOperand().getStartPosition());
		assertEquals(13, operatorAnd.getRightOperand().getEndPosition());

		// check position for OperatorAnd
		assertEquals(5, operatorAnd.getStartPosition());
		assertEquals(8, operatorAnd.getEndPosition());

		// check position for OperatorOr
		assertEquals(14, operatorOr.getStartPosition());
		assertEquals(16, operatorOr.getEndPosition());
	}

	@Test
	public void tokenKind() {
		TokenKind tk = TokenKind.NOT;
		assertFalse(tk.hasPayload());
		assertEquals("NOT(!)", tk.toString());

		tk = TokenKind.MINUS;
		assertFalse(tk.hasPayload());
		assertEquals("MINUS(-)", tk.toString());

		tk = TokenKind.LITERAL_STRING;
		assertEquals("LITERAL_STRING", tk.toString());
		assertTrue(tk.hasPayload());
	}

	@Test
	public void token() {
		Token token = new Token(TokenKind.NOT, 0, 3);
		assertEquals(TokenKind.NOT, token.kind);
		assertEquals(0, token.startpos);
		assertEquals(3, token.endpos);
		assertEquals("[NOT(!)](0,3)", token.toString());

		token = new Token(TokenKind.LITERAL_STRING, "abc".toCharArray(), 0, 3);
		assertEquals(TokenKind.LITERAL_STRING, token.kind);
		assertEquals(0, token.startpos);
		assertEquals(3, token.endpos);
		assertEquals("[LITERAL_STRING:abc](0,3)", token.toString());
	}

	@Test
	public void exceptions() {
		ExpressionException exprEx = new ExpressionException("test");
		assertEquals("test", exprEx.getSimpleMessage());
		assertEquals("test", exprEx.toDetailedString());
		assertEquals("test", exprEx.getMessage());

		exprEx = new ExpressionException("wibble", "test");
		assertEquals("test", exprEx.getSimpleMessage());
		assertEquals("Expression 'wibble': test", exprEx.toDetailedString());
		assertEquals("Expression 'wibble': test", exprEx.getMessage());

		exprEx = new ExpressionException("wibble", 3, "test");
		assertEquals("test", exprEx.getSimpleMessage());
		assertEquals("Expression 'wibble' @ 3: test", exprEx.toDetailedString());
		assertEquals("Expression 'wibble' @ 3: test", exprEx.getMessage());
	}

	@Test
	public void parseMethodsOnNumbers() {
		checkNumber("3.14.toString()", "3.14", String.class);
		checkNumber("3.toString()", "3", String.class);
	}

	@Test
	public void numerics() {
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

		checkNumberError("0x", SpelMessage.NOT_AN_INTEGER);
		checkNumberError("0xL", SpelMessage.NOT_A_LONG);

		checkNumberError(".324", SpelMessage.UNEXPECTED_DATA_AFTER_DOT);

		checkNumberError("3.4L", SpelMessage.REAL_CANNOT_BE_LONG);

		checkNumber("3.5f", 3.5f, Float.class);

		checkNumber("1.2e3", 1.2e3d, Double.class);
		checkNumber("1.2e+3", 1.2e3d, Double.class);
		checkNumber("1.2e-3", 1.2e-3d, Double.class);
		checkNumber("1.2e3", 1.2e3d, Double.class);
		checkNumber("1e+3", 1e3d, Double.class);
	}

	private void checkNumber(String expression, Object value, Class<?> type) {
		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			SpelExpression expr = parser.parseRaw(expression);
			Object o = expr.getValue();
			assertEquals(value, o);
			assertEquals(type, o.getClass());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void checkNumberError(String expression, SpelMessage expectedMessage) {
		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			parser.parseRaw(expression);
			fail();
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(expectedMessage, spe.getMessageCode());
		}
	}

}
