/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;

/**
 * Tests the evaluation of real expressions in a real context.
 * 
 * @author Andy Clement
 */
public class EvaluationTests extends ExpressionTestCase {

	public void testRelOperatorGT01() {
		evaluate("3 > 6", "false", Boolean.class);
	}

	public void testRelOperatorLT01() {
		evaluate("3 < 6", "true", Boolean.class);
	}

	public void testRelOperatorLE01() {
		evaluate("3 <= 6", "true", Boolean.class);
	}

	public void testRelOperatorGE01() {
		evaluate("3 >= 6", "false", Boolean.class);
	}

	public void testRelOperatorGE02() {
		evaluate("3 >= 3", "true", Boolean.class);
	}
	
	public void testRelOperatorsIs01() {
		evaluate("'xyz' instanceof T(int)", "false", Boolean.class);
	}

	public void testRelOperatorsIs04() {
		evaluate("null instanceof T(String)", "false", Boolean.class);
	}

	public void testRelOperatorsIs05() {
		evaluate("null instanceof T(Integer)", "false", Boolean.class);
	}

	public void testRelOperatorsIs06() {
		evaluateAndCheckError("'A' instanceof null", SpelMessages.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND, 15, "null");
	}

	public void testRelOperatorsMatches01() {
		evaluate("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'", "false", Boolean.class);
	}

	public void testRelOperatorsMatches02() {
		evaluate("'5.00' matches '^-?\\d+(\\.\\d{2})?$'", "true", Boolean.class);
	}

	public void testRelOperatorsMatches03() {
		evaluateAndCheckError("null matches '^.*$'", SpelMessages.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, 0, null);
	}

	public void testRelOperatorsMatches04() {
		evaluateAndCheckError("'abc' matches null", SpelMessages.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, 14, null);
	}

	public void testRelOperatorsMatches05() {
		evaluate("27 matches '^.*2.*$'", true, Boolean.class); // conversion int>string
	}

	// mixing operators
	public void testMixingOperators01() {
		evaluate("true and 5>3", "true", Boolean.class);
	}

	// property access
	public void testPropertyField01() {
		evaluate("name", "Nikola Tesla", String.class, false); 
		// not writable because (1) name is private (2) there is no setter, only a getter
		evaluateAndCheckError("madeup", SpelMessages.PROPERTY_OR_FIELD_NOT_READABLE, 0, "madeup",
				"org.springframework.expression.spel.testresources.Inventor");
	}

	// nested properties
	public void testPropertiesNested01() {
		evaluate("placeOfBirth.city", "SmilJan", String.class, true);
	}

	public void testPropertiesNested02() {
		evaluate("placeOfBirth.doubleIt(12)", "24", Integer.class);
	}
	
	// methods
	public void testMethods01() {
		evaluate("echo(12)", "12", String.class);
	}

	public void testMethods02() {
		evaluate("echo(name)", "Nikola Tesla", String.class);
	}

	// constructors
	public void testConstructorInvocation01() {
		evaluate("new String('hello')", "hello", String.class);
	}

	public void testConstructorInvocation05() {
		evaluate("new java.lang.String('foobar')", "foobar", String.class);
	}
	
	public void testConstructorInvocation06() throws Exception {
		// repeated evaluation to drive use of cached executor
		SpelExpression expr = (SpelExpression)parser.parseExpression("new String('wibble')");
		String newString = expr.getValue(String.class);
		assertEquals("wibble",newString);
		newString = expr.getValue(String.class);
		assertEquals("wibble",newString);
		
		// not writable
		assertFalse(expr.isWritable(new StandardEvaluationContext()));
		
		// ast
		assertEquals("new String('wibble')",expr.toStringAST());
	}

	// unary expressions
	public void testUnaryMinus01() {
		evaluate("-5", "-5", Integer.class);
	}

	public void testUnaryPlus01() {
		evaluate("+5", "5", Integer.class);
	}

	public void testUnaryNot01() {
		evaluate("!true", "false", Boolean.class);
	}
	// assignment
	public void testAssignmentToVariables01() {
		evaluate("#var1='value1'", "value1", String.class);
	}

	public void testTernaryOperator01() {
		evaluate("2>4?1:2",2,Integer.class);
	}

