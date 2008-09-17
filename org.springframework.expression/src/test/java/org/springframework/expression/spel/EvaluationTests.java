/*
 * Copyright 2004-2008 the original author or authors.
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

/**
 * Tests the evaluation of real expressions in a real context.
 * 
 * @author Andy Clement
 */
public class EvaluationTests extends ExpressionTestCase {

	// relational operators: lt, le, gt, ge, eq, ne
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

	// public void testRelOperatorsIn01() {
	// evaluate("3 in {1,2,3,4,5}", "true", Boolean.class);
	// }

	// public void testRelOperatorsIn02() {
	// evaluate("name in {null, \"Nikola Tesla\"}", "true", Boolean.class);
	// evaluate("name in {null, \"Anonymous\"}", "false", Boolean.class);
	// }
	//
	// public void testRelOperatorsBetween01() {
	// evaluate("1 between {1, 5}", "true", Boolean.class);
	// }
	//
	// public void testRelOperatorsBetween02() {
	// evaluate("'efg' between {'abc', 'xyz'}", "true", Boolean.class);
	// }
	//
	// public void testRelOperatorsBetweenErrors01() {
	// evaluateAndCheckError("1 between T(String)", SpelMessages.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST, 12);
	// }
	//
	// public void testRelOperatorsBetweenErrors02() {
	// evaluateAndCheckError("'abc' between {5,7}", SpelMessages.NOT_COMPARABLE, 6);
	// }

	public void testRelOperatorsIs01() {
		evaluate("'xyz' instanceof T(int)", "false", Boolean.class);
	}

