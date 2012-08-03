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

package org.springframework.expression.spel;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.expression.spel.testresources.TestPerson;

/**
 * Tests the evaluation of real expressions in a real context.
 * 
 * @author Andy Clement
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 3.0
 */
public class EvaluationTests extends ExpressionTestCase {

	@Test
	public void testCreateListsOnAttemptToIndexNull01() throws EvaluationException, ParseException {
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression expression = parser.parseExpression("list[0]");
		TestClass testClass = new TestClass();
		Object o = null;
		o = expression.getValue(new StandardEvaluationContext(testClass));
		assertEquals("", o);
		o = parser.parseExpression("list[3]").getValue(new StandardEvaluationContext(testClass));
		assertEquals("", o);
		assertEquals(4, testClass.list.size());
		try {
			o = parser.parseExpression("list2[3]").getValue(new StandardEvaluationContext(testClass));
			fail();
		} catch (EvaluationException ee) {
			ee.printStackTrace();
			// success!
		}
		o = parser.parseExpression("foo[3]").getValue(new StandardEvaluationContext(testClass));
		assertEquals("", o);
		assertEquals(4, testClass.getFoo().size());
	}

	@Test(expected = SpelEvaluationException.class)
	public void testCreateMapsOnAttemptToIndexNull01() throws Exception {
		TestClass testClass = new TestClass();
		StandardEvaluationContext ctx = new StandardEvaluationContext(testClass);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Object o = null;
		o = parser.parseExpression("map['a']").getValue(ctx);
		assertNull(o);
		o = parser.parseExpression("map").getValue(ctx);
		assertNotNull(o);

		o = parser.parseExpression("map2['a']").getValue(ctx);
		// map2 should be null, there is no setter
	}

	// wibble2 should be null (cannot be initialized dynamically), there is no setter
	@Test(expected = SpelEvaluationException.class)
	public void testCreateObjectsOnAttemptToReferenceNull() throws Exception {
		TestClass testClass = new TestClass();
		StandardEvaluationContext ctx = new StandardEvaluationContext(testClass);
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Object o = null;
		o = parser.parseExpression("wibble.bar").getValue(ctx);
		assertEquals("hello", o);
		o = parser.parseExpression("wibble").getValue(ctx);
		assertNotNull(o);

		o = parser.parseExpression("wibble2.bar").getValue(ctx);
	}


	static class TestClass {

		public Foo wibble;
		private Foo wibble2;
		public Map map;
		public Map<String, Integer> mapStringToInteger;
		public List<String> list;
		public List list2;
		private Map map2;
		private List<String> foo;

		public Map getMap2() { return this.map2; }
		public Foo getWibble2() { return this.wibble2; }
		public List<String> getFoo() { return this.foo; }
		public void setFoo(List<String> newfoo) { this.foo = newfoo; }
	}

	public static class Foo {
		public Foo() {}
		public String bar = "hello";
	}


	@Test
	public void testElvis01() {
		evaluate("'Andy'?:'Dave'", "Andy", String.class);
		evaluate("null?:'Dave'", "Dave", String.class);
	}

	@Test
	public void testSafeNavigation() {
		evaluate("null?.null?.null", null, null);
	}

