/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.expression.spel.SpelMessage.FUNCTION_MUST_BE_STATIC;
import static org.springframework.expression.spel.SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION;

/**
 * Tests the evaluation of expressions that access variables and functions.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @see ConstructorInvocationTests
 * @see MethodInvocationTests
 */
class VariableAndFunctionTests extends AbstractExpressionTests {

	@Test
	void variableAccess() {
		evaluate("#answer", "42", Integer.class, SHOULD_BE_WRITABLE);
		evaluate("#answer / 2", 21, Integer.class, SHOULD_NOT_BE_WRITABLE);
	}

	@Test
	void variableAccessWithWellKnownVariables() {
		evaluate("#this.getName()", "Nikola Tesla", String.class);
		evaluate("#root.getName()", "Nikola Tesla", String.class);
	}

	@Test
	void functionInvocationWithIncorrectNumberOfArguments() {
		// Method: #reverseInt(int, int, int)
		evaluateAndCheckError("#reverseInt()", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "reverseInt", 0, 3);
		evaluateAndCheckError("#reverseInt(1,2)", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "reverseInt", 2, 3);
		evaluateAndCheckError("#reverseInt(1,2,3,4)", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "reverseInt", 4, 3);

		// MethodHandle: #message(String, Object...)
		evaluateAndCheckError("#message()", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "message", 0, "1 or more");

		// MethodHandle: #add(int, int)
		evaluateAndCheckError("#add()", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "add", 0, 2);
		evaluateAndCheckError("#add(1)", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "add", 1, 2);
		evaluateAndCheckError("#add(1, 2, 3)", INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION, 0, "add", 3, 2);
	}

	@Test
	void functionInvocationWithPrimitiveArguments() {
		evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
		evaluate("#reverseInt('1',2,3)", "int[3]{3,2,1}", int[].class); // requires type conversion of '1' to 1
	}

	@Test
	void functionInvocationWithStringArgument() {
		evaluate("#reverseString('hello')", "olleh", String.class);
		evaluate("#reverseString(37)", "73", String.class); // requires type conversion of 37 to '37'
	}

	@Test
	void functionWithVarargs() {
		// static String varargsFunction(String... strings) -> Arrays.toString(strings)

		evaluate("#varargsFunction()", "[]", String.class);
		evaluate("#varargsFunction(new String[0])", "[]", String.class);
		evaluate("#varargsFunction('a')", "[a]", String.class);
		evaluate("#varargsFunction('a','b','c')", "[a, b, c]", String.class);
		evaluate("#varargsFunction(new String[]{'a','b','c'})", "[a, b, c]", String.class);
		// Conversion from int to String
		evaluate("#varargsFunction(25)", "[25]", String.class);
		evaluate("#varargsFunction('b',25)", "[b, 25]", String.class);
		evaluate("#varargsFunction(new int[]{1, 2, 3})", "[1, 2, 3]", String.class);
		// Strings that contain a comma
		evaluate("#varargsFunction('a,b')", "[a,b]", String.class);
		evaluate("#varargsFunction('a', 'x,y', 'd')", "[a, x,y, d]", String.class);
		// null values
		evaluate("#varargsFunction(null)", "[null]", String.class);
		evaluate("#varargsFunction('a',null,'b')", "[a, null, b]", String.class);

		evaluate("#varargsFunction2(9)", "9-[]", String.class);
		evaluate("#varargsFunction2(9, new String[0])", "9-[]", String.class);
		evaluate("#varargsFunction2(9,'a')", "9-[a]", String.class);
		evaluate("#varargsFunction2(9,'a','b','c')", "9-[a, b, c]", String.class);
		// Conversion from int to String
		evaluate("#varargsFunction2(9,25)", "9-[25]", String.class);
		evaluate("#varargsFunction2(9,'b',25)", "9-[b, 25]", String.class);
		// Strings that contain a comma:
		evaluate("#varargsFunction2(9, 'a,b')", "9-[a,b]", String.class);
		evaluate("#varargsFunction2(9, 'a', 'x,y', 'd')", "9-[a, x,y, d]", String.class);
		// null values
		evaluate("#varargsFunction2(9,null)", "9-[null]", String.class);
		evaluate("#varargsFunction2(9,'a',null,'b')", "9-[a, null, b]", String.class);

		evaluate("#varargsObjectFunction()", "[]", String.class);
		evaluate("#varargsObjectFunction(new String[0])", "[]", String.class);
		evaluate("#varargsObjectFunction('a')", "[a]", String.class);
		evaluate("#varargsObjectFunction('a','b','c')", "[a, b, c]", String.class);
		evaluate("#varargsObjectFunction(new String[]{'a','b','c'})", "[a, b, c]", String.class);
		// Conversion from int to String
		evaluate("#varargsObjectFunction(25)", "[25]", String.class);
		evaluate("#varargsObjectFunction('b',25)", "[b, 25]", String.class);
		// Strings that contain a comma
		evaluate("#varargsObjectFunction('a,b')", "[a,b]", String.class);
		evaluate("#varargsObjectFunction('a', 'x,y', 'd')", "[a, x,y, d]", String.class);
		// null values
		evaluate("#varargsObjectFunction(null)", "[null]", String.class);
		evaluate("#varargsObjectFunction('a',null,'b')", "[a, null, b]", String.class);
	}

