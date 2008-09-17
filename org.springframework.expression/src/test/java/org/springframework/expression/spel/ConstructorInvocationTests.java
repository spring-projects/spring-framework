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

import org.springframework.expression.spel.standard.StandardEvaluationContext;

/**
 * Tests invocation of constructors.
 * 
 * @author Andy Clement
 */
public class ConstructorInvocationTests extends ExpressionTestCase {

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

	public void testTypeConstructors() {
		evaluate("new String('hello world')", "hello world", String.class);
		// evaluate("new String(new char[]{'h','e','l','l','o'})", "hello", String.class);
	}

	// public void testErrorCases() {
	// evaluateAndCheckError("new char[7]{'a','c','d','e'}", SpelMessages.INITIALIZER_LENGTH_INCORRECT);
	// evaluateAndCheckError("new char[3]{'a','c','d','e'}", SpelMessages.INITIALIZER_LENGTH_INCORRECT);
	// evaluateAndCheckError("new char[2]{'hello','world'}", SpelMessages.TYPE_CONVERSION_ERROR);
	// evaluateAndCheckError("new String('a','c','d')", SpelMessages.CONSTRUCTOR_NOT_FOUND);
	// }

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

	/*
	 * These tests are attempting to call constructors where we need to widen or convert the argument in order to
	 * satisfy a suitable constructor.
	 */
	public void testWidening01() {
		// widening of int 3 to double 3 is OK
		evaluate("new Double(3)", 3.0d, Double.class);
		// widening of int 3 to long 3 is OK
		evaluate("new Long(3)", 3L, Long.class);
	}

	public void testArgumentConversion01() {
		// Closest ctor will be new String(String) and converter supports Double>String
		evaluate("new String(3.0d)", "3.0", String.class);
	}

	public void testVarargsInvocation01() throws Exception {
		// Calling 'public TestCode(String... strings)'
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setClasspath("target/test-classes/testcode.jar");

		@SuppressWarnings("unused")
		Object v = parser.parseExpression("new TestType('a','b','c')").getValue(ctx);
		v = parser.parseExpression("new TestType('a')").getValue(ctx);
		v = parser.parseExpression("new TestType()").getValue(ctx);
		v = parser.parseExpression("new TestType(1,2,3)").getValue(ctx);
		v = parser.parseExpression("new TestType(1)").getValue(ctx);
		v = parser.parseExpression("new TestType(1,'a',3.0d)").getValue(ctx);
		// v = parser.parseExpression("new TestType(new String[]{'a','b','c'})").getValue(ctx);
	}

}
