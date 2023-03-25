/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.expression.spel;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parse some expressions and check we get the AST we expect.
 *
 * <p>Rather than inspecting each node in the AST, we ask it to write itself to
 * a string form and check that is as expected.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
class ParsingTests {

	private final SpelExpressionParser parser = new SpelExpressionParser();


	@Nested
	class Miscellaneous {

		@Test
		void literalNull() {
			parseCheck("null");
		}

		@Test
		void literalDate01() {
			parseCheck("date('1974/08/24')");
		}

		@Test
		void literalDate02() {
			parseCheck("date('19740824T131030','yyyyMMddTHHmmss')");
		}

		@Test
		void mixedOperators() {
			parseCheck("true and 5>3", "(true and (5 > 3))");
		}

		@Test
		void assignmentToVariables() {
			parseCheck("#var1='value1'");
		}

		@Test
		void collectionProcessorsCountStringArray() {
			parseCheck("new String[] {'abc','def','xyz'}.count()");
		}

		@Test
		void collectionProcessorsCountIntArray() {
			parseCheck("new int[] {1,2,3}.count()");
		}

		@Test
		void collectionProcessorsMax() {
			parseCheck("new int[] {1,2,3}.max()");
		}

		@Test
		void collectionProcessorsMin() {
			parseCheck("new int[] {1,2,3}.min()");
		}

		@Test
		void collectionProcessorsAverage() {
			parseCheck("new int[] {1,2,3}.average()");
		}

		@Test
		void collectionProcessorsSort() {
			parseCheck("new int[] {3,2,1}.sort()");
		}

		@Test
		void collectionProcessorsNonNull() {
			parseCheck("{'a','b',null,'d',null}.nonNull()");
		}

		@Test
		void collectionProcessorsDistinct() {
			parseCheck("{'a','b','a','d','e'}.distinct()");
		}

		@Disabled("Unsupported syntax/feature")
		@Test
		void lambdaMax() {
			parseCheck("(#max = {|x,y| $x > $y ? $x : $y }; #max(5,25))",
					"(#max={|x,y| ($x > $y) ? $x : $y };#max(5,25))");
		}

		@Disabled("Unsupported syntax/feature")
		@Test
		void lambdaFactorial() {
			parseCheck("(#fact = {|n| $n <= 1 ? 1 : $n * #fact($n-1) }; #fact(5))",
					"(#fact={|n| ($n <= 1) ? 1 : ($n * #fact(($n - 1))) };#fact(5))");
		}

		@Disabled("Unsupported syntax/feature")
		@Test
		void projection() {
			parseCheck("{1,2,3,4,5,6,7,8,9,10}.!{#isEven()}");
		}

		@Disabled("Unsupported syntax/feature")
		@Test
		void selection() {
			parseCheck("{1,2,3,4,5,6,7,8,9,10}.?{#isEven(#this) == 'y'}",
					"{1,2,3,4,5,6,7,8,9,10}.?{(#isEven(#this) == 'y')}");
		}

		@Disabled("Unsupported syntax/feature")
		@Test
		void selectionFirst() {
			parseCheck("{1,2,3,4,5,6,7,8,9,10}.^{#isEven(#this) == 'y'}",
					"{1,2,3,4,5,6,7,8,9,10}.^{(#isEven(#this) == 'y')}");
		}

		@Disabled("Unsupported syntax/feature")
		@Test
		void selectionLast() {
			parseCheck("{1,2,3,4,5,6,7,8,9,10}.${#isEven(#this) == 'y'}",
					"{1,2,3,4,5,6,7,8,9,10}.${(#isEven(#this) == 'y')}");
		}
	}

	@Nested
	class LiteralBooleans {

		@Test
		void literalBooleanFalse() {
			parseCheck("false");
		}

		@Test
		void literalBooleanTrue() {
			parseCheck("true");
		}

		@Test
		void literalBooleanNotTrue() {
			parseCheck("!true");
			parseCheck("not true", "!true");
		}
	}

	@Nested
	class LiteralNumbers {

		@Test
		void literalLong() {
			parseCheck("37L", "37");
		}

		@Test
		void literalIntegers() {
			parseCheck("1");
			parseCheck("1415");
		}

		@Test
		void literalReal() {
			parseCheck("6.0221415E+23", "6.0221415E23");
		}

		@Test
		void literalHex() {
			parseCheck("0x7FFFFFFF", "2147483647");
		}
	}

	@Nested
	class LiteralStrings {

		@Test
		void insideSingleQuotes() {
			parseCheck("'hello'");
			parseCheck("'hello world'");
		}

		@Test
		void insideDoubleQuotes() {
			parseCheck("\"hello\"", "'hello'");
			parseCheck("\"hello world\"", "'hello world'");
		}

		@Test
		void singleQuotesInsideSingleQuotes() {
			parseCheck("'Tony''s Pizza'");
			parseCheck("'big ''''pizza'''' parlor'");
		}

		@Test
		void doubleQuotesInsideDoubleQuotes() {
			parseCheck("\"big \"\"pizza\"\" parlor\"", "'big \"pizza\" parlor'");
			parseCheck("\"big \"\"\"\"pizza\"\"\"\" parlor\"", "'big \"\"pizza\"\" parlor'");
		}