	@Test
	public void testRelOperatorGT01() {
		evaluate("3 > 6", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorLT01() {
		evaluate("3 < 6", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorLE01() {
		evaluate("3 <= 6", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorGE01() {
		evaluate("3 >= 6", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorGE02() {
		evaluate("3 >= 3", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof01() {
		evaluate("'xyz' instanceof T(int)", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof04() {
		evaluate("null instanceof T(String)", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof05() {
		evaluate("null instanceof T(Integer)", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsInstanceof06() {
		evaluateAndCheckError("'A' instanceof null", SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND, 15, "null");
	}

	@Test
	public void testRelOperatorsMatches01() {
		evaluate("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'", "false", Boolean.class);
	}

	@Test
	public void testRelOperatorsMatches02() {
		evaluate("'5.00' matches '^-?\\d+(\\.\\d{2})?$'", "true", Boolean.class);
	}

	@Test
	public void testRelOperatorsMatches03() {
		evaluateAndCheckError("null matches '^.*$'", SpelMessage.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, 0, null);
	}

	@Test
	public void testRelOperatorsMatches04() {
		evaluateAndCheckError("'abc' matches null", SpelMessage.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, 14, null);
	}

	@Test
	public void testRelOperatorsMatches05() {
		evaluate("27 matches '^.*2.*$'", true, Boolean.class); // conversion int>string
	}

	// mixing operators
	@Test
	public void testMixingOperators01() {
		evaluate("true and 5>3", "true", Boolean.class);
	}

	// property access
	@Test
	public void testPropertyField01() {
		evaluate("name", "Nikola Tesla", String.class, false);
		// not writable because (1) name is private (2) there is no setter, only a getter
		evaluateAndCheckError("madeup", SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, 0, "madeup",
			"org.springframework.expression.spel.testresources.Inventor");
	}

	@Test
	public void testPropertyField02_SPR7100() {
		evaluate("_name", "Nikola Tesla", String.class);
		evaluate("_name_", "Nikola Tesla", String.class);
	}

	@Test
	public void testRogueTrailingDotCausesNPE_SPR6866() {
		try {
			new SpelExpressionParser().parseExpression("placeOfBirth.foo.");
			fail("Should have failed to parse");
		} catch (ParseException e) {
			assertTrue(e instanceof SpelParseException);
			SpelParseException spe = (SpelParseException) e;
			assertEquals(SpelMessage.OOD, spe.getMessageCode());
			assertEquals(16, spe.getPosition());
		}
	}

	// nested properties
	@Test
	public void testPropertiesNested01() {
		evaluate("placeOfBirth.city", "SmilJan", String.class, true);
	}

	@Test
	public void testPropertiesNested02() {
		evaluate("placeOfBirth.doubleIt(12)", "24", Integer.class);
	}

	@Test
	public void testPropertiesNested03() throws ParseException {
		try {
			new SpelExpressionParser().parseRaw("placeOfBirth.23");
			fail();
		} catch (SpelParseException spe) {
			assertEquals(spe.getMessageCode(), SpelMessage.UNEXPECTED_DATA_AFTER_DOT);
			assertEquals("23", spe.getInserts()[0]);
		}
	}

	// methods
	@Test
	public void testMethods01() {
		evaluate("echo(12)", "12", String.class);
	}

	@Test
	public void testMethods02() {
		evaluate("echo(name)", "Nikola Tesla", String.class);
	}

	// constructors
	@Test
	public void testConstructorInvocation01() {
		evaluate("new String('hello')", "hello", String.class);
	}

	@Test
	public void testConstructorInvocation05() {
		evaluate("new java.lang.String('foobar')", "foobar", String.class);
	}

	@Test
	public void testConstructorInvocation06() throws Exception {
		// repeated evaluation to drive use of cached executor
		SpelExpression expr = (SpelExpression) parser.parseExpression("new String('wibble')");
		String newString = expr.getValue(String.class);
		assertEquals("wibble", newString);
		newString = expr.getValue(String.class);
		assertEquals("wibble", newString);

		// not writable
		assertFalse(expr.isWritable(new StandardEvaluationContext()));

		// ast
		assertEquals("new String('wibble')", expr.toStringAST());
	}

	// unary expressions
	@Test
	public void testUnaryMinus01() {
		evaluate("-5", "-5", Integer.class);
	}

	@Test
	public void testUnaryPlus01() {
		evaluate("+5", "5", Integer.class);
	}

	@Test
	public void testUnaryNot01() {
		evaluate("!true", "false", Boolean.class);
	}

	@Test
	public void testUnaryNot02() {
		evaluate("!false", "true", Boolean.class);
	}

	@Test(expected = EvaluationException.class)
	public void testUnaryNotWithNullValue() {
		parser.parseExpression("!null").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testAndWithNullValueOnLeft() {
		parser.parseExpression("null and true").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testAndWithNullValueOnRight() {
		parser.parseExpression("true and null").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testOrWithNullValueOnLeft() {
		parser.parseExpression("null or false").getValue();
	}

	@Test(expected = EvaluationException.class)
	public void testOrWithNullValueOnRight() {
		parser.parseExpression("false or null").getValue();
	}

	// assignment
	@Test
	public void testAssignmentToVariables01() {
		evaluate("#var1='value1'", "value1", String.class);
	}

	@Test
	public void testTernaryOperator01() {
		evaluate("2>4?1:2", 2, Integer.class);
	}

	@Test
	public void testTernaryOperator02() {
		evaluate("'abc'=='abc'?1:2", 1, Integer.class);
	}

	@Test
	public void testTernaryOperator03() {
		// cannot convert String to boolean
		evaluateAndCheckError("'hello'?1:2", SpelMessage.TYPE_CONVERSION_ERROR);
	}

	@Test
	public void testTernaryOperator04() throws Exception {
		Expression expr = parser.parseExpression("1>2?3:4");
		assertFalse(expr.isWritable(eContext));
	}

	@Test
	public void testTernaryOperator05() {
		evaluate("1>2?#var=4:#var=5", 5, Integer.class);
		evaluate("3?:#var=5", 3, Integer.class);
		evaluate("null?:#var=5", 5, Integer.class);
		evaluate("2>4?(3>2?true:false):(5<3?true:false)", false, Boolean.class);
	}

	@Test(expected = EvaluationException.class)
	public void testTernaryOperatorWithNullValue() {
		parser.parseExpression("null ? 0 : 1").getValue();
	}

	@Test
	public void methodCallWithRootReferenceThroughParameter() {
		evaluate("placeOfBirth.doubleIt(inventions.length)", 18, Integer.class);
	}

	@Test
	public void ctorCallWithRootReferenceThroughParameter() {
		evaluate("new org.springframework.expression.spel.testresources.PlaceOfBirth(inventions[0].toString()).city",
			"Telephone repeater", String.class);
	}

	@Test
	public void fnCallWithRootReferenceThroughParameter() {
		evaluate("#reverseInt(inventions.length, inventions.length, inventions.length)", "int[3]{9,9,9}", int[].class);
	}

	@Test
	public void methodCallWithRootReferenceThroughParameterThatIsAFunctionCall() {
		evaluate("placeOfBirth.doubleIt(#reverseInt(inventions.length,2,3)[2])", 18, Integer.class);
	}

	@Test
	public void testIndexer03() {
		evaluate("'christian'[8]", "n", String.class);
	}

	@Test
	public void testIndexerError() {
		evaluateAndCheckError("new org.springframework.expression.spel.testresources.Inventor().inventions[1]",
			SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
	}

	@Test
	public void testStaticRef02() {
		evaluate("T(java.awt.Color).green.getRGB()!=0", "true", Boolean.class);
	}

	// variables and functions
	@Test
	public void testVariableAccess01() {
		evaluate("#answer", "42", Integer.class, true);
	}

	@Test
	public void testFunctionAccess01() {
		evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
	}

	@Test
	public void testFunctionAccess02() {
		evaluate("#reverseString('hello')", "olleh", String.class);
	}

	// type references
	@Test
	public void testTypeReferences01() {
		evaluate("T(java.lang.String)", "class java.lang.String", Class.class);
	}

	@Test
	public void testTypeReferencesAndQualifiedIdentifierCaching() throws Exception {
		SpelExpression expr = (SpelExpression) parser.parseExpression("T(java.lang.String)");
		assertFalse(expr.isWritable(new StandardEvaluationContext()));
		assertEquals("T(java.lang.String)", expr.toStringAST());
		assertEquals(String.class, expr.getValue(Class.class));
		// use cached QualifiedIdentifier:
		assertEquals("T(java.lang.String)", expr.toStringAST());
		assertEquals(String.class, expr.getValue(Class.class));
	}

	@Test
	public void testTypeReferencesPrimitive() {
		evaluate("T(int)", "int", Class.class);
		evaluate("T(byte)", "byte", Class.class);
		evaluate("T(char)", "char", Class.class);
		evaluate("T(boolean)", "boolean", Class.class);
		evaluate("T(long)", "long", Class.class);
		evaluate("T(short)", "short", Class.class);
		evaluate("T(double)", "double", Class.class);
		evaluate("T(float)", "float", Class.class);
	}

	@Test
	public void testTypeReferences02() {
		evaluate("T(String)", "class java.lang.String", Class.class);
	}

	@Test
	public void testStringType() {
		evaluateAndAskForReturnType("getPlaceOfBirth().getCity()", "SmilJan", String.class);
	}

	@Test
	public void testNumbers01() {
		evaluateAndAskForReturnType("3*4+5", 17, Integer.class);
		evaluateAndAskForReturnType("3*4+5", 17L, Long.class);
		evaluateAndAskForReturnType("65", 'A', Character.class);
		evaluateAndAskForReturnType("3*4+5", (short) 17, Short.class);
		evaluateAndAskForReturnType("3*4+5", "17", String.class);
	}

	@Test
	public void testAdvancedNumerics() throws Exception {
		int twentyFour = parser.parseExpression("2.0 * 3e0 * 4").getValue(Integer.class);
		assertEquals(24, twentyFour);
		double one = parser.parseExpression("8.0 / 5e0 % 2").getValue(Double.class);
		assertEquals(1.6d, one, 0);
		int o = parser.parseExpression("8.0 / 5e0 % 2").getValue(Integer.class);
		assertEquals(1, o);
		int sixteen = parser.parseExpression("-2 ^ 4").getValue(Integer.class);
		assertEquals(16, sixteen);
		int minusFortyFive = parser.parseExpression("1+2-3*8^2/2/2").getValue(Integer.class);
		assertEquals(-45, minusFortyFive);
	}

	@Test
	public void testComparison() throws Exception {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		boolean trueValue = parser.parseExpression("T(java.util.Date) == Birthdate.Class").getValue(context,
			Boolean.class);
		assertTrue(trueValue);
	}

	@Test
	public void testResolvingList() throws Exception {
		StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		try {
			assertFalse(parser.parseExpression("T(List)!=null").getValue(context, Boolean.class));
			fail("should have failed to find List");
		} catch (EvaluationException ee) {
			// success - List not found
		}
		((StandardTypeLocator) context.getTypeLocator()).registerImport("java.util");
		assertTrue(parser.parseExpression("T(List)!=null").getValue(context, Boolean.class));
	}

	@Test
	public void testResolvingString() throws Exception {
		Class<?> stringClass = parser.parseExpression("T(String)").getValue(Class.class);
		assertEquals(String.class, stringClass);
	}

	/**
	 * SPR-6984: attempting to index a collection on write using an index that
	 * doesn't currently exist in the collection (address.crossStreets[0] below)
	 */
	@Test
	public void initializingCollectionElementsOnWrite() throws Exception {
		TestPerson person = new TestPerson();
		EvaluationContext context = new StandardEvaluationContext(person);
		SpelParserConfiguration config = new SpelParserConfiguration(true, true);
		ExpressionParser parser = new SpelExpressionParser(config);
		Expression expression = parser.parseExpression("name");
		expression.setValue(context, "Oleg");
		assertEquals("Oleg", person.getName());

		expression = parser.parseExpression("address.street");
		expression.setValue(context, "123 High St");
		assertEquals("123 High St", person.getAddress().getStreet());

		expression = parser.parseExpression("address.crossStreets[0]");
		expression.setValue(context, "Blah");
		assertEquals("Blah", person.getAddress().getCrossStreets().get(0));

		expression = parser.parseExpression("address.crossStreets[3]");
		expression.setValue(context, "Wibble");
		assertEquals("Blah", person.getAddress().getCrossStreets().get(0));
		assertEquals("Wibble", person.getAddress().getCrossStreets().get(3));
	}

	/**
	 * Verifies behavior requested in SPR-9613.
	 */
	@Test
	public void caseInsensitiveNullLiterals() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp;

		exp = parser.parseExpression("null");
		assertNull(exp.getValue());

		exp = parser.parseExpression("NULL");
		assertNull(exp.getValue());

		exp = parser.parseExpression("NuLl");
		assertNull(exp.getValue());
	}

}
