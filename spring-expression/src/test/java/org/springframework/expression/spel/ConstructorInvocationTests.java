/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.PlaceOfBirth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests invocation of constructors.
 *
 * @author Andy Clement
 */
public class ConstructorInvocationTests extends AbstractExpressionTests {

	@Test
	public void testTypeConstructors() {
		evaluate("new String('hello world')", "hello world", String.class);
	}

	@Test
	public void testNonExistentType() {
		evaluateAndCheckError("new FooBar()", SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM);
	}


	@SuppressWarnings("serial")
	static class TestException extends Exception {

	}

	static class Tester {

		public static int counter;
		public int i;


		public Tester() {
		}

		public Tester(int i) throws Exception {
			counter++;
			if (i == 1) {
				throw new IllegalArgumentException("IllegalArgumentException for 1");
			}
			if (i == 2) {
				throw new RuntimeException("RuntimeException for 2");
			}
			if (i == 4) {
				throw new TestException();
			}
			this.i = i;
		}

		public Tester(PlaceOfBirth pob) {

		}

	}


	@Test
	public void testConstructorThrowingException_SPR6760() {
		// Test ctor on inventor:
		// On 1 it will throw an IllegalArgumentException
		// On 2 it will throw a RuntimeException
		// On 3 it will exit normally
		// In each case it increments the Tester field 'counter' when invoked

		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression("new org.springframework.expression.spel.ConstructorInvocationTests$Tester(#bar).i");

		// Normal exit
		StandardEvaluationContext eContext = TestScenarioCreator.getTestEvaluationContext();
		eContext.setRootObject(new Tester());
		eContext.setVariable("bar", 3);
		Object o = expr.getValue(eContext);
		assertThat(o).isEqualTo(3);
		assertThat(parser.parseExpression("counter").getValue(eContext)).isEqualTo(1);

		// Now the expression has cached that throwException(int) is the right thing to
		// call. Let's change 'bar' to be a PlaceOfBirth which indicates the cached
		// reference is out of date.
		eContext.setVariable("bar", new PlaceOfBirth("London"));
		o = expr.getValue(eContext);
		assertThat(o).isEqualTo(0);
		// That confirms the logic to mark the cached reference stale and retry is working

		// Now let's cause the method to exit via exception and ensure it doesn't cause
		// a retry.

		// First, switch back to throwException(int)
		eContext.setVariable("bar", 3);
		o = expr.getValue(eContext);
		assertThat(o).isEqualTo(3);
		assertThat(parser.parseExpression("counter").getValue(eContext)).isEqualTo(2);

		// 4 will make it throw a checked exception - this will be wrapped by spel on the
		// way out
		eContext.setVariable("bar", 4);
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				expr.getValue(eContext))
			.withMessageContaining("Tester");
		// A problem occurred whilst attempting to construct an object of type
		// 'org.springframework.expression.spel.ConstructorInvocationTests$Tester'
		// using arguments '(java.lang.Integer)'

		// If counter is 4 then the method got called twice!
		assertThat(parser.parseExpression("counter").getValue(eContext)).isEqualTo(3);

		// 1 will make it throw a RuntimeException - SpEL will let this through
		eContext.setVariable("bar", 1);
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				expr.getValue(eContext))
			.satisfies(ex -> assertThat(ex).isNotInstanceOf(SpelEvaluationException.class));
		// A problem occurred whilst attempting to construct an object of type
		// 'org.springframework.expression.spel.ConstructorInvocationTests$Tester'
		// using arguments '(java.lang.Integer)'

		// If counter is 5 then the method got called twice!
		assertThat(parser.parseExpression("counter").getValue(eContext)).isEqualTo(4);
	}

	@Test
	public void testAddingConstructorResolvers() {
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		// reflective constructor accessor is the only one by default
		List<ConstructorResolver> constructorResolvers = ctx.getConstructorResolvers();
		assertThat(constructorResolvers.size()).isEqualTo(1);

		ConstructorResolver dummy = new DummyConstructorResolver();
		ctx.addConstructorResolver(dummy);
		assertThat(ctx.getConstructorResolvers().size()).isEqualTo(2);

		List<ConstructorResolver> copy = new ArrayList<>(ctx.getConstructorResolvers());
		assertThat(ctx.removeConstructorResolver(dummy)).isTrue();
		assertThat(ctx.removeConstructorResolver(dummy)).isFalse();
		assertThat(ctx.getConstructorResolvers().size()).isEqualTo(1);

		ctx.setConstructorResolvers(copy);
		assertThat(ctx.getConstructorResolvers().size()).isEqualTo(2);
	}


	static class DummyConstructorResolver implements ConstructorResolver {

		@Override
		public ConstructorExecutor resolve(EvaluationContext context, String typeName,
				List<TypeDescriptor> argumentTypes) throws AccessException {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

	}


	@Test
	public void testVarargsInvocation01() {
		// Calling 'Fruit(String... strings)'
		evaluate("new org.springframework.expression.spel.testresources.Fruit('a','b','c').stringscount()", 3,
			Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit('a').stringscount()", 1, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit().stringscount()", 0, Integer.class);
		// all need converting to strings
		evaluate("new org.springframework.expression.spel.testresources.Fruit(1,2,3).stringscount()", 3, Integer.class);
		// needs string conversion
		evaluate("new org.springframework.expression.spel.testresources.Fruit(1).stringscount()", 1, Integer.class);
		// first and last need conversion
		evaluate("new org.springframework.expression.spel.testresources.Fruit(1,'a',3.0d).stringscount()", 3,
			Integer.class);
	}

	@Test
	public void testVarargsInvocation02() {
		// Calling 'Fruit(int i, String... strings)' - returns int+length_of_strings
		evaluate("new org.springframework.expression.spel.testresources.Fruit(5,'a','b','c').stringscount()", 8,
			Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(2,'a').stringscount()", 3, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(4).stringscount()", 4, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(8,2,3).stringscount()", 10, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(9).stringscount()", 9, Integer.class);
		evaluate("new org.springframework.expression.spel.testresources.Fruit(2,'a',3.0d).stringscount()", 4,
			Integer.class);
		evaluate(
			"new org.springframework.expression.spel.testresources.Fruit(8,stringArrayOfThreeItems).stringscount()",
			11, Integer.class);
	}

	/*
	 * These tests are attempting to call constructors where we need to widen or convert
	 * the argument in order to satisfy a suitable constructor.
	 */
	@Test
	public void testWidening01() {
		// widening of int 3 to double 3 is OK
		evaluate("new Double(3)", 3.0d, Double.class);
		// widening of int 3 to long 3 is OK
		evaluate("new Long(3)", 3L, Long.class);
	}

	@Test
	@Disabled
	public void testArgumentConversion01() {
		// Closest ctor will be new String(String) and converter supports Double>String
		// TODO currently failing as with new ObjectToArray converter closest constructor
		// matched becomes String(byte[]) which fails...
		evaluate("new String(3.0d)", "3.0", String.class);
	}

}