		@Test
		void singleQuotesInsideDoubleQuotes() {
			parseCheck("\"Tony's Pizza\"", "'Tony''s Pizza'");
			parseCheck("\"big ''pizza'' parlor\"", "'big ''''pizza'''' parlor'");
		}

		@Test
		void doubleQuotesInsideSingleQuotes() {
			parseCheck("'big \"pizza\" parlor'");
			parseCheck("'two double \"\" quotes'");
		}

		@Test
		void inCompoundExpressions() {
			parseCheck("'123''4' == '123''4'", "('123''4' == '123''4')");
			parseCheck("('123''4'=='123''4')", "('123''4' == '123''4')");
			parseCheck("\"123\"\"4\" == \"123\"\"4\"", "('123\"4' == '123\"4')");
		}
	}

	@Nested
	class BooleanOperators {

		@Test
		void booleanOperatorsOr01() {
			parseCheck("false or false", "(false or false)");
		}

		@Test
		void booleanOperatorsOr02() {
			parseCheck("false or true", "(false or true)");
		}

		@Test
		void booleanOperatorsOr03() {
			parseCheck("true or false", "(true or false)");
		}

		@Test
		void booleanOperatorsOr04() {
			parseCheck("true or false", "(true or false)");
		}

		@Test
		void booleanOperatorsMix() {
			parseCheck("false or true and false", "(false or (true and false))");
		}
	}

	@Nested
	class RelationalOperators {

		@Test
		void relOperatorsGT() {
			parseCheck("3>6", "(3 > 6)");
		}

		@Test
		void relOperatorsLT() {
			parseCheck("3<6", "(3 < 6)");
		}

		@Test
		void relOperatorsLE() {
			parseCheck("3<=6", "(3 <= 6)");
		}

		@Test
		void relOperatorsGE01() {
			parseCheck("3>=6", "(3 >= 6)");
		}

		@Test
		void relOperatorsGE02() {
			parseCheck("3>=3", "(3 >= 3)");
		}

		@Disabled("Unsupported syntax/feature")
		@Test
		void relOperatorsIn() {
			parseCheck("3 in {1,2,3,4,5}", "(3 in {1,2,3,4,5})");
		}

		@Test
		void relOperatorsBetweenNumbers() {
			parseCheck("1 between {1, 5}", "(1 between {1,5})");
		}

		@Test
		void relOperatorsBetweenStrings() {
			parseCheck("'efg' between {'abc', 'xyz'}", "('efg' between {'abc','xyz'})");
		}

		@Test
		void relOperatorsInstanceOfInt() {
			parseCheck("'xyz' instanceof int", "('xyz' instanceof int)");
		}

		@Test
		void relOperatorsInstanceOfList() {
			parseCheck("{1, 2, 3, 4, 5} instanceof List", "({1,2,3,4,5} instanceof List)");
		}

		@Test
		void relOperatorsMatches() {
			parseCheck("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'", "('5.0067' matches '^-?\\d+(\\.\\d{2})?$')");
			parseCheck("'5.00' matches '^-?\\d+(\\.\\d{2})?$'", "('5.00' matches '^-?\\d+(\\.\\d{2})?$')");
		}
	}

	@Nested
	class MathematicalOperators {

		@Test
		void mathOperatorsAddIntegers() {
			parseCheck("2+4", "(2 + 4)");
		}

		@Test
		void mathOperatorsAddStrings() {
			parseCheck("'a'+'b'", "('a' + 'b')");
		}

		@Test
		void mathOperatorsAddMultipleStrings() {
			parseCheck("'hello'+' '+'world'", "(('hello' + ' ') + 'world')");
		}

		@Test
		void mathOperatorsSubtract() {
			parseCheck("5-4", "(5 - 4)");
		}

		@Test
		void mathOperatorsMultiply() {
			parseCheck("7*4", "(7 * 4)");
		}

		@Test
		void mathOperatorsDivide() {
			parseCheck("8/4", "(8 / 4)");
		}

		@Test
		void mathOperatorModulus() {
			parseCheck("7 % 4", "(7 % 4)");
		}
	}

	@Nested
	class References {

		@Test
		void references() {
			parseCheck("@foo");
			parseCheck("@'foo.bar'");
			parseCheck("@\"foo.bar.goo\"" , "@'foo.bar.goo'");
			parseCheck("@$$foo");
		}
	}

	@Nested
	class Properties {

		@Test
		void propertiesSingle() {
			parseCheck("name");
		}

		@Test
		void propertiesDouble() {
			parseCheck("placeofbirth.CitY");
		}

		@Test
		void propertiesMultiple() {
			parseCheck("a.b.c.d.e");
		}
	}

	@Nested
	class InlineCollections {

		@Test
		void inlineListOfIntegers() {
			parseCheck("{1,2,3,4}");
			parseCheck("{1, 2, 3, 4, 5}", "{1,2,3,4,5}");
		}

		@Test
		void inlineListOfStrings() {
			parseCheck("{'abc','xyz'}", "{'abc','xyz'}");
			parseCheck("{\"abc\",  'xyz'}", "{'abc','xyz'}");
		}

