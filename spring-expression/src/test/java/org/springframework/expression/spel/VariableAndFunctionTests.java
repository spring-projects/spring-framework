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

package org.springframework.expression.spel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;


/**
 * Tests the evaluation of expressions that access variables and functions (lambda/java).
 *
 * @author Andy Clement
 * @author Oliver Becker
 */
public class VariableAndFunctionTests extends ExpressionTestCase {

	@Test
	public void testVariableAccess01() {
		evaluate("#answer", "42", Integer.class, SHOULD_BE_WRITABLE);
		evaluate("#answer / 2", 21, Integer.class, SHOULD_NOT_BE_WRITABLE);
	}

	@Test
	public void testVariableAccess_WellKnownVariables() {
		evaluate("#this.getName()", "Nikola Tesla", String.class);
		evaluate("#root.getName()", "Nikola Tesla", String.class);
	}

	@Test
	public void testFunctionAccess01() {
		evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
		evaluate("#reverseInt('1',2,3)", "int[3]{3,2,1}", int[].class); // requires type conversion of '1' to 1
		evaluateAndCheckError("#reverseInt(1)", SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, 1, 3);
	}

	@Test
	public void testFunctionAccess02() {
		evaluate("#reverseString('hello')", "olleh", String.class);
		evaluate("#reverseString(37)", "73", String.class); // requires type conversion of 37 to '37'
	}

	@Test
	public void testCallVarargsFunction() {
		evaluate("#varargsFunctionReverseStringsAndMerge('a','b','c')", "cba", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge('a')", "a", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge()", "", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge('b',25)", "25b", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge(25)", "25", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge2(1,'a','b','c')", "1cba", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge2(2,'a')", "2a", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge2(3)", "3", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge2(4,'b',25)", "425b", String.class);
		evaluate("#varargsFunctionReverseStringsAndMerge2(5,25)", "525", String.class);
	}

	@Test
	public void testCallingIllegalFunctions() throws Exception {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("notStatic", this.getClass().getMethod("nonStatic"));
		try {
			@SuppressWarnings("unused")
			Object v = parser.parseRaw("#notStatic()").getValue(ctx);
			fail("Should have failed with exception - cannot call non static method that way");
		} catch (SpelEvaluationException se) {
			if (se.getMessageCode() != SpelMessage.FUNCTION_MUST_BE_STATIC) {
				se.printStackTrace();
				fail("Should have failed a message about the function needing to be static, not: "
						+ se.getMessageCode());
			}
		}
	}
	// this method is used by the test above
	public void nonStatic() {
	}

	@Test
	public void testMethodExecutorFunction() {
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("timesTwo", new MethodExecutor() {
			@Override
			public TypedValue execute(EvaluationContext context, Object target,
					Object... arguments) throws AccessException {
				Integer anInteger = (Integer) context.getTypeConverter().convertValue(
						arguments[0], TypeDescriptor.forObject(arguments[0]),
						TypeDescriptor.valueOf(Integer.class));
				return new TypedValue(anInteger * 2);
			}
		});
		Object value = parser.parseRaw("#timesTwo(21)").getValue(ctx);
		assertEquals(42, value);
		assertEquals(Integer.class, value.getClass());
	}

}
