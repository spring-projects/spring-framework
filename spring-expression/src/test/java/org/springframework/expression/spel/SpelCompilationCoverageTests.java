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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import example.Color;
import example.FruitMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.InlineList;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectiveIndexAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testdata.PersonInOtherPackage;
import org.springframework.expression.spel.testresources.Person;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.expression.spel.SpelMessage.EXCEPTION_DURING_INDEX_READ;
import static org.springframework.expression.spel.standard.SpelExpressionTestUtils.assertIsCompiled;

/**
 * Checks {@link org.springframework.expression.spel.standard.SpelCompiler} behavior.
 *
 * <p>This should cover compilation of all compiled node types.
 *
 * <p>Compiled nodes:
 *
 * TypeReference
 * OperatorInstanceOf
 * StringLiteral
 * NullLiteral
 * RealLiteral
 * IntLiteral
 * LongLiteral
 * BooleanLiteral
 * FloatLiteral
 * OpOr
 * OpAnd
 * OperatorNot
 * Ternary
 * Elvis
 * VariableReference
 * OpLt
 * OpLe
 * OpGt
 * OpGe
 * OpEq
 * OpNe
 * OpPlus
 * OpMinus
 * OpMultiply
 * OpDivide
 * MethodReference
 * PropertyOrFieldReference
 * Indexer
 * CompoundExpression
 * ConstructorReference
 * FunctionReference
 * InlineList
 * OpModulus
 *
 * <p>Not yet compiled (some may never need to be):
 *
 * Assign
 * BeanReference
 * Identifier
 * OpDec
 * OpBetween
 * OpMatches
 * OpPower
 * OpInc
 * Projection
 * QualifiedId
 * Selection
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 4.1
 * @see org.springframework.expression.spel.standard.SpelCompilerTests
 * @see org.springframework.expression.spel.support.ReflectiveIndexAccessorTests
 */
public class SpelCompilationCoverageTests extends AbstractExpressionTests {

	/*
	 * TODO Potential optimizations for SpEL compilation:
	 *
	 * - OpMinus with a single literal operand could be treated as a negative literal. Will save a
	 *   pointless loading of 0 and then a subtract instruction in code generation.
	 *
	 * - allow other accessors/resolvers to participate in compilation and create their own code.
	 *
	 * - A TypeReference followed by (what ends up as) a static method invocation can really skip
	 *   code generation for the TypeReference since once that is used to locate the method it is not
	 *   used again.
	 *
	 * - The opEq implementation is quite basic. It will compare numbers of the same type (allowing
	 *   them to be their boxed or unboxed variants) or compare object references. It does not
	 *   compile expressions where numbers are of different types or when objects implement
	 *   Comparable.
	 */

	private Expression expression;

	private SpelNodeImpl ast;


	@Nested
	class VariableReferenceTests {

		@ParameterizedTest  // gh-32356
		@ValueSource(strings = { "#root", "#this" })
		void rootVariableWithPublicType(String spel) {
			String string = "hello";
			expression = parser.parseExpression(spel);
			Object result = expression.getValue(string, String.class);
			assertThat(result).isEqualTo(string);
			assertCanCompile(expression);
			result = expression.getValue(string, String.class);
			assertThat(result).isEqualTo(string);

			Integer number = 42;
			expression = parser.parseExpression(spel);
			result = expression.getValue(number, Integer.class);
			assertThat(result).isEqualTo(number);
			assertCanCompile(expression);
			result = expression.getValue(number, Integer.class);
			assertThat(result).isEqualTo(number);
		}

		@ParameterizedTest  // gh-32356
		@ValueSource(strings = {
			"#root.empty ? 0 : #root.size",
			"#this.empty ? 0 : #this.size"
		})
		void rootVariableWithNonPublicType(String spel) {
			Map<String, Integer> map = Map.of("a", 13, "b", 42);

			// Prerequisite: root type must not be public for this use case.
			assertNotPublic(map.getClass());

			expression = parser.parseExpression(spel);
			Integer result = expression.getValue(map, Integer.class);
			assertThat(result).isEqualTo(2);
			assertCanCompile(expression);
			result = expression.getValue(map, Integer.class);
			assertThat(result).isEqualTo(2);
		}

		@Test
		void userDefinedVariable() {
			EvaluationContext ctx = new StandardEvaluationContext();
			ctx.setVariable("target", "abc");
			expression = parser.parseExpression("#target");
			assertThat(expression.getValue(ctx)).isEqualTo("abc");
			assertCanCompile(expression);
			assertThat(expression.getValue(ctx)).isEqualTo("abc");
			ctx.setVariable("target", "123");
			assertThat(expression.getValue(ctx)).isEqualTo("123");

			// Changing the variable type from String to Integer results in a
			// ClassCastException in the compiled code.
			ctx.setVariable("target", 42);
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> expression.getValue(ctx))
				.withCauseInstanceOf(ClassCastException.class);

			ctx.setVariable("target", "abc");
			expression = parser.parseExpression("#target.charAt(0)");
			assertThat(expression.getValue(ctx)).isEqualTo('a');
			assertCanCompile(expression);
			assertThat(expression.getValue(ctx)).isEqualTo('a');
			ctx.setVariable("target", "1");
			assertThat(expression.getValue(ctx)).isEqualTo('1');