		@Test
		void inlineMapStringToObject() {
			parseCheck("{'key1':'Value 1','today':DateTime.Today}");
		}

		@Test
		void inlineMapIntegerToString() {
			parseCheck("{1:'January',2:'February',3:'March'}");
		}
	}

	@Nested
	class MethodsConstructorsAndArrays {

		@Test
		void methods() {
			parseCheck("echo()");
			parseCheck("echo(12)");
			parseCheck("echo(name)");
			parseCheck("echo('Jane')");
			parseCheck("echo('Jane',32)");
			parseCheck("echo('Jane', 32)", "echo('Jane',32)");
			parseCheck("age.doubleItAndAdd(12)");
		}

		@Test
		void constructorWithNoArguments() {
			parseCheck("new Foo()");
			parseCheck("new example.Foo()");
		}

		@Test
		void constructorWithOneArgument() {
			parseCheck("new String('hello')");
			parseCheck("new String( 'hello' )", "new String('hello')");
			parseCheck("new String(\"hello\" )", "new String('hello')");
		}

		@Test
		void constructorWithMultipleArguments() {
			parseCheck("new example.Person('Jane',32,true)");
			parseCheck("new example.Person('Jane', 32, true)", "new example.Person('Jane',32,true)");
			parseCheck("new example.Person('Jane', 2 * 16, true)", "new example.Person('Jane',(2 * 16),true)");
		}

		@Test
		void arrayConstructionWithOneDimensionalReferenceType() {
			parseCheck("new String[3]");
		}

		@Test
		void arrayConstructionWithOneDimensionalFullyQualifiedReferenceType() {
			parseCheck("new java.lang.String[3]");
		}

		@Test
		void arrayConstructionWithOneDimensionalPrimitiveType() {
			parseCheck("new int[3]");
		}

		@Test
		void arrayConstructionWithMultiDimensionalReferenceType() {
			parseCheck("new Float[3][4]");
		}

		@Test
		void arrayConstructionWithMultiDimensionalPrimitiveType() {
			parseCheck("new int[3][4]");
		}

		@Test
		void arrayConstructionWithOneDimensionalReferenceTypeWithInitializer() {
			parseCheck("new String[] {'abc','xyz'}");
			parseCheck("new String[] {'abc', 'xyz'}", "new String[] {'abc','xyz'}");
		}

		@Test
		void arrayConstructionWithOneDimensionalPrimitiveTypeWithInitializer() {
			parseCheck("new int[] {1,2,3,4,5}");
			parseCheck("new int[] {1, 2, 3, 4, 5}", "new int[] {1,2,3,4,5}");
		}
	}

	@Nested
	class VariablesAndFunctions {

		@Test
		void variables() {
			parseCheck("#foo");
		}

		@Test
		void functions() {
			parseCheck("#fn(1,2,3)");
			parseCheck("#fn('hello')");
		}
	}

	@Nested
	class ElvisAndTernaryOperators {

		@Test
		void elvis() {
			parseCheck("3?:1", "(3 ?: 1)");
			parseCheck("(2*3)?:1*10", "((2 * 3) ?: (1 * 10))");
			parseCheck("((2*3)?:1)*10", "(((2 * 3) ?: 1) * 10)");
		}

		@Test
		void ternary() {
			parseCheck("1>2?3:4", "((1 > 2) ? 3 : 4)");
			parseCheck("(a ? 1 : 0) * 10", "((a ? 1 : 0) * 10)");
			parseCheck("(a?1:0)*10", "((a ? 1 : 0) * 10)");
			parseCheck("(4 % 2 == 0 ? 1 : 0) * 10", "((((4 % 2) == 0) ? 1 : 0) * 10)");
			parseCheck("((4 % 2 == 0) ? 1 : 0) * 10", "((((4 % 2) == 0) ? 1 : 0) * 10)");
			parseCheck("{1}.#isEven(#this) == 'y'?'it is even':'it is odd'",
					"(({1}.#isEven(#this) == 'y') ? 'it is even' : 'it is odd')");
		}
	}

	@Nested
	class TypeReferences {

		@Test
		void typeReferences01() {
			parseCheck("T(java.lang.String)");
		}

		@Test
		void typeReferences02() {
			parseCheck("T(String)");
		}
	}


	/**
	 * Parse the supplied expression and then create a string representation of the resultant AST, it should be the same
	 * as the original expression.
	 *
	 * @param expression the expression to parse *and* the expected value of the string form of the resultant AST
	 */
	private void parseCheck(String expression) {
		parseCheck(expression, expression);
	}

	/**
	 * Parse the supplied expression and then create a string representation of the resultant AST, it should be the
	 * expected value.
	 *
	 * @param expression the expression to parse
	 * @param expectedStringFormOfAST the expected string form of the AST
	 */
	private void parseCheck(String expression, String expectedStringFormOfAST) {
		SpelExpression e = parser.parseRaw(expression);
		assertThat(e).isNotNull();
		assertThat(e.toStringAST()).isEqualTo(expectedStringFormOfAST);
	}

}