	@Test  // gh-33013
	void functionWithVarargsViaMethodHandle() {
		// Calling 'public static String formatObjectVarargs(String format, Object... args)' -> String.format(format, args)

		// No var-args and no conversion necessary
		evaluate("#message('x')", "x", String.class);
		evaluate("#formatObjectVarargs('x')", "x", String.class);

		// No var-args but conversion necessary
		evaluate("#message(9)", "9", String.class);
		evaluate("#formatObjectVarargs(9)", "9", String.class);

		// No conversion necessary
		evaluate("#add(3, 4)", 7, Integer.class);
		evaluate("#message('x -> %s %s %s', 'a', 'b', 'c')", "x -> a b c", String.class);
		evaluate("#formatObjectVarargs('x -> %s', '')", "x -> ", String.class);
		evaluate("#formatObjectVarargs('x -> %s', ' ')", "x ->  ", String.class);
		evaluate("#formatObjectVarargs('x -> %s', 'a')", "x -> a", String.class);
		evaluate("#formatObjectVarargs('x -> %s %s %s', 'a', 'b', 'c')", "x -> a b c", String.class);
		evaluate("#message('x -> %s %s %s', new Object[]{'a', 'b', 'c'})", "x -> a b c", String.class); // Object[] instanceof Object[]
		evaluate("#message('x -> %s %s %s', new String[]{'a', 'b', 'c'})", "x -> a b c", String.class); // String[] instanceof Object[]
		evaluate("#message('x -> %s %s %s', new Integer[]{1, 2, 3})", "x -> 1 2 3", String.class); // Integer[] instanceof Object[]
		evaluate("#formatObjectVarargs('x -> %s %s', 2, 3)", "x -> 2 3", String.class); // Integer instanceof Object
		evaluate("#formatObjectVarargs('x -> %s %s', 'a', 3.0F)", "x -> a 3.0", String.class); // String/Float instanceof Object
		evaluate("#formatObjectVarargs('x -> %s', new Object[]{''})", "x -> ", String.class);
		evaluate("#formatObjectVarargs('x -> %s', new String[]{''})", "x -> ", String.class);
		evaluate("#formatObjectVarargs('x -> %s', new Object[]{' '})", "x ->  ", String.class);
		evaluate("#formatObjectVarargs('x -> %s', new String[]{' '})", "x ->  ", String.class);
		evaluate("#formatObjectVarargs('x -> %s', new Object[]{'a'})", "x -> a", String.class);
		evaluate("#formatObjectVarargs('x -> %s', new String[]{'a'})", "x -> a", String.class);
		evaluate("#formatObjectVarargs('x -> %s %s %s', new Object[]{'a', 'b', 'c'})", "x -> a b c", String.class);
		evaluate("#formatObjectVarargs('x -> %s %s %s', new String[]{'a', 'b', 'c'})", "x -> a b c", String.class);

		// Conversion necessary
		evaluate("#add('2', 5.0)", 7, Integer.class); // String/Double to Integer
		evaluate("#messageStatic('x -> %s %s %s', 1, 2, 3)", "x -> 1 2 3", String.class); // Integer to String
		evaluate("#messageStatic('x -> %s %s %s', new Integer[]{1, 2, 3})", "x -> 1 2 3", String.class); // Integer[] to String[]
		evaluate("#messageStatic('x -> %s %s %s', new int[]{1, 2, 3})", "x -> 1 2 3", String.class); // int[] to String[]
		evaluate("#messageStatic('x -> %s %s %s', new short[]{1, 2, 3})", "x -> 1 2 3", String.class);  // short[] to String[]
		evaluate("#formatObjectVarargs('x -> %s %s %s', new Integer[]{1, 2, 3})", "x -> 1 2 3", String.class); // Integer[] to String[]

		// Individual string contains a comma with multiple varargs arguments
		evaluate("#formatObjectVarargs('foo -> %s %s', ',', 'baz')", "foo -> , baz", String.class);
		evaluate("#formatObjectVarargs('foo -> %s %s', 'bar', ',baz')", "foo -> bar ,baz", String.class);
		evaluate("#formatObjectVarargs('foo -> %s %s', 'bar,', 'baz')", "foo -> bar, baz", String.class);

		// Individual string contains a comma with single varargs argument.
		evaluate("#formatObjectVarargs('foo -> %s', ',')", "foo -> ,", String.class);
		evaluate("#formatObjectVarargs('foo -> %s', ',bar')", "foo -> ,bar", String.class);
		evaluate("#formatObjectVarargs('foo -> %s', 'bar,')", "foo -> bar,", String.class);
		evaluate("#formatObjectVarargs('foo -> %s', 'bar,baz')", "foo -> bar,baz", String.class);
	}

