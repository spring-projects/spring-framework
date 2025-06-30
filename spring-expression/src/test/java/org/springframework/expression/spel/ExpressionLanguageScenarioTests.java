/*
 * Copyright 2002-present the original author or authors.
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

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test cases showing the common scenarios/use-cases for picking up the expression language support.
 * The first test shows very basic usage, just drop it in and go.  By 'standard infrastructure', it means:<br>
 * <ul>
 * <li>The context classloader is used (so, the default classpath)
 * <li>Some basic type converters are included
 * <li>properties/methods/constructors are discovered and invoked using reflection
 * </ul>
 * The scenarios after that then how to plug in extensions:<br>
 * <ul>
 * <li>Adding entries to the classpath that will be used to load types and define well known 'imports'
 * <li>Defining variables that are then accessible in the expression
 * <li>Changing the root context object against which non-qualified references are resolved
 * <li>Registering java methods as functions callable from the expression
 * <li>Adding a basic property resolver
 * <li>Adding an advanced (better performing) property resolver
 * <li>Adding your own type converter to support conversion between any types you like
 * </ul>
 *
 * @author Andy Clement
 */
class ExpressionLanguageScenarioTests extends AbstractExpressionTests {

	/**
	 * Scenario: using the standard infrastructure and running simple expression evaluation.
	 */
	@Test
	void testScenario_UsingStandardInfrastructure() {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Parse an expression
		Expression expr = parser.parseRaw("new String('hello world')");
		// Evaluate it using a 'standard' context
		Object value = expr.getValue();
		// They are reusable
		value = expr.getValue();

		assertThat(value).isEqualTo("hello world");
		assertThat(value.getClass()).isEqualTo(String.class);
	}

	/**
	 * Scenario: using the standard context but adding your own variables
	 */
	@Test
	void testScenario_DefiningVariablesThatWillBeAccessibleInExpressions() {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Use the standard evaluation context
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("favouriteColour","blue");
		List<Integer> primes = Arrays.asList(2, 3, 5, 7, 11, 13, 17);
		ctx.setVariable("primes", primes);

		Expression expr = parser.parseRaw("#favouriteColour");
		Object value = expr.getValue(ctx);
		assertThat(value).isEqualTo("blue");

		expr = parser.parseRaw("#primes.get(1)");
		value = expr.getValue(ctx);
		assertThat(value).isEqualTo(3);

		// all prime numbers > 10 from the list (using selection ?{...})
		expr = parser.parseRaw("#primes.?[#this>10]");
		value = expr.getValue(ctx);
		assertThat(value.toString()).isEqualTo("[11, 13, 17]");
	}

	/**
	 * Scenario: using your own root context object
	 */
	@Test
	void testScenario_UsingADifferentRootContextObject() {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Use the standard evaluation context
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		TestClass tc = new TestClass();
		tc.setProperty(42);
		tc.str = "wibble";
		ctx.setRootObject(tc);

		// read it, set it, read it again
		Expression expr = parser.parseRaw("str");
		Object value = expr.getValue(ctx);
		assertThat(value).isEqualTo("wibble");
		expr = parser.parseRaw("str");
		expr.setValue(ctx, "wobble");
		expr = parser.parseRaw("str");
		value = expr.getValue(ctx);
		assertThat(value).isEqualTo("wobble");
		// or using assignment within the expression
		expr = parser.parseRaw("str='wabble'");
		value = expr.getValue(ctx);
		expr = parser.parseRaw("str");
		value = expr.getValue(ctx);
		assertThat(value).isEqualTo("wabble");

		// private property will be accessed through getter()
		expr = parser.parseRaw("property");
		value = expr.getValue(ctx);
		assertThat(value).isEqualTo(42);

		// ... and set through setter
		expr = parser.parseRaw("property=4");
		value = expr.getValue(ctx);
		expr = parser.parseRaw("property");
		value = expr.getValue(ctx);
		assertThat(value).isEqualTo(4);
	}

	public static String repeat(String s) { return s+s; }

	/**
	 * Scenario: using your own java methods and calling them from the expression
	 */
	@Test
	void testScenario_RegisteringJavaMethodsAsFunctionsAndCallingThem() throws Exception {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Use the standard evaluation context
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.registerFunction("repeat", ExpressionLanguageScenarioTests.class.getDeclaredMethod("repeat", String.class));

		Expression expr = parser.parseRaw("#repeat('hello')");
		Object value = expr.getValue(ctx);
		assertThat(value).isEqualTo("hellohello");
	}

