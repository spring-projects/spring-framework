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

/**
 * Tests invocation of constructors.
 * 
 * @author Andy Clement
 */
public class ConstructorInvocationTests extends ExpressionTestCase {

	public void testTypeConstructors() {
		evaluate("new String('hello world')", "hello world", String.class);
	}
	
	public void testNonExistentType() {
		evaluateAndCheckError("new FooBar()",SpelMessages.PROBLEM_LOCATING_CONSTRUCTOR);
	}
	
	public void testVarargsInvocation01() {
		// Calling 'Fruit(String... strings)'
		evaluate("new org.springframework.expression.spel.testresources.Fruit('a','b','c').stringscount()", 3, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit('a').stringscount()", 1, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit().stringscount()", 0, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(1,2,3).stringscount()", 3, Integer.class); // all need converting to strings
		evaluate("new org.springframework.expression.spel.testresources.Fruit(1).stringscount()", 1, Integer.class); // needs string conversion
		evaluate("new org.springframework.expression.spel.testresources.Fruit(1,'a',3.0d).stringscount()", 3, Integer.class); // first and last need conversion
	}

	public void testVarargsInvocation02() {
	    // Calling 'Fruit(int i, String... strings)' - returns int+length_of_strings
		evaluate("new org.springframework.expression.spel.testresources.Fruit(5,'a','b','c').stringscount()", 8, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(2,'a').stringscount()", 3, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(4).stringscount()", 4, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(8,2,3).stringscount()", 10, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(9).stringscount()", 9, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(2,'a',3.0d).stringscount()", 4, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(8,stringArrayOfThreeItems).stringscount()", 11, Integer.class);
	}
	
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

}
