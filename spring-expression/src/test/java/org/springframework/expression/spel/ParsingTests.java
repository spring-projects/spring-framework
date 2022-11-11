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


	// literals
	@Test
	void literalBoolean01() {
		parseCheck("false");
	}

	@Test
	void literalLong01() {
		parseCheck("37L", "37");
	}

	@Test
	void literalBoolean02() {
		parseCheck("true");
	}

	@Test
	void literalBoolean03() {
		parseCheck("!true");
	}

	@Test
	void literalInteger01() {
		parseCheck("1");
	}

	@Test
	void literalInteger02() {
		parseCheck("1415");
	}

	@Test
	void literalString01() {
		parseCheck("'hello'");
	}

	@Test
	void literalString02() {
		parseCheck("'joe bloggs'");
	}

	@Test
	void literalString03() {
		parseCheck("'Tony''s Pizza'", "'Tony's Pizza'");
	}

	@Test
	void literalReal01() {
		parseCheck("6.0221415E+23", "6.0221415E23");
	}

	@Test
	void literalHex01() {
		parseCheck("0x7FFFFFFF", "2147483647");
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
	void literalNull01() {
		parseCheck("null");
	}

	// boolean operators
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
	void booleanOperatorsMix01() {
		parseCheck("false or true and false", "(false or (true and false))");
	}

	// relational operators
	@Test
	void relOperatorsGT01() {
		parseCheck("3>6", "(3 > 6)");
	}

	@Test
	void relOperatorsLT01() {
		parseCheck("3<6", "(3 < 6)");
	}

	@Test
	void relOperatorsLE01() {
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

	@Test
	void elvis() {
		parseCheck("3?:1", "(3 ?: 1)");
		parseCheck("(2*3)?:1*10", "((2 * 3) ?: (1 * 10))");
		parseCheck("((2*3)?:1)*10", "(((2 * 3) ?: 1) * 10)");
	}

	// void relOperatorsIn01() {
	// parseCheck("3 in {1,2,3,4,5}", "(3 in {1,2,3,4,5})");
	// }
	//
	// void relOperatorsBetween01() {
	// parseCheck("1 between {1, 5}", "(1 between {1,5})");
	// }

	// void relOperatorsBetween02() {
	// parseCheck("'efg' between {'abc', 'xyz'}", "('efg' between {'abc','xyz'})");
	// }// true

	@Test
	void relOperatorsIs01() {
		parseCheck("'xyz' instanceof int", "('xyz' instanceof int)");
	}// false

	// void relOperatorsIs02() {
	// parseCheck("{1, 2, 3, 4, 5} instanceof List", "({1,2,3,4,5} instanceof List)");
	// }// true

	@Test
	void relOperatorsMatches01() {
		parseCheck("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'", "('5.0067' matches '^-?\\d+(\\.\\d{2})?$')");
	}// false

	@Test
	void relOperatorsMatches02() {
		parseCheck("'5.00' matches '^-?\\d+(\\.\\d{2})?$'", "('5.00' matches '^-?\\d+(\\.\\d{2})?$')");
	}// true

	// mathematical operators
	@Test
	void mathOperatorsAdd01() {
		parseCheck("2+4", "(2 + 4)");
	}

	@Test
	void mathOperatorsAdd02() {
		parseCheck("'a'+'b'", "('a' + 'b')");
	}

	@Test
	void mathOperatorsAdd03() {
		parseCheck("'hello'+' '+'world'", "(('hello' + ' ') + 'world')");
	}

	@Test
	void mathOperatorsSubtract01() {
		parseCheck("5-4", "(5 - 4)");
	}

	@Test
	void mathOperatorsMultiply01() {
		parseCheck("7*4", "(7 * 4)");
	}

	@Test
	void mathOperatorsDivide01() {
		parseCheck("8/4", "(8 / 4)");
	}

	@Test
	void mathOperatorModulus01() {
		parseCheck("7 % 4", "(7 % 4)");
	}

	// mixed operators
	@Test
	void mixedOperators01() {
		parseCheck("true and 5>3", "(true and (5 > 3))");
	}

	// collection processors
	// void collectionProcessorsCount01() {
	// parseCheck("new String[] {'abc','def','xyz'}.count()");
	// }

	// void collectionProcessorsCount02() {
	// parseCheck("new int[] {1,2,3}.count()");
	// }
	//
	// void collectionProcessorsMax01() {
	// parseCheck("new int[] {1,2,3}.max()");
	// }
	//
	// void collectionProcessorsMin01() {
	// parseCheck("new int[] {1,2,3}.min()");
	// }
	//
	// void collectionProcessorsAverage01() {
	// parseCheck("new int[] {1,2,3}.average()");
	// }
	//
	// void collectionProcessorsSort01() {
	// parseCheck("new int[] {3,2,1}.sort()");
	// }
	//
	// void collectionProcessorsNonNull01() {
	// parseCheck("{'a','b',null,'d',null}.nonNull()");
	// }
	//
	// void collectionProcessorsDistinct01() {
	// parseCheck("{'a','b','a','d','e'}.distinct()");
	// }

	// references
	@Test
	void references01() {
		parseCheck("@foo");
		parseCheck("@'foo.bar'");
		parseCheck("@\"foo.bar.goo\"" , "@'foo.bar.goo'");
	}

	@Test
	void references03() {
		parseCheck("@$$foo");
	}

	// properties
	@Test
	void properties01() {
		parseCheck("name");
	}

	@Test
	void properties02() {
		parseCheck("placeofbirth.CitY");
	}

	@Test
	void properties03() {
		parseCheck("a.b.c.d.e");
	}

	// inline list creation
	@Test
	void inlineListCreation01() {
		parseCheck("{1, 2, 3, 4, 5}", "{1,2,3,4,5}");
	}

	@Test
	void inlineListCreation02() {
		parseCheck("{'abc','xyz'}", "{'abc','xyz'}");
	}

	// inline map creation
	@Test
	void inlineMapCreation01() {
		parseCheck("{'key1':'Value 1','today':DateTime.Today}");
	}

	@Test
	void inlineMapCreation02() {
		parseCheck("{1:'January',2:'February',3:'March'}");
	}

	// methods
	@Test
	void methods01() {
		parseCheck("echo(12)");
	}

	@Test
	void methods02() {
		parseCheck("echo(name)");
	}

	@Test
	void methods03() {
		parseCheck("age.doubleItAndAdd(12)");
	}

	// constructors
	@Test
	void constructors01() {
		parseCheck("new String('hello')");
	}

	// void constructors02() {
	// parseCheck("new String[3]");
	// }

	// array construction
	// void arrayConstruction01() {
	// parseCheck("new int[] {1, 2, 3, 4, 5}", "new int[] {1,2,3,4,5}");
	// }
	//
	// void arrayConstruction02() {
	// parseCheck("new String[] {'abc','xyz'}", "new String[] {'abc','xyz'}");
	// }

	// variables and functions
	@Test
	void variables01() {
		parseCheck("#foo");
	}

	@Test
	void functions01() {
		parseCheck("#fn(1,2,3)");
	}

	@Test
	void functions02() {
		parseCheck("#fn('hello')");
	}

	// projections and selections
	// void projections01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.!{#isEven()}");
	// }

	// void selections01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.?{#isEven(#this) == 'y'}",
	// "{1,2,3,4,5,6,7,8,9,10}.?{(#isEven(#this) == 'y')}");
	// }

	// void selectionsFirst01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.^{#isEven(#this) == 'y'}",
	// "{1,2,3,4,5,6,7,8,9,10}.^{(#isEven(#this) == 'y')}");
	// }

	// void selectionsLast01() {
	// parseCheck("{1,2,3,4,5,6,7,8,9,10}.${#isEven(#this) == 'y'}",
	// "{1,2,3,4,5,6,7,8,9,10}.${(#isEven(#this) == 'y')}");
	// }

	// assignment
	@Test
	void assignmentToVariables01() {
		parseCheck("#var1='value1'");
	}


	// ternary operator

	@Test
	void ternaryOperator01() {
		parseCheck("1>2?3:4", "((1 > 2) ? 3 : 4)");
		parseCheck("(a ? 1 : 0) * 10", "((a ? 1 : 0) * 10)");
		parseCheck("(a?1:0)*10", "((a ? 1 : 0) * 10)");
		parseCheck("(4 % 2 == 0 ? 1 : 0) * 10", "((((4 % 2) == 0) ? 1 : 0) * 10)");
		parseCheck("((4 % 2 == 0) ? 1 : 0) * 10", "((((4 % 2) == 0) ? 1 : 0) * 10)");
	}

	@Test
	void ternaryOperator02() {
		parseCheck("{1}.#isEven(#this) == 'y'?'it is even':'it is odd'",
				"(({1}.#isEven(#this) == 'y') ? 'it is even' : 'it is odd')");
	}

	//
	// void lambdaMax() {
	// parseCheck("(#max = {|x,y| $x > $y ? $x : $y }; #max(5,25))", "(#max={|x,y| ($x > $y) ? $x : $y };#max(5,25))");
	// }
	//
	// void lambdaFactorial() {
	// parseCheck("(#fact = {|n| $n <= 1 ? 1 : $n * #fact($n-1) }; #fact(5))",
	// "(#fact={|n| ($n <= 1) ? 1 : ($n * #fact(($n - 1))) };#fact(5))");
	// } // 120

	// Type references
	@Test
	void typeReferences01() {
		parseCheck("T(java.lang.String)");
	}

	@Test
	void typeReferences02() {
		parseCheck("T(String)");
	}

	@Test
	void inlineList1() {
		parseCheck("{1,2,3,4}");
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
