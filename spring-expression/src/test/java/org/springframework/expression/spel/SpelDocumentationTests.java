/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import example.Color;
import example.FruitMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectiveIndexAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.Inventor;
import org.springframework.expression.spel.testresources.PlaceOfBirth;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Test the examples used in the reference documentation.
 *
 * <p>NOTE: any changes in this file may indicate that you need to update the
 * documentation too!
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
@SuppressWarnings("rawtypes")
class SpelDocumentationTests extends AbstractExpressionTests {

	private static final Inventor tesla;

	private static final Inventor pupin;

	static {
		GregorianCalendar c = new GregorianCalendar();
		c.set(1856, 7, 9);
		tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");
		tesla.setPlaceOfBirth(new PlaceOfBirth("Smiljan"));
		tesla.setInventions("Telephone repeater", "Rotating magnetic field principle",
				"Polyphase alternating-current system", "Induction motor", "Alternating-current power transmission",
				"Tesla coil transformer", "Wireless communication", "Radio", "Fluorescent lights");

		pupin = new Inventor("Pupin", c.getTime(), "Idvor");
		pupin.setPlaceOfBirth(new PlaceOfBirth("Idvor"));
	}


	@Nested
	class Miscellaneous {

		@Test
		void methodInvocation() {
			evaluate("'Hello World'.concat('!')", "Hello World!", String.class);
		}

		@Test
		void beanPropertyAccess() {
			evaluate("new String('Hello World'.bytes)", "Hello World", String.class);
		}

		@Test
		void arrayLengthAccess() {
			evaluate("'Hello World'.bytes.length", 11, Integer.class);
		}

		@Test
		void rootObject() {
			GregorianCalendar c = new GregorianCalendar();
			c.set(1856, 7, 9);

			// The constructor arguments are name, birthday, and nationality.
			Inventor tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");

			ExpressionParser parser = new SpelExpressionParser();
			Expression exp = parser.parseExpression("name");

			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setRootObject(tesla);

			String name = (String) exp.getValue(context);
			assertThat(name).isEqualTo("Nikola Tesla");
		}

		@Test
		void equalityCheck() {
			ExpressionParser parser = new SpelExpressionParser();

			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setRootObject(tesla);

			Expression exp = parser.parseExpression("name == 'Nikola Tesla'");
			boolean isEqual = exp.getValue(context, Boolean.class);  // evaluates to true
			assertThat(isEqual).isTrue();
		}
	}

	@Nested
	class ExpressionsInBeanDefinitions {

		@Test
		void xmlBasedConfig() {
			evaluate("(T(java.lang.Math).random() * 100.0 )>0",true,Boolean.class);
		}
	}

	@Nested
	class LiteralExpressions {

		@Test
		void literals() {
			ExpressionParser parser = new SpelExpressionParser();

			String helloWorld = (String) parser.parseExpression("'Hello World'").getValue(); // evals to "Hello World"
			assertThat(helloWorld).isEqualTo("Hello World");

			double avogadrosNumber = (Double) parser.parseExpression("6.0221415E+23").getValue();
			assertThat(avogadrosNumber).isCloseTo(6.0221415E+23, within((double) 0));

			int maxValue = (Integer) parser.parseExpression("0x7FFFFFFF").getValue();  // evals to 2147483647
			assertThat(maxValue).isEqualTo(Integer.MAX_VALUE);

			boolean trueValue = (Boolean) parser.parseExpression("true").getValue();
			assertThat(trueValue).isTrue();

			Object nullValue = parser.parseExpression("null").getValue();
			assertThat(nullValue).isNull();
		}
	}

	@Nested
	class PropertiesArraysListsMapsAndIndexers {

		@Test
		void propertyNavigation() {
			EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();

			// evaluates to 1856
			int year = (Integer) parser.parseExpression("Birthdate.Year + 1900").getValue(context); // 1856
			assertThat(year).isEqualTo(1856);

			// evaluates to "Smiljan"
			String city = (String) parser.parseExpression("placeOfBirth.City").getValue(context);
			assertThat(city).isEqualTo("Smiljan");
		}