	@Test
	void functionWithPrimitiveVarargsViaMethodHandle() {
		// Calling 'public String formatPrimitiveVarargs(String format, int... nums)' -> effectively String.format(format, args)

		// No var-args and no conversion necessary
		evaluate("#formatPrimitiveVarargs(9)", "9", String.class);

		// No var-args but conversion necessary
		evaluate("#formatPrimitiveVarargs('7')", "7", String.class);

		// No conversion necessary
		evaluate("#formatPrimitiveVarargs('x -> %s', 9)", "x -> 9", String.class);
		evaluate("#formatPrimitiveVarargs('x -> %s %s %s', 1, 2, 3)", "x -> 1 2 3", String.class);
		evaluate("#formatPrimitiveVarargs('x -> %s', new int[]{1})", "x -> 1", String.class);
		evaluate("#formatPrimitiveVarargs('x -> %s %s %s', new int[]{1, 2, 3})", "x -> 1 2 3", String.class);

		// Conversion necessary
		evaluate("#formatPrimitiveVarargs('x -> %s %s', '2', '3')", "x -> 2 3", String.class); // String to int
		evaluate("#formatPrimitiveVarargs('x -> %s %s', '2', 3.0F)", "x -> 2 3", String.class); // String/Float to int
		evaluate("#formatPrimitiveVarargs('x -> %s %s %s', new Integer[]{1, 2, 3})", "x -> 1 2 3", String.class); // Integer[] to int[]
		evaluate("#formatPrimitiveVarargs('x -> %s %s %s', new String[]{'1', '2', '3'})", "x -> 1 2 3", String.class); // String[] to int[]
	}

	@Test
	void functionFromMethodWithVarargsAndPrimitiveArrayToObjectArrayConversion() {
		evaluate("#varargsObjectFunction(new short[]{1, 2, 3})", "[1, 2, 3]", String.class); // short[] to Object[]
		evaluate("#varargsObjectFunction(new int[]{1, 2, 3})", "[1, 2, 3]", String.class); // int[] to Object[]
	}

	@Test
	void functionFromMethodHandleWithVarargsAndPrimitiveArrayToObjectArrayConversion() {
		evaluate("#message('x -> %s %s %s', new short[]{1, 2, 3})", "x -> 1 2 3", String.class);  // short[] to Object[]
		evaluate("#message('x -> %s %s %s', new int[]{1, 2, 3})", "x -> 1 2 3", String.class); // int[] to Object[]
		evaluate("#formatObjectVarargs('x -> %s %s %s', new int[]{1, 2, 3})", "x -> 1 2 3", String.class); // int[] to Object[]
	}

	@Test  // gh-33315
	void functionFromMethodWithListConvertedToVarargsArray() {
		((StandardTypeLocator) context.getTypeLocator()).registerImport("java.util");
		String expected = "[a, b, c]";

		evaluate("#varargsFunction(T(List).of('a', 'b', 'c'))", expected, String.class);
		evaluate("#varargsFunction({'a', 'b', 'c'})", expected, String.class);

		// Calling 'public String formatObjectVarargs(String format, Object... args)' -> String.format(format, args)
		evaluate("#varargsObjectFunction(T(List).of('a', 'b', 'c'))", expected, String.class);
		evaluate("#varargsObjectFunction({'a', 'b', 'c'})", expected, String.class);
	}

	@Test  // gh-33315
	void functionFromMethodHandleWithListConvertedToVarargsArray() {
		((StandardTypeLocator) context.getTypeLocator()).registerImport("java.util");
		String expected = "x -> a b c";

		// Calling 'public static String message(String template, String... args)' -> template.formatted((Object[]) args)
		evaluate("#message('x -> %s %s %s', T(List).of('a', 'b', 'c'))", expected, String.class);
		evaluate("#message('x -> %s %s %s', {'a', 'b', 'c'})", expected, String.class);

		// Calling 'public static String formatObjectVarargs(String format, Object... args)' -> String.format(format, args)
		evaluate("#formatObjectVarargs('x -> %s %s %s', T(List).of('a', 'b', 'c'))", expected, String.class);
		evaluate("#formatObjectVarargs('x -> %s %s %s', {'a', 'b', 'c'})", expected, String.class);
	}