	// public void testRelOperatorsIs02() {
	// evaluate("{1, 2, 3, 4, 5} instanceof T(List)", "true", Boolean.class);
	// }
	//
	// public void testRelOperatorsIs03() {
	// evaluate("{1, 2, 3, 4, 5} instanceof T(List)", "true", Boolean.class);
	// }

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
		evaluate("name", "Nikola Tesla", String.class, false); // not writable because (1) name is private (2) there is
		// no
		// setter, only a getter
		evaluateAndCheckError("madeup", SpelMessages.PROPERTY_OR_FIELD_NOT_FOUND, 0, "madeup",
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

	// inline list creation
	// public void testInlineListCreation01() {
	// evaluate("{1, 2, 3, 4, 5}", "[1, 2, 3, 4, 5]", ArrayList.class);
	// }
	//
	// public void testInlineListCreation02() {
	// evaluate("{'abc', 'xyz'}", "[abc, xyz]", ArrayList.class);
	// }
	//
	// // inline map creation
	// public void testInlineMapCreation01() {
	// evaluate("#{'key1':'Value 1', 'today':'Monday'}", "{key1=Value 1, today=Monday}", HashMap.class);
	// }
	//
	// public void testInlineMapCreation02() {
	// evaluate("#{1:'January', 2:'February', 3:'March'}.size()", 3, Integer.class);// "{2=February, 1=January,
	// // 3=March}", HashMap.class);
	// }
	//
	// public void testInlineMapCreation03() {
	// evaluate("#{'key1':'Value 1', 'today':'Monday'}['key1']", "Value 1", String.class);
	// }
	//
	// public void testInlineMapCreation04() {
	// evaluate("#{1:'January', 2:'February', 3:'March'}[3]", "March", String.class);
	// }
	//
	// public void testInlineMapCreation05() {
	// evaluate("#{1:'January', 2:'February', 3:'March'}.get(2)", "February", String.class);
	// }
	//
	// // set construction
	// public void testSetConstruction01() {
	// evaluate("new HashSet().addAll({'a','b','c'})", "true", Boolean.class);
	// }

	// constructors
	public void testConstructorInvocation01() {
		evaluate("new String('hello')", "hello", String.class);
	}

	// public void testConstructorInvocation02() {
	// evaluate("new String[3]", "java.lang.String[3]{null,null,null}", String[].class);
	// }

	// public void testConstructorInvocation03() {
	// evaluateAndCheckError("new String[]", SpelMessages.NO_SIZE_OR_INITIALIZER_FOR_ARRAY_CONSTRUCTION, 4);
	// }

	// public void testConstructorInvocation04() {
	// evaluateAndCheckError("new String[3]{'abc',3,'def'}", SpelMessages.INCORRECT_ELEMENT_TYPE_FOR_ARRAY, 4);
	// }

	public void testConstructorInvocation05() {
		evaluate("new java.lang.String('foobar')", "foobar", String.class);
	}

	// array construction
	// public void testArrayConstruction01() {
	// evaluate("new int[] {1, 2, 3, 4, 5}", "int[5]{1,2,3,4,5}", int[].class);
	// }

	// public void testArrayConstruction02() {
	// evaluate("new String[] {'abc', 'xyz'}", "java.lang.String[2]{abc,xyz}", String[].class);
	// }

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

	// projection and selection
	// public void testProjection01() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.!{#isEven(#this)}", "[n, y, n, y, n, y, n, y, n, y]", ArrayList.class);
	// }
	//
	// public void testProjection02() {
	// evaluate("#{'a':'y','b':'n','c':'y'}.!{value=='y'?key:null}.nonnull().sort()", "[a, c]", ArrayList.class);
	// }
	//
	// public void testProjection03() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.!{#this>5}",
	// "[false, false, false, false, false, true, true, true, true, true]", ArrayList.class);
	// }

	// public void testProjection04() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.!{$index>5?'y':'n'}", "[n, n, n, n, n, n, y, y, y, y]", ArrayList.class);
	// }

	// public void testSelection01() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.?{#isEven(#this) == 'y'}", "[2, 4, 6, 8, 10]", ArrayList.class);
	// }

	// public void testSelectionError_NonBooleanSelectionCriteria() {
	// evaluateAndCheckError("{1,2,3,4,5,6,7,8,9,10}.?{'nonboolean'}",
	// SpelMessages.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
	// }

	// public void testSelectionUsingIndex() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.?{$index > 5 }", "[7, 8, 9, 10]", ArrayList.class);
	// }

	// public void testSelectionFirst01() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.^{#isEven(#this) == 'y'}", "2", Integer.class);
	// }
	//
	// public void testSelectionLast01() {
	// evaluate("{1,2,3,4,5,6,7,8,9,10}.${#isEven(#this) == 'y'}", "10", Integer.class);
	// }

	// assignment
	public void testAssignmentToVariables01() {
		evaluate("#var1='value1'", "value1", String.class);
	}

	// Ternary operator
	// public void testTernaryOperator01() {
	// evaluate("{1}.#isEven(#this[0]) == 'y'?'it is even':'it is odd'", "it is odd", String.class);
	// }
	//
	// public void testTernaryOperator02() {
	// evaluate("{2}.#isEven(#this[0]) == 'y'?'it is even':'it is odd'", "it is even", String.class);
	// }

	public void testTernaryOperator03() {
		evaluateAndCheckError("'hello'?1:2", SpelMessages.TYPE_CONVERSION_ERROR); // cannot convert String to boolean
	}

	public void testTernaryOperator04() {
		// an int becomes TRUE if not 0, otherwise FALSE
		evaluate("12?1:2", 1, Integer.class); // int to boolean
		evaluate("1L?1:2", 1, Integer.class); // long to boolean
	}

	// Indexer
	// public void testCutProcessor01() {
	// evaluate("{1,2,3,4,5}.cut(1,3)", "[2, 3, 4]", ArrayList.class);
	// }
	//
	// public void testCutProcessor02() {
	// evaluate("{1,2,3,4,5}.cut(3,1)", "[4, 3, 2]", ArrayList.class);
	// }

	public void testIndexer03() {
		evaluate("'christian'[8]", "n", String.class);
	}

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

	// value is not public, it is accessed through getRGB()
	// public void testStaticRef01() {
	// evaluate("T(Color).green.value!=0", "true", Boolean.class);
	// }

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

	// type references
	public void testTypeReferences01() {
		evaluate("T(java.lang.String)", "class java.lang.String", Class.class);
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

}