	/**
	 * Scenario: looking up your own MethodHandles and calling them from the expression
	 */
	@Test
	void testScenario_RegisteringJavaMethodsAsMethodHandlesAndCallingThem() throws Exception {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		//this.context is already populated with all relevant MethodHandle examples

		Expression expr = parser.parseRaw("#message('Message with %s words: <%s>', 2, 'Hello World', 'ignored')");
		Object value = expr.getValue(this.context);
		assertThat(value).isEqualTo("Message with 2 words: <Hello World>");

		expr = parser.parseRaw("#messageTemplate('bound', 2, 'Hello World', 'ignored')");
		value = expr.getValue(this.context);
		assertThat(value).isEqualTo("This is a bound message with 2 words: <Hello World>");

		expr = parser.parseRaw("#messageBound()");
		value = expr.getValue(this.context);
		assertThat(value).isEqualTo("This is a prerecorded message with 3 words: <Oh Hello World>");

		Expression staticExpr = parser.parseRaw("#messageStatic('Message with %s words: <%s>', 2, 'Hello World', 'ignored')");
		Object staticValue = staticExpr.getValue(this.context);
		assertThat(staticValue).isEqualTo("Message with 2 words: <Hello World>");

		staticExpr = parser.parseRaw("#messageStaticTemplate('bound', 2, 'Hello World', 'ignored')");
		staticValue = staticExpr.getValue(this.context);
		assertThat(staticValue).isEqualTo("This is a bound message with 2 words: <Hello World>");

		staticExpr = parser.parseRaw("#messageStaticBound()");
		staticValue = staticExpr.getValue(this.context);
		assertThat(staticValue).isEqualTo("This is a prerecorded message with 3 words: <Oh Hello World>");
	}

	/**
	 * Scenario: add a property resolver that will get called in the resolver chain, this one only supports reading.
	 */
	@Test
	void testScenario_AddingYourOwnPropertyResolvers_1() {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Use the standard evaluation context
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		ctx.addPropertyAccessor(new FruitColourAccessor());
		Expression expr = parser.parseRaw("orange");
		Object value = expr.getValue(ctx);
		assertThat(value).isEqualTo(Color.orange);
		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> expr.setValue(ctx, Color.blue))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL));
	}

	@Test
	void testScenario_AddingYourOwnPropertyResolvers_2() {
		// Create a parser
		SpelExpressionParser parser = new SpelExpressionParser();
		// Use the standard evaluation context
		StandardEvaluationContext ctx = new StandardEvaluationContext();

		ctx.addPropertyAccessor(new VegetableColourAccessor());
		Expression expr = parser.parseRaw("pea");
		Object value = expr.getValue(ctx);
		assertThat(value).isEqualTo(Color.green);

		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> expr.setValue(ctx, Color.blue))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL));
	}


	static class TestClass {
		public String str;
		private int property;
		public int getProperty() { return property; }
		public void setProperty(int i) { property = i; }
	}


	/**
	 * Regardless of the current context object, or root context object, this resolver can tell you what colour a fruit is !
	 * It only supports property reading, not writing.  To support writing it would need to override canWrite() and write()
	 */
	private static class FruitColourAccessor implements PropertyAccessor {

		private static final Map<String,Color> propertyMap = Map.of(
				"banana", Color.yellow,
				"apple", Color.red,
				"orange", Color.orange
			);

		/**
		 * Null means you might be able to read any property, if an earlier property resolver hasn't beaten you to it
		 */
		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return propertyMap.containsKey(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			return new TypedValue(propertyMap.get(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
		}
	}


	/**
	 * Regardless of the current context object, or root context object, this resolver can tell you what colour a vegetable is !
	 * It only supports property reading, not writing.
	 */
	private static class VegetableColourAccessor implements PropertyAccessor {

		private static Map<String,Color> propertyMap = Map.of(
				"carrot", Color.orange,
				"pea", Color.green
			);

		/**
		 * Null means you might be able to read any property, if an earlier property resolver hasn't beaten you to it
		 */
		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return propertyMap.containsKey(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			return new TypedValue(propertyMap.get(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
		}
	}

}
