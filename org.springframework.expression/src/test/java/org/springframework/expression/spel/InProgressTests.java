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

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * These are tests for language features that are not yet considered 'live'. Either missing implementation or
 * documentation.
 * 
 * Where implementation is missing the tests are commented out.
 * 
 * @author Andy Clement
 */
public class InProgressTests extends ExpressionTestCase {

	@Test
	public void testRelOperatorsBetween01() {
		evaluate("1 between listOneFive", "true", Boolean.class);
		// evaluate("1 between {1, 5}", "true", Boolean.class); // no inline list building at the moment
	}

	@Test
	public void testRelOperatorsBetweenErrors01() {
		evaluateAndCheckError("1 between T(String)", SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST, 10);
	}

	@Test
	public void testRelOperatorsBetweenErrors03() {
		evaluateAndCheckError("1 between listOfNumbersUpToTen",
				SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST, 10);
	}

	// PROJECTION
	@Test
	public void testProjection01() {
		evaluate("listOfNumbersUpToTen.![#this<5?'y':'n']", "[y, y, y, y, n, n, n, n, n, n]", ArrayList.class);
		// inline list creation not supported at the moment
		// evaluate("{1,2,3,4,5,6,7,8,9,10}.!{#isEven(#this)}", "[n, y, n, y, n, y, n, y, n, y]", ArrayList.class);
	}

	@Test
	public void testProjection02() {
		// inline map creation not supported at the moment
		// evaluate("#{'a':'y','b':'n','c':'y'}.![value=='y'?key:null].nonnull().sort()", "[a, c]", ArrayList.class);
		evaluate("mapOfNumbersUpToTen.![key>5?value:null]",
				"[null, null, null, null, null, six, seven, eight, nine, ten]", ArrayList.class);
	}

	@Test
	public void testProjection05() {
		evaluateAndCheckError("'abc'.![true]", SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE);
		evaluateAndCheckError("null.![true]", SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE);
		evaluate("null?.![true]", null, null);
	}

	@Test
	public void testProjection06() throws Exception {
		SpelExpression expr = (SpelExpression) parser.parseExpression("'abc'.![true]");
		Assert.assertEquals("'abc'.![true]", expr.toStringAST());
		Assert.assertFalse(expr.isWritable(new StandardEvaluationContext()));
	}

	// SELECTION

	@Test
	public void testSelection02() {
		evaluate("testMap.keySet().?[#this matches '.*o.*']", "[monday]", ArrayList.class);
		evaluate("testMap.keySet().?[#this matches '.*r.*'].contains('saturday')", "true", Boolean.class);
		evaluate("testMap.keySet().?[#this matches '.*r.*'].size()", "3", Integer.class);
	}