	@Test  // gh-34109
	void functionViaMethodHandleForStaticMethodThatAcceptsOnlyVarargs() {
		// #varargsFunctionHandle: static String varargsFunction(String... strings) -> Arrays.toString(strings)

		evaluate("#varargsFunctionHandle()", "[]", String.class);
		evaluate("#varargsFunctionHandle(new String[0])", "[]", String.class);
		evaluate("#varargsFunctionHandle('a')", "[a]", String.class);
		evaluate("#varargsFunctionHandle('a','b','c')", "[a, b, c]", String.class);
		evaluate("#varargsFunctionHandle(new String[]{'a','b','c'})", "[a, b, c]", String.class);
		// Conversion from int to String
		evaluate("#varargsFunctionHandle(25)", "[25]", String.class);
		evaluate("#varargsFunctionHandle('b',25)", "[b, 25]", String.class);
		evaluate("#varargsFunctionHandle(new int[]{1, 2, 3})", "[1, 2, 3]", String.class);
		// Strings that contain a comma
		evaluate("#varargsFunctionHandle('a,b')", "[a,b]", String.class);
		evaluate("#varargsFunctionHandle('a', 'x,y', 'd')", "[a, x,y, d]", String.class);
		// null values
		evaluate("#varargsFunctionHandle(null)", "[null]", String.class);
		evaluate("#varargsFunctionHandle('a',null,'b')", "[a, null, b]", String.class);
	}

	@Test
	void functionMethodMustBeStatic() throws Exception {
		context.registerFunction("nonStatic", getClass().getMethod("nonStatic"));
		SpelExpression expression = parser.parseRaw("#nonStatic()");
		assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> expression.getValue(context))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(FUNCTION_MUST_BE_STATIC));
	}


	// this method is used by the test above
	public void nonStatic() {
	}


	@Nested  // gh-34371
	class VarargsAndPojoToArrayConversionTests {

		private final StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();

		private final ArrayHolder arrayHolder = new ArrayHolder("a", "b", "c");


		@BeforeEach
		void setUp() {
			DefaultConversionService conversionService = new DefaultConversionService();
			conversionService.addConverter(new ArrayHolderConverter());
			context.setTypeConverter(new StandardTypeConverter(conversionService));
			context.setVariable("arrayHolder", arrayHolder);
		}

		@Test
		void functionWithVarargsAndPojoToArrayConversion() {
			// #varargsFunction: static String varargsFunction(String... strings) -> Arrays.toString(strings)
			evaluate("#varargsFunction(#arrayHolder)", "[a, b, c]");

			// #varargsObjectFunction: static String varargsObjectFunction(Object... args) -> Arrays.toString(args)
			//
			// Since ArrayHolder is an "instanceof Object" and Object is the varargs component type,
			// we expect the ArrayHolder not to be converted to an array but rather to be passed
			// "as is" as a single argument to the varargs method.
			evaluate("#varargsObjectFunction(#arrayHolder)", "[" + arrayHolder.toString() + "]");
		}

		@Test
		void functionWithVarargsAndPojoToArrayConversionViaMethodHandle() {
			// #varargsFunctionHandle: static String varargsFunction(String... strings) -> Arrays.toString(strings)
			evaluate("#varargsFunctionHandle(#arrayHolder)", "[a, b, c]");

			// #varargsObjectFunctionHandle: static String varargsObjectFunction(Object... args) -> Arrays.toString(args)
			//
			// Since ArrayHolder is an "instanceof Object" and Object is the varargs component type,
			// we expect the ArrayHolder not to be converted to an array but rather to be passed
			// "as is" as a single argument to the varargs method.
			evaluate("#varargsObjectFunctionHandle(#arrayHolder)", "[" + arrayHolder.toString() + "]");
		}

		private void evaluate(String expression, Object expectedValue) {
			Expression expr = parser.parseExpression(expression);
			assertThat(expr).as("expression").isNotNull();
			Object value = expr.getValue(context);
			assertThat(value).as("expression '" + expression + "'").isEqualTo(expectedValue);
		}


		record ArrayHolder(String... array) {
		}

		static class ArrayHolderConverter implements GenericConverter {

			@Override
			public @Nullable Set<ConvertiblePair> getConvertibleTypes() {
				return Set.of(new ConvertiblePair(ArrayHolder.class, Object[].class));
			}

			@Override
			public @Nullable String[] convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
				return ((ArrayHolder) source).array();
			}
		}

	}

}