		@Test
		void indexingIntoArraysAndCollections() {
			ExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext teslaContext = TestScenarioCreator.getTestEvaluationContext();
			StandardEvaluationContext societyContext = new StandardEvaluationContext();
			societyContext.setRootObject(new IEEE());

			// Inventions Array

			// evaluates to "Induction motor"
			String invention = parser.parseExpression("inventions[3]").getValue(teslaContext, String.class);
			assertThat(invention).isEqualTo("Induction motor");

			// Members List

			// evaluates to "Nikola Tesla"
			String name = parser.parseExpression("members[0].Name").getValue(societyContext, String.class);
			assertThat(name).isEqualTo("Nikola Tesla");

			// List and Array Indexing

			// evaluates to "Wireless communication"
			invention = parser.parseExpression("members[0].Inventions[6]").getValue(societyContext, String.class);
			assertThat(invention).isEqualTo("Wireless communication");
		}

		@Test
		void indexingIntoStrings() {
			ExpressionParser parser = new SpelExpressionParser();
			StandardEvaluationContext societyContext = new StandardEvaluationContext();
			societyContext.setRootObject(new IEEE());

			// evaluates to "T" (8th letter of "Nikola Tesla")
			String character = parser.parseExpression("members[0].name[7]")
					.getValue(societyContext, String.class);
			assertThat(character).isEqualTo("T");
		}

		@Test
		void indexingIntoMaps() {
			StandardEvaluationContext societyContext = new StandardEvaluationContext();
			societyContext.setRootObject(new IEEE());

			// Officer's Map

			// evaluates to Inventor("Pupin")
			Inventor pupin = parser.parseExpression("officers['president']")
					.getValue(societyContext, Inventor.class);
			assertThat(pupin).isNotNull();
			assertThat(pupin.getName()).isEqualTo("Pupin");

			// evaluates to "Idvor"
			String city = parser.parseExpression("officers['president'].placeOfBirth.city")
					.getValue(societyContext, String.class);
			assertThat(city).isEqualTo("Idvor");

			String countryExpression = "officers['advisors'][0].placeOfBirth.Country";

			// setting values
			parser.parseExpression(countryExpression)
					.setValue(societyContext, "Croatia");

			// evaluates to "Croatia"
			String country = parser.parseExpression(countryExpression)
					.getValue(societyContext, String.class);
			assertThat(country).isEqualTo("Croatia");
		}

		@Test
		void indexingIntoObjects() {
			ExpressionParser parser = new SpelExpressionParser();

			// Create an inventor to use as the root context object.
			Inventor tesla = new Inventor("Nikola Tesla");

			// evaluates to "Nikola Tesla"
			String name = parser.parseExpression("#root['name']")
					.getValue(context, tesla, String.class);
			assertThat(name).isEqualTo("Nikola Tesla");
		}

		@Test
		void indexingIntoCustomStructure() {
			// Create a ReflectiveIndexAccessor for FruitMap
			IndexAccessor fruitMapAccessor = new ReflectiveIndexAccessor(
					FruitMap.class, Color.class, "getFruit", "setFruit");

			// Register the IndexAccessor for FruitMap
			context.addIndexAccessor(fruitMapAccessor);

			// Register the fruitMap variable
			context.setVariable("fruitMap", new FruitMap());

			// evaluates to "cherry"
			String fruit = parser.parseExpression("#fruitMap[T(example.Color).RED]")
					.getValue(context, String.class);
			assertThat(fruit).isEqualTo("cherry");
		}

	}

	@Nested
	class Methods {

		@Test
		void methodInvocation() {
			// string literal, evaluates to "bc"
			String c = parser.parseExpression("'abc'.substring(1, 3)").getValue(String.class);
			assertThat(c).isEqualTo("bc");

			StandardEvaluationContext societyContext = new StandardEvaluationContext();
			societyContext.setRootObject(new IEEE());
			// evaluates to true
			boolean isMember = parser.parseExpression("isMember('Mihajlo Pupin')").getValue(societyContext, Boolean.class);
			assertThat(isMember).isTrue();
		}
	}

	@Nested
	class Operators {

		@Test
		void standardRelationalOperators() {
			boolean result = parser.parseExpression("2 == 2").getValue(Boolean.class);
			assertThat(result).isTrue();

			// evaluates to false
			result = parser.parseExpression("2 < -5.0").getValue(Boolean.class);
			assertThat(result).isFalse();

			// evaluates to true
			result = parser.parseExpression("'black' < 'block'").getValue(Boolean.class);
			assertThat(result).isTrue();
		}