			// Changing the variable type from String to Integer results in a
			// ClassCastException in the compiled code.
			ctx.setVariable("target", 42);
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> expression.getValue(ctx))
				.withCauseInstanceOf(ClassCastException.class);
		}

	}

	@Nested
	class IndexingTests {

		@Test
		void indexIntoPrimitiveShortArray() {
			short[] shorts = { (short) 33, (short) 44, (short) 55 };

			expression = parser.parseExpression("[2]");

			assertThat(expression.getValue(shorts)).isEqualTo((short) 55);
			assertCanCompile(expression);
			assertThat(expression.getValue(shorts)).isEqualTo((short) 55);
			assertThat(getAst().getExitDescriptor()).isEqualTo("S");
		}

		@Test
		void indexIntoPrimitiveByteArray() {
			byte[] bytes = { (byte) 2, (byte) 3, (byte) 4 };

			expression = parser.parseExpression("[2]");

			assertThat(expression.getValue(bytes)).isEqualTo((byte) 4);
			assertCanCompile(expression);
			assertThat(expression.getValue(bytes)).isEqualTo((byte) 4);
			assertThat(getAst().getExitDescriptor()).isEqualTo("B");
		}

		@Test
		void indexIntoPrimitiveIntArray() {
			int[] ints = { 8, 9, 10 };

			expression = parser.parseExpression("[2]");

			assertThat(expression.getValue(ints)).isEqualTo(10);
			assertCanCompile(expression);
			assertThat(expression.getValue(ints)).isEqualTo(10);
			assertThat(getAst().getExitDescriptor()).isEqualTo("I");
		}

		@Test
		void indexIntoPrimitiveLongArray() {
			long[] longs = { 2L, 3L, 4L };

			expression = parser.parseExpression("[0]");

			assertThat(expression.getValue(longs)).isEqualTo(2L);
			assertCanCompile(expression);
			assertThat(expression.getValue(longs)).isEqualTo(2L);
			assertThat(getAst().getExitDescriptor()).isEqualTo("J");
		}

		@Test
		void indexIntoPrimitiveFloatArray() {
			float[] floats = { 6.0f, 7.0f, 8.0f };

			expression = parser.parseExpression("[0]");

			assertThat(expression.getValue(floats)).isEqualTo(6.0f);
			assertCanCompile(expression);
			assertThat(expression.getValue(floats)).isEqualTo(6.0f);
			assertThat(getAst().getExitDescriptor()).isEqualTo("F");
		}

		@Test
		void indexIntoPrimitiveDoubleArray() {
			double[] doubles = { 3.0d, 4.0d, 5.0d };

			expression = parser.parseExpression("[1]");

			assertThat(expression.getValue(doubles)).isEqualTo(4.0d);
			assertCanCompile(expression);
			assertThat(expression.getValue(doubles)).isEqualTo(4.0d);
			assertThat(getAst().getExitDescriptor()).isEqualTo("D");
		}

		@Test
		void indexIntoPrimitiveCharArray() {
			char[] chars = { 'a', 'b', 'c' };

			expression = parser.parseExpression("[1]");

			assertThat(expression.getValue(chars)).isEqualTo('b');
			assertCanCompile(expression);
			assertThat(expression.getValue(chars)).isEqualTo('b');
			assertThat(getAst().getExitDescriptor()).isEqualTo("C");
		}

		@Test
		void indexIntoPrimitiveBooleanArray() {
			boolean[] booleans = { true, false };

			expression = parser.parseExpression("[1]");

			assertThat(expression.getValue(booleans)).isEqualTo(false);
			assertCanCompile(expression);
			assertThat(expression.getValue(booleans)).isEqualTo(false);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Z");
		}

		@Test
		void indexIntoStringArray() {
			String[] strings = { "a", "b", "c" };

			expression = parser.parseExpression("[0]");

			assertThat(expression.getValue(strings)).isEqualTo("a");
			assertCanCompile(expression);
			assertThat(expression.getValue(strings)).isEqualTo("a");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/String");
		}

		@Test
		void indexIntoNumberArray() {
			Number[] numbers = { 2, 8, 9 };

			expression = parser.parseExpression("[1]");

			assertThat(expression.getValue(numbers)).isEqualTo(8);
			assertCanCompile(expression);
			assertThat(expression.getValue(numbers)).isEqualTo(8);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Number");
		}

		@Test
		void indexInto2DPrimitiveIntArray() {
			int[][] array = new int[][] {
				{ 1, 2, 3 },
				{ 4, 5, 6 }
			};

			expression = parser.parseExpression("[1]");

			assertThat(stringify(expression.getValue(array))).isEqualTo("4 5 6");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(array))).isEqualTo("4 5 6");
			assertThat(getAst().getExitDescriptor()).isEqualTo("[I");

			expression = parser.parseExpression("[1][2]");

			assertThat(stringify(expression.getValue(array))).isEqualTo("6");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(array))).isEqualTo("6");
			assertThat(getAst().getExitDescriptor()).isEqualTo("I");
		}

		@Test
		void indexInto2DStringArray() {
			String[][] array = new String[][] {
				{ "a", "b", "c" },
				{ "d", "e", "f" }
			};

			expression = parser.parseExpression("[1]");

			assertThat(stringify(expression.getValue(array))).isEqualTo("d e f");
			assertCanCompile(expression);
			assertThat(getAst().getExitDescriptor()).isEqualTo("[Ljava/lang/String");
			assertThat(stringify(expression.getValue(array))).isEqualTo("d e f");
			assertThat(getAst().getExitDescriptor()).isEqualTo("[Ljava/lang/String");

			expression = parser.parseExpression("[1][2]");

			assertThat(stringify(expression.getValue(array))).isEqualTo("f");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(array))).isEqualTo("f");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/String");
		}

		@Test
		@SuppressWarnings("unchecked")
		void indexIntoArrayOfListOfString() {
			List<String>[] array = new List[] {
				List.of("a", "b", "c"),
				List.of("d", "e", "f")
			};

			expression = parser.parseExpression("[1]");

			assertThat(stringify(expression.getValue(array))).isEqualTo("d e f");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(array))).isEqualTo("d e f");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/util/List");

			expression = parser.parseExpression("[1][2]");

			assertThat(stringify(expression.getValue(array))).isEqualTo("f");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(array))).isEqualTo("f");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		@SuppressWarnings("unchecked")
		void indexIntoArrayOfMap() {
			Map<String, String>[] array = new Map[] { Map.of("key", "value1") };

			expression = parser.parseExpression("[0]");

			assertThat(stringify(expression.getValue(array))).isEqualTo("{key=value1}");
			assertCanCompile(expression);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/util/Map");
			assertThat(stringify(expression.getValue(array))).isEqualTo("{key=value1}");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/util/Map");

			expression = parser.parseExpression("[0]['key']");

			assertThat(stringify(expression.getValue(array))).isEqualTo("value1");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(array))).isEqualTo("value1");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		void indexIntoListOfString() {
			List<String> list = List.of("aaa", "bbb", "ccc");

			expression = parser.parseExpression("[1]");

			assertThat(expression.getValue(list)).isEqualTo("bbb");
			assertCanCompile(expression);
			assertThat(expression.getValue(list)).isEqualTo("bbb");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		void indexIntoListOfInteger() {
			List<Integer> list = List.of(123, 456, 789);

			expression = parser.parseExpression("[2]");

			assertThat(expression.getValue(list)).isEqualTo(789);
			assertCanCompile(expression);
			assertThat(expression.getValue(list)).isEqualTo(789);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		void indexIntoListOfStringArray() {
			List<String[]> list = List.of(
				new String[] { "a", "b", "c" },
				new String[] { "d", "e", "f" }
			);

			expression = parser.parseExpression("[1]");

			assertThat(stringify(expression.getValue(list))).isEqualTo("d e f");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(list))).isEqualTo("d e f");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			expression = parser.parseExpression("[1][0]");

			assertThat(stringify(expression.getValue(list))).isEqualTo("d");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(list))).isEqualTo("d");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/String");
		}

		@Test
		void indexIntoListOfIntegerArray() {
			List<Integer[]> list = List.of(
				new Integer[] { 1, 2, 3 },
				new Integer[] { 4, 5, 6 }
			);

			expression = parser.parseExpression("[0]");

			assertThat(stringify(expression.getValue(list))).isEqualTo("1 2 3");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(list))).isEqualTo("1 2 3");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			expression = parser.parseExpression("[0][1]");

			assertThat(expression.getValue(list)).isEqualTo(2);
			assertCanCompile(expression);
			assertThat(expression.getValue(list)).isEqualTo(2);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Integer");
		}

		@Test
		void indexIntoListOfListOfString() {
			List<List<String>> list = List.of(
				List.of("a", "b", "c"),
				List.of("d", "e", "f")
			);

			expression = parser.parseExpression("[1]");

			assertThat(stringify(expression.getValue(list))).isEqualTo("d e f");
			assertCanCompile(expression);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
			assertThat(stringify(expression.getValue(list))).isEqualTo("d e f");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			expression = parser.parseExpression("[1][2]");

			assertThat(stringify(expression.getValue(list))).isEqualTo("f");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(list))).isEqualTo("f");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		void indexIntoMap() {
			Map<String, Integer> map = Map.of("aaa", 111);

			expression = parser.parseExpression("['aaa']");

			assertThat(expression.getValue(map)).isEqualTo(111);
			assertCanCompile(expression);
			assertThat(expression.getValue(map)).isEqualTo(111);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// String key not enclosed in single quotes
			expression = parser.parseExpression("[aaa]");

			assertThat(expression.getValue(map)).isEqualTo(111);
			assertCanCompile(expression);
			assertThat(expression.getValue(map)).isEqualTo(111);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		void indexIntoMapOfListOfString() {
			Map<String, List<String>> map = Map.of("foo", List.of("a", "b", "c"));

			expression = parser.parseExpression("['foo']");

			assertThat(stringify(expression.getValue(map))).isEqualTo("a b c");
			assertCanCompile(expression);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
			assertThat(stringify(expression.getValue(map))).isEqualTo("a b c");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			expression = parser.parseExpression("['foo'][2]");

			assertThat(stringify(expression.getValue(map))).isEqualTo("c");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(map))).isEqualTo("c");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test  // gh-32356
		void indexIntoMapOfPrimitiveIntArray() {
			Map<String, int[]> map = Map.of("foo", new int[] { 1, 2, 3 });

			// Prerequisite: root type must not be public for this use case.
			assertNotPublic(map.getClass());

			// map key access
			expression = parser.parseExpression("['foo']");

			assertThat(stringify(expression.getValue(map))).isEqualTo("1 2 3");
			assertCanCompile(expression);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
			assertThat(stringify(expression.getValue(map))).isEqualTo("1 2 3");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// map key access via implicit #root & array index
			expression = parser.parseExpression("['foo'][1]");

			assertThat(expression.getValue(map)).isEqualTo(2);
			assertCanCompile(expression);
			assertThat(expression.getValue(map)).isEqualTo(2);

			// map key access via explicit #root & array index
			expression = parser.parseExpression("#root['foo'][1]");

			assertThat(expression.getValue(map)).isEqualTo(2);
			assertCanCompile(expression);
			assertThat(expression.getValue(map)).isEqualTo(2);

			// map key access via explicit #this & array index
			expression = parser.parseExpression("#this['foo'][1]");

			assertThat(expression.getValue(map)).isEqualTo(2);
			assertCanCompile(expression);
			assertThat(expression.getValue(map)).isEqualTo(2);
		}

		@Test  // gh-32356
		void indexIntoMapOfPrimitiveIntArrayWithCompilableMapAccessor() {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.addPropertyAccessor(new CompilableMapAccessor());

			Map<String, int[]> map = Map.of("foo", new int[] { 1, 2, 3 });

			// Prerequisite: root type must not be public for this use case.
			assertNotPublic(map.getClass());

			// map key access
			expression = parser.parseExpression("['foo']");

			assertThat(stringify(expression.getValue(context, map))).isEqualTo("1 2 3");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(context, map))).isEqualTo("1 2 3");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// custom CompilableMapAccessor via implicit #root & array index
			expression = parser.parseExpression("foo[1]");

			assertThat(expression.getValue(context, map)).isEqualTo(2);
			assertCanCompile(expression);
			assertThat(expression.getValue(context, map)).isEqualTo(2);

			// custom CompilableMapAccessor via explicit #root & array index
			expression = parser.parseExpression("#root.foo[1]");

			assertThat(expression.getValue(context, map)).isEqualTo(2);
			assertCanCompile(expression);
			assertThat(expression.getValue(context, map)).isEqualTo(2);

			// custom CompilableMapAccessor via explicit #this & array index
			expression = parser.parseExpression("#this.foo[1]");

			assertThat(expression.getValue(context, map)).isEqualTo(2);
			assertCanCompile(expression);
			assertThat(expression.getValue(context, map)).isEqualTo(2);

			// map key access & array index
			expression = parser.parseExpression("['foo'][2]");

			assertThat(stringify(expression.getValue(context, map))).isEqualTo("3");
			assertCanCompile(expression);
			assertThat(stringify(expression.getValue(context, map))).isEqualTo("3");
			assertThat(getAst().getExitDescriptor()).isEqualTo("I");
		}

		@Test
		void indexIntoSetCannotBeCompiled() {
			Set<Integer> set = Set.of(42);

			expression = parser.parseExpression("[0]");

			assertThat(expression.getValue(set)).isEqualTo(42);
			assertCannotCompile(expression);
			assertThat(expression.getValue(set)).isEqualTo(42);
			assertThat(getAst().getExitDescriptor()).isNull();
		}

		@Test
		void indexIntoStringCannotBeCompiled() {
			String text = "enigma";

			// "g" is the 4th letter in "enigma" (index 3)
			expression = parser.parseExpression("[3]");

			assertThat(expression.getValue(text)).isEqualTo("g");
			assertCannotCompile(expression);
			assertThat(expression.getValue(text)).isEqualTo("g");
			assertThat(getAst().getExitDescriptor()).isNull();
		}

		@Test
		void indexIntoObject() {
			TestClass6 tc = new TestClass6();

			// field access
			expression = parser.parseExpression("['orange']");

			assertThat(expression.getValue(tc)).isEqualTo("value1");
			assertCanCompile(expression);
			assertThat(expression.getValue(tc)).isEqualTo("value1");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/String");

			// field access
			expression = parser.parseExpression("['peach']");

			assertThat(expression.getValue(tc)).isEqualTo(34L);
			assertCanCompile(expression);
			assertThat(expression.getValue(tc)).isEqualTo(34L);
			assertThat(getAst().getExitDescriptor()).isEqualTo("J");

			// property access (getter)
			expression = parser.parseExpression("['banana']");

			assertThat(expression.getValue(tc)).isEqualTo("value3");
			assertCanCompile(expression);
			assertThat(expression.getValue(tc)).isEqualTo("value3");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/String");
		}

		@Test  // gh-32694, gh-32908
		void indexIntoArrayUsingIntegerWrapper() {
			context.setVariable("array", new int[] {1, 2, 3, 4});
			context.setVariable("index", 2);

			expression = parser.parseExpression("#array[#index]");

			assertThat(expression.getValue(context)).isEqualTo(3);
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(3);
			assertThat(getAst().getExitDescriptor()).isEqualTo("I");
		}

		@Test  // gh-32694, gh-32908
		void indexIntoListUsingIntegerWrapper() {
			context.setVariable("list", List.of(1, 2, 3, 4));
			context.setVariable("index", 2);

			expression = parser.parseExpression("#list[#index]");

			assertThat(expression.getValue(context)).isEqualTo(3);
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(3);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test  // gh-32903
		void indexIntoMapUsingPrimitiveLiteral() {
			Map<Object, String> map = Map.of(
					false, "0",   // BooleanLiteral
					1, "ABC",     // IntLiteral
					2L, "XYZ",    // LongLiteral
					9.99F, "~10", // FloatLiteral
					3.14159, "PI" // RealLiteral
				);
			context.setVariable("map", map);

			// BooleanLiteral
			expression = parser.parseExpression("#map[false]");
			assertThat(expression.getValue(context)).isEqualTo("0");
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo("0");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// IntLiteral
			expression = parser.parseExpression("#map[1]");
			assertThat(expression.getValue(context)).isEqualTo("ABC");
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo("ABC");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// LongLiteral
			expression = parser.parseExpression("#map[2L]");
			assertThat(expression.getValue(context)).isEqualTo("XYZ");
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo("XYZ");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// FloatLiteral
			expression = parser.parseExpression("#map[9.99F]");
			assertThat(expression.getValue(context)).isEqualTo("~10");
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo("~10");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// RealLiteral
			expression = parser.parseExpression("#map[3.14159]");
			assertThat(expression.getValue(context)).isEqualTo("PI");
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo("PI");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		private String stringify(Object object) {
			Stream<? extends Object> stream;
			if (object instanceof Collection<?> collection) {
				stream = collection.stream();
			}
			else if (object instanceof Object[] objects) {
				stream = Arrays.stream(objects);
			}
			else if (object instanceof int[] ints) {
				stream = Arrays.stream(ints).mapToObj(Integer::valueOf);
			}
			else {
				return String.valueOf(object);
			}
			return stream.map(Object::toString).collect(joining(" "));
		}

		@Nested
		class IndexAccessorTests {

			@Test
			void indexWithPrimitiveIndexTypeAndReferenceValueTypeAccessedViaRoot() {
				String exitTypeDescriptor = CodeFlow.toDescriptor(Color.class);
				Colors colors = new Colors();

				StandardEvaluationContext context = new StandardEvaluationContext();
				context.addIndexAccessor(new ColorsIndexAccessor());

				expression = parser.parseExpression("[0]");
				assertCannotCompile(expression);

				assertThatExceptionOfType(SpelEvaluationException.class)
						.isThrownBy(() -> expression.getValue(context, colors))
						.withMessageEndingWith("A problem occurred while attempting to read index '%s' in '%s'",
								0, Colors.class.getName())
						.withCauseInstanceOf(IndexOutOfBoundsException.class)
						.extracting(SpelEvaluationException::getMessageCode).isEqualTo(EXCEPTION_DURING_INDEX_READ);
				assertCannotCompile(expression);

				// IntLiteral as index --> represented as an int in compiled bytecode,
				// which does not require unboxing since get(int) method expects an int.
				// Falls in range [ICONST_0, ICONST_5]
				expression = parser.parseExpression("[1]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context, colors)).isEqualTo(Color.BLUE);
				assertCanCompile(expression);
				assertThat(expression.getValue(context, colors)).isEqualTo(Color.BLUE);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// IntLiteral as index --> represented as an int in compiled bytecode,
				// which does not require unboxing since get(int) method expects an int.
				// Does not fall in range [ICONST_0, ICONST_5]
				expression = parser.parseExpression("[42]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context, colors)).isEqualTo(Color.PURPLE);
				assertCanCompile(expression);
				assertThat(expression.getValue(context, colors)).isEqualTo(Color.PURPLE);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Integer variable as index --> represented as an Integer in compiled bytecode,
				// which requires unboxing from Integer to int since get(int) method expects an int.
				context.setVariable("colorIndex", 2);
				expression = parser.parseExpression("[#colorIndex]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context, colors)).isEqualTo(Color.GREEN);
				assertCanCompile(expression);
				assertThat(expression.getValue(context, colors)).isEqualTo(Color.GREEN);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Reuse expression but change value of colorIndex.
				context.setVariable("colorIndex", 3);

				assertThat(expression.getValue(context, colors)).isEqualTo(Color.ORANGE);
				assertCanCompile(expression);
				assertThat(expression.getValue(context, colors)).isEqualTo(Color.ORANGE);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Set color at index 3.
				expression.setValue(context, colors, Color.RED);
				assertCanCompile(expression);
				assertThat(expression.getValue(context, colors)).isEqualTo(Color.RED);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);
			}

			@Test
			void indexWithPrimitiveIndexTypeAndReferenceValueTypeAccessedViaList() {
				String exitTypeDescriptor = CodeFlow.toDescriptor(Color.class);

				StandardEvaluationContext context = new StandardEvaluationContext();
				context.addIndexAccessor(new ColorsIndexAccessor());
				context.setVariable("list", List.of(new Colors()));

				expression = parser.parseExpression("#list.get(0)[0]");
				assertCannotCompile(expression);

				assertThatExceptionOfType(SpelEvaluationException.class)
						.isThrownBy(() -> expression.getValue(context))
						.withMessageEndingWith("A problem occurred while attempting to read index '%s' in '%s'",
								0, Colors.class.getName())
						.withCauseInstanceOf(IndexOutOfBoundsException.class)
						.extracting(SpelEvaluationException::getMessageCode).isEqualTo(EXCEPTION_DURING_INDEX_READ);
				assertCannotCompile(expression);

				// IntLiteral as index --> represented as an int in compiled bytecode,
				// which does not require unboxing since get(int) method expects an int.
				expression = parser.parseExpression("#list.get(0)[1]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context)).isEqualTo(Color.BLUE);
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo(Color.BLUE);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Integer variable as index --> represented as an Integer in compiled bytecode,
				// which requires unboxing from Integer to int since get(int) method expects an int.
				context.setVariable("colorIndex", 2);
				expression = parser.parseExpression("#list.get(0)[#colorIndex]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context)).isEqualTo(Color.GREEN);
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo(Color.GREEN);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Reuse expression but change value of colorIndex.
				context.setVariable("colorIndex", 3);

				assertThat(expression.getValue(context)).isEqualTo(Color.ORANGE);
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo(Color.ORANGE);
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);
			}

			@Test
			void indexWithReferenceIndexTypeAndPrimitiveValueType() {
				String exitTypeDescriptor = CodeFlow.toDescriptor(int.class);

				StandardEvaluationContext context = new StandardEvaluationContext();
				context.addIndexAccessor(new ColorOrdinalsIndexAccessor());
				context.setVariable("colorOrdinals", new ColorOrdinals());
				context.setVariable("color", Color.GREEN);

				expression = parser.parseExpression("#colorOrdinals[#color]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context)).isEqualTo(Color.GREEN.ordinal());
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo(Color.GREEN.ordinal());
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Reuse expression but change value of color.
				context.setVariable("color", Color.BLUE);

				assertThat(expression.getValue(context)).isEqualTo(Color.BLUE.ordinal());
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo(Color.BLUE.ordinal());
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);
			}

			@ParameterizedTest(name = "{0}")
			@MethodSource("fruitMapIndexAccessors")
			void indexWithReferenceIndexTypeAndReferenceValueType(IndexAccessor indexAccessor) {
				String exitTypeDescriptor = CodeFlow.toDescriptor(String.class);

				StandardEvaluationContext context = new StandardEvaluationContext();
				context.addIndexAccessor(indexAccessor);
				context.setVariable("list", List.of(new FruitMap()));

				expression = parser.parseExpression("#list.get(0)[T(example.Color).PURPLE]");
				assertCannotCompile(expression);

				assertThatExceptionOfType(SpelEvaluationException.class)
						.isThrownBy(() -> expression.getValue(context))
						.withMessageEndingWith("A problem occurred while attempting to read index '%s' in '%s'",
								Color.PURPLE, FruitMap.class.getName())
						.withCauseInstanceOf(IllegalArgumentException.class)
						.extracting(SpelEvaluationException::getMessageCode).isEqualTo(EXCEPTION_DURING_INDEX_READ);
				assertCannotCompile(expression);

				expression = parser.parseExpression("#list[0][T(example.Color).RED]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context)).isEqualTo("cherry");
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo("cherry");
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				context.setVariable("color", Color.GREEN);
				expression = parser.parseExpression("#list[0][#color]");
				assertCannotCompile(expression);

				assertThat(expression.getValue(context)).isEqualTo("kiwi");
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo("kiwi");
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Reuse expression but change value of color.
				context.setVariable("color", Color.BLUE);

				assertThat(expression.getValue(context)).isEqualTo("blueberry");
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo("blueberry");
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Set fruit for purple
				context.setVariable("color", Color.PURPLE);
				expression.setValue(context, "plum");
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo("plum");
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);
			}

			static Stream<Arguments> fruitMapIndexAccessors() {
				return Stream.of(
					arguments(named("FruitMapIndexAccessor",
							new FruitMapIndexAccessor())),
					arguments(named("ReflectiveIndexAccessor",
							new ReflectiveIndexAccessor(FruitMap.class, Color.class, "getFruit", "setFruit")))
				);
			}
		}
	}

	@Nested
	class NullSafeIndexTests {  // gh-29847

		private final RootContextWithIndexedProperties rootContext = new RootContextWithIndexedProperties();

		private final StandardEvaluationContext context = new StandardEvaluationContext(rootContext);

		@Test
		void nullSafeIndexIntoPrimitiveIntArray() {
			expression = parser.parseExpression("intArray?.[0]");

			// Cannot compile before the array type is known.
			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.intArray = new int[] { 8, 9, 10 };
			assertThat(expression.getValue(context)).isEqualTo(8);
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(8);
			// Normally we would expect the exit type descriptor to be "I" for an
			// element of an int[]. However, with null-safe indexing support the
			// only way for it to evaluate to null is to box the 'int' to an 'Integer'.
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Integer");

			// Null-safe support should have been compiled once the array type is known.
			rootContext.intArray = null;
			assertThat(expression.getValue(context)).isNull();
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Integer");
		}

		@Test
		void nullSafeIndexIntoNumberArray() {
			expression = parser.parseExpression("numberArray?.[0]");

			// Cannot compile before the array type is known.
			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.numberArray = new Number[] { 8, 9, 10 };
			assertThat(expression.getValue(context)).isEqualTo(8);
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(8);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Number");

			// Null-safe support should have been compiled once the array type is known.
			rootContext.numberArray = null;
			assertThat(expression.getValue(context)).isNull();
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Number");
		}

		@Test
		void nullSafeIndexIntoList() {
			expression = parser.parseExpression("list?.[0]");

			// Cannot compile before the list type is known.
			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.list = List.of(42);
			assertThat(expression.getValue(context)).isEqualTo(42);
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(42);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// Null-safe support should have been compiled once the list type is known.
			rootContext.list = null;
			assertThat(expression.getValue(context)).isNull();
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		void nullSafeIndexIntoSetCannotBeCompiled() {
			expression = parser.parseExpression("set?.[0]");

			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.set = Set.of(42);
			assertThat(expression.getValue(context)).isEqualTo(42);
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(42);
			assertThat(getAst().getExitDescriptor()).isNull();
		}

		@Test
		void nullSafeIndexIntoStringCannotBeCompiled() {
			expression = parser.parseExpression("string?.[0]");

			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.string = "XYZ";
			assertThat(expression.getValue(context)).isEqualTo("X");
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo("X");
			assertThat(getAst().getExitDescriptor()).isNull();
		}

		@Test
		void nullSafeIndexIntoMap() {
			expression = parser.parseExpression("map?.['enigma']");

			// Cannot compile before the map type is known.
			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.map = Map.of("enigma", 42);
			assertThat(expression.getValue(context)).isEqualTo(42);
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(42);
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");

			// Null-safe support should have been compiled once the map type is known.
			rootContext.map = null;
			assertThat(expression.getValue(context)).isNull();
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Object");
		}

		@Test
		void nullSafeIndexIntoObjectViaPrimitiveProperty() {
			expression = parser.parseExpression("person?.['age']");

			// Cannot compile before the Person type is known.
			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.person = new Person("Jane");
			rootContext.person.setAge(42);
			assertThat(expression.getValue(context)).isEqualTo(42);
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo(42);
			// Normally we would expect the exit type descriptor to be "I" for
			// an int. However, with null-safe indexing support the only way
			// for it to evaluate to null is to box the 'int' to an 'Integer'.
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Integer");

			// Null-safe support should have been compiled once the Person type is known.
			rootContext.person = null;
			assertThat(expression.getValue(context)).isNull();
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/Integer");
		}

		@Test
		void nullSafeIndexIntoObjectViaStringProperty() {
			expression = parser.parseExpression("person?.['name']");

			// Cannot compile before the Person type is known.
			assertThat(expression.getValue(context)).isNull();
			assertCannotCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isNull();

			rootContext.person = new Person("Jane");
			assertThat(expression.getValue(context)).isEqualTo("Jane");
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isEqualTo("Jane");
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/String");

			// Null-safe support should have been compiled once the Person type is known.
			rootContext.person = null;
			assertThat(expression.getValue(context)).isNull();
			assertCanCompile(expression);
			assertThat(expression.getValue(context)).isNull();
			assertThat(getAst().getExitDescriptor()).isEqualTo("Ljava/lang/String");
		}

		@Nested
		class NullSafeIndexAccessorTests {

			@Test
			void nullSafeIndexWithReferenceIndexTypeAndPrimitiveValueType() {
				// Integer instead of int, since null-safe operators can return null.
				String exitTypeDescriptor = CodeFlow.toDescriptor(Integer.class);

				StandardEvaluationContext context = new StandardEvaluationContext();
				context.addIndexAccessor(new ColorOrdinalsIndexAccessor());
				context.setVariable("color", Color.GREEN);

				expression = parser.parseExpression("#colorOrdinals?.[#color]");
				assertCannotCompile(expression);

				// Cannot compile before the indexed value type is known.
				assertThat(expression.getValue(context)).isNull();
				assertCannotCompile(expression);
				assertThat(expression.getValue(context)).isNull();
				assertThat(getAst().getExitDescriptor()).isNull();

				context.setVariable("colorOrdinals", new ColorOrdinals());

				assertThat(expression.getValue(context)).isEqualTo(Color.GREEN.ordinal());
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo(Color.GREEN.ordinal());
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Null-safe support should have been compiled once the indexed value type is known.
				context.setVariable("colorOrdinals", null);
				assertThat(expression.getValue(context)).isNull();
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isNull();
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);
			}

			@Test
			void nullSafeIndexWithReferenceIndexTypeAndReferenceValueType() {
				String exitTypeDescriptor = CodeFlow.toDescriptor(String.class);

				StandardEvaluationContext context = new StandardEvaluationContext();
				context.addIndexAccessor(new FruitMapIndexAccessor());
				context.setVariable("color", Color.RED);

				expression = parser.parseExpression("#fruitMap?.[#color]");

				// Cannot compile before the indexed value type is known.
				assertThat(expression.getValue(context)).isNull();
				assertCannotCompile(expression);
				assertThat(expression.getValue(context)).isNull();
				assertThat(getAst().getExitDescriptor()).isNull();

				context.setVariable("fruitMap", new FruitMap());

				assertThat(expression.getValue(context)).isEqualTo("cherry");
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isEqualTo("cherry");
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);

				// Null-safe support should have been compiled once the indexed value type is known.
				context.setVariable("fruitMap", null);
				assertThat(expression.getValue(context)).isNull();
				assertCanCompile(expression);
				assertThat(expression.getValue(context)).isNull();
				assertThat(getAst().getExitDescriptor()).isEqualTo(exitTypeDescriptor);
			}
		}
	}

	@Nested
	class PropertyVisibilityTests {

		@Test
		void privateSubclassOverridesPropertyInPublicInterface() {
			expression = parser.parseExpression("text");
			PrivateSubclass privateSubclass = new PrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("enigma");

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("enigma");
		}

		@Test
		void privateSubclassOverridesPropertyInPrivateInterface() {
			expression = parser.parseExpression("message");
			PrivateSubclass privateSubclass = new PrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("hello");

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("hello");
		}

		@Test
		void privateSubclassOverridesPropertyInPublicSuperclass() {
			expression = parser.parseExpression("number");
			PrivateSubclass privateSubclass = new PrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			Integer result = expression.getValue(context, privateSubclass, Integer.class);
			assertThat(result).isEqualTo(2);

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, Integer.class);
			assertThat(result).isEqualTo(2);
		}

		@Test
		void indexIntoPropertyInPrivateSubclassThatOverridesPropertyInPublicInterface() {
			expression = parser.parseExpression("#root['text']");
			PrivateSubclass privateSubclass = new PrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("enigma");

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("enigma");
		}

		@Test
		void indexIntoPropertyInPrivateSubclassThatOverridesPropertyInPrivateInterface() {
			expression = parser.parseExpression("#root['message']");
			PrivateSubclass privateSubclass = new PrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("hello");

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("hello");
		}

		@Test
		void indexIntoPropertyInPrivateSubclassThatOverridesPropertyInPublicSuperclass() {
			expression = parser.parseExpression("#root['number']");
			PrivateSubclass privateSubclass = new PrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			Integer result = expression.getValue(context, privateSubclass, Integer.class);
			assertThat(result).isEqualTo(2);

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, Integer.class);
			assertThat(result).isEqualTo(2);
		}
	}

	@Nested
	class MethodVisibilityTests {

		/**
		 * Note that {@link InlineList} creates a list and wraps it via
		 * {@link Collections#unmodifiableList(List)}, whose concrete type is
		 * package private.
		 */
		@Test
		void packagePrivateSubclassOverridesMethodInPublicInterface() {
			expression = parser.parseExpression("{2021, 2022}");
			List<?> inlineList = expression.getValue(List.class);

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(inlineList.getClass());

			expression = parser.parseExpression("{2021, 2022}.contains(2022)");
			Boolean result = expression.getValue(context, Boolean.class);
			assertThat(result).isTrue();

			assertCanCompile(expression);
			result = expression.getValue(context, Boolean.class);
			assertThat(result).isTrue();
		}

		@Test
		void packagePrivateSubclassOverridesMethodInPrivateInterface() {
			expression = parser.parseExpression("greet('Jane')");
			LocalPrivateSubclass privateSubclass = new LocalPrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("Hello, Jane");

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("Hello, Jane");
		}

		@Test
		void privateSubclassOverridesMethodInPublicSuperclass() {
			expression = parser.parseExpression("process(2)");
			LocalPrivateSubclass privateSubclass = new LocalPrivateSubclass();

			// Prerequisite: type must not be public for this use case.
			assertNotPublic(privateSubclass.getClass());

			Integer result = expression.getValue(context, privateSubclass, Integer.class);
			assertThat(result).isEqualTo(2 * 2);

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, Integer.class);
			assertThat(result).isEqualTo(2 * 2);
		}

		// Cannot be named PrivateInterface due to issues with the Kotlin compiler.
		private interface LocalPrivateInterface {

			String greet(String name);
		}

		// Cannot be named PrivateSubclass due to issues with the Kotlin compiler.
		private static class LocalPrivateSubclass extends PublicSuperclass implements LocalPrivateInterface {

			@Override
			public int process(int num) {
				return num * 2;
			}

			@Override
			public String greet(String name) {
				return "Hello, " + name;
			}
		}
	}

	@Nested
	class ReflectiveIndexAccessorVisibilityTests {

		@Test
		void privateSubclassOverridesIndexReadMethodInPublicInterface() {
			PrivateSubclass privateSubclass = new PrivateSubclass();
			Class<?> targetType = privateSubclass.getClass();
			assertNotPublic(targetType);

			context.addIndexAccessor(new ReflectiveIndexAccessor(targetType, int.class, "getFruit"));
			expression = parser.parseExpression("[1]");

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("fruit-1");

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("fruit-1");
		}

		@Test
		void privateSubclassOverridesIndexReadMethodInPrivateInterface() {
			PrivateSubclass privateSubclass = new PrivateSubclass();
			Class<?> targetType = privateSubclass.getClass();
			assertNotPublic(targetType);

			context.addIndexAccessor(new ReflectiveIndexAccessor(targetType, int.class, "getIndex"));
			expression = parser.parseExpression("[1]");

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("value-1");

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("value-1");
		}

		@Test
		void privateSubclassOverridesIndexReadMethodInPublicSuperclass() {
			PrivateSubclass privateSubclass = new PrivateSubclass();
			Class<?> targetType = privateSubclass.getClass();
			assertNotPublic(targetType);

			context.addIndexAccessor(new ReflectiveIndexAccessor(targetType, int.class, "getIndex2"));
			expression = parser.parseExpression("[2]");

			String result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("sub-4"); // 2 * 2

			assertCanCompile(expression);
			result = expression.getValue(context, privateSubclass, String.class);
			assertThat(result).isEqualTo("sub-4"); // 2 * 2
		}
	}


	@Test
	void typeReference() {
		expression = parse("T(String)");
		assertThat(expression.getValue()).isEqualTo(String.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(String.class);

		expression = parse("T(java.io.IOException)");
		assertThat(expression.getValue()).isEqualTo(IOException.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(IOException.class);

		expression = parse("T(java.io.IOException[])");
		assertThat(expression.getValue()).isEqualTo(IOException[].class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(IOException[].class);

		expression = parse("T(int[][])");
		assertThat(expression.getValue()).isEqualTo(int[][].class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(int[][].class);

		expression = parse("T(int)");
		assertThat(expression.getValue()).isEqualTo(int.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(int.class);

		expression = parse("T(byte)");
		assertThat(expression.getValue()).isEqualTo(byte.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(byte.class);

		expression = parse("T(char)");
		assertThat(expression.getValue()).isEqualTo(char.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(char.class);

		expression = parse("T(short)");
		assertThat(expression.getValue()).isEqualTo(short.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(short.class);

		expression = parse("T(long)");
		assertThat(expression.getValue()).isEqualTo(long.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(long.class);

		expression = parse("T(float)");
		assertThat(expression.getValue()).isEqualTo(float.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(float.class);

		expression = parse("T(double)");
		assertThat(expression.getValue()).isEqualTo(double.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(double.class);

		expression = parse("T(boolean)");
		assertThat(expression.getValue()).isEqualTo(boolean.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(boolean.class);

		expression = parse("T(Missing)");
		assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(expression::getValue)
				.withMessageEndingWith("Type cannot be found 'Missing'");
		assertCannotCompile(expression);
	}

	@SuppressWarnings("unchecked")
	@Test
	void operatorInstanceOf() {
		expression = parse("'xyz' instanceof T(String)");
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();

		expression = parse("'xyz' instanceof T(Integer)");
		assertThat(expression.getValue()).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(false);

		List<String> list = new ArrayList<>();
		expression = parse("#root instanceof T(java.util.List)");
		assertThat(expression.getValue(list)).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(list)).asInstanceOf(BOOLEAN).isTrue();

		List<String>[] arrayOfLists = new List[] {new ArrayList<String>()};
		expression = parse("#root instanceof T(java.util.List[])");
		assertThat(expression.getValue(arrayOfLists)).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(arrayOfLists)).asInstanceOf(BOOLEAN).isTrue();

		int[] intArray = new int[] {1,2,3};
		expression = parse("#root instanceof T(int[])");
		assertThat(expression.getValue(intArray)).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(intArray)).asInstanceOf(BOOLEAN).isTrue();

		String root = null;
		expression = parse("#root instanceof T(Integer)");
		assertThat(expression.getValue(root)).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue(root)).isEqualTo(false);

		// root still null
		expression = parse("#root instanceof T(java.lang.Object)");
		assertThat(expression.getValue(root)).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue(root)).isEqualTo(false);

		root = "howdy!";
		expression = parse("#root instanceof T(java.lang.Object)");
		assertThat(expression.getValue(root)).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(root)).asInstanceOf(BOOLEAN).isTrue();
	}

	@Test
	void operatorInstanceOf_SPR14250() {
		// primitive left operand - should get boxed, return true
		expression = parse("3 instanceof T(Integer)");
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();

		// primitive left operand - should get boxed, return false
		expression = parse("3 instanceof T(String)");
		assertThat(expression.getValue()).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(false);

		// double slot left operand - should get boxed, return false
		expression = parse("3.0d instanceof T(Integer)");
		assertThat(expression.getValue()).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(false);

		// double slot left operand - should get boxed, return true
		expression = parse("3.0d instanceof T(Double)");
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();

		// Only when the right-hand operand is a direct type reference
		// will it be compilable.
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("foo", String.class);
		expression = parse("3 instanceof #foo");
		assertThat(expression.getValue(ctx)).isEqualTo(false);
		assertCannotCompile(expression);

		// use of primitive as type for instanceof check - compilable
		// but always false
		expression = parse("3 instanceof T(int)");
		assertThat(expression.getValue()).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(false);

		expression = parse("3 instanceof T(long)");
		assertThat(expression.getValue()).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(false);
	}

	@Test
	void stringLiteral() {
		expression = parser.parseExpression("'abcde'");
		assertThat(expression.getValue(new TestClass1(), String.class)).isEqualTo("abcde");
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(), String.class);
		assertThat(resultC).isEqualTo("abcde");
		assertThat(expression.getValue(String.class)).isEqualTo("abcde");
		assertThat(expression.getValue()).isEqualTo("abcde");
		assertThat(expression.getValue(new StandardEvaluationContext())).isEqualTo("abcde");
		expression = parser.parseExpression("\"abcde\"");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("abcde");
	}

	@Test
	void nullLiteral() {
		expression = parser.parseExpression("null");
		Object resultI = expression.getValue(new TestClass1(), Object.class);
		assertCanCompile(expression);
		Object resultC = expression.getValue(new TestClass1(), Object.class);
		assertThat(resultI).isNull();
		assertThat(resultC).isNull();
		assertThat(resultC).isNull();
	}

	@Test
	void realLiteral() {
		expression = parser.parseExpression("3.4d");
		double resultI = expression.getValue(new TestClass1(), double.class);
		assertCanCompile(expression);
		double resultC = expression.getValue(new TestClass1(), double.class);
		assertThat(resultI).isCloseTo(3.4d, within(0.1d));

		assertThat(resultC).isCloseTo(3.4d, within(0.1d));

		assertThat(expression.getValue()).isEqualTo(3.4d);
	}

	@SuppressWarnings("rawtypes")
	@Test
	void inlineList() {
		expression = parser.parseExpression("'abcde'.substring({1,3,4}[0])");
		Object o = expression.getValue();
		assertThat(o).isEqualTo("bcde");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("bcde");

		expression = parser.parseExpression("{'abc','def'}");
		List<?> l = (List) expression.getValue();
		assertThat(l.toString()).isEqualTo("[abc, def]");
		assertCanCompile(expression);
		l = (List) expression.getValue();
		assertThat(l.toString()).isEqualTo("[abc, def]");

		expression = parser.parseExpression("{'abc','def'}[0]");
		o = expression.getValue();
		assertThat(o).isEqualTo("abc");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("abc");

		expression = parser.parseExpression("{'abcde','ijklm'}[0].substring({1,3,4}[0])");
		o = expression.getValue();
		assertThat(o).isEqualTo("bcde");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("bcde");

		expression = parser.parseExpression("{'abcde','ijklm'}[0].substring({1,3,4}[0],{1,3,4}[1])");
		o = expression.getValue();
		assertThat(o).isEqualTo("bc");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("bc");
	}

	@SuppressWarnings("rawtypes")
	@Test
	void nestedInlineLists() {
		Object o = null;

		expression = parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}");
		o = expression.getValue();
		assertThat(o.toString()).isEqualTo("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o.toString()).isEqualTo("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]");

		expression = parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}.toString()");
		o = expression.getValue();
		assertThat(o).isEqualTo("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("[[1, 2, 3], [4, 5, 6], [7, 8, 9]]");

		expression = parser.parseExpression("{{1,2,3},{4,5,6},{7,8,9}}[1][0]");
		o = expression.getValue();
		assertThat(o).isEqualTo(4);
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo(4);

		expression = parser.parseExpression("{{1,2,3},'abc',{7,8,9}}[1]");
		o = expression.getValue();
		assertThat(o).isEqualTo("abc");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("abc");

		expression = parser.parseExpression("'abcde'.substring({{1,3},1,3,4}[0][1])");
		o = expression.getValue();
		assertThat(o).isEqualTo("de");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("de");

		expression = parser.parseExpression("'abcde'.substring({{1,3},1,3,4}[1])");
		o = expression.getValue();
		assertThat(o).isEqualTo("bcde");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("bcde");

		expression = parser.parseExpression("{'abc',{'def','ghi'}}");
		List<?> l = (List) expression.getValue();
		assertThat(l.toString()).isEqualTo("[abc, [def, ghi]]");
		assertCanCompile(expression);
		l = (List) expression.getValue();
		assertThat(l.toString()).isEqualTo("[abc, [def, ghi]]");

		expression = parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[0].substring({1,3,4}[0])");
		o = expression.getValue();
		assertThat(o).isEqualTo("bcde");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("bcde");

		expression = parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[1][0].substring({1,3,4}[0])");
		o = expression.getValue();
		assertThat(o).isEqualTo("jklm");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("jklm");

		expression = parser.parseExpression("{'abcde',{'ijklm','nopqr'}}[1][1].substring({1,3,4}[0],{1,3,4}[1])");
		o = expression.getValue();
		assertThat(o).isEqualTo("op");
		assertCanCompile(expression);
		o = expression.getValue();
		assertThat(o).isEqualTo("op");
	}

	@Test
	void intLiteral() {
		expression = parser.parseExpression("42");
		int resultI = expression.getValue(new TestClass1(), int.class);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(), int.class);
		assertThat(resultI).isEqualTo(42);
		assertThat(resultC).isEqualTo(42);

		expression = parser.parseExpression("T(Integer).valueOf(42)");
		expression.getValue(Integer.class);
		assertCanCompile(expression);
		assertThat(expression.getValue(Integer.class)).isEqualTo(42);

		// Code gen is different for -1 .. 6 because there are bytecode instructions specifically for those values

		// Not an int literal but an opMinus with one operand:
		expression = parser.parseExpression("-1");
		expression.getValue(Integer.class);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-1);

		expression = parser.parseExpression("0");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(0);

		expression = parser.parseExpression("2");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2);

		expression = parser.parseExpression("7");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(7);
	}

	@Test
	void longLiteral() {
		expression = parser.parseExpression("99L");
		long resultI = expression.getValue(new TestClass1(), long.class);
		assertCanCompile(expression);
		long resultC = expression.getValue(new TestClass1(), long.class);
		assertThat(resultI).isEqualTo(99L);
		assertThat(resultC).isEqualTo(99L);
	}

	@Test
	void booleanLiteral() {
		expression = parser.parseExpression("true");
		boolean resultI = expression.getValue(1, boolean.class);
		assertThat(resultI).isTrue();
		assertThat(SpelCompiler.compile(expression)).isTrue();
		boolean resultC = expression.getValue(1, boolean.class);
		assertThat(resultC).isTrue();

		expression = parser.parseExpression("false");
		resultI = expression.getValue(1, boolean.class);
		assertThat(resultI).isFalse();
		assertThat(SpelCompiler.compile(expression)).isTrue();
		resultC = expression.getValue(1, boolean.class);
		assertThat(resultC).isFalse();
	}

	@Test
	void floatLiteral() {
		expression = parser.parseExpression("3.4f");
		float resultI = expression.getValue(new TestClass1(), float.class);
		assertCanCompile(expression);
		float resultC = expression.getValue(new TestClass1(), float.class);
		assertThat(resultI).isCloseTo(3.4f, within(0.1f));

		assertThat(resultC).isCloseTo(3.4f, within(0.1f));

		assertThat(expression.getValue()).isEqualTo(3.4f);
	}

	@Test
	void opOr() {
		Expression expression = parser.parseExpression("false or false");
		boolean resultI = expression.getValue(1, boolean.class);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isFalse();
		assertThat(resultC).isFalse();

		expression = parser.parseExpression("false or true");
		resultI = expression.getValue(1, boolean.class);
		assertCanCompile(expression);
		resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isTrue();
		assertThat(resultC).isTrue();

		expression = parser.parseExpression("true or false");
		resultI = expression.getValue(1, boolean.class);
		assertCanCompile(expression);
		resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isTrue();
		assertThat(resultC).isTrue();

		expression = parser.parseExpression("true or true");
		resultI = expression.getValue(1, boolean.class);
		assertCanCompile(expression);
		resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isTrue();
		assertThat(resultC).isTrue();

		TestClass4 tc = new TestClass4();
		expression = parser.parseExpression("getfalse() or gettrue()");
		resultI = expression.getValue(tc, boolean.class);
		assertCanCompile(expression);
		resultC = expression.getValue(tc, boolean.class);
		assertThat(resultI).isTrue();
		assertThat(resultC).isTrue();

		// Can't compile this as we aren't going down the getfalse() branch in our evaluation
		expression = parser.parseExpression("gettrue() or getfalse()");
		resultI = expression.getValue(tc, boolean.class);
		assertCannotCompile(expression);

		expression = parser.parseExpression("getA() or getB()");
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc, boolean.class);
		assertCannotCompile(expression); // Haven't yet been into second branch
		tc.a = false;
		tc.b = true;
		resultI = expression.getValue(tc, boolean.class);
		assertCanCompile(expression); // Now been down both
		assertThat(resultI).isTrue();

		boolean b = false;
		expression = parse("#root or #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertThat((Boolean) resultI2).isFalse();
		assertThat((Boolean) expression.getValue(b)).isFalse();
	}

	@Test
	void opAnd() {
		Expression expression = parser.parseExpression("false and false");
		boolean resultI = expression.getValue(1, boolean.class);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isFalse();
		assertThat(resultC).isFalse();

		expression = parser.parseExpression("false and true");
		resultI = expression.getValue(1, boolean.class);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isFalse();
		assertThat(resultC).isFalse();

		expression = parser.parseExpression("true and false");
		resultI = expression.getValue(1, boolean.class);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isFalse();
		assertThat(resultC).isFalse();

		expression = parser.parseExpression("true and true");
		resultI = expression.getValue(1, boolean.class);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1, boolean.class);
		assertThat(resultI).isTrue();
		assertThat(resultC).isTrue();

		TestClass4 tc = new TestClass4();

		// Can't compile this as we aren't going down the gettrue() branch in our evaluation
		expression = parser.parseExpression("getfalse() and gettrue()");
		resultI = expression.getValue(tc, boolean.class);
		assertCannotCompile(expression);

		expression = parser.parseExpression("getA() and getB()");
		tc.a = false;
		tc.b = false;
		resultI = expression.getValue(tc, boolean.class);
		assertCannotCompile(expression); // Haven't yet been into second branch
		tc.a = true;
		tc.b = false;
		resultI = expression.getValue(tc, boolean.class);
		assertCanCompile(expression); // Now been down both
		assertThat(resultI).isFalse();
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc, boolean.class);
		assertThat(resultI).isTrue();

		boolean b = true;
		expression = parse("#root and #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertThat((Boolean) resultI2).isTrue();
		assertThat((Boolean) expression.getValue(b)).isTrue();
	}

	@Test
	void operatorNot() {
		expression = parse("!true");
		assertThat(expression.getValue()).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(false);

		expression = parse("!false");
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).asInstanceOf(BOOLEAN).isTrue();

		boolean b = true;
		expression = parse("!#root");
		assertThat(expression.getValue(b)).isEqualTo(false);
		assertCanCompile(expression);
		assertThat(expression.getValue(b)).isEqualTo(false);

		b = false;
		expression = parse("!#root");
		assertThat(expression.getValue(b)).asInstanceOf(BOOLEAN).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(b)).asInstanceOf(BOOLEAN).isTrue();
	}

	@Test
	void ternary() {
		Expression expression = parser.parseExpression("true?'a':'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertThat(resultI).isEqualTo("a");
		assertThat(resultC).isEqualTo("a");

		expression = parser.parseExpression("false?'a':'b'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertThat(resultI).isEqualTo("b");
		assertThat(resultC).isEqualTo("b");

		expression = parser.parseExpression("false?1:'b'");
		// All literals so we can do this straight away
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("b");

		boolean root = true;
		expression = parser.parseExpression("(#root and true)?T(Integer).valueOf(1):T(Long).valueOf(3L)");
		assertThat(expression.getValue(root)).isEqualTo(1);
		assertCannotCompile(expression); // Have not gone down false branch
		root = false;
		assertThat(expression.getValue(root)).isEqualTo(3L);
		assertCanCompile(expression);
		assertThat(expression.getValue(root)).isEqualTo(3L);
		root = true;
		assertThat(expression.getValue(root)).isEqualTo(1);
	}

	@Test
	void ternaryWithBooleanReturn_SPR12271() {
		expression = parser.parseExpression("T(Boolean).TRUE?'abc':'def'");
		assertThat(expression.getValue()).isEqualTo("abc");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("abc");

		expression = parser.parseExpression("T(Boolean).FALSE?'abc':'def'");
		assertThat(expression.getValue()).isEqualTo("def");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("def");
	}

	@Test
	void nullsafeFieldPropertyDereferencing_SPR16489() {
		FooObjectHolder foh = new FooObjectHolder();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(foh);

		// First non compiled:
		SpelExpression expression = (SpelExpression) parser.parseExpression("foo?.object");
		assertThat(expression.getValue(context)).isEqualTo("hello");
		foh.foo = null;
		assertThat(expression.getValue(context)).isNull();

		// Now revert state of foh and try compiling it:
		foh.foo = new FooObject();
		assertThat(expression.getValue(context)).isEqualTo("hello");
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo("hello");
		foh.foo = null;
		assertThat(expression.getValue(context)).isNull();

		// Static references
		expression = (SpelExpression) parser.parseExpression("#var?.propertya");
		context.setVariable("var", StaticsHelper.class);
		assertThat(expression.getValue(context).toString()).isEqualTo("sh");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", StaticsHelper.class);
		assertThat(expression.getValue(context).toString()).isEqualTo("sh");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Single size primitive (boolean)
		expression = (SpelExpression) parser.parseExpression("#var?.a");
		context.setVariable("var", new TestClass4());
		assertThat((Boolean) expression.getValue(context)).isFalse();
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", new TestClass4());
		assertThat((Boolean) expression.getValue(context)).isFalse();
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Double slot primitives
		expression = (SpelExpression) parser.parseExpression("#var?.four");
		context.setVariable("var", new Three());
		assertThat(expression.getValue(context).toString()).isEqualTo("0.04");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", new Three());
		assertThat(expression.getValue(context).toString()).isEqualTo("0.04");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
	}

	@Test  // gh-27421
	public void nullSafeMethodChainingWithNonStaticVoidMethod() {
		FooObjectHolder foh = new FooObjectHolder();
		StandardEvaluationContext context = new StandardEvaluationContext(foh);
		SpelExpression expression = (SpelExpression) parser.parseExpression("getFoo()?.doFoo()");

		FooObject.doFooInvoked = false;
		assertThat(expression.getValue(context)).isNull();
		assertThat(FooObject.doFooInvoked).isTrue();

		FooObject.doFooInvoked = false;
		foh.foo = null;
		assertThat(expression.getValue(context)).isNull();
		assertThat(FooObject.doFooInvoked).isFalse();

		assertCanCompile(expression);

		FooObject.doFooInvoked = false;
		foh.foo = new FooObject();
		assertThat(expression.getValue(context)).isNull();
		assertThat(FooObject.doFooInvoked).isTrue();

		FooObject.doFooInvoked = false;
		foh.foo = null;
		assertThat(expression.getValue(context)).isNull();
		assertThat(FooObject.doFooInvoked).isFalse();
	}

	@Test
	void nullsafeMethodChaining_SPR16489() {
		FooObjectHolder foh = new FooObjectHolder();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(foh);

		// First non compiled:
		SpelExpression expression = (SpelExpression) parser.parseExpression("getFoo()?.getObject()");
		assertThat(expression.getValue(context)).isEqualTo("hello");
		foh.foo = null;
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		foh.foo = new FooObject();
		assertThat(expression.getValue(context)).isEqualTo("hello");
		foh.foo = null;
		assertThat(expression.getValue(context)).isNull();

		// Static method references
		expression = (SpelExpression) parser.parseExpression("#var?.methoda()");
		context.setVariable("var", StaticsHelper.class);
		assertThat(expression.getValue(context).toString()).isEqualTo("sh");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", StaticsHelper.class);
		assertThat(expression.getValue(context).toString()).isEqualTo("sh");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression) parser.parseExpression("#var?.intValue()");
		context.setVariable("var", 4);
		assertThat(expression.getValue(context).toString()).isEqualTo("4");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", 4);
		assertThat(expression.getValue(context).toString()).isEqualTo("4");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression) parser.parseExpression("#var?.booleanValue()");
		context.setVariable("var", false);
		assertThat(expression.getValue(context).toString()).isEqualTo("false");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", false);
		assertThat(expression.getValue(context).toString()).isEqualTo("false");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression) parser.parseExpression("#var?.booleanValue()");
		context.setVariable("var", true);
		assertThat(expression.getValue(context).toString()).isEqualTo("true");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", true);
		assertThat(expression.getValue(context).toString()).isEqualTo("true");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression) parser.parseExpression("#var?.longValue()");
		context.setVariable("var", 5L);
		assertThat(expression.getValue(context).toString()).isEqualTo("5");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", 5L);
		assertThat(expression.getValue(context).toString()).isEqualTo("5");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression) parser.parseExpression("#var?.floatValue()");
		context.setVariable("var", 3f);
		assertThat(expression.getValue(context).toString()).isEqualTo("3.0");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", 3f);
		assertThat(expression.getValue(context).toString()).isEqualTo("3.0");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();

		// Nullsafe guard on expression element evaluating to primitive/null
		expression = (SpelExpression) parser.parseExpression("#var?.shortValue()");
		context.setVariable("var", (short)8);
		assertThat(expression.getValue(context).toString()).isEqualTo("8");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
		assertCanCompile(expression);
		context.setVariable("var", (short)8);
		assertThat(expression.getValue(context).toString()).isEqualTo("8");
		context.setVariable("var", null);
		assertThat(expression.getValue(context)).isNull();
	}

	@Test
	void elvis() {
		Expression expression = parser.parseExpression("'a'?:'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertThat(resultI).isEqualTo("a");
		assertThat(resultC).isEqualTo("a");

		expression = parser.parseExpression("null?:'a'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertThat(resultI).isEqualTo("a");
		assertThat(resultC).isEqualTo("a");

		String s = "abc";
		expression = parser.parseExpression("#root?:'b'");
		assertCannotCompile(expression);
		resultI = expression.getValue(s, String.class);
		assertThat(resultI).isEqualTo("abc");
		assertCanCompile(expression);
	}


	public static String concat(String a, String b) {
		return a+b;
	}

	public static String concat2(Object... args) {
		return Arrays.stream(args)
				.map(Objects::toString)
				.collect(Collectors.joining());
	}

	public static String join(String...strings) {
		StringBuilder buf = new StringBuilder();
		for (String string: strings) {
			buf.append(string);
		}
		return buf.toString();
	}

	@Test
	void compiledExpressionShouldWorkWhenUsingCustomFunctionWithVarargs() throws Exception {
		StandardEvaluationContext context;

		// single string argument
		expression = parser.parseExpression("#doFormat('hey %s', 'there')");
		context = new StandardEvaluationContext();
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class, Object[].class));
		((SpelExpression) expression).setEvaluationContext(context);

		assertThat(expression.getValue(String.class)).isEqualTo("hey there");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("hey there");

		// single string argument from root array access
		expression = parser.parseExpression("#doFormat([0], 'there')");
		context = new StandardEvaluationContext(new Object[] {"hey %s"});
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class, Object[].class));
		((SpelExpression) expression).setEvaluationContext(context);

		assertThat(expression.getValue(String.class)).isEqualTo("hey there");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("hey there");

		// single string from variable
		expression = parser.parseExpression("#doFormat([0], #arg)");
		context = new StandardEvaluationContext(new Object[] {"hey %s"});
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class, Object[].class));
		context.setVariable("arg", "there");
		((SpelExpression) expression).setEvaluationContext(context);

		assertThat(expression.getValue(String.class)).isEqualTo("hey there");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("hey there");

		// string array argument
		expression = parser.parseExpression("#doFormat('hey %s', #arg)");
		context = new StandardEvaluationContext();
		context.registerFunction("doFormat",
				DelegatingStringFormat.class.getDeclaredMethod("format", String.class, Object[].class));
		context.setVariable("arg", new String[] { "there" });
		((SpelExpression) expression).setEvaluationContext(context);
		assertThat(expression.getValue(String.class)).isEqualTo("hey there");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("hey there");
	}

	@Test
	void functionReference() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		Method m = getClass().getDeclaredMethod("concat", String.class, String.class);
		ctx.setVariable("concat", m);
		Method m2 = getClass().getDeclaredMethod("concat2", Object[].class);
		ctx.setVariable("concat2", m2);
		Method m3 = getClass().getDeclaredMethod("join", String[].class);
		ctx.setVariable("join", m3);

		expression = parser.parseExpression("#concat('a','b')");
		assertThat(expression.getValue(ctx)).isEqualTo("ab");
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx)).isEqualTo("ab");

		expression = parser.parseExpression("#concat(#concat('a','b'),'c').charAt(1)");
		assertThat(expression.getValue(ctx)).isEqualTo('b');
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx)).isEqualTo('b');

		// varargs
		expression = parser.parseExpression("#join(#stringArray)");
		ctx.setVariable("stringArray", new String[] { "a", "b", "c" });
		assertThat(expression.getValue(ctx)).isEqualTo("abc");
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx)).isEqualTo("abc");

		// varargs with argument component type that is a subtype of the varargs component type.
		expression = parser.parseExpression("#concat2(#stringArray)");
		ctx.setVariable("stringArray", new String[] { "a", "b", "c" });
		assertThat(expression.getValue(ctx)).isEqualTo("abc");
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx)).isEqualTo("abc");

		expression = parser.parseExpression("#concat(#a,#b)");
		ctx.setVariable("a", "foo");
		ctx.setVariable("b", "bar");
		assertThat(expression.getValue(ctx)).isEqualTo("foobar");
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx)).isEqualTo("foobar");
		ctx.setVariable("b", "boo");
		assertThat(expression.getValue(ctx)).isEqualTo("fooboo");

		m = Math.class.getDeclaredMethod("pow", double.class, double.class);
		ctx.setVariable("kapow",m);
		expression = parser.parseExpression("#kapow(2.0d,2.0d)");
		assertThat(expression.getValue(ctx).toString()).isEqualTo("4.0");
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx).toString()).isEqualTo("4.0");
	}

	@ParameterizedTest
	@ValueSource(strings = {"voidMethod", "voidWrapperMethod"})
	public void voidFunctionReference(String method) throws Exception {
		assertVoidFunctionReferenceBehavior(method);
	}

	private void assertVoidFunctionReferenceBehavior(String methodName) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName, String.class);

		EvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("voidMethod", method);

		expression = parser.parseExpression("#voidMethod('a')");

		voidMethodInvokedWith = null;
		expression.getValue(ctx);
		assertThat(voidMethodInvokedWith).isEqualTo("a");
		assertCanCompile(expression);

		voidMethodInvokedWith = null;
		expression.getValue(ctx);
		assertThat(voidMethodInvokedWith).isEqualTo("a");
		assertCanCompile(expression);

		voidMethodInvokedWith = null;
		expression.getValue(ctx);
		assertThat(voidMethodInvokedWith).isEqualTo("a");
		assertCanCompile(expression);

		expression = parser.parseExpression("#voidMethod(#a)");
		ctx.setVariable("a", "foo");

		voidMethodInvokedWith = null;
		expression.getValue(ctx);
		assertThat(voidMethodInvokedWith).isEqualTo("foo");
		assertCanCompile(expression);

		voidMethodInvokedWith = null;
		expression.getValue(ctx);
		assertThat(voidMethodInvokedWith).isEqualTo("foo");
		assertCanCompile(expression);
	}

	private static String voidMethodInvokedWith;

	public static Void voidWrapperMethod(String str) {
		voidMethodInvokedWith = str;
		return null;
	}

	public static void voidMethod(String str) {
		voidMethodInvokedWith = str;
	}

	@Test
	void functionReferenceVisibility_SPR12359() throws Exception {
		// Confirms visibility of what is being called.
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {"1"});
		context.registerFunction("doCompare", SomeCompareMethod.class.getDeclaredMethod(
				"compare", Object.class, Object.class));
		context.setVariable("arg", "2");
		// type nor method are public
		expression = parser.parseExpression("#doCompare([0],#arg)");
		assertThat(expression.getValue(context, Integer.class).toString()).isEqualTo("-1");
		assertCannotCompile(expression);

		// type not public but method is
		context = new StandardEvaluationContext(new Object[] {"1"});
		context.registerFunction("doCompare", SomeCompareMethod.class.getDeclaredMethod(
				"compare2", Object.class, Object.class));
		context.setVariable("arg", "2");
		expression = parser.parseExpression("#doCompare([0],#arg)");
		assertThat(expression.getValue(context, Integer.class).toString()).isEqualTo("-1");
		assertCannotCompile(expression);
	}

	@Test
	void functionReferenceNonCompilableArguments_SPR12359() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {"1"});
		context.registerFunction("negate", SomeCompareMethod2.class.getDeclaredMethod(
				"negate", int.class));
		context.setVariable("arg", "2");
		int[] ints = new int[] {1,2,3};
		context.setVariable("ints",ints);

		expression = parser.parseExpression("#negate(#ints.?[#this<2][0])");
		assertThat(expression.getValue(context, Integer.class).toString()).isEqualTo("-1");
		// Selection isn't compilable.
		assertThat(((SpelNodeImpl)((SpelExpression) expression).getAST()).isCompilable()).isFalse();
	}

	@Test
	void functionReferenceVarargs_SPR12359() throws Exception {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.registerFunction("append",
				SomeCompareMethod2.class.getDeclaredMethod("append", String[].class));
		context.registerFunction("append2",
				SomeCompareMethod2.class.getDeclaredMethod("append2", Object[].class));
		context.registerFunction("append3",
				SomeCompareMethod2.class.getDeclaredMethod("append3", String[].class));
		context.registerFunction("append4",
				SomeCompareMethod2.class.getDeclaredMethod("append4", String.class, String[].class));
		context.registerFunction("appendChar",
				SomeCompareMethod2.class.getDeclaredMethod("appendChar", char[].class));
		context.registerFunction("sum",
				SomeCompareMethod2.class.getDeclaredMethod("sum", int[].class));
		context.registerFunction("sumDouble",
				SomeCompareMethod2.class.getDeclaredMethod("sumDouble", double[].class));
		context.registerFunction("sumFloat",
				SomeCompareMethod2.class.getDeclaredMethod("sumFloat", float[].class));
		context.setVariable("stringArray", new String[] {"x","y","z"});
		context.setVariable("intArray", new int[] {5,6,9});
		context.setVariable("doubleArray", new double[] {5.0d,6.0d,9.0d});
		context.setVariable("floatArray", new float[] {5.0f,6.0f,9.0f});

		expression = parser.parseExpression("#append('a','b','c')");
		assertThat(expression.getValue(context).toString()).isEqualTo("abc");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("abc");

		expression = parser.parseExpression("#append('a')");
		assertThat(expression.getValue(context).toString()).isEqualTo("a");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("a");

		expression = parser.parseExpression("#append()");
		assertThat(expression.getValue(context).toString()).isEmpty();
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEmpty();

		expression = parser.parseExpression("#append(#stringArray)");
		assertThat(expression.getValue(context).toString()).isEqualTo("xyz");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("xyz");

		// This is a methodreference invocation, to compare with functionreference
		expression = parser.parseExpression("append(#stringArray)");
		assertThat(expression.getValue(context, new SomeCompareMethod2()).toString()).isEqualTo("xyz");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context, new SomeCompareMethod2()).toString()).isEqualTo("xyz");

		expression = parser.parseExpression("#append2('a','b','c')");
		assertThat(expression.getValue(context).toString()).isEqualTo("abc");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("abc");

		expression = parser.parseExpression("append2('a','b')");
		assertThat(expression.getValue(context, new SomeCompareMethod2()).toString()).isEqualTo("ab");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context, new SomeCompareMethod2()).toString()).isEqualTo("ab");

		expression = parser.parseExpression("#append2('a','b')");
		assertThat(expression.getValue(context).toString()).isEqualTo("ab");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("ab");

		expression = parser.parseExpression("#append2()");
		assertThat(expression.getValue(context).toString()).isEmpty();
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEmpty();

		expression = parser.parseExpression("#append3(#stringArray)");
		assertThat(expression.getValue(context, new SomeCompareMethod2()).toString()).isEqualTo("xyz");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context, new SomeCompareMethod2()).toString()).isEqualTo("xyz");

		expression = parser.parseExpression("#append2(#stringArray)");
		assertThat(expression.getValue(context)).hasToString("xyz");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).hasToString("xyz");

		expression = parser.parseExpression("#sum(1,2,3)");
		assertThat(expression.getValue(context)).isEqualTo(6);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(6);

		expression = parser.parseExpression("#sum(2)");
		assertThat(expression.getValue(context)).isEqualTo(2);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(2);

		expression = parser.parseExpression("#sum()");
		assertThat(expression.getValue(context)).isEqualTo(0);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(0);

		expression = parser.parseExpression("#sum(#intArray)");
		assertThat(expression.getValue(context)).isEqualTo(20);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(20);

		expression = parser.parseExpression("#sumDouble(1.0d,2.0d,3.0d)");
		assertThat(expression.getValue(context)).isEqualTo(6);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(6);

		expression = parser.parseExpression("#sumDouble(2.0d)");
		assertThat(expression.getValue(context)).isEqualTo(2);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(2);

		expression = parser.parseExpression("#sumDouble()");
		assertThat(expression.getValue(context)).isEqualTo(0);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(0);

		expression = parser.parseExpression("#sumDouble(#doubleArray)");
		assertThat(expression.getValue(context)).isEqualTo(20);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(20);

		expression = parser.parseExpression("#sumFloat(1.0f,2.0f,3.0f)");
		assertThat(expression.getValue(context)).isEqualTo(6);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(6);

		expression = parser.parseExpression("#sumFloat(2.0f)");
		assertThat(expression.getValue(context)).isEqualTo(2);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(2);

		expression = parser.parseExpression("#sumFloat()");
		assertThat(expression.getValue(context)).isEqualTo(0);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(0);

		expression = parser.parseExpression("#sumFloat(#floatArray)");
		assertThat(expression.getValue(context)).isEqualTo(20);
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo(20);

		expression = parser.parseExpression("#appendChar('abc'.charAt(0),'abc'.charAt(1))");
		assertThat(expression.getValue(context)).isEqualTo("ab");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo("ab");

		expression = parser.parseExpression("#append4('a','b','c')");
		assertThat(expression.getValue(context).toString()).isEqualTo("a::bc");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("a::bc");

		expression = parser.parseExpression("#append4('a','b')");
		assertThat(expression.getValue(context).toString()).isEqualTo("a::b");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("a::b");

		expression = parser.parseExpression("#append4('a')");
		assertThat(expression.getValue(context).toString()).isEqualTo("a::");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("a::");

		expression = parser.parseExpression("#append4('a',#stringArray)");
		assertThat(expression.getValue(context).toString()).isEqualTo("a::xyz");
		assertThat(((SpelExpression) expression).getAST().isCompilable()).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context).toString()).isEqualTo("a::xyz");
	}

	@Test
	void functionReferenceVarargs() throws Exception {
		EvaluationContext ctx = new StandardEvaluationContext();
		Method m = getClass().getDeclaredMethod("join", String[].class);
		ctx.setVariable("join", m);
		expression = parser.parseExpression("#join('a','b','c')");
		assertThat(expression.getValue(ctx)).isEqualTo("abc");
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx)).isEqualTo("abc");
	}

	@Test
	void opLt() {
		expression = parse("3.0d < 4.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("3446.0d < 1123.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("3 < 1");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("2 < 4");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3.0f < 1.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("1.0f < 5.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("30L < 30L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("15L < 20L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		// Differing types of number, not yet supported
		expression = parse("1 < 3.0d");
		assertCannotCompile(expression);

		expression = parse("T(Integer).valueOf(3) < 4");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Integer).valueOf(3) < T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("5 < T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
	}

	@Test
	void opLe() {
		expression = parse("3.0d <= 4.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("3446.0d <= 1123.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("3446.0d <= 3446.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3 <= 1");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("2 <= 4");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("3 <= 3");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3.0f <= 1.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("1.0f <= 5.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("2.0f <= 2.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("30L <= 30L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("15L <= 20L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		// Differing types of number, not yet supported
		expression = parse("1 <= 3.0d");
		assertCannotCompile(expression);

		expression = parse("T(Integer).valueOf(3) <= 4");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Integer).valueOf(3) <= T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("5 <= T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
	}

	@Test
	void opGt() {
		expression = parse("3.0d > 4.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("3446.0d > 1123.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3 > 1");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("2 > 4");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("3.0f > 1.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("1.0f > 5.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("30L > 30L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("15L > 20L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		// Differing types of number, not yet supported
		expression = parse("1 > 3.0d");
		assertCannotCompile(expression);

		expression = parse("T(Integer).valueOf(3) > 4");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Integer).valueOf(3) > T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("5 > T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
	}

	@Test
	void opGe() {
		expression = parse("3.0d >= 4.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("3446.0d >= 1123.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("3446.0d >= 3446.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3 >= 1");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("2 >= 4");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("3 >= 3");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3.0f >= 1.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("1.0f >= 5.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("3.0f >= 3.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("40L >= 30L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("15L >= 20L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("30L >= 30L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		// Differing types of number, not yet supported
		expression = parse("1 >= 3.0d");
		assertCannotCompile(expression);

		expression = parse("T(Integer).valueOf(3) >= 4");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Integer).valueOf(3) >= T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("5 >= T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
	}

	@Test
	void opEq() {
		String tvar = "35";
		expression = parse("#root == 35");
		assertThat((Boolean) expression.getValue(tvar)).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(tvar)).isFalse();

		expression = parse("35 == #root");
		expression.getValue(tvar);
		assertThat((Boolean) expression.getValue(tvar)).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(tvar)).isFalse();

		TestClass7 tc7 = new TestClass7();
		expression = parse("property == 'UK'");
		assertThat((Boolean) expression.getValue(tc7)).isTrue();
		TestClass7.property = null;
		assertThat((Boolean) expression.getValue(tc7)).isFalse();
		assertCanCompile(expression);
		TestClass7.reset();
		assertThat((Boolean) expression.getValue(tc7)).isTrue();
		TestClass7.property = "UK";
		assertThat((Boolean) expression.getValue(tc7)).isTrue();
		TestClass7.reset();
		TestClass7.property = null;
		assertThat((Boolean) expression.getValue(tc7)).isFalse();
		expression = parse("property == null");
		assertThat((Boolean) expression.getValue(tc7)).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(tc7)).isTrue();

		expression = parse("3.0d == 4.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("3446.0d == 3446.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3 == 1");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("3 == 3");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("3.0f == 1.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("2.0f == 2.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("30L == 30L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("15L == 20L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		// number types are not the same
		expression = parse("1 == 3.0d");
		assertCannotCompile(expression);

		Double d = 3.0d;
		expression = parse("#root==3.0d");
		assertThat((Boolean) expression.getValue(d)).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(d)).isTrue();

		Integer i = 3;
		expression = parse("#root==3");
		assertThat((Boolean) expression.getValue(i)).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(i)).isTrue();

		Float f = 3.0f;
		expression = parse("#root==3.0f");
		assertThat((Boolean) expression.getValue(f)).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(f)).isTrue();

		long l = 300L;
		expression = parse("#root==300l");
		assertThat((Boolean) expression.getValue(l)).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(l)).isTrue();

		boolean b = true;
		expression = parse("#root==true");
		assertThat((Boolean) expression.getValue(b)).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue(b)).isTrue();

		expression = parse("T(Integer).valueOf(3) == 4");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Integer).valueOf(3) == T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("5 == T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Float).valueOf(3.0f) == 4.0f");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Float).valueOf(3.0f) == T(Float).valueOf(3.0f)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("5.0f == T(Float).valueOf(3.0f)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Long).valueOf(3L) == 4L");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Long).valueOf(3L) == T(Long).valueOf(3L)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("5L == T(Long).valueOf(3L)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("false == true");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Boolean).valueOf('true') == true");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("false == T(Boolean).valueOf('false')");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
	}

	@Test
	void opNe() {
		expression = parse("3.0d != 4.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("3446.0d != 3446.0d");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("3 != 1");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("3 != 3");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("3.0f != 1.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
		expression = parse("2.0f != 2.0f");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("30L != 30L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();
		expression = parse("15L != 20L");
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		// not compatible number types
		expression = parse("1 != 3.0d");
		assertCannotCompile(expression);

		expression = parse("T(Integer).valueOf(3) != 4");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Integer).valueOf(3) != T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("5 != T(Integer).valueOf(3)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Float).valueOf(3.0f) != 4.0f");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Float).valueOf(3.0f) != T(Float).valueOf(3.0f)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("5.0f != T(Float).valueOf(3.0f)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Long).valueOf(3L) != 4L");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Long).valueOf(3L) != T(Long).valueOf(3L)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("5L != T(Long).valueOf(3L)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Double).valueOf(3.0d) == 4.0d");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Double).valueOf(3.0d) == T(Double).valueOf(3.0d)");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("5.0d == T(Double).valueOf(3.0d)");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("false == true");
		assertThat((Boolean) expression.getValue()).isFalse();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isFalse();

		expression = parse("T(Boolean).valueOf('true') == T(Boolean).valueOf('true')");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("T(Boolean).valueOf('true') == true");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();

		expression = parse("false == T(Boolean).valueOf('false')");
		assertThat((Boolean) expression.getValue()).isTrue();
		assertCanCompile(expression);
		assertThat((Boolean) expression.getValue()).isTrue();
	}

	@Test
	void opNe_SPR14863() {
		SpelParserConfiguration configuration =
				new SpelParserConfiguration(SpelCompilerMode.MIXED, ClassLoader.getSystemClassLoader());
		SpelExpressionParser parser = new SpelExpressionParser(configuration);
		Expression expression = parser.parseExpression("data['my-key'] != 'my-value'");

		Map<String, String> data = new HashMap<>();
		data.put("my-key", "my-value");
		StandardEvaluationContext context = new StandardEvaluationContext(new MyContext(data));
		assertThat(expression.getValue(context, Boolean.class)).isFalse();
		assertCanCompile(expression);
		((SpelExpression) expression).compileExpression();
		assertThat(expression.getValue(context, Boolean.class)).isFalse();

		List<String> ls = new ArrayList<>();
		ls.add("foo");
		context = new StandardEvaluationContext(ls);
		expression = parse("get(0) != 'foo'");
		assertThat(expression.getValue(context, Boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(context, Boolean.class)).isFalse();

		ls.remove(0);
		ls.add("goo");
		assertThat(expression.getValue(context, Boolean.class)).isTrue();
	}

	@Test
	void opEq_SPR14863() {
		// Exercise the comparator invocation code that runs in
		// equalityCheck() (called from interpreted and compiled code)
		expression = parser.parseExpression("#aa==#bb");
		StandardEvaluationContext sec = new StandardEvaluationContext();
		Apple aa = new Apple(1);
		Apple bb = new Apple(2);
		sec.setVariable("aa",aa);
		sec.setVariable("bb",bb);
		boolean b = expression.getValue(sec, Boolean.class);
		// Verify what the expression caused aa to be compared to
		assertThat(aa.gotComparedTo).isEqualTo(bb);
		assertThat(b).isFalse();
		bb.setValue(1);
		b = expression.getValue(sec, Boolean.class);
		assertThat(aa.gotComparedTo).isEqualTo(bb);
		assertThat(b).isTrue();

		assertCanCompile(expression);

		// Similar test with compiled expression
		aa = new Apple(99);
		bb = new Apple(100);
		sec.setVariable("aa",aa);
		sec.setVariable("bb",bb);
		b = expression.getValue(sec, Boolean.class);
		assertThat(b).isFalse();
		assertThat(aa.gotComparedTo).isEqualTo(bb);
		bb.setValue(99);
		b = expression.getValue(sec, Boolean.class);
		assertThat(b).isTrue();
		assertThat(aa.gotComparedTo).isEqualTo(bb);

		List<String> ls = new ArrayList<>();
		ls.add("foo");
		StandardEvaluationContext context = new StandardEvaluationContext(ls);
		expression = parse("get(0) == 'foo'");
		assertThat(expression.getValue(context, Boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context, Boolean.class)).isTrue();

		ls.remove(0);
		ls.add("goo");
		assertThat(expression.getValue(context, Boolean.class)).isFalse();
	}

	@Test
	void opPlus() {
		expression = parse("2+2");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4);

		expression = parse("2L+2L");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4L);

		expression = parse("2.0f+2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4.0f);

		expression = parse("3.0d+4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(7.0d);

		expression = parse("+1");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1);

		expression = parse("+1L");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1L);

		expression = parse("+1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1.5f);

		expression = parse("+2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2.5d);

		expression = parse("+T(Double).valueOf(2.5d)");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2.5d);

		expression = parse("T(Integer).valueOf(2)+6");
		assertThat(expression.getValue()).isEqualTo(8);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(8);

		expression = parse("T(Integer).valueOf(1)+T(Integer).valueOf(3)");
		assertThat(expression.getValue()).isEqualTo(4);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4);

		expression = parse("1+T(Integer).valueOf(3)");
		assertThat(expression.getValue()).isEqualTo(4);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4);

		expression = parse("T(Float).valueOf(2.0f)+6");
		assertThat(expression.getValue()).isEqualTo(8.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(8.0f);

		expression = parse("T(Float).valueOf(2.0f)+T(Float).valueOf(3.0f)");
		assertThat(expression.getValue()).isEqualTo(5.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(5.0f);

		expression = parse("3L+T(Long).valueOf(4L)");
		assertThat(expression.getValue()).isEqualTo(7L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(7L);

		expression = parse("T(Long).valueOf(2L)+6");
		assertThat(expression.getValue()).isEqualTo(8L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(8L);

		expression = parse("T(Long).valueOf(2L)+T(Long).valueOf(3L)");
		assertThat(expression.getValue()).isEqualTo(5L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(5L);

		expression = parse("1L+T(Long).valueOf(2L)");
		assertThat(expression.getValue()).isEqualTo(3L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(3L);
	}

	@Test
	void opDivide_mixedNumberTypes() {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB/60D",2d);
		checkCalc(p,"payload.valueBB/60D",2d);
		checkCalc(p,"payload.valueFB/60D",2d);
		checkCalc(p,"payload.valueDB/60D",2d);
		checkCalc(p,"payload.valueJB/60D",2d);
		checkCalc(p,"payload.valueIB/60D",2d);

		checkCalc(p,"payload.valueS/60D",2d);
		checkCalc(p,"payload.valueB/60D",2d);
		checkCalc(p,"payload.valueF/60D",2d);
		checkCalc(p,"payload.valueD/60D",2d);
		checkCalc(p,"payload.valueJ/60D",2d);
		checkCalc(p,"payload.valueI/60D",2d);

		checkCalc(p,"payload.valueSB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueBB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueFB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueDB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueIB/payload.valueDB60",2d);

		checkCalc(p,"payload.valueS/payload.valueDB60",2d);
		checkCalc(p,"payload.valueB/payload.valueDB60",2d);
		checkCalc(p,"payload.valueF/payload.valueDB60",2d);
		checkCalc(p,"payload.valueD/payload.valueDB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueDB60",2d);
		checkCalc(p,"payload.valueI/payload.valueDB60",2d);

		// right is a float
		checkCalc(p,"payload.valueSB/60F",2F);
		checkCalc(p,"payload.valueBB/60F",2F);
		checkCalc(p,"payload.valueFB/60F",2f);
		checkCalc(p,"payload.valueDB/60F",2d);
		checkCalc(p,"payload.valueJB/60F",2F);
		checkCalc(p,"payload.valueIB/60F",2F);

		checkCalc(p,"payload.valueS/60F",2F);
		checkCalc(p,"payload.valueB/60F",2F);
		checkCalc(p,"payload.valueF/60F",2f);
		checkCalc(p,"payload.valueD/60F",2d);
		checkCalc(p,"payload.valueJ/60F",2F);
		checkCalc(p,"payload.valueI/60F",2F);

		checkCalc(p,"payload.valueSB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueBB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueFB/payload.valueFB60",2f);
		checkCalc(p,"payload.valueDB/payload.valueFB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueIB/payload.valueFB60",2F);

		checkCalc(p,"payload.valueS/payload.valueFB60",2F);
		checkCalc(p,"payload.valueB/payload.valueFB60",2F);
		checkCalc(p,"payload.valueF/payload.valueFB60",2f);
		checkCalc(p,"payload.valueD/payload.valueFB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueFB60",2F);
		checkCalc(p,"payload.valueI/payload.valueFB60",2F);

		// right is a long
		checkCalc(p,"payload.valueSB/60L",2L);
		checkCalc(p,"payload.valueBB/60L",2L);
		checkCalc(p,"payload.valueFB/60L",2f);
		checkCalc(p,"payload.valueDB/60L",2d);
		checkCalc(p,"payload.valueJB/60L",2L);
		checkCalc(p,"payload.valueIB/60L",2L);

		checkCalc(p,"payload.valueS/60L",2L);
		checkCalc(p,"payload.valueB/60L",2L);
		checkCalc(p,"payload.valueF/60L",2f);
		checkCalc(p,"payload.valueD/60L",2d);
		checkCalc(p,"payload.valueJ/60L",2L);
		checkCalc(p,"payload.valueI/60L",2L);

		checkCalc(p,"payload.valueSB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueBB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueFB/payload.valueJB60",2f);
		checkCalc(p,"payload.valueDB/payload.valueJB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueIB/payload.valueJB60",2L);

		checkCalc(p,"payload.valueS/payload.valueJB60",2L);
		checkCalc(p,"payload.valueB/payload.valueJB60",2L);
		checkCalc(p,"payload.valueF/payload.valueJB60",2f);
		checkCalc(p,"payload.valueD/payload.valueJB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueJB60",2L);
		checkCalc(p,"payload.valueI/payload.valueJB60",2L);

		// right is an int
		checkCalc(p,"payload.valueSB/60",2);
		checkCalc(p,"payload.valueBB/60",2);
		checkCalc(p,"payload.valueFB/60",2f);
		checkCalc(p,"payload.valueDB/60",2d);
		checkCalc(p,"payload.valueJB/60",2L);
		checkCalc(p,"payload.valueIB/60",2);

		checkCalc(p,"payload.valueS/60",2);
		checkCalc(p,"payload.valueB/60",2);
		checkCalc(p,"payload.valueF/60",2f);
		checkCalc(p,"payload.valueD/60",2d);
		checkCalc(p,"payload.valueJ/60",2L);
		checkCalc(p,"payload.valueI/60",2);

		checkCalc(p,"payload.valueSB/payload.valueIB60",2);
		checkCalc(p,"payload.valueBB/payload.valueIB60",2);
		checkCalc(p,"payload.valueFB/payload.valueIB60",2f);
		checkCalc(p,"payload.valueDB/payload.valueIB60",2d);
		checkCalc(p,"payload.valueJB/payload.valueIB60",2L);
		checkCalc(p,"payload.valueIB/payload.valueIB60",2);

		checkCalc(p,"payload.valueS/payload.valueIB60",2);
		checkCalc(p,"payload.valueB/payload.valueIB60",2);
		checkCalc(p,"payload.valueF/payload.valueIB60",2f);
		checkCalc(p,"payload.valueD/payload.valueIB60",2d);
		checkCalc(p,"payload.valueJ/payload.valueIB60",2L);
		checkCalc(p,"payload.valueI/payload.valueIB60",2);

		// right is a short
		checkCalc(p,"payload.valueSB/payload.valueS",1);
		checkCalc(p,"payload.valueBB/payload.valueS",1);
		checkCalc(p,"payload.valueFB/payload.valueS",1f);
		checkCalc(p,"payload.valueDB/payload.valueS",1d);
		checkCalc(p,"payload.valueJB/payload.valueS",1L);
		checkCalc(p,"payload.valueIB/payload.valueS",1);

		checkCalc(p,"payload.valueS/payload.valueS",1);
		checkCalc(p,"payload.valueB/payload.valueS",1);
		checkCalc(p,"payload.valueF/payload.valueS",1f);
		checkCalc(p,"payload.valueD/payload.valueS",1d);
		checkCalc(p,"payload.valueJ/payload.valueS",1L);
		checkCalc(p,"payload.valueI/payload.valueS",1);

		checkCalc(p,"payload.valueSB/payload.valueSB",1);
		checkCalc(p,"payload.valueBB/payload.valueSB",1);
		checkCalc(p,"payload.valueFB/payload.valueSB",1f);
		checkCalc(p,"payload.valueDB/payload.valueSB",1d);
		checkCalc(p,"payload.valueJB/payload.valueSB",1L);
		checkCalc(p,"payload.valueIB/payload.valueSB",1);

		checkCalc(p,"payload.valueS/payload.valueSB",1);
		checkCalc(p,"payload.valueB/payload.valueSB",1);
		checkCalc(p,"payload.valueF/payload.valueSB",1f);
		checkCalc(p,"payload.valueD/payload.valueSB",1d);
		checkCalc(p,"payload.valueJ/payload.valueSB",1L);
		checkCalc(p,"payload.valueI/payload.valueSB",1);

		// right is a byte
		checkCalc(p,"payload.valueSB/payload.valueB",1);
		checkCalc(p,"payload.valueBB/payload.valueB",1);
		checkCalc(p,"payload.valueFB/payload.valueB",1f);
		checkCalc(p,"payload.valueDB/payload.valueB",1d);
		checkCalc(p,"payload.valueJB/payload.valueB",1L);
		checkCalc(p,"payload.valueIB/payload.valueB",1);

		checkCalc(p,"payload.valueS/payload.valueB",1);
		checkCalc(p,"payload.valueB/payload.valueB",1);
		checkCalc(p,"payload.valueF/payload.valueB",1f);
		checkCalc(p,"payload.valueD/payload.valueB",1d);
		checkCalc(p,"payload.valueJ/payload.valueB",1L);
		checkCalc(p,"payload.valueI/payload.valueB",1);

		checkCalc(p,"payload.valueSB/payload.valueBB",1);
		checkCalc(p,"payload.valueBB/payload.valueBB",1);
		checkCalc(p,"payload.valueFB/payload.valueBB",1f);
		checkCalc(p,"payload.valueDB/payload.valueBB",1d);
		checkCalc(p,"payload.valueJB/payload.valueBB",1L);
		checkCalc(p,"payload.valueIB/payload.valueBB",1);

		checkCalc(p,"payload.valueS/payload.valueBB",1);
		checkCalc(p,"payload.valueB/payload.valueBB",1);
		checkCalc(p,"payload.valueF/payload.valueBB",1f);
		checkCalc(p,"payload.valueD/payload.valueBB",1d);
		checkCalc(p,"payload.valueJ/payload.valueBB",1L);
		checkCalc(p,"payload.valueI/payload.valueBB",1);
	}

	@Test
	void opPlus_mixedNumberTypes() {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB+60D",180d);
		checkCalc(p,"payload.valueBB+60D",180d);
		checkCalc(p,"payload.valueFB+60D",180d);
		checkCalc(p,"payload.valueDB+60D",180d);
		checkCalc(p,"payload.valueJB+60D",180d);
		checkCalc(p,"payload.valueIB+60D",180d);

		checkCalc(p,"payload.valueS+60D",180d);
		checkCalc(p,"payload.valueB+60D",180d);
		checkCalc(p,"payload.valueF+60D",180d);
		checkCalc(p,"payload.valueD+60D",180d);
		checkCalc(p,"payload.valueJ+60D",180d);
		checkCalc(p,"payload.valueI+60D",180d);

		checkCalc(p,"payload.valueSB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueBB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueFB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueDB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueIB+payload.valueDB60",180d);

		checkCalc(p,"payload.valueS+payload.valueDB60",180d);
		checkCalc(p,"payload.valueB+payload.valueDB60",180d);
		checkCalc(p,"payload.valueF+payload.valueDB60",180d);
		checkCalc(p,"payload.valueD+payload.valueDB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueDB60",180d);
		checkCalc(p,"payload.valueI+payload.valueDB60",180d);

		// right is a float
		checkCalc(p,"payload.valueSB+60F",180F);
		checkCalc(p,"payload.valueBB+60F",180F);
		checkCalc(p,"payload.valueFB+60F",180f);
		checkCalc(p,"payload.valueDB+60F",180d);
		checkCalc(p,"payload.valueJB+60F",180F);
		checkCalc(p,"payload.valueIB+60F",180F);

		checkCalc(p,"payload.valueS+60F",180F);
		checkCalc(p,"payload.valueB+60F",180F);
		checkCalc(p,"payload.valueF+60F",180f);
		checkCalc(p,"payload.valueD+60F",180d);
		checkCalc(p,"payload.valueJ+60F",180F);
		checkCalc(p,"payload.valueI+60F",180F);

		checkCalc(p,"payload.valueSB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueBB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueFB+payload.valueFB60",180f);
		checkCalc(p,"payload.valueDB+payload.valueFB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueIB+payload.valueFB60",180F);

		checkCalc(p,"payload.valueS+payload.valueFB60",180F);
		checkCalc(p,"payload.valueB+payload.valueFB60",180F);
		checkCalc(p,"payload.valueF+payload.valueFB60",180f);
		checkCalc(p,"payload.valueD+payload.valueFB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueFB60",180F);
		checkCalc(p,"payload.valueI+payload.valueFB60",180F);

		// right is a long
		checkCalc(p,"payload.valueSB+60L",180L);
		checkCalc(p,"payload.valueBB+60L",180L);
		checkCalc(p,"payload.valueFB+60L",180f);
		checkCalc(p,"payload.valueDB+60L",180d);
		checkCalc(p,"payload.valueJB+60L",180L);
		checkCalc(p,"payload.valueIB+60L",180L);

		checkCalc(p,"payload.valueS+60L",180L);
		checkCalc(p,"payload.valueB+60L",180L);
		checkCalc(p,"payload.valueF+60L",180f);
		checkCalc(p,"payload.valueD+60L",180d);
		checkCalc(p,"payload.valueJ+60L",180L);
		checkCalc(p,"payload.valueI+60L",180L);

		checkCalc(p,"payload.valueSB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueBB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueFB+payload.valueJB60",180f);
		checkCalc(p,"payload.valueDB+payload.valueJB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueIB+payload.valueJB60",180L);

		checkCalc(p,"payload.valueS+payload.valueJB60",180L);
		checkCalc(p,"payload.valueB+payload.valueJB60",180L);
		checkCalc(p,"payload.valueF+payload.valueJB60",180f);
		checkCalc(p,"payload.valueD+payload.valueJB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueJB60",180L);
		checkCalc(p,"payload.valueI+payload.valueJB60",180L);

		// right is an int
		checkCalc(p,"payload.valueSB+60",180);
		checkCalc(p,"payload.valueBB+60",180);
		checkCalc(p,"payload.valueFB+60",180f);
		checkCalc(p,"payload.valueDB+60",180d);
		checkCalc(p,"payload.valueJB+60",180L);
		checkCalc(p,"payload.valueIB+60",180);

		checkCalc(p,"payload.valueS+60",180);
		checkCalc(p,"payload.valueB+60",180);
		checkCalc(p,"payload.valueF+60",180f);
		checkCalc(p,"payload.valueD+60",180d);
		checkCalc(p,"payload.valueJ+60",180L);
		checkCalc(p,"payload.valueI+60",180);

		checkCalc(p,"payload.valueSB+payload.valueIB60",180);
		checkCalc(p,"payload.valueBB+payload.valueIB60",180);
		checkCalc(p,"payload.valueFB+payload.valueIB60",180f);
		checkCalc(p,"payload.valueDB+payload.valueIB60",180d);
		checkCalc(p,"payload.valueJB+payload.valueIB60",180L);
		checkCalc(p,"payload.valueIB+payload.valueIB60",180);

		checkCalc(p,"payload.valueS+payload.valueIB60",180);
		checkCalc(p,"payload.valueB+payload.valueIB60",180);
		checkCalc(p,"payload.valueF+payload.valueIB60",180f);
		checkCalc(p,"payload.valueD+payload.valueIB60",180d);
		checkCalc(p,"payload.valueJ+payload.valueIB60",180L);
		checkCalc(p,"payload.valueI+payload.valueIB60",180);

		// right is a short
		checkCalc(p,"payload.valueSB+payload.valueS",240);
		checkCalc(p,"payload.valueBB+payload.valueS",240);
		checkCalc(p,"payload.valueFB+payload.valueS",240f);
		checkCalc(p,"payload.valueDB+payload.valueS",240d);
		checkCalc(p,"payload.valueJB+payload.valueS",240L);
		checkCalc(p,"payload.valueIB+payload.valueS",240);

		checkCalc(p,"payload.valueS+payload.valueS",240);
		checkCalc(p,"payload.valueB+payload.valueS",240);
		checkCalc(p,"payload.valueF+payload.valueS",240f);
		checkCalc(p,"payload.valueD+payload.valueS",240d);
		checkCalc(p,"payload.valueJ+payload.valueS",240L);
		checkCalc(p,"payload.valueI+payload.valueS",240);

		checkCalc(p,"payload.valueSB+payload.valueSB",240);
		checkCalc(p,"payload.valueBB+payload.valueSB",240);
		checkCalc(p,"payload.valueFB+payload.valueSB",240f);
		checkCalc(p,"payload.valueDB+payload.valueSB",240d);
		checkCalc(p,"payload.valueJB+payload.valueSB",240L);
		checkCalc(p,"payload.valueIB+payload.valueSB",240);

		checkCalc(p,"payload.valueS+payload.valueSB",240);
		checkCalc(p,"payload.valueB+payload.valueSB",240);
		checkCalc(p,"payload.valueF+payload.valueSB",240f);
		checkCalc(p,"payload.valueD+payload.valueSB",240d);
		checkCalc(p,"payload.valueJ+payload.valueSB",240L);
		checkCalc(p,"payload.valueI+payload.valueSB",240);

		// right is a byte
		checkCalc(p,"payload.valueSB+payload.valueB",240);
		checkCalc(p,"payload.valueBB+payload.valueB",240);
		checkCalc(p,"payload.valueFB+payload.valueB",240f);
		checkCalc(p,"payload.valueDB+payload.valueB",240d);
		checkCalc(p,"payload.valueJB+payload.valueB",240L);
		checkCalc(p,"payload.valueIB+payload.valueB",240);

		checkCalc(p,"payload.valueS+payload.valueB",240);
		checkCalc(p,"payload.valueB+payload.valueB",240);
		checkCalc(p,"payload.valueF+payload.valueB",240f);
		checkCalc(p,"payload.valueD+payload.valueB",240d);
		checkCalc(p,"payload.valueJ+payload.valueB",240L);
		checkCalc(p,"payload.valueI+payload.valueB",240);

		checkCalc(p,"payload.valueSB+payload.valueBB",240);
		checkCalc(p,"payload.valueBB+payload.valueBB",240);
		checkCalc(p,"payload.valueFB+payload.valueBB",240f);
		checkCalc(p,"payload.valueDB+payload.valueBB",240d);
		checkCalc(p,"payload.valueJB+payload.valueBB",240L);
		checkCalc(p,"payload.valueIB+payload.valueBB",240);

		checkCalc(p,"payload.valueS+payload.valueBB",240);
		checkCalc(p,"payload.valueB+payload.valueBB",240);
		checkCalc(p,"payload.valueF+payload.valueBB",240f);
		checkCalc(p,"payload.valueD+payload.valueBB",240d);
		checkCalc(p,"payload.valueJ+payload.valueBB",240L);
		checkCalc(p,"payload.valueI+payload.valueBB",240);
	}

	private void checkCalc(PayloadX p, String expression, int expectedResult) {
		Expression expr = parse(expression);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
		assertCanCompile(expr);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
	}

	private void checkCalc(PayloadX p, String expression, float expectedResult) {
		Expression expr = parse(expression);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
		assertCanCompile(expr);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
	}

	private void checkCalc(PayloadX p, String expression, long expectedResult) {
		Expression expr = parse(expression);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
		assertCanCompile(expr);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
	}

	private void checkCalc(PayloadX p, String expression, double expectedResult) {
		Expression expr = parse(expression);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
		assertCanCompile(expr);
		assertThat(expr.getValue(p)).isEqualTo(expectedResult);
	}

	@Test
	void opPlusString() {
		expression = parse("'hello' + 'world'");
		assertThat(expression.getValue()).isEqualTo("helloworld");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("helloworld");

		// Method with string return
		expression = parse("'hello' + getWorld()");
		assertThat(expression.getValue(new Greeter())).isEqualTo("helloworld");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("helloworld");

		// Method with string return
		expression = parse("getWorld() + 'hello'");
		assertThat(expression.getValue(new Greeter())).isEqualTo("worldhello");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("worldhello");

		// Three strings, optimal bytecode would only use one StringBuilder
		expression = parse("'hello' + getWorld() + ' spring'");
		assertThat(expression.getValue(new Greeter())).isEqualTo("helloworld spring");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("helloworld spring");

		// Three strings, optimal bytecode would only use one StringBuilder
		expression = parse("'hello' + 3 + ' spring'");
		assertThat(expression.getValue(new Greeter())).isEqualTo("hello3 spring");
		assertCannotCompile(expression);

		expression = parse("object + 'a'");
		assertThat(expression.getValue(new Greeter())).isEqualTo("objecta");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("objecta");

		expression = parse("'a'+object");
		assertThat(expression.getValue(new Greeter())).isEqualTo("aobject");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("aobject");

		expression = parse("'a'+object+'a'");
		assertThat(expression.getValue(new Greeter())).isEqualTo("aobjecta");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("aobjecta");

		expression = parse("object+'a'+object");
		assertThat(expression.getValue(new Greeter())).isEqualTo("objectaobject");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("objectaobject");

		expression = parse("object+object");
		assertThat(expression.getValue(new Greeter())).isEqualTo("objectobject");
		assertCanCompile(expression);
		assertThat(expression.getValue(new Greeter())).isEqualTo("objectobject");
	}

	@Test
	void opMinus() {
		expression = parse("2-2");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(0);

		expression = parse("4L-2L");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2L);

		expression = parse("4.0f-2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2.0f);

		expression = parse("3.0d-4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-1.0d);

		expression = parse("-1");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-1);

		expression = parse("-1L");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-1L);

		expression = parse("-1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-1.5f);

		expression = parse("-2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-2.5d);

		expression = parse("T(Integer).valueOf(2)-6");
		assertThat(expression.getValue()).isEqualTo(-4);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-4);

		expression = parse("T(Integer).valueOf(1)-T(Integer).valueOf(3)");
		assertThat(expression.getValue()).isEqualTo(-2);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-2);

		expression = parse("4-T(Integer).valueOf(3)");
		assertThat(expression.getValue()).isEqualTo(1);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1);

		expression = parse("T(Float).valueOf(2.0f)-6");
		assertThat(expression.getValue()).isEqualTo(-4.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-4.0f);

		expression = parse("T(Float).valueOf(8.0f)-T(Float).valueOf(3.0f)");
		assertThat(expression.getValue()).isEqualTo(5.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(5.0f);

		expression = parse("11L-T(Long).valueOf(4L)");
		assertThat(expression.getValue()).isEqualTo(7L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(7L);

		expression = parse("T(Long).valueOf(9L)-6");
		assertThat(expression.getValue()).isEqualTo(3L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(3L);

		expression = parse("T(Long).valueOf(4L)-T(Long).valueOf(3L)");
		assertThat(expression.getValue()).isEqualTo(1L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1L);

		expression = parse("8L-T(Long).valueOf(2L)");
		assertThat(expression.getValue()).isEqualTo(6L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(6L);
	}

	@Test
	void opMinus_mixedNumberTypes() {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB-60D",60d);
		checkCalc(p,"payload.valueBB-60D",60d);
		checkCalc(p,"payload.valueFB-60D",60d);
		checkCalc(p,"payload.valueDB-60D",60d);
		checkCalc(p,"payload.valueJB-60D",60d);
		checkCalc(p,"payload.valueIB-60D",60d);

		checkCalc(p,"payload.valueS-60D",60d);
		checkCalc(p,"payload.valueB-60D",60d);
		checkCalc(p,"payload.valueF-60D",60d);
		checkCalc(p,"payload.valueD-60D",60d);
		checkCalc(p,"payload.valueJ-60D",60d);
		checkCalc(p,"payload.valueI-60D",60d);

		checkCalc(p,"payload.valueSB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueBB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueFB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueDB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueIB-payload.valueDB60",60d);

		checkCalc(p,"payload.valueS-payload.valueDB60",60d);
		checkCalc(p,"payload.valueB-payload.valueDB60",60d);
		checkCalc(p,"payload.valueF-payload.valueDB60",60d);
		checkCalc(p,"payload.valueD-payload.valueDB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueDB60",60d);
		checkCalc(p,"payload.valueI-payload.valueDB60",60d);

		// right is a float
		checkCalc(p,"payload.valueSB-60F",60F);
		checkCalc(p,"payload.valueBB-60F",60F);
		checkCalc(p,"payload.valueFB-60F",60f);
		checkCalc(p,"payload.valueDB-60F",60d);
		checkCalc(p,"payload.valueJB-60F",60F);
		checkCalc(p,"payload.valueIB-60F",60F);

		checkCalc(p,"payload.valueS-60F",60F);
		checkCalc(p,"payload.valueB-60F",60F);
		checkCalc(p,"payload.valueF-60F",60f);
		checkCalc(p,"payload.valueD-60F",60d);
		checkCalc(p,"payload.valueJ-60F",60F);
		checkCalc(p,"payload.valueI-60F",60F);

		checkCalc(p,"payload.valueSB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueBB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueFB-payload.valueFB60",60f);
		checkCalc(p,"payload.valueDB-payload.valueFB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueIB-payload.valueFB60",60F);

		checkCalc(p,"payload.valueS-payload.valueFB60",60F);
		checkCalc(p,"payload.valueB-payload.valueFB60",60F);
		checkCalc(p,"payload.valueF-payload.valueFB60",60f);
		checkCalc(p,"payload.valueD-payload.valueFB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueFB60",60F);
		checkCalc(p,"payload.valueI-payload.valueFB60",60F);

		// right is a long
		checkCalc(p,"payload.valueSB-60L",60L);
		checkCalc(p,"payload.valueBB-60L",60L);
		checkCalc(p,"payload.valueFB-60L",60f);
		checkCalc(p,"payload.valueDB-60L",60d);
		checkCalc(p,"payload.valueJB-60L",60L);
		checkCalc(p,"payload.valueIB-60L",60L);

		checkCalc(p,"payload.valueS-60L",60L);
		checkCalc(p,"payload.valueB-60L",60L);
		checkCalc(p,"payload.valueF-60L",60f);
		checkCalc(p,"payload.valueD-60L",60d);
		checkCalc(p,"payload.valueJ-60L",60L);
		checkCalc(p,"payload.valueI-60L",60L);

		checkCalc(p,"payload.valueSB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueBB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueFB-payload.valueJB60",60f);
		checkCalc(p,"payload.valueDB-payload.valueJB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueIB-payload.valueJB60",60L);

		checkCalc(p,"payload.valueS-payload.valueJB60",60L);
		checkCalc(p,"payload.valueB-payload.valueJB60",60L);
		checkCalc(p,"payload.valueF-payload.valueJB60",60f);
		checkCalc(p,"payload.valueD-payload.valueJB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueJB60",60L);
		checkCalc(p,"payload.valueI-payload.valueJB60",60L);

		// right is an int
		checkCalc(p,"payload.valueSB-60",60);
		checkCalc(p,"payload.valueBB-60",60);
		checkCalc(p,"payload.valueFB-60",60f);
		checkCalc(p,"payload.valueDB-60",60d);
		checkCalc(p,"payload.valueJB-60",60L);
		checkCalc(p,"payload.valueIB-60",60);

		checkCalc(p,"payload.valueS-60",60);
		checkCalc(p,"payload.valueB-60",60);
		checkCalc(p,"payload.valueF-60",60f);
		checkCalc(p,"payload.valueD-60",60d);
		checkCalc(p,"payload.valueJ-60",60L);
		checkCalc(p,"payload.valueI-60",60);

		checkCalc(p,"payload.valueSB-payload.valueIB60",60);
		checkCalc(p,"payload.valueBB-payload.valueIB60",60);
		checkCalc(p,"payload.valueFB-payload.valueIB60",60f);
		checkCalc(p,"payload.valueDB-payload.valueIB60",60d);
		checkCalc(p,"payload.valueJB-payload.valueIB60",60L);
		checkCalc(p,"payload.valueIB-payload.valueIB60",60);

		checkCalc(p,"payload.valueS-payload.valueIB60",60);
		checkCalc(p,"payload.valueB-payload.valueIB60",60);
		checkCalc(p,"payload.valueF-payload.valueIB60",60f);
		checkCalc(p,"payload.valueD-payload.valueIB60",60d);
		checkCalc(p,"payload.valueJ-payload.valueIB60",60L);
		checkCalc(p,"payload.valueI-payload.valueIB60",60);

		// right is a short
		checkCalc(p,"payload.valueSB-payload.valueS20",100);
		checkCalc(p,"payload.valueBB-payload.valueS20",100);
		checkCalc(p,"payload.valueFB-payload.valueS20",100f);
		checkCalc(p,"payload.valueDB-payload.valueS20",100d);
		checkCalc(p,"payload.valueJB-payload.valueS20",100L);
		checkCalc(p,"payload.valueIB-payload.valueS20",100);

		checkCalc(p,"payload.valueS-payload.valueS20",100);
		checkCalc(p,"payload.valueB-payload.valueS20",100);
		checkCalc(p,"payload.valueF-payload.valueS20",100f);
		checkCalc(p,"payload.valueD-payload.valueS20",100d);
		checkCalc(p,"payload.valueJ-payload.valueS20",100L);
		checkCalc(p,"payload.valueI-payload.valueS20",100);

		checkCalc(p,"payload.valueSB-payload.valueSB20",100);
		checkCalc(p,"payload.valueBB-payload.valueSB20",100);
		checkCalc(p,"payload.valueFB-payload.valueSB20",100f);
		checkCalc(p,"payload.valueDB-payload.valueSB20",100d);
		checkCalc(p,"payload.valueJB-payload.valueSB20",100L);
		checkCalc(p,"payload.valueIB-payload.valueSB20",100);

		checkCalc(p,"payload.valueS-payload.valueSB20",100);
		checkCalc(p,"payload.valueB-payload.valueSB20",100);
		checkCalc(p,"payload.valueF-payload.valueSB20",100f);
		checkCalc(p,"payload.valueD-payload.valueSB20",100d);
		checkCalc(p,"payload.valueJ-payload.valueSB20",100L);
		checkCalc(p,"payload.valueI-payload.valueSB20",100);

		// right is a byte
		checkCalc(p,"payload.valueSB-payload.valueB20",100);
		checkCalc(p,"payload.valueBB-payload.valueB20",100);
		checkCalc(p,"payload.valueFB-payload.valueB20",100f);
		checkCalc(p,"payload.valueDB-payload.valueB20",100d);
		checkCalc(p,"payload.valueJB-payload.valueB20",100L);
		checkCalc(p,"payload.valueIB-payload.valueB20",100);

		checkCalc(p,"payload.valueS-payload.valueB20",100);
		checkCalc(p,"payload.valueB-payload.valueB20",100);
		checkCalc(p,"payload.valueF-payload.valueB20",100f);
		checkCalc(p,"payload.valueD-payload.valueB20",100d);
		checkCalc(p,"payload.valueJ-payload.valueB20",100L);
		checkCalc(p,"payload.valueI-payload.valueB20",100);

		checkCalc(p,"payload.valueSB-payload.valueBB20",100);
		checkCalc(p,"payload.valueBB-payload.valueBB20",100);
		checkCalc(p,"payload.valueFB-payload.valueBB20",100f);
		checkCalc(p,"payload.valueDB-payload.valueBB20",100d);
		checkCalc(p,"payload.valueJB-payload.valueBB20",100L);
		checkCalc(p,"payload.valueIB-payload.valueBB20",100);

		checkCalc(p,"payload.valueS-payload.valueBB20",100);
		checkCalc(p,"payload.valueB-payload.valueBB20",100);
		checkCalc(p,"payload.valueF-payload.valueBB20",100f);
		checkCalc(p,"payload.valueD-payload.valueBB20",100d);
		checkCalc(p,"payload.valueJ-payload.valueBB20",100L);
		checkCalc(p,"payload.valueI-payload.valueBB20",100);
	}

	@Test
	void opMultiply_mixedNumberTypes() {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB*60D",7200d);
		checkCalc(p,"payload.valueBB*60D",7200d);
		checkCalc(p,"payload.valueFB*60D",7200d);
		checkCalc(p,"payload.valueDB*60D",7200d);
		checkCalc(p,"payload.valueJB*60D",7200d);
		checkCalc(p,"payload.valueIB*60D",7200d);

		checkCalc(p,"payload.valueS*60D",7200d);
		checkCalc(p,"payload.valueB*60D",7200d);
		checkCalc(p,"payload.valueF*60D",7200d);
		checkCalc(p,"payload.valueD*60D",7200d);
		checkCalc(p,"payload.valueJ*60D",7200d);
		checkCalc(p,"payload.valueI*60D",7200d);

		checkCalc(p,"payload.valueSB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueBB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueFB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueDB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueIB*payload.valueDB60",7200d);

		checkCalc(p,"payload.valueS*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueB*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueF*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueD*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueDB60",7200d);
		checkCalc(p,"payload.valueI*payload.valueDB60",7200d);

		// right is a float
		checkCalc(p,"payload.valueSB*60F",7200F);
		checkCalc(p,"payload.valueBB*60F",7200F);
		checkCalc(p,"payload.valueFB*60F",7200f);
		checkCalc(p,"payload.valueDB*60F",7200d);
		checkCalc(p,"payload.valueJB*60F",7200F);
		checkCalc(p,"payload.valueIB*60F",7200F);

		checkCalc(p,"payload.valueS*60F",7200F);
		checkCalc(p,"payload.valueB*60F",7200F);
		checkCalc(p,"payload.valueF*60F",7200f);
		checkCalc(p,"payload.valueD*60F",7200d);
		checkCalc(p,"payload.valueJ*60F",7200F);
		checkCalc(p,"payload.valueI*60F",7200F);

		checkCalc(p,"payload.valueSB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueBB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueFB*payload.valueFB60",7200f);
		checkCalc(p,"payload.valueDB*payload.valueFB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueIB*payload.valueFB60",7200F);

		checkCalc(p,"payload.valueS*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueB*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueF*payload.valueFB60",7200f);
		checkCalc(p,"payload.valueD*payload.valueFB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueFB60",7200F);
		checkCalc(p,"payload.valueI*payload.valueFB60",7200F);

		// right is a long
		checkCalc(p,"payload.valueSB*60L",7200L);
		checkCalc(p,"payload.valueBB*60L",7200L);
		checkCalc(p,"payload.valueFB*60L",7200f);
		checkCalc(p,"payload.valueDB*60L",7200d);
		checkCalc(p,"payload.valueJB*60L",7200L);
		checkCalc(p,"payload.valueIB*60L",7200L);

		checkCalc(p,"payload.valueS*60L",7200L);
		checkCalc(p,"payload.valueB*60L",7200L);
		checkCalc(p,"payload.valueF*60L",7200f);
		checkCalc(p,"payload.valueD*60L",7200d);
		checkCalc(p,"payload.valueJ*60L",7200L);
		checkCalc(p,"payload.valueI*60L",7200L);

		checkCalc(p,"payload.valueSB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueBB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueFB*payload.valueJB60",7200f);
		checkCalc(p,"payload.valueDB*payload.valueJB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueIB*payload.valueJB60",7200L);

		checkCalc(p,"payload.valueS*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueB*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueF*payload.valueJB60",7200f);
		checkCalc(p,"payload.valueD*payload.valueJB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueJB60",7200L);
		checkCalc(p,"payload.valueI*payload.valueJB60",7200L);

		// right is an int
		checkCalc(p,"payload.valueSB*60",7200);
		checkCalc(p,"payload.valueBB*60",7200);
		checkCalc(p,"payload.valueFB*60",7200f);
		checkCalc(p,"payload.valueDB*60",7200d);
		checkCalc(p,"payload.valueJB*60",7200L);
		checkCalc(p,"payload.valueIB*60",7200);

		checkCalc(p,"payload.valueS*60",7200);
		checkCalc(p,"payload.valueB*60",7200);
		checkCalc(p,"payload.valueF*60",7200f);
		checkCalc(p,"payload.valueD*60",7200d);
		checkCalc(p,"payload.valueJ*60",7200L);
		checkCalc(p,"payload.valueI*60",7200);

		checkCalc(p,"payload.valueSB*payload.valueIB60",7200);
		checkCalc(p,"payload.valueBB*payload.valueIB60",7200);
		checkCalc(p,"payload.valueFB*payload.valueIB60",7200f);
		checkCalc(p,"payload.valueDB*payload.valueIB60",7200d);
		checkCalc(p,"payload.valueJB*payload.valueIB60",7200L);
		checkCalc(p,"payload.valueIB*payload.valueIB60",7200);

		checkCalc(p,"payload.valueS*payload.valueIB60",7200);
		checkCalc(p,"payload.valueB*payload.valueIB60",7200);
		checkCalc(p,"payload.valueF*payload.valueIB60",7200f);
		checkCalc(p,"payload.valueD*payload.valueIB60",7200d);
		checkCalc(p,"payload.valueJ*payload.valueIB60",7200L);
		checkCalc(p,"payload.valueI*payload.valueIB60",7200);

		// right is a short
		checkCalc(p,"payload.valueSB*payload.valueS20",2400);
		checkCalc(p,"payload.valueBB*payload.valueS20",2400);
		checkCalc(p,"payload.valueFB*payload.valueS20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueS20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueS20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueS20",2400);

		checkCalc(p,"payload.valueS*payload.valueS20",2400);
		checkCalc(p,"payload.valueB*payload.valueS20",2400);
		checkCalc(p,"payload.valueF*payload.valueS20",2400f);
		checkCalc(p,"payload.valueD*payload.valueS20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueS20",2400L);
		checkCalc(p,"payload.valueI*payload.valueS20",2400);

		checkCalc(p,"payload.valueSB*payload.valueSB20",2400);
		checkCalc(p,"payload.valueBB*payload.valueSB20",2400);
		checkCalc(p,"payload.valueFB*payload.valueSB20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueSB20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueSB20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueSB20",2400);

		checkCalc(p,"payload.valueS*payload.valueSB20",2400);
		checkCalc(p,"payload.valueB*payload.valueSB20",2400);
		checkCalc(p,"payload.valueF*payload.valueSB20",2400f);
		checkCalc(p,"payload.valueD*payload.valueSB20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueSB20",2400L);
		checkCalc(p,"payload.valueI*payload.valueSB20",2400);

		// right is a byte
		checkCalc(p,"payload.valueSB*payload.valueB20",2400);
		checkCalc(p,"payload.valueBB*payload.valueB20",2400);
		checkCalc(p,"payload.valueFB*payload.valueB20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueB20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueB20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueB20",2400);

		checkCalc(p,"payload.valueS*payload.valueB20",2400);
		checkCalc(p,"payload.valueB*payload.valueB20",2400);
		checkCalc(p,"payload.valueF*payload.valueB20",2400f);
		checkCalc(p,"payload.valueD*payload.valueB20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueB20",2400L);
		checkCalc(p,"payload.valueI*payload.valueB20",2400);

		checkCalc(p,"payload.valueSB*payload.valueBB20",2400);
		checkCalc(p,"payload.valueBB*payload.valueBB20",2400);
		checkCalc(p,"payload.valueFB*payload.valueBB20",2400f);
		checkCalc(p,"payload.valueDB*payload.valueBB20",2400d);
		checkCalc(p,"payload.valueJB*payload.valueBB20",2400L);
		checkCalc(p,"payload.valueIB*payload.valueBB20",2400);

		checkCalc(p,"payload.valueS*payload.valueBB20",2400);
		checkCalc(p,"payload.valueB*payload.valueBB20",2400);
		checkCalc(p,"payload.valueF*payload.valueBB20",2400f);
		checkCalc(p,"payload.valueD*payload.valueBB20",2400d);
		checkCalc(p,"payload.valueJ*payload.valueBB20",2400L);
		checkCalc(p,"payload.valueI*payload.valueBB20",2400);
	}

	@Test
	void opModulus_mixedNumberTypes() {
		PayloadX p = new PayloadX();

		// This is what you had to do before the changes in order for it to compile:
		//	expression = parse("(T(java.lang.Double).parseDouble(payload.valueI.toString()))/60D");

		// right is a double
		checkCalc(p,"payload.valueSB%58D",4d);
		checkCalc(p,"payload.valueBB%58D",4d);
		checkCalc(p,"payload.valueFB%58D",4d);
		checkCalc(p,"payload.valueDB%58D",4d);
		checkCalc(p,"payload.valueJB%58D",4d);
		checkCalc(p,"payload.valueIB%58D",4d);

		checkCalc(p,"payload.valueS%58D",4d);
		checkCalc(p,"payload.valueB%58D",4d);
		checkCalc(p,"payload.valueF%58D",4d);
		checkCalc(p,"payload.valueD%58D",4d);
		checkCalc(p,"payload.valueJ%58D",4d);
		checkCalc(p,"payload.valueI%58D",4d);

		checkCalc(p,"payload.valueSB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueBB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueFB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueDB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueIB%payload.valueDB58",4d);

		checkCalc(p,"payload.valueS%payload.valueDB58",4d);
		checkCalc(p,"payload.valueB%payload.valueDB58",4d);
		checkCalc(p,"payload.valueF%payload.valueDB58",4d);
		checkCalc(p,"payload.valueD%payload.valueDB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueDB58",4d);
		checkCalc(p,"payload.valueI%payload.valueDB58",4d);

		// right is a float
		checkCalc(p,"payload.valueSB%58F",4F);
		checkCalc(p,"payload.valueBB%58F",4F);
		checkCalc(p,"payload.valueFB%58F",4f);
		checkCalc(p,"payload.valueDB%58F",4d);
		checkCalc(p,"payload.valueJB%58F",4F);
		checkCalc(p,"payload.valueIB%58F",4F);

		checkCalc(p,"payload.valueS%58F",4F);
		checkCalc(p,"payload.valueB%58F",4F);
		checkCalc(p,"payload.valueF%58F",4f);
		checkCalc(p,"payload.valueD%58F",4d);
		checkCalc(p,"payload.valueJ%58F",4F);
		checkCalc(p,"payload.valueI%58F",4F);

		checkCalc(p,"payload.valueSB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueBB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueFB%payload.valueFB58",4f);
		checkCalc(p,"payload.valueDB%payload.valueFB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueIB%payload.valueFB58",4F);

		checkCalc(p,"payload.valueS%payload.valueFB58",4F);
		checkCalc(p,"payload.valueB%payload.valueFB58",4F);
		checkCalc(p,"payload.valueF%payload.valueFB58",4f);
		checkCalc(p,"payload.valueD%payload.valueFB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueFB58",4F);
		checkCalc(p,"payload.valueI%payload.valueFB58",4F);

		// right is a long
		checkCalc(p,"payload.valueSB%58L",4L);
		checkCalc(p,"payload.valueBB%58L",4L);
		checkCalc(p,"payload.valueFB%58L",4f);
		checkCalc(p,"payload.valueDB%58L",4d);
		checkCalc(p,"payload.valueJB%58L",4L);
		checkCalc(p,"payload.valueIB%58L",4L);

		checkCalc(p,"payload.valueS%58L",4L);
		checkCalc(p,"payload.valueB%58L",4L);
		checkCalc(p,"payload.valueF%58L",4f);
		checkCalc(p,"payload.valueD%58L",4d);
		checkCalc(p,"payload.valueJ%58L",4L);
		checkCalc(p,"payload.valueI%58L",4L);

		checkCalc(p,"payload.valueSB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueBB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueFB%payload.valueJB58",4f);
		checkCalc(p,"payload.valueDB%payload.valueJB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueIB%payload.valueJB58",4L);

		checkCalc(p,"payload.valueS%payload.valueJB58",4L);
		checkCalc(p,"payload.valueB%payload.valueJB58",4L);
		checkCalc(p,"payload.valueF%payload.valueJB58",4f);
		checkCalc(p,"payload.valueD%payload.valueJB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueJB58",4L);
		checkCalc(p,"payload.valueI%payload.valueJB58",4L);

		// right is an int
		checkCalc(p,"payload.valueSB%58",4);
		checkCalc(p,"payload.valueBB%58",4);
		checkCalc(p,"payload.valueFB%58",4f);
		checkCalc(p,"payload.valueDB%58",4d);
		checkCalc(p,"payload.valueJB%58",4L);
		checkCalc(p,"payload.valueIB%58",4);

		checkCalc(p,"payload.valueS%58",4);
		checkCalc(p,"payload.valueB%58",4);
		checkCalc(p,"payload.valueF%58",4f);
		checkCalc(p,"payload.valueD%58",4d);
		checkCalc(p,"payload.valueJ%58",4L);
		checkCalc(p,"payload.valueI%58",4);

		checkCalc(p,"payload.valueSB%payload.valueIB58",4);
		checkCalc(p,"payload.valueBB%payload.valueIB58",4);
		checkCalc(p,"payload.valueFB%payload.valueIB58",4f);
		checkCalc(p,"payload.valueDB%payload.valueIB58",4d);
		checkCalc(p,"payload.valueJB%payload.valueIB58",4L);
		checkCalc(p,"payload.valueIB%payload.valueIB58",4);

		checkCalc(p,"payload.valueS%payload.valueIB58",4);
		checkCalc(p,"payload.valueB%payload.valueIB58",4);
		checkCalc(p,"payload.valueF%payload.valueIB58",4f);
		checkCalc(p,"payload.valueD%payload.valueIB58",4d);
		checkCalc(p,"payload.valueJ%payload.valueIB58",4L);
		checkCalc(p,"payload.valueI%payload.valueIB58",4);

		// right is a short
		checkCalc(p,"payload.valueSB%payload.valueS18",12);
		checkCalc(p,"payload.valueBB%payload.valueS18",12);
		checkCalc(p,"payload.valueFB%payload.valueS18",12f);
		checkCalc(p,"payload.valueDB%payload.valueS18",12d);
		checkCalc(p,"payload.valueJB%payload.valueS18",12L);
		checkCalc(p,"payload.valueIB%payload.valueS18",12);

		checkCalc(p,"payload.valueS%payload.valueS18",12);
		checkCalc(p,"payload.valueB%payload.valueS18",12);
		checkCalc(p,"payload.valueF%payload.valueS18",12f);
		checkCalc(p,"payload.valueD%payload.valueS18",12d);
		checkCalc(p,"payload.valueJ%payload.valueS18",12L);
		checkCalc(p,"payload.valueI%payload.valueS18",12);

		checkCalc(p,"payload.valueSB%payload.valueSB18",12);
		checkCalc(p,"payload.valueBB%payload.valueSB18",12);
		checkCalc(p,"payload.valueFB%payload.valueSB18",12f);
		checkCalc(p,"payload.valueDB%payload.valueSB18",12d);
		checkCalc(p,"payload.valueJB%payload.valueSB18",12L);
		checkCalc(p,"payload.valueIB%payload.valueSB18",12);

		checkCalc(p,"payload.valueS%payload.valueSB18",12);
		checkCalc(p,"payload.valueB%payload.valueSB18",12);
		checkCalc(p,"payload.valueF%payload.valueSB18",12f);
		checkCalc(p,"payload.valueD%payload.valueSB18",12d);
		checkCalc(p,"payload.valueJ%payload.valueSB18",12L);
		checkCalc(p,"payload.valueI%payload.valueSB18",12);

		// right is a byte
		checkCalc(p,"payload.valueSB%payload.valueB18",12);
		checkCalc(p,"payload.valueBB%payload.valueB18",12);
		checkCalc(p,"payload.valueFB%payload.valueB18",12f);
		checkCalc(p,"payload.valueDB%payload.valueB18",12d);
		checkCalc(p,"payload.valueJB%payload.valueB18",12L);
		checkCalc(p,"payload.valueIB%payload.valueB18",12);

		checkCalc(p,"payload.valueS%payload.valueB18",12);
		checkCalc(p,"payload.valueB%payload.valueB18",12);
		checkCalc(p,"payload.valueF%payload.valueB18",12f);
		checkCalc(p,"payload.valueD%payload.valueB18",12d);
		checkCalc(p,"payload.valueJ%payload.valueB18",12L);
		checkCalc(p,"payload.valueI%payload.valueB18",12);

		checkCalc(p,"payload.valueSB%payload.valueBB18",12);
		checkCalc(p,"payload.valueBB%payload.valueBB18",12);
		checkCalc(p,"payload.valueFB%payload.valueBB18",12f);
		checkCalc(p,"payload.valueDB%payload.valueBB18",12d);
		checkCalc(p,"payload.valueJB%payload.valueBB18",12L);
		checkCalc(p,"payload.valueIB%payload.valueBB18",12);

		checkCalc(p,"payload.valueS%payload.valueBB18",12);
		checkCalc(p,"payload.valueB%payload.valueBB18",12);
		checkCalc(p,"payload.valueF%payload.valueBB18",12f);
		checkCalc(p,"payload.valueD%payload.valueBB18",12d);
		checkCalc(p,"payload.valueJ%payload.valueBB18",12L);
		checkCalc(p,"payload.valueI%payload.valueBB18",12);
	}

	@Test
	void opMultiply() {
		expression = parse("2*2");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4);

		expression = parse("2L*2L");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4L);

		expression = parse("2.0f*2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4.0f);

		expression = parse("3.0d*4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(12.0d);

		expression = parse("T(Float).valueOf(2.0f)*6");
		assertThat(expression.getValue()).isEqualTo(12.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(12.0f);

		expression = parse("T(Float).valueOf(8.0f)*T(Float).valueOf(3.0f)");
		assertThat(expression.getValue()).isEqualTo(24.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(24.0f);

		expression = parse("11L*T(Long).valueOf(4L)");
		assertThat(expression.getValue()).isEqualTo(44L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(44L);

		expression = parse("T(Long).valueOf(9L)*6");
		assertThat(expression.getValue()).isEqualTo(54L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(54L);

		expression = parse("T(Long).valueOf(4L)*T(Long).valueOf(3L)");
		assertThat(expression.getValue()).isEqualTo(12L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(12L);

		expression = parse("8L*T(Long).valueOf(2L)");
		assertThat(expression.getValue()).isEqualTo(16L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(16L);

		expression = parse("T(Float).valueOf(8.0f)*-T(Float).valueOf(3.0f)");
		assertThat(expression.getValue()).isEqualTo(-24.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-24.0f);
	}

	@Test
	void opDivide() {
		expression = parse("2/2");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1);

		expression = parse("2L/2L");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1L);

		expression = parse("2.0f/2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1.0f);

		expression = parse("3.0d/4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(0.75d);

		expression = parse("T(Float).valueOf(6.0f)/2");
		assertThat(expression.getValue()).isEqualTo(3.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(3.0f);

		expression = parse("T(Float).valueOf(8.0f)/T(Float).valueOf(2.0f)");
		assertThat(expression.getValue()).isEqualTo(4.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4.0f);

		expression = parse("12L/T(Long).valueOf(4L)");
		assertThat(expression.getValue()).isEqualTo(3L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(3L);

		expression = parse("T(Long).valueOf(44L)/11");
		assertThat(expression.getValue()).isEqualTo(4L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4L);

		expression = parse("T(Long).valueOf(4L)/T(Long).valueOf(2L)");
		assertThat(expression.getValue()).isEqualTo(2L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2L);

		expression = parse("8L/T(Long).valueOf(2L)");
		assertThat(expression.getValue()).isEqualTo(4L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(4L);

		expression = parse("T(Float).valueOf(8.0f)/-T(Float).valueOf(4.0f)");
		assertThat(expression.getValue()).isEqualTo(-2.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(-2.0f);
	}

	@Test
	void opModulus_12041() {
		expression = parse("2%2");
		assertThat(expression.getValue()).isEqualTo(0);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(0);

		expression = parse("payload%2==0");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4), boolean.class)).isTrue();
		assertThat(expression.getValue(new GenericMessageTestHelper<>(5), boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4), boolean.class)).isTrue();
		assertThat(expression.getValue(new GenericMessageTestHelper<>(5), boolean.class)).isFalse();

		expression = parse("8%3");
		assertThat(expression.getValue()).isEqualTo(2);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2);

		expression = parse("17L%5L");
		assertThat(expression.getValue()).isEqualTo(2L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2L);

		expression = parse("3.0f%2.0f");
		assertThat(expression.getValue()).isEqualTo(1.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1.0f);

		expression = parse("3.0d%4.0d");
		assertThat(expression.getValue()).isEqualTo(3.0d);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(3.0d);

		expression = parse("T(Float).valueOf(6.0f)%2");
		assertThat(expression.getValue()).isEqualTo(0.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(0.0f);

		expression = parse("T(Float).valueOf(6.0f)%4");
		assertThat(expression.getValue()).isEqualTo(2.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2.0f);

		expression = parse("T(Float).valueOf(8.0f)%T(Float).valueOf(3.0f)");
		assertThat(expression.getValue()).isEqualTo(2.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(2.0f);

		expression = parse("13L%T(Long).valueOf(4L)");
		assertThat(expression.getValue()).isEqualTo(1L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1L);

		expression = parse("T(Long).valueOf(44L)%12");
		assertThat(expression.getValue()).isEqualTo(8L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(8L);

		expression = parse("T(Long).valueOf(9L)%T(Long).valueOf(2L)");
		assertThat(expression.getValue()).isEqualTo(1L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1L);

		expression = parse("7L%T(Long).valueOf(2L)");
		assertThat(expression.getValue()).isEqualTo(1L);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1L);

		expression = parse("T(Float).valueOf(9.0f)%-T(Float).valueOf(4.0f)");
		assertThat(expression.getValue()).isEqualTo(1.0f);
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo(1.0f);
	}

	@Test
	void compilationOfBasicNullSafeMethodReference() {
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.OFF, getClass().getClassLoader()));
		SpelExpression expression = parser.parseRaw("#it?.equals(3)");
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {1});
		context.setVariable("it", 3);
		expression.setEvaluationContext(context);
		assertThat(expression.getValue(Boolean.class)).isTrue();
		context.setVariable("it", null);
		assertThat(expression.getValue(Boolean.class)).isNull();

		assertCanCompile(expression);

		context.setVariable("it", 3);
		assertThat(expression.getValue(Boolean.class)).isTrue();
		context.setVariable("it", null);
		assertThat(expression.getValue(Boolean.class)).isNull();
	}

	@Test
	void failsWhenSettingContextForExpression_SPR12326() {
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.OFF, getClass().getClassLoader()));
		Person3 person = new Person3("foo", 1);
		SpelExpression expression = parser.parseRaw("#it?.age?.equals([0])");
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {1});
		context.setVariable("it", person);
		expression.setEvaluationContext(context);
		assertThat(expression.getValue(Boolean.class)).isTrue();
		// This will trigger compilation (second usage)
		assertThat(expression.getValue(Boolean.class)).isTrue();
		context.setVariable("it", null);
		assertThat(expression.getValue(Boolean.class)).isNull();

		assertCanCompile(expression);

		context.setVariable("it", person);
		assertThat(expression.getValue(Boolean.class)).isTrue();
		context.setVariable("it", null);
		assertThat(expression.getValue(Boolean.class)).isNull();
	}

	/**
	 * Test variants of using T(...) and static/non-static method/property/field references.
	 */
	@Test
	void constructorReference_SPR13781() {
		// Static field access on a T() referenced type
		expression = parser.parseExpression("T(java.util.Locale).ENGLISH");
		assertThat(expression.getValue().toString()).isEqualTo("en");
		assertCanCompile(expression);
		assertThat(expression.getValue().toString()).isEqualTo("en");

		// The actual expression from the bug report. It fails if the ENGLISH reference fails
		// to pop the type reference for Locale off the stack (if it isn't popped then
		// toLowerCase() will be called with a Locale parameter). In this situation the
		// code generation for ENGLISH should notice there is something on the stack that
		// is not required and pop it off.
		expression = parser.parseExpression("#userId.toString().toLowerCase(T(java.util.Locale).ENGLISH)");
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("userId", "RoDnEy");
		assertThat(expression.getValue(context)).isEqualTo("rodney");
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo("rodney");

		// Property access on a class object
		expression = parser.parseExpression("T(String).name");
		assertThat(expression.getValue()).isEqualTo("java.lang.String");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("java.lang.String");

		// Now the type reference isn't on the stack, and needs loading
		context = new StandardEvaluationContext(String.class);
		expression = parser.parseExpression("name");
		assertThat(expression.getValue(context)).isEqualTo("java.lang.String");
		assertCanCompile(expression);
		assertThat(expression.getValue(context)).isEqualTo("java.lang.String");

		expression = parser.parseExpression("T(String).getName()");
		assertThat(expression.getValue()).isEqualTo("java.lang.String");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("java.lang.String");

		// These tests below verify that the chain of static accesses (either method/property or field)
		// leave the right thing on top of the stack for processing by any outer consuming code.
		// Here the consuming code is the String.valueOf() function.  If the wrong thing were on
		// the stack (for example if the compiled code for static methods wasn't popping the
		// previous thing off the stack) the valueOf() would operate on the wrong value.

		String shclass = StaticsHelper.class.getName();
		// Basic chain: property access then method access
		expression = parser.parseExpression("T(String).valueOf(T(String).name.valueOf(1))");
		assertThat(expression.getValue()).isEqualTo("1");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("1");

		// chain of statics ending with static method
		expression = parser.parseExpression("T(String).valueOf(T(" + shclass + ").methoda().methoda().methodb())");
		assertThat(expression.getValue()).isEqualTo("mb");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("mb");

		// chain of statics ending with static field
		expression = parser.parseExpression("T(String).valueOf(T(" + shclass + ").fielda.fielda.fieldb)");
		assertThat(expression.getValue()).isEqualTo("fb");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("fb");

		// chain of statics ending with static property access
		expression = parser.parseExpression("T(String).valueOf(T(" + shclass + ").propertya.propertya.propertyb)");
		assertThat(expression.getValue()).isEqualTo("pb");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("pb");

		// variety chain
		expression = parser.parseExpression("T(String).valueOf(T(" + shclass + ").fielda.methoda().propertya.fieldb)");
		assertThat(expression.getValue()).isEqualTo("fb");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("fb");

		expression = parser.parseExpression("T(String).valueOf(fielda.fieldb)");
		assertThat(expression.getValue(StaticsHelper.sh)).isEqualTo("fb");
		assertCanCompile(expression);
		assertThat(expression.getValue(StaticsHelper.sh)).isEqualTo("fb");

		expression = parser.parseExpression("T(String).valueOf(propertya.propertyb)");
		assertThat(expression.getValue(StaticsHelper.sh)).isEqualTo("pb");
		assertCanCompile(expression);
		assertThat(expression.getValue(StaticsHelper.sh)).isEqualTo("pb");

		expression = parser.parseExpression("T(String).valueOf(methoda().methodb())");
		assertThat(expression.getValue(StaticsHelper.sh)).isEqualTo("mb");
		assertCanCompile(expression);
		assertThat(expression.getValue(StaticsHelper.sh)).isEqualTo("mb");
	}

	@Test
	void constructorReference_SPR12326() {
		String type = getClass().getName();
		String prefix = "new " + type + ".Obj";

		expression = parser.parseExpression(prefix + "([0])");
		assertThat(((Obj) expression.getValue(new Object[]{"test"})).param1).isEqualTo("test");
		assertCanCompile(expression);
		assertThat(((Obj) expression.getValue(new Object[]{"test"})).param1).isEqualTo("test");

		expression = parser.parseExpression(prefix + "2('foo','bar').output");
		assertThat(expression.getValue(String.class)).isEqualTo("foobar");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("foobar");

		expression = parser.parseExpression(prefix + "2('foo').output");
		assertThat(expression.getValue(String.class)).isEqualTo("foo");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("foo");

		expression = parser.parseExpression(prefix + "2().output");
		assertThat(expression.getValue(String.class)).isEmpty();
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEmpty();

		expression = parser.parseExpression(prefix + "3(1,2,3).output");
		assertThat(expression.getValue(String.class)).isEqualTo("123");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("123");

		expression = parser.parseExpression(prefix + "3(1).output");
		assertThat(expression.getValue(String.class)).isEqualTo("1");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("1");

		expression = parser.parseExpression(prefix + "3().output");
		assertThat(expression.getValue(String.class)).isEmpty();
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEmpty();

		expression = parser.parseExpression(prefix + "3('abc',5.0f,1,2,3).output");
		assertThat(expression.getValue(String.class)).isEqualTo("abc:5.0:123");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("abc:5.0:123");

		expression = parser.parseExpression(prefix + "3('abc',5.0f,1).output");
		assertThat(expression.getValue(String.class)).isEqualTo("abc:5.0:1");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("abc:5.0:1");

		expression = parser.parseExpression(prefix + "3('abc',5.0f).output");
		assertThat(expression.getValue(String.class)).isEqualTo("abc:5.0:");
		assertCanCompile(expression);
		assertThat(expression.getValue(String.class)).isEqualTo("abc:5.0:");

		expression = parser.parseExpression(prefix + "4(#root).output");
		assertThat(expression.getValue(new int[] {1,2,3}, String.class)).isEqualTo("123");
		assertCanCompile(expression);
		assertThat(expression.getValue(new int[] {1,2,3}, String.class)).isEqualTo("123");
	}

	@Test
	void methodReferenceMissingCastAndRootObjectAccessing_SPR12326() {
		// Need boxing code on the 1 so that toString() can be called
		expression = parser.parseExpression("1.toString()");
		assertThat(expression.getValue()).isEqualTo("1");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("1");

		expression = parser.parseExpression("#it?.age.equals([0])");
		Person person = new Person(1);
		StandardEvaluationContext context = new StandardEvaluationContext(new Object[] {person.getAge()});
		context.setVariable("it", person);
		assertThat(expression.getValue(context, Boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(context, Boolean.class)).isTrue();

		// Variant of above more like what was in the bug report:
		SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, getClass().getClassLoader()));

		SpelExpression ex = parser.parseRaw("#it?.age.equals([0])");
		context = new StandardEvaluationContext(new Object[] {person.getAge()});
		context.setVariable("it", person);
		assertThat(ex.getValue(context, Boolean.class)).isTrue();

		PersonInOtherPackage person2 = new PersonInOtherPackage(1);
		ex = parser.parseRaw("#it?.age.equals([0])");
		context = new StandardEvaluationContext(new Object[] {person2.getAge()});
		context.setVariable("it", person2);
		assertThat(ex.getValue(context, Boolean.class)).isTrue();

		ex = parser.parseRaw("#it?.age.equals([0])");
		context = new StandardEvaluationContext(new Object[] {person2.getAge()});
		context.setVariable("it", person2);
		assertThat((Boolean) ex.getValue(context)).isTrue();
	}

	@Test
	void constructorReference() {
		// simple constructor
		expression = parser.parseExpression("new String('123')");
		assertThat(expression.getValue()).isEqualTo("123");
		assertCanCompile(expression);
		assertThat(expression.getValue()).isEqualTo("123");

		String testclass8 = TestClass8.class.getName();
		Object result;

		// multi arg constructor that includes primitives
		expression = parser.parseExpression("new " + testclass8 + "(42,'123',4.0d,true)");
		result = expression.getValue();
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		assertCanCompile(expression);
		result = expression.getValue();
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		TestClass8 tc8 = (TestClass8) result;
		assertThat(tc8.i).isEqualTo(42);
		assertThat(tc8.s).isEqualTo("123");
		assertThat(tc8.d).isCloseTo(4.0d, within(0.5d));

		assertThat(tc8.z).isTrue();

		// no-arg constructor
		expression = parser.parseExpression("new " + testclass8 + "()");
		result = expression.getValue();
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		assertCanCompile(expression);
		result = expression.getValue();
		assertThat(result).isExactlyInstanceOf(TestClass8.class);

		// pass primitive to reference type constructor
		expression = parser.parseExpression("new " + testclass8 + "(42)");
		result = expression.getValue();
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		assertCanCompile(expression);
		result = expression.getValue();
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		tc8 = (TestClass8) result;
		assertThat(tc8.i).isEqualTo(42);

		// varargs
		expression = parser.parseExpression("new " + testclass8 + "(#root)");
		Object[] objectArray = { "a", "b", "c" };
		result = expression.getValue(objectArray);
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		assertCanCompile(expression);
		result = expression.getValue(objectArray);
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		tc8 = (TestClass8) result;
		assertThat(tc8.args).containsExactly("a", "b", "c");

		// varargs with argument component type that is a subtype of the varargs component type.
		expression = parser.parseExpression("new " + testclass8 + "(#root)");
		String[] stringArray = { "a", "b", "c" };
		result = expression.getValue(stringArray);
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		assertCanCompile(expression);
		result = expression.getValue(stringArray);
		assertThat(result).isExactlyInstanceOf(TestClass8.class);
		tc8 = (TestClass8) result;
		assertThat(tc8.args).containsExactly("a", "b", "c");

		// private class, can't compile it
		String testclass9 = TestClass9.class.getName();
		expression = parser.parseExpression("new " + testclass9 + "(42)");
		result = expression.getValue();
		assertThat(result).isExactlyInstanceOf(TestClass9.class);
		assertCannotCompile(expression);
	}

	@Test
	void methodReferenceReflectiveMethodSelectionWithVarargs() {
		TestClass10 tc = new TestClass10();

		// Should call the non varargs version of concat
		// (which causes the '::' prefix in test output)
		expression = parser.parseExpression("concat('test')");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("::test");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("::test");
		tc.reset();

		// This will call the varargs concat with an empty array
		expression = parser.parseExpression("concat()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEmpty();
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEmpty();
		tc.reset();

		// Should call the non varargs version of concat
		// (which causes the '::' prefix in test output)
		expression = parser.parseExpression("concat2('test')");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("::test");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("::test");
		tc.reset();

		// This will call the varargs concat with an empty array
		expression = parser.parseExpression("concat2()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEmpty();
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEmpty();
		tc.reset();
	}

	@Test
	void methodReferenceVarargs() {
		TestClass5 tc = new TestClass5();

		// varargs string
		expression = parser.parseExpression("eleven()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEmpty();
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEmpty();
		tc.reset();

		// varargs string
		expression = parser.parseExpression("eleven('aaa')");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa");
		tc.reset();

		// varargs string
		expression = parser.parseExpression("eleven(stringArray)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		tc.reset();

		// varargs string
		expression = parser.parseExpression("eleven('aaa','bbb','ccc')");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		tc.reset();

		// varargs object
		expression = parser.parseExpression("sixteen('aaa','bbb','ccc')");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		tc.reset();

		// string array from property in varargs object
		expression = parser.parseExpression("sixteen(seventeen)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		tc.reset();

		// string array from variable in varargs object
		expression = parser.parseExpression("sixteen(stringArray)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaabbbccc");
		tc.reset();

		// string array in varargs object with other parameter
		expression = parser.parseExpression("eighteen('AAA', stringArray)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("AAA::aaabbbccc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("AAA::aaabbbccc");
		tc.reset();

		// varargs int
		expression = parser.parseExpression("twelve(1,2,3)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.i).isEqualTo(6);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.i).isEqualTo(6);
		tc.reset();

		expression = parser.parseExpression("twelve(1)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.i).isEqualTo(1);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.i).isEqualTo(1);
		tc.reset();

		// one string then varargs string
		expression = parser.parseExpression("thirteen('aaa','bbb','ccc')");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::bbbccc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::bbbccc");
		tc.reset();

		// nothing passed to varargs parameter
		expression = parser.parseExpression("thirteen('aaa')");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::");
		tc.reset();

		// nested arrays
		expression = parser.parseExpression("fourteen('aaa',stringArray,stringArray)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::{aaabbbccc}{aaabbbccc}");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::{aaabbbccc}{aaabbbccc}");
		tc.reset();

		// nested primitive array
		expression = parser.parseExpression("fifteen('aaa',intArray,intArray)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::{112233}{112233}");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("aaa::{112233}{112233}");
		tc.reset();

		// varargs boolean
		expression = parser.parseExpression("arrayz(true,true,false)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("truetruefalse");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("truetruefalse");
		tc.reset();

		expression = parser.parseExpression("arrayz(true)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("true");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("true");
		tc.reset();

		// varargs short
		expression = parser.parseExpression("arrays(s1,s2,s3)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("123");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("123");
		tc.reset();

		expression = parser.parseExpression("arrays(s1)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1");
		tc.reset();

		// varargs double
		expression = parser.parseExpression("arrayd(1.0d,2.0d,3.0d)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.02.03.0");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.02.03.0");
		tc.reset();

		expression = parser.parseExpression("arrayd(1.0d)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.0");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.0");
		tc.reset();

		// varargs long
		expression = parser.parseExpression("arrayj(l1,l2,l3)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("123");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("123");
		tc.reset();

		expression = parser.parseExpression("arrayj(l1)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1");
		tc.reset();

		// varargs char
		expression = parser.parseExpression("arrayc(c1,c2,c3)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("abc");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("abc");
		tc.reset();

		expression = parser.parseExpression("arrayc(c1)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("a");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("a");
		tc.reset();

		// varargs byte
		expression = parser.parseExpression("arrayb(b1,b2,b3)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("656667");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("656667");
		tc.reset();

		expression = parser.parseExpression("arrayb(b1)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("65");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("65");
		tc.reset();

		// varargs float
		expression = parser.parseExpression("arrayf(f1,f2,f3)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.02.03.0");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.02.03.0");
		tc.reset();

		expression = parser.parseExpression("arrayf(f1)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.0");
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("1.0");
		tc.reset();
	}

	@Test  // gh-27421
	public void nullSafeInvocationOfNonStaticVoidMethod() {
		// non-static method, no args, void return
		expression = parser.parseExpression("new %s()?.one()".formatted(TestClass5.class.getName()));

		assertCannotCompile(expression);

		TestClass5._i = 0;
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);

		TestClass5._i = 0;
		assertCanCompile(expression);
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);
	}

	@Test  // gh-27421
	public void nullSafeInvocationOfStaticVoidMethod() {
		// static method, no args, void return
		expression = parser.parseExpression("T(%s)?.two()".formatted(TestClass5.class.getName()));

		assertCannotCompile(expression);

		TestClass5._i = 0;
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);

		TestClass5._i = 0;
		assertCanCompile(expression);
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);
	}

	@Test  // gh-27421
	public void nullSafeInvocationOfNonStaticVoidWrapperMethod() {
		// non-static method, no args, Void return
		expression = parser.parseExpression("new %s()?.oneVoidWrapper()".formatted(TestClass5.class.getName()));

		assertCannotCompile(expression);

		TestClass5._i = 0;
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);

		TestClass5._i = 0;
		assertCanCompile(expression);
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);
	}

	@Test  // gh-27421
	public void nullSafeInvocationOfStaticVoidWrapperMethod() {
		// static method, no args, Void return
		expression = parser.parseExpression("T(%s)?.twoVoidWrapper()".formatted(TestClass5.class.getName()));

		assertCannotCompile(expression);

		TestClass5._i = 0;
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);

		TestClass5._i = 0;
		assertCanCompile(expression);
		assertThat(expression.getValue()).isNull();
		assertThat(TestClass5._i).isEqualTo(1);
	}

	@Test
	void methodReference() {
		TestClass5 tc = new TestClass5();

		// non-static method, no args, void return
		expression = parser.parseExpression("one()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.i).isEqualTo(1);
		tc.reset();

		// static method, no args, void return
		expression = parser.parseExpression("two()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(TestClass5._i).isEqualTo(1);
		tc.reset();

		// non-static method, reference type return
		expression = parser.parseExpression("three()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertThat(expression.getValue(tc)).isEqualTo("hello");
		tc.reset();

		// non-static method, primitive type return
		expression = parser.parseExpression("four()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertThat(expression.getValue(tc)).isEqualTo(3277700L);
		tc.reset();

		// static method, reference type return
		expression = parser.parseExpression("five()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertThat(expression.getValue(tc)).isEqualTo("hello");
		tc.reset();

		// static method, primitive type return
		expression = parser.parseExpression("six()");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		assertThat(expression.getValue(tc)).isEqualTo(3277700L);
		tc.reset();

		// non-static method, one parameter of reference type
		expression = parser.parseExpression("seven(\"foo\")");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("foo");
		tc.reset();

		// static method, one parameter of reference type
		expression = parser.parseExpression("eight(\"bar\")");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(TestClass5._s).isEqualTo("bar");
		tc.reset();

		// non-static method, one parameter of primitive type
		expression = parser.parseExpression("nine(231)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(tc.i).isEqualTo(231);
		tc.reset();

		// static method, one parameter of primitive type
		expression = parser.parseExpression("ten(111)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertCanCompile(expression);
		tc.reset();
		expression.getValue(tc);
		assertThat(TestClass5._i).isEqualTo(111);
		tc.reset();

		// method that gets type converted parameters

		// Converting from an int to a string
		expression = parser.parseExpression("seven(123)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("123");
		assertCannotCompile(expression); // Uncompilable as argument conversion is occurring

		Expression expression = parser.parseExpression("'abcd'.substring(index1,index2)");
		String resultI = expression.getValue(new TestClass1(), String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(), String.class);
		assertThat(resultI).isEqualTo("bc");
		assertThat(resultC).isEqualTo("bc");

		// Converting from an int to a Number
		expression = parser.parseExpression("takeNumber(123)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("123");
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("123");

		// Passing a subtype
		expression = parser.parseExpression("takeNumber(T(Integer).valueOf(42))");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("42");
		tc.reset();
		assertCanCompile(expression); // The generated code should include boxing of the int to a Number
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("42");

		// Passing a subtype
		expression = parser.parseExpression("takeString(T(Integer).valueOf(42))");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("42");
		tc.reset();
		assertCannotCompile(expression); // method takes a string and we are passing an Integer
	}

	@Test
	void errorHandling() {
		TestClass5 tc = new TestClass5();

		// changing target

		// from primitive array to reference type array
		int[] is = new int[] {1,2,3};
		String[] strings = new String[] {"a","b","c"};
		expression = parser.parseExpression("[1]");
		assertThat(expression.getValue(is)).isEqualTo(2);
		assertCanCompile(expression);
		assertThat(expression.getValue(is)).isEqualTo(2);
		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> expression.getValue(strings))
			.withCauseInstanceOf(ClassCastException.class);
		SpelCompiler.revertToInterpreted(expression);
		assertThat(expression.getValue(strings)).isEqualTo("b");
		assertCanCompile(expression);
		assertThat(expression.getValue(strings)).isEqualTo("b");


		tc.field = "foo";
		expression = parser.parseExpression("seven(field)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("foo");
		assertCanCompile(expression);
		tc.reset();
		tc.field="bar";
		expression.getValue(tc);

		// method with changing parameter types (change reference type)
		tc.obj = "foo";
		expression = parser.parseExpression("seven(obj)");
		assertCannotCompile(expression);
		expression.getValue(tc);
		assertThat(tc.s).isEqualTo("foo");
		assertCanCompile(expression);
		tc.reset();
		tc.obj=42;
		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> expression.getValue(tc))
			.withCauseInstanceOf(ClassCastException.class);


		// method with changing target
		expression = parser.parseExpression("#root.charAt(0)");
		assertThat(expression.getValue("abc")).isEqualTo('a');
		assertCanCompile(expression);
		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> expression.getValue(42))
			.withCauseInstanceOf(ClassCastException.class);
	}

	@Test
	void methodReference_staticMethod() {
		Expression expression = parser.parseExpression("T(Integer).valueOf(42)");
		int resultI = expression.getValue(new TestClass1(), int.class);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(), int.class);
		assertThat(resultI).isEqualTo(42);
		assertThat(resultC).isEqualTo(42);
	}

	@Test
	void methodReference_literalArguments_int() {
		Expression expression = parser.parseExpression("'abcd'.substring(1,3)");
		String resultI = expression.getValue(new TestClass1(), String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(), String.class);
		assertThat(resultI).isEqualTo("bc");
		assertThat(resultC).isEqualTo("bc");
	}

	@Test
	void methodReference_simpleInstanceMethodNoArg() {
		Expression expression = parser.parseExpression("toString()");
		String resultI = expression.getValue(42, String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(42, String.class);
		assertThat(resultI).isEqualTo("42");
		assertThat(resultC).isEqualTo("42");
	}

	@Test
	void methodReference_simpleInstanceMethodNoArgReturnPrimitive() {
		expression = parser.parseExpression("intValue()");
		int resultI = expression.getValue(42, int.class);
		assertThat(resultI).isEqualTo(42);
		assertCanCompile(expression);
		int resultC = expression.getValue(42, int.class);
		assertThat(resultC).isEqualTo(42);
	}

	@Test
	void methodReference_simpleInstanceMethodOneArgReturnPrimitive1() {
		Expression expression = parser.parseExpression("indexOf('b')");
		int resultI = expression.getValue("abc", int.class);
		assertCanCompile(expression);
		int resultC = expression.getValue("abc", int.class);
		assertThat(resultI).isEqualTo(1);
		assertThat(resultC).isEqualTo(1);
	}

	@Test
	void methodReference_simpleInstanceMethodOneArgReturnPrimitive2() {
		expression = parser.parseExpression("charAt(2)");
		char resultI = expression.getValue("abc", char.class);
		assertThat(resultI).isEqualTo('c');
		assertCanCompile(expression);
		char resultC = expression.getValue("abc", char.class);
		assertThat(resultC).isEqualTo('c');
	}

	@Test
	void compoundExpression() {
		Payload payload = new Payload();
		expression = parser.parseExpression("DR[0]");
		assertThat(expression.getValue(payload).toString()).isEqualTo("instanceof Two");
		assertCanCompile(expression);
		assertThat(expression.getValue(payload).toString()).isEqualTo("instanceof Two");
		ast = getAst();
		assertThat(ast.getExitDescriptor()).isEqualTo("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two");

		expression = parser.parseExpression("holder.three");
		assertThat(expression.getValue(payload).getClass().getName()).isEqualTo("org.springframework.expression.spel.SpelCompilationCoverageTests$Three");
		assertCanCompile(expression);
		assertThat(expression.getValue(payload).getClass().getName()).isEqualTo("org.springframework.expression.spel.SpelCompilationCoverageTests$Three");
		ast = getAst();
		assertThat(ast.getExitDescriptor()).isEqualTo("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three");

		expression = parser.parseExpression("DR[0]");
		assertThat(expression.getValue(payload).getClass().getName()).isEqualTo("org.springframework.expression.spel.SpelCompilationCoverageTests$Two");
		assertCanCompile(expression);
		assertThat(expression.getValue(payload).getClass().getName()).isEqualTo("org.springframework.expression.spel.SpelCompilationCoverageTests$Two");
		assertThat(getAst().getExitDescriptor()).isEqualTo("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Two");

		expression = parser.parseExpression("DR[0].three");
		assertThat(expression.getValue(payload).getClass().getName()).isEqualTo("org.springframework.expression.spel.SpelCompilationCoverageTests$Three");
		assertCanCompile(expression);
		assertThat(expression.getValue(payload).getClass().getName()).isEqualTo("org.springframework.expression.spel.SpelCompilationCoverageTests$Three");
		ast = getAst();
		assertThat(ast.getExitDescriptor()).isEqualTo("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three");

		expression = parser.parseExpression("DR[0].three.four");
		assertThat(expression.getValue(payload)).isEqualTo(0.04d);
		assertCanCompile(expression);
		assertThat(expression.getValue(payload)).isEqualTo(0.04d);
		assertThat(getAst().getExitDescriptor()).isEqualTo("D");
	}

	@Test
	void mixingItUp_indexerOpEqTernary() {
		Map<String, String> m = new HashMap<>();
		m.put("andy","778");

		expression = parse("['andy']==null?1:2");
		assertThat(expression.getValue(m)).isEqualTo(2);
		assertCanCompile(expression);
		assertThat(expression.getValue(m)).isEqualTo(2);
		m.remove("andy");
		assertThat(expression.getValue(m)).isEqualTo(1);
	}

	@Test
	void propertyReference() {
		TestClass6 tc = new TestClass6();

		// non-static field
		expression = parser.parseExpression("orange");
		assertCannotCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value1");
		assertCanCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value1");

		// static field
		expression = parser.parseExpression("apple");
		assertCannotCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value2");
		assertCanCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value2");

		// non static getter
		expression = parser.parseExpression("banana");
		assertCannotCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value3");
		assertCanCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value3");

		// static getter
		expression = parser.parseExpression("plum");
		assertCannotCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value4");
		assertCanCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value4");

		// record-style accessor
		expression = parser.parseExpression("strawberry");
		assertCannotCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value5");
		assertCanCompile(expression);
		assertThat(expression.getValue(tc)).isEqualTo("value5");
	}

	@Test
	void propertyReferenceVisibility_SPR12771() {
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("httpServletRequest", HttpServlet3RequestFactory.getOne());
		// Without a fix compilation was inserting a checkcast to a private type
		expression = parser.parseExpression("#httpServletRequest.servletPath");
		assertThat(expression.getValue(ctx)).isEqualTo("wibble");
		assertCanCompile(expression);
		assertThat(expression.getValue(ctx)).isEqualTo("wibble");
	}

	@Test
	void plusNeedingCheckcast_SPR12426() {
		expression = parser.parseExpression("object + ' world'");
		Object v = expression.getValue(new FooObject());
		assertThat(v).isEqualTo("hello world");
		assertCanCompile(expression);
		assertThat(v).isEqualTo("hello world");

		expression = parser.parseExpression("object + ' world'");
		v = expression.getValue(new FooString());
		assertThat(v).isEqualTo("hello world");
		assertCanCompile(expression);
		assertThat(v).isEqualTo("hello world");
	}

	@Test
	void mixingItUp_propertyAccessIndexerOpLtTernaryRootNull() {
		Payload payload = new Payload();

		expression = parser.parseExpression("DR[0].three");
		Object v = expression.getValue(payload);
		assertThat(getAst().getExitDescriptor()).isEqualTo("Lorg/springframework/expression/spel/SpelCompilationCoverageTests$Three");

		Expression expression = parser.parseExpression("DR[0].three.four lt 0.1d?#root:null");
		v = expression.getValue(payload);

		SpelExpression sExpr = (SpelExpression) expression;
		Ternary ternary = (Ternary) sExpr.getAST();
		OpLT oplt = (OpLT) ternary.getChild(0);
		CompoundExpression cExpr = (CompoundExpression) oplt.getLeftOperand();
		String cExprExitDescriptor = cExpr.getExitDescriptor();
		assertThat(cExprExitDescriptor).isEqualTo("D");
		assertThat(oplt.getExitDescriptor()).isEqualTo("Z");

		assertCanCompile(expression);
		Object vc = expression.getValue(payload);
		assertThat(v).isEqualTo(payload);
		assertThat(vc).isEqualTo(payload);
		payload.DR[0].three.four = 0.13d;
		vc = expression.getValue(payload);
		assertThat(vc).isNull();
	}

	@Test
	void variantGetter() {
		Payload2Holder holder = new Payload2Holder();
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.addPropertyAccessor(new MyPropertyAccessor());
		expression = parser.parseExpression("payload2.var1");
		Object v = expression.getValue(ctx,holder);
		assertThat(v).isEqualTo("abc");

		assertCanCompile(expression);
		v = expression.getValue(ctx,holder);
		assertThat(v).isEqualTo("abc");
	}

	@Test
	void compilerWithGenerics_12040() {
		expression = parser.parseExpression("payload!=2");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class)).isFalse();

		expression = parser.parseExpression("2!=payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class)).isFalse();

		expression = parser.parseExpression("payload!=6L");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4L), Boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6L), Boolean.class)).isFalse();

		expression = parser.parseExpression("payload==2");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class)).isTrue();

		expression = parser.parseExpression("2==payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class)).isTrue();

		expression = parser.parseExpression("payload==6L");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4L), Boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6L), Boolean.class)).isTrue();

		expression = parser.parseExpression("2==payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4), Boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(2), Boolean.class)).isTrue();

		expression = parser.parseExpression("payload/2");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(2);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6))).isEqualTo(3);

		expression = parser.parseExpression("100/payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(25);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(10))).isEqualTo(10);

		expression = parser.parseExpression("payload+2");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(6);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6))).isEqualTo(8);

		expression = parser.parseExpression("100+payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(104);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(10))).isEqualTo(110);

		expression = parser.parseExpression("payload-2");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(2);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6))).isEqualTo(4);

		expression = parser.parseExpression("100-payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(96);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(10))).isEqualTo(90);

		expression = parser.parseExpression("payload*2");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(8);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6))).isEqualTo(12);

		expression = parser.parseExpression("100*payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4))).isEqualTo(400);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(10))).isEqualTo(1000);

		expression = parser.parseExpression("payload/2L");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4L))).isEqualTo(2L);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6L))).isEqualTo(3L);

		expression = parser.parseExpression("100L/payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4L))).isEqualTo(25L);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(10L))).isEqualTo(10L);

		expression = parser.parseExpression("payload/2f");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4f))).isEqualTo(2f);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6f))).isEqualTo(3f);

		expression = parser.parseExpression("100f/payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4f))).isEqualTo(25f);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(10f))).isEqualTo(10f);

		expression = parser.parseExpression("payload/2d");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4d))).isEqualTo(2d);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(6d))).isEqualTo(3d);

		expression = parser.parseExpression("100d/payload");
		assertThat(expression.getValue(new GenericMessageTestHelper<>(4d))).isEqualTo(25d);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper<>(10d))).isEqualTo(10d);
	}

	// The new helper class here uses an upper bound on the generic
	@Test
	void compilerWithGenerics_12040_2() {
		expression = parser.parseExpression("payload/2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4))).isEqualTo(2);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6))).isEqualTo(3);

		expression = parser.parseExpression("9/payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(9))).isEqualTo(1);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(3))).isEqualTo(3);

		expression = parser.parseExpression("payload+2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4))).isEqualTo(6);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6))).isEqualTo(8);

		expression = parser.parseExpression("100+payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4))).isEqualTo(104);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(10))).isEqualTo(110);

		expression = parser.parseExpression("payload-2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4))).isEqualTo(2);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6))).isEqualTo(4);

		expression = parser.parseExpression("100-payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4))).isEqualTo(96);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(10))).isEqualTo(90);

		expression = parser.parseExpression("payload*2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4))).isEqualTo(8);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6))).isEqualTo(12);

		expression = parser.parseExpression("100*payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4))).isEqualTo(400);
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(10))).isEqualTo(1000);
	}

	// The other numeric operators
	@Test
	void compilerWithGenerics_12040_3() {
		expression = parser.parseExpression("payload >= 2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4), boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isFalse();

		expression = parser.parseExpression("2 >= payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(5), boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isTrue();

		expression = parser.parseExpression("payload > 2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(4), boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isFalse();

		expression = parser.parseExpression("2 > payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(5), boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isTrue();

		expression = parser.parseExpression("payload <=2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6), boolean.class)).isFalse();

		expression = parser.parseExpression("2 <= payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6), boolean.class)).isTrue();

		expression = parser.parseExpression("payload < 2");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isTrue();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6), boolean.class)).isFalse();

		expression = parser.parseExpression("2 < payload");
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(1), boolean.class)).isFalse();
		assertCanCompile(expression);
		assertThat(expression.getValue(new GenericMessageTestHelper2<>(6), boolean.class)).isTrue();
	}

	@Test
	void indexerMapAccessor_12045() {
		SpelParserConfiguration spc = new SpelParserConfiguration(
				SpelCompilerMode.IMMEDIATE,getClass().getClassLoader());
		SpelExpressionParser sep = new SpelExpressionParser(spc);
		expression=sep.parseExpression("headers[command]");
		MyMessage root = new MyMessage();
		assertThat(expression.getValue(root)).isEqualTo("wibble");
		// This next call was failing because the isCompilable check in Indexer
		// did not check on the key being compilable (and also generateCode in the
		// Indexer was missing the optimization that it didn't need necessarily
		// need to call generateCode for that accessor)
		assertThat(expression.getValue(root)).isEqualTo("wibble");
		assertCanCompile(expression);

		// What about a map key that is an expression - ensure the getKey() is evaluated in the right scope
		expression=sep.parseExpression("headers[getKey()]");
		assertThat(expression.getValue(root)).isEqualTo("wobble");
		assertThat(expression.getValue(root)).isEqualTo("wobble");

		expression=sep.parseExpression("list[getKey2()]");
		assertThat(expression.getValue(root)).isEqualTo("wobble");
		assertThat(expression.getValue(root)).isEqualTo("wobble");

		expression = sep.parseExpression("ia[getKey2()]");
		assertThat(expression.getValue(root)).isEqualTo(3);
		assertThat(expression.getValue(root)).isEqualTo(3);
	}

	@Test
	void elvisOperator_SPR15192() {
		SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		Expression exp;

		exp = new SpelExpressionParser(configuration).parseExpression("bar()");
		assertThat(exp.getValue(new Foo(), String.class)).isEqualTo("BAR");
		assertCanCompile(exp);
		assertThat(exp.getValue(new Foo(), String.class)).isEqualTo("BAR");
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("bar('baz')");
		assertThat(exp.getValue(new Foo(), String.class)).isEqualTo("BAZ");
		assertCanCompile(exp);
		assertThat(exp.getValue(new Foo(), String.class)).isEqualTo("BAZ");
		assertIsCompiled(exp);

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("map", Collections.singletonMap("foo", "qux"));

		exp = new SpelExpressionParser(configuration).parseExpression("bar(#map['foo'])");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("QUX");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("QUX");
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("bar(#map['foo'] ?: 'qux')");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("QUX");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("QUX");
		assertIsCompiled(exp);

		// When the condition is a primitive
		exp = new SpelExpressionParser(configuration).parseExpression("3?:'foo'");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertIsCompiled(exp);

		// When the condition is a double slot primitive
		exp = new SpelExpressionParser(configuration).parseExpression("3L?:'foo'");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertIsCompiled(exp);

		// When the condition is an empty string
		exp = new SpelExpressionParser(configuration).parseExpression("''?:4L");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("4");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("4");
		assertIsCompiled(exp);

		// null condition
		exp = new SpelExpressionParser(configuration).parseExpression("null?:4L");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("4");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("4");
		assertIsCompiled(exp);

		// variable access returning primitive
		exp = new SpelExpressionParser(configuration).parseExpression("#x?:'foo'");
		context.setVariable("x",50);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("50");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("50");
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("#x?:'foo'");
		context.setVariable("x",null);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertIsCompiled(exp);

		// variable access returning array
		exp = new SpelExpressionParser(configuration).parseExpression("#x?:'foo'");
		context.setVariable("x",new int[]{1,2,3});
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("1,2,3");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("1,2,3");
		assertIsCompiled(exp);
	}

	@Test
	void elvisOperator_SPR17214() {
		SpelParserConfiguration spc = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		SpelExpressionParser sep = new SpelExpressionParser(spc);

		RecordHolder rh = null;

		expression = sep.parseExpression("record.get('abc')?:record.put('abc',expression.someLong?.longValue())");
		rh = new RecordHolder();
		assertThat(expression.getValue(rh)).isNull();
		assertThat(expression.getValue(rh)).isEqualTo(3L);
		assertCanCompile(expression);
		rh = new RecordHolder();
		assertThat(expression.getValue(rh)).isNull();
		assertThat(expression.getValue(rh)).isEqualTo(3L);

		expression = sep.parseExpression("record.get('abc')?:record.put('abc',3L.longValue())");
		rh = new RecordHolder();
		assertThat(expression.getValue(rh)).isNull();
		assertThat(expression.getValue(rh)).isEqualTo(3L);
		assertCanCompile(expression);
		rh = new RecordHolder();
		assertThat(expression.getValue(rh)).isNull();
		assertThat(expression.getValue(rh)).isEqualTo(3L);

		expression = sep.parseExpression("record.get('abc')?:record.put('abc',3L.longValue())");
		rh = new RecordHolder();
		assertThat(expression.getValue(rh)).isNull();
		assertThat(expression.getValue(rh)).isEqualTo(3L);
		assertCanCompile(expression);
		rh = new RecordHolder();
		assertThat(expression.getValue(rh)).isNull();
		assertThat(expression.getValue(rh)).isEqualTo(3L);

		expression = sep.parseExpression("record.get('abc')==null?record.put('abc',expression.someLong?.longValue()):null");
		rh = new RecordHolder();
		rh.expression.someLong=6L;
		assertThat(expression.getValue(rh)).isNull();
		assertThat(rh.get("abc")).isEqualTo(6L);
		assertThat(expression.getValue(rh)).isNull();
		assertCanCompile(expression);
		rh = new RecordHolder();
		rh.expression.someLong=6L;
		assertThat(expression.getValue(rh)).isNull();
		assertThat(rh.get("abc")).isEqualTo(6L);
		assertThat(expression.getValue(rh)).isNull();
	}

	@Test
	void testNullComparison_SPR22358() {
		SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.OFF, null);
		SpelExpressionParser parser = new SpelExpressionParser(configuration);
		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setRootObject(new Reg(1));
		verifyCompilationAndBehaviourWithNull("value>1", parser, ctx );
		verifyCompilationAndBehaviourWithNull("value<1", parser, ctx );
		verifyCompilationAndBehaviourWithNull("value>=1", parser, ctx );
		verifyCompilationAndBehaviourWithNull("value<=1", parser, ctx );

		verifyCompilationAndBehaviourWithNull2("value>value2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("value<value2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("value>=value2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("value<=value2", parser, ctx );

		verifyCompilationAndBehaviourWithNull("valueD>1.0d", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueD<1.0d", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueD>=1.0d", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueD<=1.0d", parser, ctx );

		verifyCompilationAndBehaviourWithNull2("valueD>valueD2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("valueD<valueD2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("valueD>=valueD2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("valueD<=valueD2", parser, ctx );

		verifyCompilationAndBehaviourWithNull("valueL>1L", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueL<1L", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueL>=1L", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueL<=1L", parser, ctx );

		verifyCompilationAndBehaviourWithNull2("valueL>valueL2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("valueL<valueL2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("valueL>=valueL2", parser, ctx );
		verifyCompilationAndBehaviourWithNull2("valueL<=valueL2", parser, ctx );

		verifyCompilationAndBehaviourWithNull("valueF>1.0f", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueF<1.0f", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueF>=1.0f", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueF<=1.0f", parser, ctx );

		verifyCompilationAndBehaviourWithNull("valueF>valueF2", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueF<valueF2", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueF>=valueF2", parser, ctx );
		verifyCompilationAndBehaviourWithNull("valueF<=valueF2", parser, ctx );
	}

	private void verifyCompilationAndBehaviourWithNull(String expressionText, SpelExpressionParser parser, StandardEvaluationContext ctx) {
		Reg r = (Reg)ctx.getRootObject().getValue();
		r.setValue2(1);  // having a value in value2 fields will enable compilation to succeed, then can switch it to null
		SpelExpression fast = (SpelExpression) parser.parseExpression(expressionText);
		SpelExpression slow = (SpelExpression) parser.parseExpression(expressionText);
		fast.getValue(ctx);
		assertThat(fast.compileExpression()).isTrue();
		r.setValue2(null);
		// try the numbers 0,1,2,null
		for (int i = 0; i < 4; i++) {
			r.setValue(i < 3 ? i : null);
			boolean slowResult = (Boolean)slow.getValue(ctx);
			boolean fastResult = (Boolean)fast.getValue(ctx);
			assertThat(fastResult).as("Differing results: expression=" + expressionText +
						" value=" + r.getValue() + " slow=" + slowResult + " fast="+fastResult).isEqualTo(slowResult);
		}
	}

	private void verifyCompilationAndBehaviourWithNull2(String expressionText, SpelExpressionParser parser, StandardEvaluationContext ctx) {
		SpelExpression fast = (SpelExpression) parser.parseExpression(expressionText);
		SpelExpression slow = (SpelExpression) parser.parseExpression(expressionText);
		fast.getValue(ctx);
		assertThat(fast.compileExpression()).isTrue();
		Reg r = (Reg)ctx.getRootObject().getValue();
		// try the numbers 0,1,2,null
		for (int i = 0; i < 4; i++) {
			r.setValue(i < 3 ? i : null);
			boolean slowResult = (Boolean)slow.getValue(ctx);
			boolean fastResult = (Boolean)fast.getValue(ctx);
			assertThat(fastResult).as("Differing results: expression=" + expressionText +
					" value=" + r.getValue() + " slow=" + slowResult + " fast="+fastResult).isEqualTo(slowResult);
		}
	}

	@Test
	void ternaryOperator_SPR15192() {
		SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
		Expression exp;
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("map", Collections.singletonMap("foo", "qux"));

		exp = new SpelExpressionParser(configuration).parseExpression("bar(#map['foo'] != null ? #map['foo'] : 'qux')");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("QUX");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("QUX");
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("3==3?3:'foo'");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertIsCompiled(exp);
		exp = new SpelExpressionParser(configuration).parseExpression("3!=3?3:'foo'");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertIsCompiled(exp);

		// When the condition is a double slot primitive
		exp = new SpelExpressionParser(configuration).parseExpression("3==3?3L:'foo'");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("3");
		assertIsCompiled(exp);
		exp = new SpelExpressionParser(configuration).parseExpression("3!=3?3L:'foo'");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertIsCompiled(exp);

		// When the condition is an empty string
		exp = new SpelExpressionParser(configuration).parseExpression("''==''?'abc':4L");
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("abc");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("abc");
		assertIsCompiled(exp);

		// null condition
		exp = new SpelExpressionParser(configuration).parseExpression("3==3?null:4L");
		assertThat(exp.getValue(context, new Foo(), String.class)).isNull();
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isNull();
		assertIsCompiled(exp);

		// variable access returning primitive
		exp = new SpelExpressionParser(configuration).parseExpression("#x==#x?50:'foo'");
		context.setVariable("x",50);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("50");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("50");
		assertIsCompiled(exp);

		exp = new SpelExpressionParser(configuration).parseExpression("#x!=#x?50:'foo'");
		context.setVariable("x",null);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("foo");
		assertIsCompiled(exp);

		// variable access returning array
		exp = new SpelExpressionParser(configuration).parseExpression("#x==#x?'1,2,3':'foo'");
		context.setVariable("x",new int[]{1,2,3});
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("1,2,3");
		assertCanCompile(exp);
		assertThat(exp.getValue(context, new Foo(), String.class)).isEqualTo("1,2,3");
		assertIsCompiled(exp);
	}

	@Test
	void repeatedCompilation() throws Exception {
		// Verifying that after a number of compilations, the classloaders
		// used to load the compiled expressions are discarded/replaced.
		// See SpelCompiler.loadClass()
		Field f = SpelExpression.class.getDeclaredField("compiledAst");
		Set<Object> classloadersUsed = new HashSet<>();
		for (int i = 0; i < 1500; i++) {  // 1500 is greater than SpelCompiler.CLASSES_DEFINED_LIMIT
			expression = parser.parseExpression("4 + 5");
			assertThat((int) expression.getValue(Integer.class)).isEqualTo(9);
			assertCanCompile(expression);
			f.setAccessible(true);
			CompiledExpression cEx = (CompiledExpression) f.get(expression);
			classloadersUsed.add(cEx.getClass().getClassLoader());
			assertThat((int) expression.getValue(Integer.class)).isEqualTo(9);
		}
		assertThat(classloadersUsed.size()).isGreaterThan(1);
	}


	// Helper methods

	private SpelNodeImpl getAst() {
		SpelExpression spelExpression = (SpelExpression) expression;
		SpelNode ast = spelExpression.getAST();
		return (SpelNodeImpl)ast;
	}

	private void assertCanCompile(Expression expression) {
		assertThat(SpelCompiler.compile(expression))
				.as(() -> "Expression <%s> should be compilable"
						.formatted(((SpelExpression) expression).toStringAST()))
				.isTrue();
	}

	private void assertCannotCompile(Expression expression) {
		assertThat(SpelCompiler.compile(expression))
				.as(() -> "Expression <%s> should not be compilable"
						.formatted(((SpelExpression) expression).toStringAST()))
				.isFalse();
	}

	private Expression parse(String expression) {
		return parser.parseExpression(expression);
	}

	private static void assertNotPublic(Class<?> clazz) {
		assertThat(Modifier.isPublic(clazz.getModifiers())).as("%s must be private", clazz.getName()).isFalse();
	}


	// Nested types

	public interface Message<T> {

		MessageHeaders getHeaders();

		@SuppressWarnings("rawtypes")
		List getList();

		int[] getIa();
	}


	public static class MyMessage implements Message<String> {

		@Override
		public MessageHeaders getHeaders() {
			MessageHeaders mh = new MessageHeaders();
			mh.put("command", "wibble");
			mh.put("command2", "wobble");
			return mh;
		}

		@Override
		public int[] getIa() { return new int[] {5,3}; }

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public List getList() {
			List l = new ArrayList();
			l.add("wibble");
			l.add("wobble");
			return l;
		}

		public String getKey() {
			return "command2";
		}

		public int getKey2() {
			return 1;
		}
	}


	@SuppressWarnings("serial")
	public static class MessageHeaders extends HashMap<String, Object> {
	}


	public static class GenericMessageTestHelper<T> {

		private T payload;

		GenericMessageTestHelper(T value) {
			this.payload = value;
		}

		public T getPayload() {
			return payload;
		}
	}


	// This test helper has a bound on the type variable
	public static class GenericMessageTestHelper2<T extends Number> {

		private T payload;

		GenericMessageTestHelper2(T value) {
			this.payload = value;
		}

		public T getPayload() {
			return payload;
		}
	}


	static class MyPropertyAccessor implements CompilablePropertyAccessor {

		private Method method;

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Payload2.class};
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			// target is a Payload2 instance
			return true;
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			Payload2 payload2 = (Payload2)target;
			return new TypedValue(payload2.getField(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getPropertyType() {
			return Object.class;
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			if (method == null) {
				try {
					method = Payload2.class.getDeclaredMethod("getField", String.class);
				}
				catch (Exception ex) {
				}
			}
			String descriptor = cf.lastDescriptor();
			String memberDeclaringClassSlashedDescriptor = method.getDeclaringClass().getName().replace('.','/');
			if (descriptor == null) {
				cf.loadTarget(mv);
			}
			if (descriptor == null || !memberDeclaringClassSlashedDescriptor.equals(descriptor.substring(1))) {
				mv.visitTypeInsn(CHECKCAST, memberDeclaringClassSlashedDescriptor);
			}
			mv.visitLdcInsn(propertyName);
			mv.visitMethodInsn(INVOKEVIRTUAL, memberDeclaringClassSlashedDescriptor, method.getName(),
					CodeFlow.createSignatureDescriptor(method), false);
		}
	}


	public static class Greeter {

		public String getWorld() {
			return "world";
		}

		public Object getObject() {
			return "object";
		}
	}

	public static class FooObjectHolder {

		private FooObject foo = new FooObject();

		public FooObject getFoo() {
			return foo;
		}
	}

	public static class FooObject {

		static boolean doFooInvoked = false;

		public Object getObject() { return "hello"; }
		public void doFoo() { doFooInvoked = true; }
	}


	public static class FooString {

		public String getObject() { return "hello"; }
	}


	public static class Payload {

		Two[] DR = new Two[] {new Two()};

		public Two holder = new Two();

		public Two[] getDR() {
			return DR;
		}
	}


	public static class Payload2 {

		String var1 = "abc";
		String var2 = "def";

		public Object getField(String name) {
			if (name.equals("var1")) {
				return var1;
			}
			else if (name.equals("var2")) {
				return var2;
			}
			return null;
		}
	}


	public static class Payload2Holder {

		public Payload2 payload2 = new Payload2();
	}


	public static class Person3 {

		private int age;

		public Person3(String name, int age) {
			this.age = age;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}


	public static class Two {

		Three three = new Three();

		public Three getThree() {
			return three;
		}
		@Override
		public String toString() {
			return "instanceof Two";
		}
	}


	public static class Three {

		double four = 0.04d;

		public double getFour() {
			return four;
		}
	}


	public class PayloadX {

		public int valueI = 120;
		public Integer valueIB = 120;
		public Integer valueIB58 = 58;
		public Integer valueIB60 = 60;
		public long valueJ = 120L;
		public Long valueJB = 120L;
		public Long valueJB58 = 58L;
		public Long valueJB60 = 60L;
		public double valueD = 120D;
		public Double valueDB = 120D;
		public Double valueDB58 = 58D;
		public Double valueDB60 = 60D;
		public float valueF = 120F;
		public Float valueFB = 120F;
		public Float valueFB58 = 58F;
		public Float valueFB60 = 60F;
		public byte valueB = (byte)120;
		public byte valueB18 = (byte)18;
		public byte valueB20 = (byte)20;
		public Byte valueBB = (byte)120;
		public Byte valueBB18 = (byte)18;
		public Byte valueBB20 = (byte)20;
		public char valueC = (char)120;
		public Character valueCB = (char)120;
		public short valueS = (short)120;
		public short valueS18 = (short)18;
		public short valueS20 = (short)20;
		public Short valueSB = (short)120;
		public Short valueSB18 = (short)18;
		public Short valueSB20 = (short)20;

		public PayloadX payload;

		public PayloadX() {
			payload = this;
		}
	}


	public static class TestClass1 {

		public int index1 = 1;
		public int index2 = 3;
		public String word = "abcd";
	}


	public static class TestClass4 {

		public boolean a,b;
		public boolean gettrue() { return true; }
		public boolean getfalse() { return false; }
		public boolean getA() { return a; }
		public boolean getB() { return b; }
	}


	public static class TestClass10 {

		public String s = null;

		public void reset() {
			s = null;
		}

		public void concat(String arg) {
			s = "::"+arg;
		}

		public void concat(String... vargs) {
			if (vargs == null) {
				s = "";
			}
			else {
				s = "";
				for (String varg : vargs) {
					s += varg;
				}
			}
		}

		public void concat2(Object arg) {
			s = "::"+arg;
		}

		public void concat2(Object... vargs) {
			if (vargs == null) {
				s = "";
			}
			else {
				s = "";
				for (Object varg : vargs) {
					s += varg;
				}
			}
		}
	}


	public static class TestClass5 {

		public int i = 0;
		public String s = null;
		public static int _i = 0;
		public static String _s = null;

		public static short s1 = (short)1;
		public static short s2 = (short)2;
		public static short s3 = (short)3;

		public static long l1 = 1L;
		public static long l2 = 2L;
		public static long l3 = 3L;

		public static float f1 = 1f;
		public static float f2 = 2f;
		public static float f3 = 3f;

		public static char c1 = 'a';
		public static char c2 = 'b';
		public static char c3 = 'c';

		public static byte b1 = (byte)65;
		public static byte b2 = (byte)66;
		public static byte b3 = (byte)67;

		public static String[] stringArray = new String[] {"aaa","bbb","ccc"};
		public static int[] intArray = new int[] {11,22,33};

		public Object obj = null;

		public String field = null;

		public void reset() {
			i = 0;
			_i = 0;
			s = null;
			_s = null;
			field = null;
		}

		public void one() {
			_i = 1;
			this.i = 1;
		}

		public Void oneVoidWrapper() {
			_i = 1;
			this.i = 1;
			return null;
		}

		public static void two() { _i = 1; }

		public static Void twoVoidWrapper() {
			_i = 1;
			return null;
		}

		public String three() { return "hello"; }
		public long four() { return 3277700L; }

		public static String five() { return "hello"; }
		public static long six() { return 3277700L; }

		public void seven(String toset) { s = toset; }
		// public void seven(Number n) { s = n.toString(); }

		public void takeNumber(Number n) { s = n.toString(); }
		public void takeString(String s) { this.s = s; }
		public static void eight(String toset) { _s = toset; }

		public void nine(int toset) { i = toset; }
		public static void ten(int toset) { _i = toset; }

		public void eleven(String... vargs) {
			if (vargs == null) {
				s = "";
			}
			else {
				s = "";
				for (String varg: vargs) {
					s += varg;
				}
			}
		}

		public void twelve(int... vargs) {
			if (vargs == null) {
				i = 0;
			}
			else {
				i = 0;
				for (int varg: vargs) {
					i += varg;
				}
			}
		}

		public void thirteen(String a, String... vargs) {
			if (vargs == null) {
				s = a + "::";
			}
			else {
				s = a+"::";
				for (String varg: vargs) {
					s += varg;
				}
			}
		}

		public void arrayz(boolean... bs) {
			s = "";
			if (bs != null) {
				s = "";
				for (boolean b: bs) {
					s += Boolean.toString(b);
				}
			}
		}

		public void arrays(short... ss) {
			s = "";
			if (ss != null) {
				s = "";
				for (short s: ss) {
					this.s += Short.toString(s);
				}
			}
		}

		public void arrayd(double... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (double v: vargs) {
					this.s += Double.toString(v);
				}
			}
		}

		public void arrayf(float... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (float v: vargs) {
					this.s += Float.toString(v);
				}
			}
		}

		public void arrayj(long... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (long v: vargs) {
					this.s += Long.toString(v);
				}
			}
		}

		public void arrayb(byte... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (Byte v: vargs) {
					this.s += Byte.toString(v);
				}
			}
		}

		public void arrayc(char... vargs) {
			s = "";
			if (vargs != null) {
				s = "";
				for (char v: vargs) {
					this.s += Character.toString(v);
				}
			}
		}

		public void fourteen(String a, String[]... vargs) {
			if (vargs == null) {
				s = a+"::";
			}
			else {
				s = a+"::";
				for (String[] varg: vargs) {
					s += "{";
					for (String v: varg) {
						s += v;
					}
					s += "}";
				}
			}
		}

		public void fifteen(String a, int[]... vargs) {
			if (vargs == null) {
				s = a+"::";
			}
			else {
				s = a+"::";
				for (int[] varg: vargs) {
					s += "{";
					for (int v: varg) {
						s += Integer.toString(v);
					}
					s += "}";
				}
			}
		}

		public void sixteen(Object... vargs) {
			if (vargs == null) {
				s = "";
			}
			else {
				s = "";
				for (Object varg: vargs) {
					s += varg;
				}
			}
		}

		public String[] seventeen() {
			return new String[] { "aaa", "bbb", "ccc" };
		}

		public void eighteen(String a, Object... vargs) {
			if (vargs == null) {
				s = a + "::";
			}
			else {
				s = a + "::";
				for (Object varg: vargs) {
					s += varg;
				}
			}
		}
	}


	public static class TestClass6 {

		public static String apple = "value2";

		public String orange = "value1";
		public long peach = 34L;

		public String getBanana() {
			return "value3";
		}

		public String strawberry() {
			return "value5";
		}

		public static String getPlum() {
			return "value4";
		}
	}


	public static class TestClass7 {

		public static String property;

		static {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}

		public static void reset() {
			String s = "UK 123";
			StringTokenizer st = new StringTokenizer(s);
			property = st.nextToken();
		}
	}


	public static class TestClass8 {

		public int i;
		public String s;
		public double d;
		public boolean z;
		public Object[] args;

		public TestClass8(int i, String s, double d, boolean z) {
			this.i = i;
			this.s = s;
			this.d = d;
			this.z = z;
		}

		public TestClass8() {
		}

		public TestClass8(Integer i) {
			this.i = i;
		}

		public TestClass8(Object... args) {
			this.args = args;
		}

		@SuppressWarnings("unused")
		private TestClass8(String a, String b) {
			this.s = a+b;
		}
	}


	public static class Obj {

		private final String param1;

		public Obj(String param1){
			this.param1 = param1;
		}
	}


	public static class Obj2 {

		public final String output;

		public Obj2(String... params){
			StringBuilder b = new StringBuilder();
			for (String param: params) {
				b.append(param);
			}
			output = b.toString();
		}
	}


	public static class Obj3 {

		public final String output;

		public Obj3(int... params) {
			StringBuilder b = new StringBuilder();
			for (int param: params) {
				b.append(param);
			}
			output = b.toString();
		}

		public Obj3(String s, Float f, int... ints) {
			StringBuilder b = new StringBuilder();
			b.append(s);
			b.append(':');
			b.append(f);
			b.append(':');
			for (int param: ints) {
				b.append(param);
			}
			output = b.toString();
		}
	}


	public static class Obj4 {

		public final String output;

		public Obj4(int[] params) {
			StringBuilder b = new StringBuilder();
			for (int param: params) {
				b.append(param);
			}
			output = b.toString();
		}
	}


	@SuppressWarnings("unused")
	private static class TestClass9 {

		public TestClass9(int i) {
		}
	}


	// These test classes simulate a pattern of public/private classes seen in Spring Security

	// final class HttpServlet3RequestFactory implements HttpServletRequestFactory
	static class HttpServlet3RequestFactory {

		static Servlet3SecurityContextHolderAwareRequestWrapper getOne() {
			HttpServlet3RequestFactory outer = new HttpServlet3RequestFactory();
			return outer.new Servlet3SecurityContextHolderAwareRequestWrapper();
		}

		// private class Servlet3SecurityContextHolderAwareRequestWrapper extends SecurityContextHolderAwareRequestWrapper
		private class Servlet3SecurityContextHolderAwareRequestWrapper extends SecurityContextHolderAwareRequestWrapper {
		}
	}


	// public class SecurityContextHolderAwareRequestWrapper extends HttpServletRequestWrapper
	static class SecurityContextHolderAwareRequestWrapper extends HttpServletRequestWrapper {
	}


	public static class HttpServletRequestWrapper {

		public String getServletPath() {
			return "wibble";
		}
	}


	// Here the declaring class is not public
	static class SomeCompareMethod {

		// method not public
		static int compare(Object o1, Object o2) {
			return -1;
		}

		// public
		public static int compare2(Object o1, Object o2) {
			return -1;
		}
	}


	public static class SomeCompareMethod2 {

		public static int negate(int i1) {
			return -i1;
		}

		public static String append(String... strings) {
			StringBuilder b = new StringBuilder();
			for (String string : strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String append2(Object... objects) {
			StringBuilder b = new StringBuilder();
			for (Object object : objects) {
				b.append(object.toString());
			}
			return b.toString();
		}

		public static String append3(String[] strings) {
			StringBuilder b = new StringBuilder();
			for (String string : strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String append4(String s, String... strings) {
			StringBuilder b = new StringBuilder();
			b.append(s).append("::");
			for (String string : strings) {
				b.append(string);
			}
			return b.toString();
		}

		public static String appendChar(char... values) {
			StringBuilder b = new StringBuilder();
			for (char ch : values) {
				b.append(ch);
			}
			return b.toString();
		}

		public static int sum(int... ints) {
			int total = 0;
			for (int i : ints) {
				total += i;
			}
			return total;
		}

		public static int sumDouble(double... values) {
			int total = 0;
			for (double i : values) {
				total += i;
			}
			return total;
		}

		public static int sumFloat(float... values) {
			int total = 0;
			for (float i : values) {
				total += i;
			}
			return total;
		}
	}


	public static class DelegatingStringFormat {

		public static String format(String s, Object... args) {
			return String.format(s, args);
		}
	}


	public static class StaticsHelper {

		static StaticsHelper sh = new StaticsHelper();
		public static StaticsHelper fielda = sh;
		public static String fieldb = "fb";

		public static StaticsHelper methoda() {
			return sh;
		}
		public static String methodb() {
			return "mb";
		}

		public static StaticsHelper getPropertya() {
			return sh;
		}

		public static String getPropertyb() {
			return "pb";
		}

		@Override
		public String toString() {
			return "sh";
		}
	}


	public static class Apple implements Comparable<Apple> {

		public Object gotComparedTo = null;
		public int i;

		public Apple(int i) {
			this.i = i;
		}

		public void setValue(int i) {
			this.i = i;
		}

		@Override
		public int compareTo(Apple that) {
			this.gotComparedTo = that;
			return Integer.compare(this.i, that.i);
		}
	}


	// For opNe_SPR14863
	public static class MyContext {

		private final Map<String, String> data;

		public MyContext(Map<String, String> data) {
			this.data = data;
		}

		public Map<String, String> getData() {
			return data;
		}
	}


	public static class Foo {

		public String bar() {
			return "BAR";
		}

		public String bar(String arg) {
			return arg.toUpperCase();
		}
	}


	public static class RecordHolder {

		public Map<String,Long> record = new HashMap<>();

		public LongHolder expression = new LongHolder();

		public void add(String key, Long value) {
			record.put(key, value);
		}

		public long get(String key) {
			return record.get(key);
		}
	}


	public static class LongHolder {

		public Long someLong = 3L;
	}


	public class Reg {

		private Integer _value,_value2;
		private Long _valueL,_valueL2;
		private Double _valueD,_valueD2;
		private Float _valueF,_valueF2;

		public Reg(int v) {
			this._value = v;
			this._valueL = (long) v;
			this._valueD = (double) v;
			this._valueF = (float) v;
		}

		public Integer getValue() {
			return _value;
		}

		public Long getValueL() {
			return _valueL;
		}

		public Double getValueD() {
			return _valueD;
		}

		public Float getValueF() {
			return _valueF;
		}

		public Integer getValue2() {
			return _value2;
		}

		public Long getValueL2() {
			return _valueL2;
		}

		public Double getValueD2() {
			return _valueD2;
		}

		public Float getValueF2() {
			return _valueF2;
		}

		public void setValue(Integer value) {
			_value = value;
			_valueL = value==null?null:Long.valueOf(value);
			_valueD = value==null?null:Double.valueOf(value);
			_valueF = value==null?null:Float.valueOf(value);
		}

		public void setValue2(Integer value) {
			_value2 = value;
			_valueL2 = value==null?null:Long.valueOf(value);
			_valueD2 = value==null?null:Double.valueOf(value);
			_valueF2 = value==null?null:Float.valueOf(value);
		}
	}

	private interface PrivateInterface {

		String getMessage();

		String getIndex(int index);
	}

	private static class PrivateSubclass extends PublicSuperclass implements PublicInterface, PrivateInterface {

		@Override
		public int getNumber() {
			return 2;
		}

		@Override
		public String getText() {
			return "enigma";
		}

		@Override
		public String getMessage() {
			return "hello";
		}

		@Override
		public String getIndex2(int index) {
			return "sub-" + (2 * index);
		}

		@Override
		public String getFruit(int index) {
			return "fruit-" + index;
		}
	}

	// Must be public with public fields/properties.
	public static class RootContextWithIndexedProperties {
		public int[] intArray;
		public Number[] numberArray;
		public List<Integer> list;
		public Set<Integer> set;
		public String string;
		public Map<String, Integer> map;
		public Person person;
	}

	/**
	 * Type that can be indexed by an int or an Integer and whose indexed values
	 * are enums.
	 */
	public static class Colors {

		private final Map<Integer, Color> map = new HashMap<>();

		{
			this.map.put(1, Color.BLUE);
			this.map.put(2, Color.GREEN);
			this.map.put(3, Color.ORANGE);
			this.map.put(42, Color.PURPLE);
		}

		public Color get(int index) {
			if (!this.map.containsKey(index)) {
				throw new IndexOutOfBoundsException("No color for index " + index);
			}
			return this.map.get(index);
		}

		public void set(int index, Color color) {
			this.map.put(index, color);
		}
	}

	/**
	 * {@link CompilableIndexAccessor} that knows how to index into {@link Colors}.
	 */
	private static class ColorsIndexAccessor extends ReflectiveIndexAccessor {

		ColorsIndexAccessor() {
			super(Colors.class, int.class, "get", "set");
		}
	}

	/**
	 * Type that can be indexed by an enum and whose indexed values are primitive
	 * integers.
	 */
	public static class ColorOrdinals {

		public int get(Color color) {
			return color.ordinal();
		}
	}

	/**
	 * {@link CompilableIndexAccessor} that knows how to index into {@link ColorOrdinals}.
	 */
	private static class ColorOrdinalsIndexAccessor extends ReflectiveIndexAccessor {

		ColorOrdinalsIndexAccessor() {
			super(ColorOrdinals.class, Color.class, "get");
		}
	}

	/**
	 * Manually implemented {@link CompilableIndexAccessor} that knows how to
	 * index into {@link FruitMap} for reading, writing, and compilation.
	 */
	private static class FruitMapIndexAccessor implements CompilableIndexAccessor {

		private final Method method = ReflectionUtils.findMethod(FruitMap.class, "getFruit", Color.class);

		private final String targetTypeDesc = CodeFlow.toDescriptor(FruitMap.class);

		private final String classDesc = this.targetTypeDesc.substring(1);

		private final String methodDescr = CodeFlow.createSignatureDescriptor(this.method);


		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] { FruitMap.class };
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, Object index) {
			return (target instanceof FruitMap && index instanceof Color);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, Object index) {
			FruitMap fruitMap = (FruitMap) target;
			Color color = (Color) index;
			return new TypedValue(fruitMap.getFruit(color));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, Object index) {
			return canRead(context, target, index);
		}

		@Override
		public void write(EvaluationContext context, Object target, Object index, @Nullable Object newValue) {
			FruitMap fruitMap = (FruitMap) target;
			Color color = (Color) index;
			String fruit = String.valueOf(newValue);
			fruitMap.setFruit(color, fruit);
		}

		@Override
		public boolean isCompilable() {
			return true;
		}

		@Override
		public Class<?> getIndexedValueType() {
			return String.class;
		}

		@Override
		public void generateCode(SpelNode index, MethodVisitor mv, CodeFlow cf) {
			String lastDesc = cf.lastDescriptor();
			// Ensure the current object on the stack is the target type.
			if (lastDesc == null || !lastDesc.equals(this.targetTypeDesc)) {
				CodeFlow.insertCheckCast(mv, this.targetTypeDesc);
			}
			// Push the index onto the stack.
			cf.generateCodeForArgument(mv, index, Color.class);
			// Invoke the read-method.
			mv.visitMethodInsn(INVOKEVIRTUAL, this.classDesc, this.method.getName(), this.methodDescr, false);
		}
	}

}
