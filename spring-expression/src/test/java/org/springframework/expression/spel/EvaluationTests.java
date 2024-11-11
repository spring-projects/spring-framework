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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.expression.spel.testresources.TestPerson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.expression.spel.SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST;

/**
 * Tests the evaluation of real expressions in a real context.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
class EvaluationTests extends AbstractExpressionTests {

	@Nested
	class MiscellaneousTests {

		@Test
		void expressionLength() {
			String expression = "'X' + '%s'".formatted(" ".repeat(9_992));
			assertThat(expression).hasSize(10_000);
			Expression expr = parser.parseExpression(expression);
			String result = expr.getValue(context, String.class);
			assertThat(result).hasSize(9_993);
			assertThat(result.trim()).isEqualTo("X");

			expression = "'X' + '%s'".formatted(" ".repeat(9_993));
			assertThat(expression).hasSize(10_001);
			evaluateAndCheckError(expression, String.class, SpelMessage.MAX_EXPRESSION_LENGTH_EXCEEDED);
		}

		@Test
		void maxExpressionLengthIsConfigurable() {
			int maximumExpressionLength = 20_000;

			String expression = "'%s'".formatted("Y".repeat(19_998));
			assertThat(expression).hasSize(maximumExpressionLength);

			SpelParserConfiguration configuration =
					new SpelParserConfiguration(null, null, false, false, 0, maximumExpressionLength);
			ExpressionParser parser = new SpelExpressionParser(configuration);

			Expression expr = parser.parseExpression(expression);
			String result = expr.getValue(String.class);
			assertThat(result).hasSize(19_998);

			expression = "'%s'".formatted("Y".repeat(25_000));
			assertThat(expression).hasSize(25_002);
			evaluateAndCheckError(parser, expression, String.class, SpelMessage.MAX_EXPRESSION_LENGTH_EXCEEDED);
		}

		@Test
		void createListsOnAttemptToIndexNull01() throws EvaluationException, ParseException {
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e = parser.parseExpression("list[0]");
			TestClass testClass = new TestClass();

			Object o = e.getValue(new StandardEvaluationContext(testClass));
			assertThat(o).isEqualTo("");
			o = parser.parseExpression("list[3]").getValue(new StandardEvaluationContext(testClass));
			assertThat(o).isEqualTo("");
			assertThat(testClass.list).hasSize(4);

			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(() -> parser.parseExpression("list2[3]").getValue(new StandardEvaluationContext(testClass)));

			o = parser.parseExpression("foo[3]").getValue(new StandardEvaluationContext(testClass));
			assertThat(o).isEqualTo("");
			assertThat(testClass.getFoo()).hasSize(4);
		}

		@Test
		void createMapsOnAttemptToIndexNull() {
			TestClass testClass = new TestClass();
			StandardEvaluationContext ctx = new StandardEvaluationContext(testClass);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

			Object o = parser.parseExpression("map['a']").getValue(ctx);
			assertThat(o).isNull();
			o = parser.parseExpression("map").getValue(ctx);
			assertThat(o).isNotNull();

			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> parser.parseExpression("map2['a']").getValue(ctx));
			// map2 should be null, there is no setter
		}

		// wibble2 should be null (cannot be initialized dynamically), there is no setter
		@Test
		void createObjectsOnAttemptToReferenceNull() {
			TestClass testClass = new TestClass();
			StandardEvaluationContext ctx = new StandardEvaluationContext(testClass);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

			Object o = parser.parseExpression("wibble.bar").getValue(ctx);
			assertThat(o).isEqualTo("hello");
			o = parser.parseExpression("wibble").getValue(ctx);
			assertThat(o).isNotNull();

			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> parser.parseExpression("wibble2.bar").getValue(ctx));
		}

		@Test
		void elvisOperator() {
			evaluate("'Andy'?:'Dave'", "Andy", String.class);
			evaluate("null?:'Dave'", "Dave", String.class);
			evaluate("3?:1", 3, Integer.class);
			evaluate("(2*3)?:1*10", 6, Integer.class);
			evaluate("null?:2*10", 20, Integer.class);
			evaluate("(null?:1)*10", 10, Integer.class);
		}

		@Test
		void safeNavigation() {
			evaluate("null?.null?.null", null, null);
		}

		// mixing operators
		@Test
		void mixingOperators() {
			evaluate("true and 5>3", "true", Boolean.class);
		}

		// assignment
		@Test
		void assignmentToVariableWithStandardEvaluationContext() {
			evaluate("#var1 = 'value1'", "value1", String.class);
		}

		@ParameterizedTest
		@CsvSource(quoteCharacter = '"', delimiterString = "->", textBlock = """
				"#var1 = 'value1'"      -> #var1
				"true ? #myVar = 4 : 0" -> #myVar
				""")
		void assignmentToVariableWithSimpleEvaluationContext(String expression, String varName) {
			EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
			Expression expr = parser.parseExpression(expression);
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> expr.getValue(context))
				.satisfies(ex -> {
					assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED);
					assertThat(ex.getInserts()).as("inserts").containsExactly(varName);
				});
		}

		@Test
		void operatorVariants() {
			SpelExpression e = (SpelExpression) parser.parseExpression("#a < #b");
			EvaluationContext ctx = new StandardEvaluationContext();
			ctx.setVariable("a", (short) 3);
			ctx.setVariable("b", (short) 6);
			assertThat(e.getValue(ctx, Boolean.class)).isTrue();
			ctx.setVariable("b", (byte) 6);
			assertThat(e.getValue(ctx, Boolean.class)).isTrue();
			ctx.setVariable("a", (byte) 9);
			ctx.setVariable("b", (byte) 6);
			assertThat(e.getValue(ctx, Boolean.class)).isFalse();
			ctx.setVariable("a", 10L);
			ctx.setVariable("b", (short) 30);
			assertThat(e.getValue(ctx, Boolean.class)).isTrue();
			ctx.setVariable("a", (byte) 3);
			ctx.setVariable("b", (short) 30);
			assertThat(e.getValue(ctx, Boolean.class)).isTrue();
			ctx.setVariable("a", (byte) 3);
			ctx.setVariable("b", 30L);
			assertThat(e.getValue(ctx, Boolean.class)).isTrue();
			ctx.setVariable("a", (byte) 3);
			ctx.setVariable("b", 30f);
			assertThat(e.getValue(ctx, Boolean.class)).isTrue();
			ctx.setVariable("a", new BigInteger("10"));
			ctx.setVariable("b", new BigInteger("20"));
			assertThat(e.getValue(ctx, Boolean.class)).isTrue();
		}

		@Test
		void indexer03() {
			evaluate("'christian'[8]", "n", String.class);
		}

		@Test
		void indexerError() {
			evaluateAndCheckError("new org.springframework.expression.spel.testresources.Inventor().inventions[1]",
					SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
		}

		@Test
		void stringType() {
			evaluateAndAskForReturnType("getPlaceOfBirth().getCity()", "Smiljan", String.class);
		}

		@Test
		void numbers() {
			evaluateAndAskForReturnType("3*4+5", 17, Integer.class);
			evaluateAndAskForReturnType("3*4+5", 17L, Long.class);
			evaluateAndAskForReturnType("65", 'A', Character.class);
			evaluateAndAskForReturnType("3*4+5", (short) 17, Short.class);
			evaluateAndAskForReturnType("3*4+5", "17", String.class);
		}

		@Test
		void advancedNumerics() {
			int twentyFour = parser.parseExpression("2.0 * 3e0 * 4").getValue(Integer.class);
			assertThat(twentyFour).isEqualTo(24);
			double one = parser.parseExpression("8.0 / 5e0 % 2").getValue(Double.class);
			assertThat((float) one).isCloseTo((float) 1.6d, within((float) 0d));
			int o = parser.parseExpression("8.0 / 5e0 % 2").getValue(Integer.class);
			assertThat(o).isEqualTo(1);
			int sixteen = parser.parseExpression("-2 ^ 4").getValue(Integer.class);
			assertThat(sixteen).isEqualTo(16);
			int minusFortyFive = parser.parseExpression("1+2-3*8^2/2/2").getValue(Integer.class);
			assertThat(minusFortyFive).isEqualTo(-45);
		}

		@Test
		void comparison() {
			EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
			boolean trueValue = parser.parseExpression("T(java.util.Date) == Birthdate.Class").getValue(
					context, Boolean.class);
			assertThat(trueValue).isTrue();
		}

		@Test
		void resolvingList() {
			StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(() -> parser.parseExpression("T(List)!=null").getValue(context, Boolean.class));
			((StandardTypeLocator) context.getTypeLocator()).registerImport("java.util");
			assertThat(parser.parseExpression("T(List)!=null").getValue(context, Boolean.class)).isTrue();
		}

		@Test
		void resolvingString() {
			Class<?> stringClass = parser.parseExpression("T(String)").getValue(Class.class);
			assertThat(stringClass).isEqualTo(String.class);
		}

		/**
		 * SPR-6984: attempting to index a collection on write using an index that
		 * doesn't currently exist in the collection (address.crossStreets[0] below)
		 */
		@Test
		void initializingCollectionElementsOnWrite() {
			TestPerson person = new TestPerson();
			EvaluationContext context = new StandardEvaluationContext(person);
			SpelParserConfiguration config = new SpelParserConfiguration(true, true);
			ExpressionParser parser = new SpelExpressionParser(config);
			Expression e = parser.parseExpression("name");
			e.setValue(context, "Oleg");
			assertThat(person.getName()).isEqualTo("Oleg");

			e = parser.parseExpression("address.street");
			e.setValue(context, "123 High St");
			assertThat(person.getAddress().getStreet()).isEqualTo("123 High St");

			e = parser.parseExpression("address.crossStreets[0]");
			e.setValue(context, "Blah");
			assertThat(person.getAddress().getCrossStreets()).element(0).isEqualTo("Blah");

			e = parser.parseExpression("address.crossStreets[3]");
			e.setValue(context, "Wibble");
			assertThat(person.getAddress().getCrossStreets()).element(0).isEqualTo("Blah");
			assertThat(person.getAddress().getCrossStreets()).element(3).isEqualTo("Wibble");
		}

		/**
		 * Verifies behavior requested in SPR-9613.
		 */
		@Test
		void caseInsensitiveNullLiterals() {
			ExpressionParser parser = new SpelExpressionParser();

			Expression e = parser.parseExpression("null");
			assertThat(e.getValue()).isNull();

			e = parser.parseExpression("NULL");
			assertThat(e.getValue()).isNull();

			e = parser.parseExpression("NuLl");
			assertThat(e.getValue()).isNull();
		}

		/**
		 * Verifies behavior requested in SPR-9621.
		 */
		@Test
		void customMethodFilter() {
			StandardEvaluationContext context = new StandardEvaluationContext();

			// Register a custom MethodResolver...
			context.setMethodResolvers(List.of((evaluationContext, targetObject, name, argumentTypes) -> null));

			// or simply...
			// context.setMethodResolvers(new ArrayList<MethodResolver>());

			// Register a custom MethodFilter...
			MethodFilter methodFilter = methods -> null;
			assertThatIllegalStateException()
				.isThrownBy(() -> context.registerMethodFilter(String.class, methodFilter))
				.withMessage("Method filter cannot be set as the reflective method resolver is not in use");
		}

		/**
		 * This test is checking that with the changes for 9751 that the refactoring in Indexer is
		 * coping correctly for references beyond collection boundaries.
		 */
		@Test
		void collectionGrowingViaIndexer() {
			Spr9751 instance = new Spr9751();

			// Add a new element to the list
			StandardEvaluationContext ctx = new StandardEvaluationContext(instance);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e = parser.parseExpression("listOfStrings[++index3]='def'");
			e.getValue(ctx);
			assertThat(instance.listOfStrings).hasSize(2);
			assertThat(instance.listOfStrings).element(1).isEqualTo("def");

			// Check reference beyond end of collection
			ctx = new StandardEvaluationContext(instance);
			parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			e = parser.parseExpression("listOfStrings[0]");
			String value = e.getValue(ctx, String.class);
			assertThat(value).isEqualTo("abc");
			e = parser.parseExpression("listOfStrings[1]");
			value = e.getValue(ctx, String.class);
			assertThat(value).isEqualTo("def");
			e = parser.parseExpression("listOfStrings[2]");
			value = e.getValue(ctx, String.class);
			assertThat(value).isEmpty();

			// Now turn off growing and reference off the end
			StandardEvaluationContext failCtx = new StandardEvaluationContext(instance);
			parser = new SpelExpressionParser(new SpelParserConfiguration(false, false));
			Expression failExp = parser.parseExpression("listOfStrings[3]");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> failExp.getValue(failCtx, String.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS));
		}

		@Test
		void limitCollectionGrowing() {
			TestClass instance = new TestClass();
			StandardEvaluationContext ctx = new StandardEvaluationContext(instance);
			SpelExpressionParser parser = new SpelExpressionParser( new SpelParserConfiguration(true, true, 3));
			Expression e = parser.parseExpression("foo[2]");
			e.setValue(ctx, "2");
			assertThat(instance.getFoo()).hasSize(3);
			e = parser.parseExpression("foo[3]");
			try {
				e.setValue(ctx, "3");
			}
			catch (SpelEvaluationException see) {
				assertThat(see.getMessageCode()).isEqualTo(SpelMessage.UNABLE_TO_GROW_COLLECTION);
				assertThat(instance.getFoo()).hasSize(3);
			}
		}

	}

	@Nested
	class StringLiterals {

		@Test
		void insideSingleQuotes() {
			evaluate("'hello'", "hello", String.class);
			evaluate("'hello world'", "hello world", String.class);
		}

		@Test
		void insideDoubleQuotes() {
			evaluate("\"hello\"", "hello", String.class);
			evaluate("\"hello world\"", "hello world", String.class);
		}

		@Test
		void singleQuotesInsideSingleQuotes() {
			evaluate("'Tony''s Pizza'", "Tony's Pizza", String.class);
			evaluate("'big ''''pizza'''' parlor'", "big ''pizza'' parlor", String.class);
		}

		@Test
		void doubleQuotesInsideDoubleQuotes() {
			evaluate("\"big \"\"pizza\"\" parlor\"", "big \"pizza\" parlor", String.class);
			evaluate("\"big \"\"\"\"pizza\"\"\"\" parlor\"", "big \"\"pizza\"\" parlor", String.class);
		}

		@Test
		void singleQuotesInsideDoubleQuotes() {
			evaluate("\"Tony's Pizza\"", "Tony's Pizza", String.class);
			evaluate("\"big ''pizza'' parlor\"", "big ''pizza'' parlor", String.class);
		}

		@Test
		void doubleQuotesInsideSingleQuotes() {
			evaluate("'big \"pizza\" parlor'", "big \"pizza\" parlor", String.class);
			evaluate("'two double \"\" quotes'", "two double \"\" quotes", String.class);
		}

		@Test
		void inCompoundExpressions() {
			evaluate("'123''4' == '123''4'", true, Boolean.class);
			evaluate("""
				"123""4" == "123""4"\
				""", true, Boolean.class);
		}

	}

	@Nested
	class RelationalOperatorTests {

		@Test
		void relOperatorGT() {
			evaluate("3 > 6", "false", Boolean.class);
		}

		@Test
		void relOperatorLT() {
			evaluate("3 < 6", "true", Boolean.class);
		}

		@Test
		void relOperatorLE() {
			evaluate("3 <= 6", "true", Boolean.class);
		}

		@Test
		void relOperatorGE01() {
			evaluate("3 >= 6", "false", Boolean.class);
		}

		@Test
		void relOperatorGE02() {
			evaluate("3 >= 3", "true", Boolean.class);
		}

		@Test
		void relOperatorsInstanceof01() {
			evaluate("'xyz' instanceof T(int)", "false", Boolean.class);
		}

		@Test
		void relOperatorsInstanceof04() {
			evaluate("null instanceof T(String)", "false", Boolean.class);
		}

		@Test
		void relOperatorsInstanceof05() {
			evaluate("null instanceof T(Integer)", "false", Boolean.class);
		}

		@Test
		void relOperatorsInstanceof06() {
			evaluateAndCheckError("'A' instanceof null", SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND, 15, "null");
		}

		@Test
		void matchesTrue() {
			evaluate("'5.00' matches '^-?\\d+(\\.\\d{2})?$'", "true", Boolean.class);
		}

		@Test
		void matchesFalse() {
			evaluate("'5.0067' matches '^-?\\d+(\\.\\d{2})?$'", "false", Boolean.class);
		}

		@Test
		void matchesWithInputConversion() {
			evaluate("27 matches '^.*2.*$'", true, Boolean.class);  // conversion int --> string
		}

		@Test
		void matchesWithNullInput() {
			evaluateAndCheckError("null matches '^.*$'", SpelMessage.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, 0, null);
		}

		@Test
		void matchesWithNullPattern() {
			evaluateAndCheckError("'abc' matches null", SpelMessage.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, 14, null);
		}

		@Test  // SPR-16731
		void matchesWithPatternAccessThreshold() {
			String pattern = "^(?=[a-z0-9-]{1,47})([a-z0-9]+[-]{0,1}){1,47}[a-z0-9]{1}$";
			String expression = "'abcde-fghijklmn-o42pasdfasdfasdf.qrstuvwxyz10x.xx.yyy.zasdfasfd' matches '" + pattern + "'";
			evaluateAndCheckError(expression, SpelMessage.FLAWED_PATTERN);
		}

		@Test
		void matchesWithPatternLengthThreshold() {
			String pattern = "^(%s|X)".formatted("12345".repeat(199));
			assertThat(pattern).hasSize(1000);
			Expression expr = parser.parseExpression("'X' matches '" + pattern + "'");
			assertThat(expr.getValue(context, Boolean.class)).isTrue();

			pattern += "?";
			assertThat(pattern).hasSize(1001);
			evaluateAndCheckError("'X' matches '" + pattern + "'", Boolean.class, SpelMessage.MAX_REGEX_LENGTH_EXCEEDED);
		}

		@Test
		void betweenOperator() {
			evaluate("1 between listOneFive", "true", Boolean.class);
			evaluate("1 between {1, 5}", "true", Boolean.class);
		}

		@Test
		void betweenOperatorErrors() {
			evaluateAndCheckError("1 between T(String)", BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST, 10);
			evaluateAndCheckError("1 between listOfNumbersUpToTen", BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST, 10);
		}

	}

	@Nested
	class PropertyAccessTests {

		@Test
		void propertyField() {
			evaluate("name", "Nikola Tesla", String.class, true);
			// not writable because (1) name is private (2) there is no setter, only a getter
			evaluateAndCheckError("madeup", SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, 0, "madeup",
					"org.springframework.expression.spel.testresources.Inventor");
		}

		@Test
		void propertyField_SPR7100() {
			evaluate("_name", "Nikola Tesla", String.class);
			evaluate("_name_", "Nikola Tesla", String.class);
		}

		@Test
		void rogueTrailingDotCausesNPE_SPR6866() {
			assertThatExceptionOfType(SpelParseException.class)
				.isThrownBy(() -> new SpelExpressionParser().parseExpression("placeOfBirth.foo."))
				.satisfies(ex -> {
					assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OOD);
					assertThat(ex.getPosition()).isEqualTo(16);
				});
		}

		@Nested
		class NestedPropertiesTests {

			// nested properties
			@Test
			void propertiesNested01() {
				evaluate("placeOfBirth.city", "Smiljan", String.class, true);
			}

			@Test
			void propertiesNested02() {
				evaluate("placeOfBirth.doubleIt(12)", "24", Integer.class);
			}

			@Test
			void propertiesNested03() throws ParseException {
				assertThatExceptionOfType(SpelParseException.class)
					.isThrownBy(() -> new SpelExpressionParser().parseRaw("placeOfBirth.23"))
					.satisfies(ex -> {
						assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.UNEXPECTED_DATA_AFTER_DOT);
						assertThat(ex.getInserts()[0]).isEqualTo("23");
					});
			}

		}

	}

	@Nested
	class MethodAndConstructorTests {

		@Test
		void methods01() {
			evaluate("echo(12)", "12", String.class);
		}

		@Test
		void methods02() {
			evaluate("echo(name)", "Nikola Tesla", String.class);
		}

		@Test
		void constructorInvocation01() {
			evaluate("new String('hello')", "hello", String.class);
		}

		@Test
		void constructorInvocation05() {
			evaluate("new java.lang.String('foobar')", "foobar", String.class);
		}

		@Test
		void constructorInvocation06() {
			// repeated evaluation to drive use of cached executor
			SpelExpression e = (SpelExpression) parser.parseExpression("new String('wibble')");
			String newString = e.getValue(String.class);
			assertThat(newString).isEqualTo("wibble");
			newString = e.getValue(String.class);
			assertThat(newString).isEqualTo("wibble");

			// not writable
			assertThat(e.isWritable(new StandardEvaluationContext())).isFalse();

			// ast
			assertThat(e.toStringAST()).isEqualTo("new String('wibble')");
		}

	}

	@Nested
	class UnaryOperatorTests {

		@Test
		void unaryMinus() {
			evaluate("-5", "-5", Integer.class);
		}

		@Test
		void unaryPlus() {
			evaluate("+5", "5", Integer.class);
		}

		@Test
		void unaryNot01() {
			evaluate("!true", "false", Boolean.class);
		}

		@Test
		void unaryNot02() {
			evaluate("!false", "true", Boolean.class);
		}

		@Test
		void unaryNotWithNullValue() {
			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(parser.parseExpression("!null")::getValue);
		}

	}

	@Nested
	class BinaryOperatorTests {

		@Test
		void andWithNullValueOnLeft() {
			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(parser.parseExpression("null and true")::getValue);
		}

		@Test
		void andWithNullValueOnRight() {
			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(parser.parseExpression("true and null")::getValue);
		}

		@Test
		void orWithNullValueOnLeft() {
			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(parser.parseExpression("null or false")::getValue);
		}

		@Test
		void orWithNullValueOnRight() {
			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(parser.parseExpression("false or null")::getValue);
		}

	}

	@Nested
	class TernaryOperatorTests {

		@Test
		void ternaryOperator01() {
			evaluate("2>4?1:2", 2, Integer.class);
		}

		@Test
		void ternaryOperator02() {
			evaluate("'abc'=='abc'?1:2", 1, Integer.class);
		}

		@Test
		void ternaryOperator03() {
			// cannot convert String to boolean
			evaluateAndCheckError("'hello'?1:2", SpelMessage.TYPE_CONVERSION_ERROR);
		}

		@Test
		void ternaryOperator04() {
			Expression e = parser.parseExpression("1>2?3:4");
			assertThat(e.isWritable(context)).isFalse();
		}

		@Test
		void ternaryOperator05() {
			evaluate("1>2?#var=4:#var=5", 5, Integer.class);
			evaluate("3?:#var=5", 3, Integer.class);
			evaluate("null?:#var=5", 5, Integer.class);
			evaluate("2>4?(3>2?true:false):(5<3?true:false)", false, Boolean.class);
		}

		@Test
		void ternaryOperator06() {
			evaluate("3?:#var=5", 3, Integer.class);
			evaluate("null?:#var=5", 5, Integer.class);
			evaluate("2>4?(3>2?true:false):(5<3?true:false)", false, Boolean.class);
		}

		@Test
		void ternaryExpressionWithImplicitGrouping() {
			evaluate("4 % 2 == 0 ? 2 : 3 * 10", 2, Integer.class);
			evaluate("4 % 2 == 1 ? 2 : 3 * 10", 30, Integer.class);
		}

		@Test
		void ternaryExpressionWithExplicitGrouping() {
			evaluate("((4 % 2 == 0) ? 2 : 1) * 10", 20, Integer.class);
		}

		@Test
		void ternaryOperatorWithNullValue() {
			assertThatExceptionOfType(EvaluationException.class)
				.isThrownBy(parser.parseExpression("null ? 0 : 1")::getValue);
		}

	}

	@Nested
	class MethodConstructorAndFunctionInvocationTests {

		@Test
		void methodCallWithRootReferenceThroughParameter() {
			evaluate("placeOfBirth.doubleIt(inventions.length)", 18, Integer.class);
		}

		@Test
		void ctorCallWithRootReferenceThroughParameter() {
			evaluate("new org.springframework.expression.spel.testresources.PlaceOfBirth(inventions[0].toString()).city",
					"Telephone repeater", String.class);
		}

		@Test
		void fnCallWithRootReferenceThroughParameter() {
			evaluate("#reverseInt(inventions.length, inventions.length, inventions.length)", "int[3]{9,9,9}", int[].class);
		}

		@Test
		void methodCallWithRootReferenceThroughParameterThatIsAFunctionCall() {
			evaluate("placeOfBirth.doubleIt(#reverseInt(inventions.length,2,3)[2])", 18, Integer.class);
		}

	}

	@Nested
	class VariableAndFunctionAccessTests {

		@Test
		void variableAccess() {
			evaluate("#answer", "42", Integer.class, true);
		}

		@Test
		void functionAccess() {
			evaluate("#reverseInt(1,2,3)", "int[3]{3,2,1}", int[].class);
			evaluate("#reverseString('hello')", "olleh", String.class);
		}

	}

	@Nested
	class TypeReferenceTests {

		@Test
		void typeReferences() {
			evaluate("T(java.lang.String)", "class java.lang.String", Class.class);
			evaluate("T(String)", "class java.lang.String", Class.class);
		}

		@Test
		void typeReferencesAndQualifiedIdentifierCaching() {
			SpelExpression e = (SpelExpression) parser.parseExpression("T(java.lang.String)");
			assertThat(e.isWritable(new StandardEvaluationContext())).isFalse();
			assertThat(e.toStringAST()).isEqualTo("T(java.lang.String)");
			assertThat(e.getValue(Class.class)).isEqualTo(String.class);
			// use cached QualifiedIdentifier:
			assertThat(e.toStringAST()).isEqualTo("T(java.lang.String)");
			assertThat(e.getValue(Class.class)).isEqualTo(String.class);
		}

		@Test
		void typeReferencesPrimitive() {
			evaluate("T(int)", "int", Class.class);
			evaluate("T(byte)", "byte", Class.class);
			evaluate("T(char)", "char", Class.class);
			evaluate("T(boolean)", "boolean", Class.class);
			evaluate("T(long)", "long", Class.class);
			evaluate("T(short)", "short", Class.class);
			evaluate("T(double)", "double", Class.class);
			evaluate("T(float)", "float", Class.class);
		}

		@Test
		void staticMethodReferences() {
			evaluate("T(java.awt.Color).green.getRGB() != 0", true, Boolean.class);
			evaluate("(T(java.lang.Math).random() * 100.0 ) > 0", true, Boolean.class);
			evaluate("(T(Math).random() * 100.0) > 0", true, Boolean.class);
			evaluate("T(Character).isUpperCase('Test'.charAt(0)) ? 'uppercase' : 'lowercase'", "uppercase", String.class);
			evaluate("T(Character).isUpperCase('Test'.charAt(1)) ? 'uppercase' : 'lowercase'", "lowercase", String.class);
		}

	}

	@Nested
	class IncrementAndDecrementTests {

		// For now I am making #this not assignable
		@Test
		void increment01root() {
			Integer i = 42;
			StandardEvaluationContext ctx = new StandardEvaluationContext(i);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e = parser.parseExpression("#this++");
			assertThat(i).isEqualTo(42);
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e.getValue(ctx, Integer.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.NOT_ASSIGNABLE));
		}

		@Test
		void increment02postfix() {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e;

			// BigDecimal
			e = parser.parseExpression("bd++");
			assertThat(new BigDecimal("2").equals(helper.bd)).isTrue();
			BigDecimal return_bd = e.getValue(ctx, BigDecimal.class);
			assertThat(new BigDecimal("2")).isEqualTo(return_bd);
			assertThat(new BigDecimal("3").equals(helper.bd)).isTrue();

			// double
			e = parser.parseExpression("ddd++");
			assertThat((float) helper.ddd).isCloseTo((float) 2.0d, within((float) 0d));
			double return_ddd = e.getValue(ctx, double.class);
			assertThat((float) return_ddd).isCloseTo((float) 2.0d, within((float) 0d));
			assertThat((float) helper.ddd).isCloseTo((float) 3.0d, within((float) 0d));

			// float
			e = parser.parseExpression("fff++");
			assertThat(helper.fff).isCloseTo(3.0f, within((float) 0d));
			float return_fff = e.getValue(ctx, float.class);
			assertThat(return_fff).isCloseTo(3.0f, within((float) 0d));
			assertThat(helper.fff).isCloseTo(4.0f, within((float) 0d));

			// long
			e = parser.parseExpression("lll++");
			assertThat(helper.lll).isEqualTo(66666L);
			long return_lll = e.getValue(ctx, long.class);
			assertThat(return_lll).isEqualTo(66666L);
			assertThat(helper.lll).isEqualTo(66667L);

			// int
			e = parser.parseExpression("iii++");
			assertThat(helper.iii).isEqualTo(42);
			int return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(42);
			assertThat(helper.iii).isEqualTo(43);
			return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(43);
			assertThat(helper.iii).isEqualTo(44);

			// short
			e = parser.parseExpression("sss++");
			assertThat(helper.sss).isEqualTo((short) 15);
			short return_sss = e.getValue(ctx, short.class);
			assertThat(return_sss).isEqualTo((short) 15);
			assertThat(helper.sss).isEqualTo((short) 16);
		}

		@Test
		void increment02prefix() {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e;

			// BigDecimal
			e = parser.parseExpression("++bd");
			assertThat(new BigDecimal("2").equals(helper.bd)).isTrue();
			BigDecimal return_bd = e.getValue(ctx, BigDecimal.class);
			assertThat(new BigDecimal("3")).isEqualTo(return_bd);
			assertThat(new BigDecimal("3").equals(helper.bd)).isTrue();

			// double
			e = parser.parseExpression("++ddd");
			assertThat((float) helper.ddd).isCloseTo((float) 2.0d, within((float) 0d));
			double return_ddd = e.getValue(ctx, double.class);
			assertThat((float) return_ddd).isCloseTo((float) 3.0d, within((float) 0d));
			assertThat((float) helper.ddd).isCloseTo((float) 3.0d, within((float) 0d));

			// float
			e = parser.parseExpression("++fff");
			assertThat(helper.fff).isCloseTo(3.0f, within((float) 0d));
			float return_fff = e.getValue(ctx, float.class);
			assertThat(return_fff).isCloseTo(4.0f, within((float) 0d));
			assertThat(helper.fff).isCloseTo(4.0f, within((float) 0d));

			// long
			e = parser.parseExpression("++lll");
			assertThat(helper.lll).isEqualTo(66666L);
			long return_lll = e.getValue(ctx, long.class);
			assertThat(return_lll).isEqualTo(66667L);
			assertThat(helper.lll).isEqualTo(66667L);

			// int
			e = parser.parseExpression("++iii");
			assertThat(helper.iii).isEqualTo(42);
			int return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(43);
			assertThat(helper.iii).isEqualTo(43);
			return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(44);
			assertThat(helper.iii).isEqualTo(44);

			// short
			e = parser.parseExpression("++sss");
			assertThat(helper.sss).isEqualTo((short) 15);
			int return_sss = (Integer) e.getValue(ctx);
			assertThat(return_sss).isEqualTo((short) 16);
			assertThat(helper.sss).isEqualTo((short) 16);
		}

		@Test
		void increment03() {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

			Expression e1 = parser.parseExpression("m()++");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e1.getValue(ctx, double.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERAND_NOT_INCREMENTABLE));

			Expression e2 = parser.parseExpression("++m()");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e2.getValue(ctx, double.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERAND_NOT_INCREMENTABLE));
		}

		@Test
		void increment04() {
			Integer i = 42;
			StandardEvaluationContext ctx = new StandardEvaluationContext(i);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e1 = parser.parseExpression("++1");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e1.getValue(ctx, double.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.NOT_ASSIGNABLE));
			Expression e2 = parser.parseExpression("1++");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e2.getValue(ctx, double.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.NOT_ASSIGNABLE));
		}

		@Test
		void decrement01root() {
			Integer i = 42;
			StandardEvaluationContext ctx = new StandardEvaluationContext(i);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e = parser.parseExpression("#this--");
			assertThat(i).isEqualTo(42);
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e.getValue(ctx, Integer.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.NOT_ASSIGNABLE));
		}

		@Test
		void decrement02postfix() {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e;

			// BigDecimal
			e = parser.parseExpression("bd--");
			assertThat(new BigDecimal("2").equals(helper.bd)).isTrue();
			BigDecimal return_bd = e.getValue(ctx,BigDecimal.class);
			assertThat(new BigDecimal("2")).isEqualTo(return_bd);
			assertThat(new BigDecimal("1").equals(helper.bd)).isTrue();

			// double
			e = parser.parseExpression("ddd--");
			assertThat((float) helper.ddd).isCloseTo((float) 2.0d, within((float) 0d));
			double return_ddd = e.getValue(ctx, double.class);
			assertThat((float) return_ddd).isCloseTo((float) 2.0d, within((float) 0d));
			assertThat((float) helper.ddd).isCloseTo((float) 1.0d, within((float) 0d));

			// float
			e = parser.parseExpression("fff--");
			assertThat(helper.fff).isCloseTo(3.0f, within((float) 0d));
			float return_fff = e.getValue(ctx, float.class);
			assertThat(return_fff).isCloseTo(3.0f, within((float) 0d));
			assertThat(helper.fff).isCloseTo(2.0f, within((float) 0d));

			// long
			e = parser.parseExpression("lll--");
			assertThat(helper.lll).isEqualTo(66666L);
			long return_lll = e.getValue(ctx, long.class);
			assertThat(return_lll).isEqualTo(66666L);
			assertThat(helper.lll).isEqualTo(66665L);

			// int
			e = parser.parseExpression("iii--");
			assertThat(helper.iii).isEqualTo(42);
			int return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(42);
			assertThat(helper.iii).isEqualTo(41);
			return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(41);
			assertThat(helper.iii).isEqualTo(40);

			// short
			e = parser.parseExpression("sss--");
			assertThat(helper.sss).isEqualTo((short) 15);
			short return_sss = e.getValue(ctx, short.class);
			assertThat(return_sss).isEqualTo((short) 15);
			assertThat(helper.sss).isEqualTo((short) 14);
		}

		@Test
		void decrement02prefix() {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e;

			// BigDecimal
			e = parser.parseExpression("--bd");
			assertThat(new BigDecimal("2").equals(helper.bd)).isTrue();
			BigDecimal return_bd = e.getValue(ctx,BigDecimal.class);
			assertThat(new BigDecimal("1")).isEqualTo(return_bd);
			assertThat(new BigDecimal("1").equals(helper.bd)).isTrue();

			// double
			e = parser.parseExpression("--ddd");
			assertThat((float) helper.ddd).isCloseTo((float) 2.0d, within((float) 0d));
			double return_ddd = e.getValue(ctx, double.class);
			assertThat((float) return_ddd).isCloseTo((float) 1.0d, within((float) 0d));
			assertThat((float) helper.ddd).isCloseTo((float) 1.0d, within((float) 0d));

			// float
			e = parser.parseExpression("--fff");
			assertThat(helper.fff).isCloseTo(3.0f, within((float) 0d));
			float return_fff = e.getValue(ctx, float.class);
			assertThat(return_fff).isCloseTo(2.0f, within((float) 0d));
			assertThat(helper.fff).isCloseTo(2.0f, within((float) 0d));

			// long
			e = parser.parseExpression("--lll");
			assertThat(helper.lll).isEqualTo(66666L);
			long return_lll = e.getValue(ctx, long.class);
			assertThat(return_lll).isEqualTo(66665L);
			assertThat(helper.lll).isEqualTo(66665L);

			// int
			e = parser.parseExpression("--iii");
			assertThat(helper.iii).isEqualTo(42);
			int return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(41);
			assertThat(helper.iii).isEqualTo(41);
			return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(40);
			assertThat(helper.iii).isEqualTo(40);

			// short
			e = parser.parseExpression("--sss");
			assertThat(helper.sss).isEqualTo((short) 15);
			int return_sss = (Integer)e.getValue(ctx);
			assertThat(return_sss).isEqualTo(14);
			assertThat(helper.sss).isEqualTo((short) 14);
		}

		@Test
		void decrement03() {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

			Expression e1 = parser.parseExpression("m()--");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e1.getValue(ctx, double.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERAND_NOT_DECREMENTABLE));

			Expression e2 = parser.parseExpression("--m()");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e2.getValue(ctx, double.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERAND_NOT_DECREMENTABLE));
		}

		@Test
		void decrement04() {
			Integer i = 42;
			StandardEvaluationContext ctx = new StandardEvaluationContext(i);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e1 = parser.parseExpression("--1");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e1.getValue(ctx, Integer.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.NOT_ASSIGNABLE));

			Expression e2 = parser.parseExpression("1--");
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> e2.getValue(ctx, Integer.class))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.NOT_ASSIGNABLE));
		}

		@Test
		void incrementAndDecrementTogether() {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e;

			// index1 is 2 at the start - the 'intArray[#root.index1++]' should not be evaluated twice!
			// intArray[2] is 3
			e = parser.parseExpression("intArray[#root.index1++]++");
			e.getValue(ctx, Integer.class);
			assertThat(helper.index1).isEqualTo(3);
			assertThat(helper.intArray[2]).isEqualTo(4);

			// index1 is 3 intArray[3] is 4
			e = parser.parseExpression("intArray[#root.index1++]--");
			assertThat(e.getValue(ctx, Integer.class)).isEqualTo(4);
			assertThat(helper.index1).isEqualTo(4);
			assertThat(helper.intArray[3]).isEqualTo(3);

			// index1 is 4, intArray[3] is 3
			e = parser.parseExpression("intArray[--#root.index1]++");
			assertThat(e.getValue(ctx, Integer.class)).isEqualTo(3);
			assertThat(helper.index1).isEqualTo(3);
			assertThat(helper.intArray[3]).isEqualTo(4);
		}

		// Verify how all the nodes behave with assignment (++, --, =)
		@Test
		void incrementAllNodeTypes() throws SecurityException, NoSuchMethodException {
			Spr9751 helper = new Spr9751();
			StandardEvaluationContext ctx = new StandardEvaluationContext(helper);
			ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
			Expression e;

			// BooleanLiteral
			expectFailNotAssignable(parser, ctx, "true++");
			expectFailNotAssignable(parser, ctx, "--false");
			expectFailSetValueNotSupported(parser, ctx, "true=false");

			// IntLiteral
			expectFailNotAssignable(parser, ctx, "12++");
			expectFailNotAssignable(parser, ctx, "--1222");
			expectFailSetValueNotSupported(parser, ctx, "12=16");

			// LongLiteral
			expectFailNotAssignable(parser, ctx, "1.0d++");
			expectFailNotAssignable(parser, ctx, "--3.4d");
			expectFailSetValueNotSupported(parser, ctx, "1.0d=3.2d");

			// NullLiteral
			expectFailNotAssignable(parser, ctx, "null++");
			expectFailNotAssignable(parser, ctx, "--null");
			expectFailSetValueNotSupported(parser, ctx, "null=null");
			expectFailSetValueNotSupported(parser, ctx, "null=123");

			// OpAnd
			expectFailNotAssignable(parser, ctx, "(true && false)++");
			expectFailNotAssignable(parser, ctx, "--(false AND true)");
			expectFailSetValueNotSupported(parser, ctx, "(true && false)=(false && true)");

			// OpDivide
			expectFailNotAssignable(parser, ctx, "(3/4)++");
			expectFailNotAssignable(parser, ctx, "--(2/5)");
			expectFailSetValueNotSupported(parser, ctx, "(1/2)=(3/4)");

			// OpEq
			expectFailNotAssignable(parser, ctx, "(3==4)++");
			expectFailNotAssignable(parser, ctx, "--(2==5)");
			expectFailSetValueNotSupported(parser, ctx, "(1==2)=(3==4)");

			// OpGE
			expectFailNotAssignable(parser, ctx, "(3>=4)++");
			expectFailNotAssignable(parser, ctx, "--(2>=5)");
			expectFailSetValueNotSupported(parser, ctx, "(1>=2)=(3>=4)");

			// OpGT
			expectFailNotAssignable(parser, ctx, "(3>4)++");
			expectFailNotAssignable(parser, ctx, "--(2>5)");
			expectFailSetValueNotSupported(parser, ctx, "(1>2)=(3>4)");

			// OpLE
			expectFailNotAssignable(parser, ctx, "(3<=4)++");
			expectFailNotAssignable(parser, ctx, "--(2<=5)");
			expectFailSetValueNotSupported(parser, ctx, "(1<=2)=(3<=4)");

			// OpLT
			expectFailNotAssignable(parser, ctx, "(3<4)++");
			expectFailNotAssignable(parser, ctx, "--(2<5)");
			expectFailSetValueNotSupported(parser, ctx, "(1<2)=(3<4)");

			// OpMinus
			expectFailNotAssignable(parser, ctx, "(3-4)++");
			expectFailNotAssignable(parser, ctx, "--(2-5)");
			expectFailSetValueNotSupported(parser, ctx, "(1-2)=(3-4)");

			// OpModulus
			expectFailNotAssignable(parser, ctx, "(3%4)++");
			expectFailNotAssignable(parser, ctx, "--(2%5)");
			expectFailSetValueNotSupported(parser, ctx, "(1%2)=(3%4)");

			// OpMultiply
			expectFailNotAssignable(parser, ctx, "(3*4)++");
			expectFailNotAssignable(parser, ctx, "--(2*5)");
			expectFailSetValueNotSupported(parser, ctx, "(1*2)=(3*4)");

			// OpNE
			expectFailNotAssignable(parser, ctx, "(3!=4)++");
			expectFailNotAssignable(parser, ctx, "--(2!=5)");
			expectFailSetValueNotSupported(parser, ctx, "(1!=2)=(3!=4)");

			// OpOr
			expectFailNotAssignable(parser, ctx, "(true || false)++");
			expectFailNotAssignable(parser, ctx, "--(false OR true)");
			expectFailSetValueNotSupported(parser, ctx, "(true || false)=(false OR true)");

			// OpPlus
			expectFailNotAssignable(parser, ctx, "(3+4)++");
			expectFailNotAssignable(parser, ctx, "--(2+5)");
			expectFailSetValueNotSupported(parser, ctx, "(1+2)=(3+4)");

			// RealLiteral
			expectFailNotAssignable(parser, ctx, "1.0d++");
			expectFailNotAssignable(parser, ctx, "--2.0d");
			expectFailSetValueNotSupported(parser, ctx, "(1.0d)=(3.0d)");
			expectFailNotAssignable(parser, ctx, "1.0f++");
			expectFailNotAssignable(parser, ctx, "--2.0f");
			expectFailSetValueNotSupported(parser, ctx, "(1.0f)=(3.0f)");

			// StringLiteral
			expectFailNotAssignable(parser, ctx, "'abc'++");
			expectFailNotAssignable(parser, ctx, "--'def'");
			expectFailSetValueNotSupported(parser, ctx, "'abc'='def'");

			// Ternary
			expectFailNotAssignable(parser, ctx, "(true?true:false)++");
			expectFailNotAssignable(parser, ctx, "--(true?true:false)");
			expectFailSetValueNotSupported(parser, ctx, "(true?true:false)=(true?true:false)");

			// TypeReference
			expectFailNotAssignable(parser, ctx, "T(String)++");
			expectFailNotAssignable(parser, ctx, "--T(Integer)");
			expectFailSetValueNotSupported(parser, ctx, "T(String)=T(Integer)");

			// OperatorBetween
			expectFailNotAssignable(parser, ctx, "(3 between {1,5})++");
			expectFailNotAssignable(parser, ctx, "--(3 between {1,5})");
			expectFailSetValueNotSupported(parser, ctx, "(3 between {1,5})=(3 between {1,5})");

			// OperatorInstanceOf
			expectFailNotAssignable(parser, ctx, "(type instanceof T(String))++");
			expectFailNotAssignable(parser, ctx, "--(type instanceof T(String))");
			expectFailSetValueNotSupported(parser, ctx, "(type instanceof T(String))=(type instanceof T(String))");

			// Elvis
			expectFailNotAssignable(parser, ctx, "(true?:false)++");
			expectFailNotAssignable(parser, ctx, "--(true?:false)");
			expectFailSetValueNotSupported(parser, ctx, "(true?:false)=(true?:false)");

			// OpInc
			expectFailNotAssignable(parser, ctx, "(iii++)++");
			expectFailNotAssignable(parser, ctx, "--(++iii)");
			expectFailSetValueNotSupported(parser, ctx, "(iii++)=(++iii)");

			// OpDec
			expectFailNotAssignable(parser, ctx, "(iii--)++");
			expectFailNotAssignable(parser, ctx, "--(--iii)");
			expectFailSetValueNotSupported(parser, ctx, "(iii--)=(--iii)");

			// OperatorNot
			expectFailNotAssignable(parser, ctx, "(!true)++");
			expectFailNotAssignable(parser, ctx, "--(!false)");
			expectFailSetValueNotSupported(parser, ctx, "(!true)=(!false)");

			// OperatorPower
			expectFailNotAssignable(parser, ctx, "(iii^2)++");
			expectFailNotAssignable(parser, ctx, "--(iii^2)");
			expectFailSetValueNotSupported(parser, ctx, "(iii^2)=(iii^3)");

			// Assign
			// iii=42
			e = parser.parseExpression("iii=iii++");
			assertThat(helper.iii).isEqualTo(42);
			int return_iii = e.getValue(ctx, int.class);
			assertThat(helper.iii).isEqualTo(42);
			assertThat(return_iii).isEqualTo(42);

			// Identifier
			e = parser.parseExpression("iii++");
			assertThat(helper.iii).isEqualTo(42);
			return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(42);
			assertThat(helper.iii).isEqualTo(43);

			e = parser.parseExpression("--iii");
			assertThat(helper.iii).isEqualTo(43);
			return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(42);
			assertThat(helper.iii).isEqualTo(42);

			e = parser.parseExpression("iii=99");
			assertThat(helper.iii).isEqualTo(42);
			return_iii = e.getValue(ctx, int.class);
			assertThat(return_iii).isEqualTo(99);
			assertThat(helper.iii).isEqualTo(99);

			// CompoundExpression
			// foo.iii == 99
			e = parser.parseExpression("foo.iii++");
			assertThat(helper.foo.iii).isEqualTo(99);
			int return_foo_iii = e.getValue(ctx, int.class);
			assertThat(return_foo_iii).isEqualTo(99);
			assertThat(helper.foo.iii).isEqualTo(100);

			e = parser.parseExpression("--foo.iii");
			assertThat(helper.foo.iii).isEqualTo(100);
			return_foo_iii = e.getValue(ctx, int.class);
			assertThat(return_foo_iii).isEqualTo(99);
			assertThat(helper.foo.iii).isEqualTo(99);

			e = parser.parseExpression("foo.iii=999");
			assertThat(helper.foo.iii).isEqualTo(99);
			return_foo_iii = e.getValue(ctx, int.class);
			assertThat(return_foo_iii).isEqualTo(999);
			assertThat(helper.foo.iii).isEqualTo(999);

			// ConstructorReference
			expectFailNotAssignable(parser, ctx, "(new String('abc'))++");
			expectFailNotAssignable(parser, ctx, "--(new String('abc'))");
			expectFailSetValueNotSupported(parser, ctx, "(new String('abc'))=(new String('abc'))");

			// MethodReference
			expectFailNotIncrementable(parser, ctx, "m()++");
			expectFailNotDecrementable(parser, ctx, "--m()");
			expectFailSetValueNotSupported(parser, ctx, "m()=m()");

			// OperatorMatches
			expectFailNotAssignable(parser, ctx, "('abc' matches '^a..')++");
			expectFailNotAssignable(parser, ctx, "--('abc' matches '^a..')");
			expectFailSetValueNotSupported(parser, ctx, "('abc' matches '^a..')=('abc' matches '^a..')");

			// Selection
			ctx.registerFunction("isEven", Spr9751.class.getDeclaredMethod("isEven", int.class));

			expectFailNotIncrementable(parser, ctx, "({1,2,3}.?[#isEven(#this)])++");
			expectFailNotDecrementable(parser, ctx, "--({1,2,3}.?[#isEven(#this)])");
			expectFailNotAssignable(parser, ctx, "({1,2,3}.?[#isEven(#this)])=({1,2,3}.?[#isEven(#this)])");

			// slightly diff here because return value isn't a list, it is a single entity
			expectFailNotAssignable(parser, ctx, "({1,2,3}.^[#isEven(#this)])++");
			expectFailNotAssignable(parser, ctx, "--({1,2,3}.^[#isEven(#this)])");
			expectFailNotAssignable(parser, ctx, "({1,2,3}.^[#isEven(#this)])=({1,2,3}.^[#isEven(#this)])");

			expectFailNotAssignable(parser, ctx, "({1,2,3}.$[#isEven(#this)])++");
			expectFailNotAssignable(parser, ctx, "--({1,2,3}.$[#isEven(#this)])");
			expectFailNotAssignable(parser, ctx, "({1,2,3}.$[#isEven(#this)])=({1,2,3}.$[#isEven(#this)])");

			// FunctionReference
			expectFailNotAssignable(parser, ctx, "#isEven(3)++");
			expectFailNotAssignable(parser, ctx, "--#isEven(4)");
			expectFailSetValueNotSupported(parser, ctx, "#isEven(3)=#isEven(5)");

			// VariableReference
			ctx.setVariable("wibble", "hello world");
			expectFailNotIncrementable(parser, ctx, "#wibble++");
			expectFailNotDecrementable(parser, ctx, "--#wibble");
			e = parser.parseExpression("#wibble=#wibble+#wibble");
			String s = e.getValue(ctx, String.class);
			assertThat(s).isEqualTo("hello worldhello world");
			assertThat(ctx.lookupVariable("wibble")).isEqualTo("hello worldhello world");

			ctx.setVariable("wobble", 3);
			e = parser.parseExpression("#wobble++");
			assertThat(((Integer) ctx.lookupVariable("wobble"))).isEqualTo(3);
			int r = e.getValue(ctx, int.class);
			assertThat(r).isEqualTo(3);
			assertThat(((Integer) ctx.lookupVariable("wobble"))).isEqualTo(4);

			e = parser.parseExpression("--#wobble");
			assertThat(((Integer) ctx.lookupVariable("wobble"))).isEqualTo(4);
			r = e.getValue(ctx, int.class);
			assertThat(r).isEqualTo(3);
			assertThat(((Integer) ctx.lookupVariable("wobble"))).isEqualTo(3);

			e = parser.parseExpression("#wobble=34");
			assertThat(((Integer) ctx.lookupVariable("wobble"))).isEqualTo(3);
			r = e.getValue(ctx, int.class);
			assertThat(r).isEqualTo(34);
			assertThat(((Integer) ctx.lookupVariable("wobble"))).isEqualTo(34);

			// Projection
			expectFailNotIncrementable(parser, ctx, "({1,2,3}.![#isEven(#this)])++");  // projection would be {false,true,false}
			expectFailNotDecrementable(parser, ctx, "--({1,2,3}.![#isEven(#this)])");  // projection would be {false,true,false}
			expectFailNotAssignable(parser, ctx, "({1,2,3}.![#isEven(#this)])=({1,2,3}.![#isEven(#this)])");

			// InlineList
			expectFailNotAssignable(parser, ctx, "({1,2,3})++");
			expectFailNotAssignable(parser, ctx, "--({1,2,3})");
			expectFailSetValueNotSupported(parser, ctx, "({1,2,3})=({1,2,3})");

			// InlineMap
			expectFailNotAssignable(parser, ctx, "({'a':1,'b':2,'c':3})++");
			expectFailNotAssignable(parser, ctx, "--({'a':1,'b':2,'c':3})");
			expectFailSetValueNotSupported(parser, ctx, "({'a':1,'b':2,'c':3})=({'a':1,'b':2,'c':3})");

			// BeanReference
			BeanResolver beanResolver = (context, beanName) -> {
				if (beanName.equals("foo") || beanName.equals("bar")) {
					return new Spr9751_2();
				}
				throw new AccessException("unknown bean " + beanName);
			};
			ctx.setBeanResolver(beanResolver);
			expectFailNotAssignable(parser, ctx, "@foo++");
			expectFailNotAssignable(parser, ctx, "--@foo");
			expectFailSetValueNotSupported(parser, ctx, "@foo=@bar");

			// PropertyOrFieldReference
			helper.iii = 42;
			e = parser.parseExpression("iii++");
			assertThat(helper.iii).isEqualTo(42);
			r = e.getValue(ctx, int.class);
			assertThat(r).isEqualTo(42);
			assertThat(helper.iii).isEqualTo(43);

			e = parser.parseExpression("--iii");
			assertThat(helper.iii).isEqualTo(43);
			r = e.getValue(ctx, int.class);
			assertThat(r).isEqualTo(42);
			assertThat(helper.iii).isEqualTo(42);

			e = parser.parseExpression("iii=100");
			assertThat(helper.iii).isEqualTo(42);
			r = e.getValue(ctx, int.class);
			assertThat(r).isEqualTo(100);
			assertThat(helper.iii).isEqualTo(100);
		}

		private void expectFailNotAssignable(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
			expectFail(parser, eContext, expressionString, SpelMessage.NOT_ASSIGNABLE);
		}

		private void expectFailSetValueNotSupported(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
			expectFail(parser, eContext, expressionString, SpelMessage.SETVALUE_NOT_SUPPORTED);
		}

		private void expectFailNotIncrementable(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
			expectFail(parser, eContext, expressionString, SpelMessage.OPERAND_NOT_INCREMENTABLE);
		}

		private void expectFailNotDecrementable(ExpressionParser parser, EvaluationContext eContext, String expressionString) {
			expectFail(parser, eContext, expressionString, SpelMessage.OPERAND_NOT_DECREMENTABLE);
		}

		private void expectFail(ExpressionParser parser, EvaluationContext eContext, String expressionString, SpelMessage messageCode) {
			assertThatExceptionOfType(SpelEvaluationException.class)
				.isThrownBy(() -> {
					Expression e = parser.parseExpression(expressionString);
					if (DEBUG) {
						SpelUtilities.printAbstractSyntaxTree(System.out, e);
					}
					e.getValue(eContext);
				})
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(messageCode));
		}

	}


	@SuppressWarnings("rawtypes")
	static class TestClass {

		public Foo wibble;
		private Foo wibble2;
		public Map map;
		public Map<String, Integer> mapStringToInteger;
		public List<String> list;
		public List list2;
		private Map map2;
		private List<String> foo;

		public Map getMap2() { return this.map2; }
		public Foo getWibble2() { return this.wibble2; }
		public List<String> getFoo() { return this.foo; }
		public void setFoo(List<String> newfoo) { this.foo = newfoo; }
	}


	public static class Foo {

		public String bar = "hello";

		public Foo() {}
	}


	// increment/decrement operators - SPR-9751
	static class Spr9751 {

		public String type = "hello";
		public BigDecimal bd = new BigDecimal("2");
		public double ddd = 2.0d;
		public float fff = 3.0f;
		public long lll = 66666L;
		public int iii = 42;
		public short sss = (short)15;
		public Spr9751_2 foo = new Spr9751_2();

		public int[] intArray = new int[]{1,2,3,4,5};
		public int index1 = 2;

		public Integer[] integerArray;
		public int index2 = 2;

		public List<String> listOfStrings;
		public int index3 = 0;

		public Spr9751() {
			integerArray = new Integer[5];
			integerArray[0] = 1;
			integerArray[1] = 2;
			integerArray[2] = 3;
			integerArray[3] = 4;
			integerArray[4] = 5;
			listOfStrings = new ArrayList<>();
			listOfStrings.add("abc");
		}

		public void m() {}

		public static boolean isEven(int i) {
			return (i%2)==0;
		}
	}


	static class Spr9751_2 {

		public int iii = 99;
	}

}