		@Test
		void otherRelationalOperators() {
			boolean result;

			// evaluates to true
			result = parser.parseExpression(
					"1 between {1, 5}").getValue(Boolean.class);
			assertThat(result).isTrue();

			// evaluates to false
			result = parser.parseExpression(
					"1 between {10, 15}").getValue(Boolean.class);
			assertThat(result).isFalse();

			// evaluates to true
			result = parser.parseExpression(
					"'elephant' between {'aardvark', 'zebra'}").getValue(Boolean.class);
			assertThat(result).isTrue();

			// evaluates to false
			result = parser.parseExpression(
					"'elephant' between {'aardvark', 'cobra'}").getValue(Boolean.class);
			assertThat(result).isFalse();

			// evaluates to true
			result = parser.parseExpression(
					"123 instanceof T(Integer)").getValue(Boolean.class);
			assertThat(result).isTrue();

			// evaluates to false
			result = parser.parseExpression(
					"'xyz' instanceof T(Integer)").getValue(Boolean.class);
			assertThat(result).isFalse();

			// evaluates to true
			result = parser.parseExpression(
					"'5.00' matches '^-?\\d+(\\.\\d{2})?$'").getValue(Boolean.class);
			assertThat(result).isTrue();

			// evaluates to false
			result = parser.parseExpression(
					"'5.0067' matches '^-?\\d+(\\.\\d{2})?$'").getValue(Boolean.class);
			assertThat(result).isFalse();
		}

		@Test
		void logicalOperators() {
			StandardEvaluationContext societyContext = new StandardEvaluationContext();
			societyContext.setRootObject(new IEEE());

			// -- AND --

			// evaluates to false
			boolean falseValue = parser.parseExpression("true and false").getValue(Boolean.class);
			assertThat(falseValue).isFalse();
			// evaluates to true
			String expression = "isMember('Nikola Tesla') and isMember('Mihajlo Pupin')";
			boolean trueValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);

			// -- OR --

			// evaluates to true
			trueValue = parser.parseExpression("true or false").getValue(Boolean.class);
			assertThat(trueValue).isTrue();

			// evaluates to true
			expression = "isMember('Nikola Tesla') or isMember('Albert Einstien')";
			trueValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);
			assertThat(trueValue).isTrue();

			// -- NOT --

			// evaluates to false
			falseValue = parser.parseExpression("!true").getValue(Boolean.class);
			assertThat(falseValue).isFalse();

			// -- AND and NOT --