	public void testTernaryOperator02() {
		evaluate("'abc'=='abc'?1:2",1,Integer.class);
	}
	
	public void testTernaryOperator03() {
		evaluateAndCheckError("'hello'?1:2", SpelMessages.TYPE_CONVERSION_ERROR); // cannot convert String to boolean
	}

	public void testTernaryOperator04() throws Exception {
		Expression expr = parser.parseExpression("1>2?3:4");
		assertFalse(expr.isWritable(eContext));
	}

	public void testIndexer03() {
		evaluate("'christian'[8]", "n", String.class);
	}
	
	public void testIndexerError() {
		evaluateAndCheckError("new org.springframework.expression.spel.testresources.Inventor().inventions[1]",SpelMessages.CANNOT_INDEX_INTO_NULL_VALUE);
	}

	public void testStaticRef02() {
		evaluate("T(java.awt.Color).green.getRGB()!=0", "true", Boolean.class);
	}

	// variables and functions
	public void testVariableAccess01() {
		evaluate("#answer", "42", Integer.class, true);
	}

	public void testFunctionAccess01() {
		evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
	}

	public void testFunctionAccess02() {
		evaluate("#reverseString('hello')", "olleh", String.class);
	}

	// type references
	public void testTypeReferences01() {
		evaluate("T(java.lang.String)", "class java.lang.String", Class.class);
	}
	
	public void testTypeReferencesAndQualifiedIdentifierCaching() throws Exception {
		SpelExpression expr = (SpelExpression)parser.parseExpression("T(java.lang.String)");
		assertFalse(expr.isWritable(new StandardEvaluationContext()));
		assertEquals("T(java.lang.String)",expr.toStringAST());
		assertEquals(String.class,expr.getValue(Class.class));
		// use cached QualifiedIdentifier:
		assertEquals("T(java.lang.String)",expr.toStringAST());
		assertEquals(String.class,expr.getValue(Class.class));
	}

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

	public void testTypeReferences02() {
		evaluate("T(String)", "class java.lang.String", Class.class);
	}

	public void testStringType() {
		evaluateAndAskForReturnType("getPlaceOfBirth().getCity()", "SmilJan", String.class);
	}

	public void testNumbers01() {
		evaluateAndAskForReturnType("3*4+5", 17, Integer.class);
		evaluateAndAskForReturnType("3*4+5", 17L, Long.class);
		evaluateAndAskForReturnType("65", 'A', Character.class);
		evaluateAndAskForReturnType("3*4+5", (short) 17, Short.class);
		evaluateAndAskForReturnType("3*4+5", "17", String.class);
	}

	
	public void testAdvancedNumerics() throws Exception {
		int twentyFour = parser.parseExpression("2.0 * 3e0 * 4").getValue(Integer.class);
		assertEquals(24,twentyFour);
		double one = parser.parseExpression("8.0 / 5e0 % 2").getValue(Double.class);
		assertEquals(1.6d,one);
		int o = parser.parseExpression("8.0 / 5e0 % 2").getValue(Integer.class);
		assertEquals(1,o);
		int sixteen = parser.parseExpression("-2 ^ 4").getValue(Integer.class);
		assertEquals(16,sixteen);
		int minusFortyFive = parser.parseExpression("1+2-3*8^2/2/2").getValue(Integer.class);
		assertEquals(-45,minusFortyFive);
	}
	
	public void testComparison() throws Exception {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		boolean trueValue = parser.parseExpression("T(java.util.Date) == Birthdate.Class").getValue(context, Boolean.class);
		assertTrue(trueValue);
	}
	
	public void testResolvingList() throws Exception {
		StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		try {
			assertFalse(parser.parseExpression("T(List)!=null").getValue(context, Boolean.class));
			fail("should have failed to find List");
		} catch (EvaluationException ee) {
			// success - List not found
		}
		((StandardTypeLocator)context.getTypeLocator()).registerImport("java.util");
		assertTrue(parser.parseExpression("T(List)!=null").getValue(context, Boolean.class));
	}
	
	public void testResolvingString() throws Exception {
		Class stringClass = parser.parseExpression("T(String)").getValue(Class.class);
		assertEquals(String.class,stringClass);
	}
}