	@Test
	public void testSelectionError_NonBooleanSelectionCriteria() {
		evaluateAndCheckError("listOfNumbersUpToTen.?['nonboolean']",
				SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
	}

	@Test
	public void testSelection03() {
		evaluate("mapOfNumbersUpToTen.?[key>5].size()", "5", Integer.class);
		// evaluate("listOfNumbersUpToTen.?{#this>5}", "5", ArrayList.class);
	}

	@Test
	public void testSelection04() {
		evaluateAndCheckError("mapOfNumbersUpToTen.?['hello'].size()",
				SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
	}

	@Test
	public void testSelection05() {
		evaluate("mapOfNumbersUpToTen.?[key>11].size()", "0", Integer.class);
		evaluate("mapOfNumbersUpToTen.^[key>11]", null, null);
		evaluate("mapOfNumbersUpToTen.$[key>11]", null, null);
		evaluate("null?.$[key>11]", null, null);
		evaluateAndCheckError("null.?[key>11]", SpelMessage.INVALID_TYPE_FOR_SELECTION);
		evaluateAndCheckError("'abc'.?[key>11]", SpelMessage.INVALID_TYPE_FOR_SELECTION);
	}

	@Test
	public void testSelectionFirst01() {
		evaluate("listOfNumbersUpToTen.^[#isEven(#this) == 'y']", "2", Integer.class);
	}

	@Test
	public void testSelectionFirst02() {
		evaluate("mapOfNumbersUpToTen.^[key>5].size()", "1", Integer.class);
	}

	@Test
	public void testSelectionLast01() {
		evaluate("listOfNumbersUpToTen.$[#isEven(#this) == 'y']", "10", Integer.class);
	}

	@Test
	public void testSelectionLast02() {
		evaluate("mapOfNumbersUpToTen.$[key>5]", "{10=ten}", HashMap.class);
		evaluate("mapOfNumbersUpToTen.$[key>5].size()", "1", Integer.class);
	}

	@Test
	public void testSelectionAST() throws Exception {
		SpelExpression expr = (SpelExpression) parser.parseExpression("'abc'.^[true]");
		Assert.assertEquals("'abc'.^[true]", expr.toStringAST());
		Assert.assertFalse(expr.isWritable(new StandardEvaluationContext()));
		expr = (SpelExpression) parser.parseExpression("'abc'.?[true]");
		Assert.assertEquals("'abc'.?[true]", expr.toStringAST());
		Assert.assertFalse(expr.isWritable(new StandardEvaluationContext()));
		expr = (SpelExpression) parser.parseExpression("'abc'.$[true]");
		Assert.assertEquals("'abc'.$[true]", expr.toStringAST());
		Assert.assertFalse(expr.isWritable(new StandardEvaluationContext()));
	}

	// Constructor invocation

	// public void testPrimitiveTypeArrayConstructors() {
	// evaluate("new int[]{1,2,3,4}.count()", 4, Integer.class);
	// evaluate("new boolean[]{true,false,true}.count()", 3, Integer.class);
	// evaluate("new char[]{'a','b','c'}.count()", 3, Integer.class);
	// evaluate("new long[]{1,2,3,4,5}.count()", 5, Integer.class);
	// evaluate("new short[]{2,3,4,5,6}.count()", 5, Integer.class);
	// evaluate("new double[]{1d,2d,3d,4d}.count()", 4, Integer.class);
	// evaluate("new float[]{1f,2f,3f,4f}.count()", 4, Integer.class);
	// evaluate("new byte[]{1,2,3,4}.count()", 4, Integer.class);
	// }
	//
	// public void testPrimitiveTypeArrayConstructorsElements() {
	// evaluate("new int[]{1,2,3,4}[0]", 1, Integer.class);
	// evaluate("new boolean[]{true,false,true}[0]", true, Boolean.class);
	// evaluate("new char[]{'a','b','c'}[0]", 'a', Character.class);
	// evaluate("new long[]{1,2,3,4,5}[0]", 1L, Long.class);
	// evaluate("new short[]{2,3,4,5,6}[0]", (short) 2, Short.class);
	// evaluate("new double[]{1d,2d,3d,4d}[0]", (double) 1, Double.class);
	// evaluate("new float[]{1f,2f,3f,4f}[0]", (float) 1, Float.class);
	// evaluate("new byte[]{1,2,3,4}[0]", (byte) 1, Byte.class);
	// }
	//
	// public void testErrorCases() {
	// evaluateAndCheckError("new char[7]{'a','c','d','e'}", SpelMessages.INITIALIZER_LENGTH_INCORRECT);
	// evaluateAndCheckError("new char[3]{'a','c','d','e'}", SpelMessages.INITIALIZER_LENGTH_INCORRECT);
	// evaluateAndCheckError("new char[2]{'hello','world'}", SpelMessages.TYPE_CONVERSION_ERROR);
	// evaluateAndCheckError("new String('a','c','d')", SpelMessages.CONSTRUCTOR_NOT_FOUND);
	// }
	//
	// public void testTypeArrayConstructors() {
	// evaluate("new String[]{'a','b','c','d'}[1]", "b", String.class);
	// evaluateAndCheckError("new String[]{'a','b','c','d'}.size()", SpelMessages.METHOD_NOT_FOUND, 30, "size()",
	// "java.lang.String[]");
	// evaluateAndCheckError("new String[]{'a','b','c','d'}.juggernaut", SpelMessages.PROPERTY_OR_FIELD_NOT_FOUND, 30,
	// "juggernaut", "java.lang.String[]");
	// evaluate("new String[]{'a','b','c','d'}.length", 4, Integer.class);
	// }

	// public void testMultiDimensionalArrays() {
	// evaluate(
	// "new String[3,4]",
	// "[Ljava.lang.String;[3]{java.lang.String[4]{null,null,null,null},java.lang.String[4]{null,null,null,null},java.lang.String[4]{null,null,null,null}}"
	// ,
	// new String[3][4].getClass());
	// }
	//
	//
	// evaluate("new String(new char[]{'h','e','l','l','o'})", "hello", String.class);
	//
	//
	//
	// public void testRelOperatorsIn01() {
	// evaluate("3 in {1,2,3,4,5}", "true", Boolean.class);
	// }
	//
	// public void testRelOperatorsIn02() {
	// evaluate("name in {null, \"Nikola Tesla\"}", "true", Boolean.class);
	// evaluate("name in {null, \"Anonymous\"}", "false", Boolean.class);
	// }
	//
	//
	// public void testRelOperatorsBetween02() {
	// evaluate("'efg' between {'abc', 'xyz'}", "true", Boolean.class);
	// }
	//
	//
	// public void testRelOperatorsBetweenErrors02() {
	// evaluateAndCheckError("'abc' between {5,7}", SpelMessages.NOT_COMPARABLE, 6);
	// }
	// Lambda calculations
	//
	//
	// public void testLambda02() {
	// evaluate("(#max={|x,y| $x > $y ? $x : $y };true)", "true", Boolean.class);
	// }
	//
	// public void testLambdaMax() {
	// evaluate("(#max = {|x,y| $x > $y ? $x : $y }; #max(5,25))", "25", Integer.class);
	// }
	//
	// public void testLambdaFactorial01() {
	// evaluate("(#fact = {|n| $n <= 1 ? 1 : $n * #fact($n-1) }; #fact(5))", "120", Integer.class);
	// }
	//
	// public void testLambdaFactorial02() {
	// evaluate("(#fact = {|n| $n <= 1 ? 1 : #fact($n-1) * $n }; #fact(5))", "120", Integer.class);
	// }
	//
	// public void testLambdaAlphabet01() {
	// evaluate("(#alpha = {|l,s| $l>'z'?$s:#alpha($l+1,$s+$l)};#alphabet={||#alpha('a','')}; #alphabet())",
	// "abcdefghijklmnopqrstuvwxyz", String.class);
	// }
	//
	// public void testLambdaAlphabet02() {
	// evaluate("(#alphabet = {|l,s| $l>'z'?$s:#alphabet($l+1,$s+$l)};#alphabet('a',''))",
	// "abcdefghijklmnopqrstuvwxyz", String.class);
	// }
	//
	// public void testLambdaDelegation01() {
	// evaluate("(#sqrt={|n| T(Math).sqrt($n)};#delegate={|f,n| $f($n)};#delegate(#sqrt,4))", "2.0", Double.class);
	// }
	//
	// public void testVariableReferences() {
	// evaluate("(#answer=42;#answer)", "42", Integer.class, true);
	// evaluate("($answer=42;$answer)", "42", Integer.class, true);
	// }

//	// inline map creation
	//	@Test
	//	public void testInlineMapCreation01() {
	//		evaluate("#{'key1':'Value 1', 'today':'Monday'}", "{key1=Value 1, today=Monday}", HashMap.class);
	//	}
	//
	//	@Test
	//	public void testInlineMapCreation02() {
	//		// "{2=February, 1=January, 3=March}", HashMap.class);
	//		evaluate("#{1:'January', 2:'February', 3:'March'}.size()", 3, Integer.class);
	//	}
	//
	//	@Test
	//	public void testInlineMapCreation03() {
	//		evaluate("#{'key1':'Value 1', 'today':'Monday'}['key1']", "Value 1", String.class);
//	}
//
//	@Test
//	public void testInlineMapCreation04() {
//		evaluate("#{1:'January', 2:'February', 3:'March'}[3]", "March", String.class);
//	}
//
//	@Test
//	public void testInlineMapCreation05() {
//		evaluate("#{1:'January', 2:'February', 3:'March'}.get(2)", "February", String.class);
//	}

	// set construction
	@Test
	public void testSetConstruction01() {
		evaluate("new java.util.HashSet().addAll({'a','b','c'})", "true", Boolean.class);
	}

	//
	// public void testConstructorInvocation02() {
	// evaluate("new String[3]", "java.lang.String[3]{null,null,null}", String[].class);
	// }
	//
	// public void testConstructorInvocation03() {
	// evaluateAndCheckError("new String[]", SpelMessages.NO_SIZE_OR_INITIALIZER_FOR_ARRAY_CONSTRUCTION, 4);
	// }
	//
	// public void testConstructorInvocation04() {
	// evaluateAndCheckError("new String[3]{'abc',3,'def'}", SpelMessages.INCORRECT_ELEMENT_TYPE_FOR_ARRAY, 4);
	// }
	// array construction
	// @Test
	// public void testArrayConstruction01() {
	// evaluate("new int[] {1, 2, 3, 4, 5}", "int[5]{1,2,3,4,5}", int[].class);
	// }
	// public void testArrayConstruction02() {
	// evaluate("new String[] {'abc', 'xyz'}", "java.lang.String[2]{abc,xyz}", String[].class);
	// }
	//
	// collection processors
	// from spring.net: count,sum,max,min,average,sort,orderBy,distinct,nonNull
	// public void testProcessorsCount01() {
	// evaluate("new String[] {'abc','def','xyz'}.count()", "3", Integer.class);
	// }
	//
	// public void testProcessorsCount02() {
	// evaluate("new int[] {1,2,3}.count()", "3", Integer.class);
	// }
	//
	// public void testProcessorsMax01() {
	// evaluate("new int[] {1,2,3}.max()", "3", Integer.class);
	// }
	//
	// public void testProcessorsMin01() {
	// evaluate("new int[] {1,2,3}.min()", "1", Integer.class);
	// }
	//
	// public void testProcessorsKeys01() {
	// evaluate("#{1:'January', 2:'February', 3:'March'}.keySet().sort()", "[1, 2, 3]", ArrayList.class);
	// }
	//
	// public void testProcessorsValues01() {
	// evaluate("#{1:'January', 2:'February', 3:'March'}.values().sort()", "[February, January, March]",
	// ArrayList.class);
	// }
	//
	// public void testProcessorsAverage01() {
	// evaluate("new int[] {1,2,3}.average()", "2", Integer.class);
	// }
	//
	// public void testProcessorsSort01() {
	// evaluate("new int[] {3,2,1}.sort()", "int[3]{1,2,3}", int[].class);
	// }
	//
	// public void testCollectionProcessorsNonNull01() {
	// evaluate("{'a','b',null,'d',null}.nonnull()", "[a, b, d]", ArrayList.class);
	// }
	//
	// public void testCollectionProcessorsDistinct01() {
	// evaluate("{'a','b','a','d','e'}.distinct()", "[a, b, d, e]", ArrayList.class);
	// }
	//
	// public void testProjection03() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.!{#this>5}",
	// "[false, false, false, false, false, true, true, true, true, true]", ArrayList.class);
	// }
	//
	// public void testProjection04() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.!{$index>5?'y':'n'}", "[n, n, n, n, n, n, y, y, y, y]", ArrayList.class);
	// }
	// Bean references
	// public void testReferences01() {
	// evaluate("@(apple).name", "Apple", String.class, true);
	// }
	//
	// public void testReferences02() {
	// evaluate("@(fruits:banana).name", "Banana", String.class, true);
	// }
	//
	// public void testReferences03() {
	// evaluate("@(a.b.c)", null, null);
	// } // null - no context, a.b.c treated as name
	//
	// public void testReferences05() {
	// evaluate("@(a/b/c:orange).name", "Orange", String.class, true);
	// }
	//
	// public void testReferences06() {
	// evaluate("@(apple).color.getRGB() == 	T(java.awt.Color).green.getRGB()", "true", Boolean.class);
	// }
	//
	// public void testReferences07() {
	// evaluate("@(apple).color.getRGB().equals(T(java.awt.Color).green.getRGB())", "true", Boolean.class);
	// }
	//
	// value is not public, it is accessed through getRGB()
	// public void testStaticRef01() {
	// evaluate("T(Color).green.value!=0", "true", Boolean.class);
	// }	
	// Indexer
	// public void testCutProcessor01() {
	// evaluate("{1,2,3,4,5}.cut(1,3)", "[2, 3, 4]", ArrayList.class);
	// }
	//
	// public void testCutProcessor02() {
	// evaluate("{1,2,3,4,5}.cut(3,1)", "[4, 3, 2]", ArrayList.class);
	// }
	// Ternary operator
	// public void testTernaryOperator01() {
	// evaluate("{1}.#isEven(#this[0]) == 'y'?'it is even':'it is odd'", "it is odd", String.class);
	// }
	//
	// public void testTernaryOperator02() {
	// evaluate("{2}.#isEven(#this[0]) == 'y'?'it is even':'it is odd'", "it is even", String.class);
	// }
	// public void testSelectionUsingIndex() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.?{$index > 5 }", "[7, 8, 9, 10]", ArrayList.class);
	// }
	// public void testSelection01() {
	// inline list creation not supported:
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.?{#isEven(#this) == 'y'}", "[2, 4, 6, 8, 10]", ArrayList.class);
	// }
	//
	// public void testSelectionUsingIndex() {
	// evaluate("listOfNumbersUpToTen.?[#index > 5 ]", "[7, 8, 9, 10]", ArrayList.class);
	// }

}