			expression = "isMember('Nikola Tesla') and !isMember('Mihajlo Pupin')";
			falseValue = parser.parseExpression(expression).getValue(societyContext, Boolean.class);
			assertThat(falseValue).isFalse();
		}

		@Test
		void stringOperators() {
			// -- Concatenation --

			// evaluates to "hello world"
			String helloWorld = parser.parseExpression("'hello' + ' ' + 'world'").getValue(String.class);
			assertThat(helloWorld).isEqualTo("hello world");

			// -- Subtraction --

			// evaluates to 'a'
			char ch = parser.parseExpression("'d' - 3").getValue(char.class);
			assertThat(ch).isEqualTo('a');

			// -- Repeat --

			// evaluates to "abcabc"
			String repeated = parser.parseExpression("'abc' * 2").getValue(String.class);
			assertThat(repeated).isEqualTo("abcabc");
		}

		@Test
		void mathematicalOperators() {
			Inventor inventor = new Inventor();
			EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();

			// -- Addition --

			int two = parser.parseExpression("1 + 1").getValue(int.class);  // 2
			assertThat(two).isEqualTo(2);

			// -- Subtraction --

			int four = parser.parseExpression("1 - -3").getValue(int.class);  // 4
			assertThat(four).isEqualTo(4);

			double d = parser.parseExpression("1000.00 - 1e4").getValue(double.class);  // -9000
			assertThat(d).isCloseTo(-9000.0d, within((double) 0));

			// -- Increment --

			// The counter property in Inventor has an initial value of 0.

			// evaluates to 2; counter is now 1
			two = parser.parseExpression("counter++ + 2").getValue(context, inventor, int.class);
			assertThat(two).isEqualTo(2);

			// evaluates to 5; counter is now 2
			int five = parser.parseExpression("3 + ++counter").getValue(context, inventor, int.class);
			assertThat(five).isEqualTo(5);

			// -- Decrement --

			// The counter property in Inventor has a value of 2.

			// evaluates to 6; counter is now 1
			int six = parser.parseExpression("counter-- + 4").getValue(context, inventor, int.class);
			assertThat(six).isEqualTo(6);

			// evaluates to 5; counter is now 0
			five = parser.parseExpression("5 + --counter").getValue(context, inventor, int.class);
			assertThat(five).isEqualTo(5);

			// -- Multiplication --

			six = parser.parseExpression("-2 * -3").getValue(int.class);  // 6
			assertThat(six).isEqualTo(6);

			double twentyFour = parser.parseExpression("2.0 * 3e0 * 4").getValue(double.class);  // 24.0
			assertThat(twentyFour).isCloseTo(24.0d, within((double) 0));

			// -- Division --

			int minusTwo = parser.parseExpression("6 / -3").getValue(int.class);  // -2
			assertThat(minusTwo).isEqualTo(-2);

			double one = parser.parseExpression("8.0 / 4e0 / 2").getValue(double.class);  // 1.0
			assertThat(one).isCloseTo(1.0d, within((double) 0));

			// -- Modulus --

			int three = parser.parseExpression("7 % 4").getValue(int.class);  // 3
			assertThat(three).isEqualTo(3);

			int oneInt = parser.parseExpression("8 / 5 % 2").getValue(int.class);  // 1
			assertThat(oneInt).isEqualTo(1);

			// -- Exponential power --

			int maxInt = parser.parseExpression("(2^31) - 1").getValue(int.class);  // Integer.MAX_VALUE
			assertThat(maxInt).isEqualTo(Integer.MAX_VALUE);

			int minInt = parser.parseExpression("-2^31").getValue(int.class);  // Integer.MIN_VALUE
			assertThat(minInt).isEqualTo(Integer.MIN_VALUE);

			// -- Operator precedence --

			int minusTwentyOne = parser.parseExpression("1+2-3*8").getValue(int.class);  // -21
			assertThat(minusTwentyOne).isEqualTo(-21);
		}

		@Test
		void assignment() {
			Inventor inventor = new Inventor();
			EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();

			parser.parseExpression("foo").setValue(context, inventor, "Alexander Seovic2");

			assertThat(parser.parseExpression("foo").getValue(context, inventor, String.class)).isEqualTo("Alexander Seovic2");

			// alternatively
			String aleks = parser.parseExpression("foo = 'Alexandar Seovic'").getValue(context, inventor, String.class);
			assertThat(parser.parseExpression("foo").getValue(context, inventor, String.class)).isEqualTo("Alexandar Seovic");
			assertThat(aleks).isEqualTo("Alexandar Seovic");
		}

		@Test
		@SuppressWarnings("unchecked")
		void overloadingOperators() {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setOperatorOverloader(new ListConcatenation());

			// evaluates to [1, 2, 3, 4, 5]
			List list = parser.parseExpression("{1, 2, 3} + {2 + 2, 5}").getValue(context, List.class);
			assertThat(list).containsExactly(1, 2, 3, 4, 5);
		}

	}

	@Nested
	class Types {

		@Test
		void types() {
			Class<?> dateClass = parser.parseExpression("T(java.util.Date)").getValue(Class.class);
			assertThat(dateClass).isEqualTo(Date.class);
			boolean trueValue = parser.parseExpression("T(java.math.RoundingMode).CEILING < T(java.math.RoundingMode).FLOOR").getValue(Boolean.class);
			assertThat(trueValue).isTrue();
		}
	}

	@Nested
	class Constructors {

		@Test
		void constructors() {
			StandardEvaluationContext societyContext = new StandardEvaluationContext();
			societyContext.setRootObject(new IEEE());
			Inventor einstein =
					parser.parseExpression("new org.springframework.expression.spel.testresources.Inventor('Albert Einstein',new java.util.Date(), 'German')").getValue(Inventor.class);
			assertThat(einstein.getName()).isEqualTo("Albert Einstein");
			//create new inventor instance within add method of List
			parser.parseExpression("members.add(new org.springframework.expression.spel.testresources.Inventor('Albert Einstein', 'German'))").getValue(societyContext);
		}
	}

	@Nested
	class Variables {

		@Test
		void variables() {
			Inventor tesla = new Inventor("Nikola Tesla", "Serbian");

			EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
			context.setVariable("newName", "Mike Tesla");

			parser.parseExpression("name = #newName").getValue(context, tesla);

			assertThat(tesla.getName()).isEqualTo("Mike Tesla");
		}

		@Test
		@SuppressWarnings("unchecked")
		void thisVariable() {
			// Create a list of prime integers.
			List<Integer> primes = List.of(2, 3, 5, 7, 11, 13, 17);

			// Create parser and set variable 'primes' as the list of integers.
			ExpressionParser parser = new SpelExpressionParser();
			EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
			context.setVariable("primes", primes);

			// Select all prime numbers > 10 from the list (using selection ?{...}).
			String expression = "#primes.?[#this > 10]";

			// Evaluates to a list containing [11, 13, 17].
			List<Integer> primesGreaterThanTen =
					parser.parseExpression(expression).getValue(context, List.class);

			assertThat(primesGreaterThanTen).containsExactly(11, 13, 17);
		}

		@Test
		@SuppressWarnings("unchecked")
		void thisAndRootVariables() {
			// Create parser and evaluation context.
			ExpressionParser parser = new SpelExpressionParser();
			EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();

			// Create an inventor to use as the root context object.
			Inventor tesla = new Inventor("Nikola Tesla");
			tesla.setInventions("Telephone repeater", "Tesla coil transformer");

			// Iterate over all inventions of the Inventor referenced as the #root
			// object, and generate a list of strings whose contents take the form
			// "<inventor's name> invented the <invention>." (using projection !{...}).
			String expression = "#root.inventions.![#root.name + ' invented the ' + #this + '.']";

			// Evaluates to a list containing:
			// Nikola Tesla invented the Telephone repeater.
			// Nikola Tesla invented the Tesla coil transformer.
			List<String> results = parser.parseExpression(expression).getValue(context, tesla, List.class);

			assertThat(results).containsExactly(
				"Nikola Tesla invented the Telephone repeater.",
					"Nikola Tesla invented the Tesla coil transformer.");
		}
	}

	@Nested
	class Functions {

		private final ExpressionParser parser = new SpelExpressionParser();

		private final StandardEvaluationContext context = new StandardEvaluationContext();

		private final Method reverseStringMethod = ReflectionUtils.findMethod(StringUtils.class, "reverseString", String.class);

		@Test
		void registerFunctionViaMethodWithStandardEvaluationContext() {
			context.registerFunction("reverseString", reverseStringMethod);

			String result = parser.parseExpression("#reverseString('hello world')").getValue(context, String.class);
			assertThat(result).isEqualTo("dlrow olleh");
		}

		@Test
		void registerFunctionViaMethodWithSimpleEvaluationContext() {
			SimpleEvaluationContext simpleContext = SimpleEvaluationContext.forReadOnlyDataBinding().build();
			simpleContext.setVariable("reverseString", reverseStringMethod);

			String result = parser.parseExpression("#reverseString('hello world')").getValue(simpleContext, String.class);
			assertThat(result).isEqualTo("dlrow olleh");
		}

		@Test
		void registerFunctionViaMethodHandleNotBound() throws Exception {
			MethodHandle methodHandle = MethodHandles.lookup().findVirtual(String.class, "formatted",
				MethodType.methodType(String.class, Object[].class));
			context.registerFunction("message", methodHandle);

			String message = parser.parseExpression("#message('Simple message: <%s>', 'Hello World', 'ignored')")
					.getValue(context, String.class);
			assertThat(message).isEqualTo("Simple message: <Hello World>");
		}

		@Test
		void registerFunctionViaMethodHandleFullyBound() throws Exception {
			String template = "This is a %s message with %s words: <%s>";
			Object varargs = new Object[] { "prerecorded", 3, "Oh Hello World!", "ignored" };
			MethodHandle methodHandle = MethodHandles.lookup().findVirtual(String.class, "formatted",
				MethodType.methodType(String.class, Object[].class))
					.bindTo(template)
					.bindTo(varargs); // here we have to provide arguments in a single array binding
			context.registerFunction("message", methodHandle);

			String message = parser.parseExpression("#message()").getValue(context, String.class);
			assertThat(message).isEqualTo("This is a prerecorded message with 3 words: <Oh Hello World!>");
		}
	}

	@Nested
	class TernaryOperator {

		@Test
		void ternary() {
			String falseString = parser.parseExpression("false ? 'trueExp' : 'falseExp'").getValue(String.class);
			assertThat(falseString).isEqualTo("falseExp");

			StandardEvaluationContext societyContext = new StandardEvaluationContext();
			societyContext.setRootObject(new IEEE());

			parser.parseExpression("Name").setValue(societyContext, "IEEE");
			societyContext.setVariable("queryName", "Nikola Tesla");

			String expression = "isMember(#queryName) ? #queryName + ' is a member of the ' "
					+ "+ Name + ' Society' : #queryName + ' is not a member of the ' + Name + ' Society'";

			String queryResultString = parser.parseExpression(expression).getValue(societyContext, String.class);
			assertThat(queryResultString).isEqualTo("Nikola Tesla is a member of the IEEE Society");
			// queryResultString = "Nikola Tesla is a member of the IEEE Society"
		}
	}

	@Nested
	class SaveNavigationOperator {

		@Test
		void nullSafePropertyAccess() {
			Inventor tesla = new Inventor("Nikola Tesla", "Serbian");
			tesla.setPlaceOfBirth(new PlaceOfBirth("Smiljan"));

			// evaluates to "Smiljan"
			String city = parser.parseExpression("placeOfBirth?.city") // <1>
					.getValue(context, tesla, String.class);
			assertThat(city).isEqualTo("Smiljan");

			tesla.setPlaceOfBirth(null);

			// evaluates to null - does not throw a NullPointerException
			city = parser.parseExpression("placeOfBirth?.city") // <2>
					.getValue(context, tesla, String.class);
			assertThat(city).isNull();
		}

		@Test
		void nullSafeIndexing() {
			IEEE society = new IEEE();
			EvaluationContext context = new StandardEvaluationContext(society);

			// evaluates to Inventor("Nikola Tesla")
			Inventor inventor = parser.parseExpression("members?.[0]") // <1>
					.getValue(context, Inventor.class);
			assertThat(inventor).extracting(Inventor::getName).isEqualTo("Nikola Tesla");

			society.members = null;

			// evaluates to null - does not throw an Exception
			inventor = parser.parseExpression("members?.[0]") // <2>
					.getValue(context, Inventor.class);
			assertThat(inventor).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void nullSafeSelection() {
			IEEE society = new IEEE();
			StandardEvaluationContext context = new StandardEvaluationContext(society);
			String expression = "members?.?[nationality == 'Serbian']"; // <1>

			// evaluates to [Inventor("Nikola Tesla")]
			List<Inventor> list = (List<Inventor>) parser.parseExpression(expression)
					.getValue(context);
			assertThat(list).map(Inventor::getName).containsOnly("Nikola Tesla");

			society.members = null;

			// evaluates to null - does not throw a NullPointerException
			list = (List<Inventor>) parser.parseExpression(expression)
					.getValue(context);
			assertThat(list).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void nullSafeSelectFirst() {
			IEEE society = new IEEE();
			StandardEvaluationContext context = new StandardEvaluationContext(society);
			String expression = "members?.^[nationality == 'Serbian' || nationality == 'Idvor']"; // <1>

			// evaluates to Inventor("Nikola Tesla")
			Inventor inventor = parser.parseExpression(expression)
					.getValue(context, Inventor.class);
			assertThat(inventor).extracting(Inventor::getName).isEqualTo("Nikola Tesla");

			society.members = null;

			// evaluates to null - does not throw a NullPointerException
			inventor = parser.parseExpression(expression)
					.getValue(context, Inventor.class);
			assertThat(inventor).isNull();
		}

		@Test
		void nullSafeSelectFirstAndPropertyAccess() {
			IEEE society = new IEEE();
			StandardEvaluationContext context = new StandardEvaluationContext(society);
			String expression = "members?.^[nationality == 'Serbian']?.name"; // <1>

			// evaluates to "Nikola Tesla"
			String name = parser.parseExpression(expression)
					.getValue(context, String.class);
			assertThat(name).isEqualTo("Nikola Tesla");

			society.members = null;

			// evaluates to null - does not throw a NullPointerException
			name = parser.parseExpression(expression)
					.getValue(context, String.class);
			assertThat(name).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void nullSafeSelectLast() {
			IEEE society = new IEEE();
			StandardEvaluationContext context = new StandardEvaluationContext(society);
			String expression = "members?.$[nationality == 'Serbian' || nationality == 'Idvor']"; // <1>

			// evaluates to Inventor("Pupin")
			Inventor inventor = parser.parseExpression(expression)
					.getValue(context, Inventor.class);
			assertThat(inventor).extracting(Inventor::getName).isEqualTo("Pupin");

			society.members = null;

			// evaluates to null - does not throw a NullPointerException
			inventor = parser.parseExpression(expression)
					.getValue(context, Inventor.class);
			assertThat(inventor).isNull();
		}

		@Test
		@SuppressWarnings("unchecked")
		void nullSafeProjection() {
			IEEE society = new IEEE();
			StandardEvaluationContext context = new StandardEvaluationContext(society);

			// evaluates to ["Smiljan", "Idvor"]
			List placesOfBirth = parser.parseExpression("members?.![placeOfBirth.city]") // <1>
					.getValue(context, List.class);
			assertThat(placesOfBirth).containsExactly("Smiljan", "Idvor");

			society.members = null;

			// evaluates to null - does not throw a NullPointerException
			placesOfBirth = parser.parseExpression("members?.![placeOfBirth.city]") // <2>
					.getValue(context, List.class);
			assertThat(placesOfBirth).isNull();
		}
	}

	@Nested
	class CollectionSelection {

		@Test
		@SuppressWarnings("unchecked")
		void selection() {
			StandardEvaluationContext societyContext = new StandardEvaluationContext(new IEEE());
			// evaluates to ["Nikola Tesla"]
			List<Inventor> list = (List<Inventor>) parser.parseExpression("members.?[nationality == 'Serbian']")
					.getValue(societyContext);
			assertThat(list).map(Inventor::getName).containsOnly("Nikola Tesla");
		}
	}

	@Nested
	class CollectionProjection {

		@Test
		@SuppressWarnings("unchecked")
		void projection() {
			StandardEvaluationContext societyContext = new StandardEvaluationContext(new IEEE());
			// evaluates to ["Smiljan", "Idvor"]
			List placesOfBirth = parser.parseExpression("members.![placeOfBirth.city]")
					.getValue(societyContext, List.class);
			assertThat(placesOfBirth).containsExactly("Smiljan", "Idvor");
		}
	}

	@Nested
	class ExpressionTemplating {

		@Test
		void templating() {
			String randomPhrase =
					parser.parseExpression("random number is ${T(java.lang.Math).random()}",
						new TemplateParserContext()).getValue(String.class);
			assertThat(randomPhrase).startsWith("random number");
		}
	}


	static class IEEE {

		private String name;

		public List<Inventor> members = new ArrayList<>();

		public Map<String,Object> officers = Map.of(
				"president", pupin,
				"advisors", List.of(tesla));

		public List<Map<String, Object>> reverse = new ArrayList<>();

		IEEE() {
			members.add(tesla);
			members.add(pupin);

			reverse.add(officers);
		}

		public boolean isMember(String name) {
			return true;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String n) {
			this.name = n;
		}
	}

	static class StringUtils {

		public static String reverseString(String input) {
			return new StringBuilder(input).reverse().toString();
		}
	}

	private static class ListConcatenation implements OperatorOverloader {

		@Override
		public boolean overridesOperation(Operation operation, Object left, Object right) {
			return (operation == Operation.ADD &&
					left instanceof List && right instanceof List);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object operate(Operation operation, Object left, Object right) {
			if (operation == Operation.ADD &&
					left instanceof List list1 && right instanceof List list2) {

				List result = new ArrayList(list1);
				result.addAll(list2);
				return result;
			}
			throw new UnsupportedOperationException(
				"No overload for operation %s and operands [%s] and [%s]"
					.formatted(operation, left, right));
		}
	}

}
