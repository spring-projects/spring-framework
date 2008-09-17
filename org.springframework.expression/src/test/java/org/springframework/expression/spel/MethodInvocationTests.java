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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.standard.StandardEvaluationContext;

/**
 * Tests invocation of methods.
 * 
 * @author Andy Clement
 */
@SuppressWarnings("unused")
public class MethodInvocationTests extends ExpressionTestCase {

	public void testSimpleAccess01() {
		evaluate("getPlaceOfBirth().getCity()", "SmilJan", String.class);
	}

	// public void testBuiltInProcessors() {
	// evaluate("new int[]{1,2,3,4}.count()", 4, Integer.class);
	// evaluate("new int[]{4,3,2,1}.sort()[3]", 4, Integer.class);
	// evaluate("new int[]{4,3,2,1}.average()", 2, Integer.class);
	// evaluate("new int[]{4,3,2,1}.max()", 4, Integer.class);
	// evaluate("new int[]{4,3,2,1}.min()", 1, Integer.class);
	// evaluate("new int[]{4,3,2,1,2,3}.distinct().count()", 4, Integer.class);
	// evaluate("{1,2,3,null}.nonnull().count()", 3, Integer.class);
	// evaluate("new int[]{4,3,2,1,2,3}.distinct().count()", 4, Integer.class);
	// }

	public void testStringClass() {
		evaluate("new java.lang.String('hello').charAt(2)", 'l', Character.class);
		evaluate("new java.lang.String('hello').charAt(2).equals('l'.charAt(0))", true, Boolean.class);
		evaluate("'HELLO'.toLowerCase()", "hello", String.class);
		evaluate("'   abcba '.trim()", "abcba", String.class);
	}

	public void testNonExistentMethods() {
		// name is ok but madeup() does not exist
		evaluateAndCheckError("name.madeup()", SpelMessages.METHOD_NOT_FOUND, 5);
	}

	public void testWidening01() {
		// widening of int 3 to double 3 is OK
		evaluate("new Double(3.0d).compareTo(8)", -1, Integer.class);
		evaluate("new Double(3.0d).compareTo(3)", 0, Integer.class);
		evaluate("new Double(3.0d).compareTo(2)", 1, Integer.class);
	}

	public void testArgumentConversion01() {
		// Rely on Double>String conversion for calling startsWith()
		evaluate("new String('hello 2.0 to you').startsWith(7.0d)", false, Boolean.class);
		evaluate("new String('7.0 foobar').startsWith(7.0d)", true, Boolean.class);
	}

	public void testVarargsInvocation01() {
		// Calling 'public int aVarargsMethod(String... strings)'
		evaluate("aVarargsMethod('a','b','c')", 3, Integer.class);
		evaluate("aVarargsMethod('a')", 1, Integer.class);
		evaluate("aVarargsMethod()", 0, Integer.class);
		evaluate("aVarargsMethod(1,2,3)", 3, Integer.class); // all need converting to strings
		evaluate("aVarargsMethod(1)", 1, Integer.class); // needs string conversion
		evaluate("aVarargsMethod(1,'a',3.0d)", 3, Integer.class); // first and last need conversion
		// evaluate("aVarargsMethod(new String[]{'a','b','c'})", 3, Integer.class);
	}

	public void testVarargsInvocation02() {
		// Calling 'public int aVarargsMethod2(int i, String... strings)' - returns int+length_of_strings
		evaluate("aVarargsMethod2(5,'a','b','c')", 8, Integer.class);
		evaluate("aVarargsMethod2(2,'a')", 3, Integer.class);
		evaluate("aVarargsMethod2(4)", 4, Integer.class);
		evaluate("aVarargsMethod2(8,2,3)", 10, Integer.class);
		evaluate("aVarargsMethod2(9)", 9, Integer.class);
		evaluate("aVarargsMethod2(2,'a',3.0d)", 4, Integer.class);
		// evaluate("aVarargsMethod2(8,new String[]{'a','b','c'})", 11, Integer.class);
	}

	// Due to conversion there are two possible methods to call ...
	public void testVarargsInvocation03() throws Exception {
		// Calling 'm(String... strings)' and 'm(int i,String... strings)'
		try {
			SpelExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			ctx.setClasspath("target/test-classes/testcode.jar");

			Object v = null;
			v = parser.parseExpression("new TestType().m(1,2,3)").getValue(ctx);
			// v = parser.parseExpression("new TestType().m('a','b','c')").getValue(ctx);
			// v = parser.parseExpression("new TestType().m(5,'a','b','c')").getValue(ctx);
			// v = parser.parseExpression("new TestType().m()").getValue(ctx);
			// v = parser.parseExpression("new TestType().m(1)").getValue(ctx);
			// v = parser.parseExpression("new TestType().m(1,'a',3.0d)").getValue(ctx);
			// v = parser.parseExpression("new TestType().m(new String[]{'a','b','c'})").getValue(ctx);
			fail("Should have detected ambiguity, there are two possible matches");
		} catch (EvaluationException ee) {
		}
	}

}
