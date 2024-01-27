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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.PlaceOfBirth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests set value expressions.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Sam Brannen
 */
class SetValueTests extends AbstractExpressionTests {

	private static final boolean DEBUG = false;


	@Test
	void setValueFailsWhenLeftOperandIsNotAssignable() {
		setValueAndExpectError("3=4", "enigma");
	}

	@Test
	void setProperty() {
		setValue("wonNobelPrize", true);
	}

	@Test
	void setNestedProperty() {
		setValue("placeOfBirth.city", "Wien");
	}

	@Test
	void setArrayElementValueToStringFromString() {
		setValue("inventions[0]", "Just the telephone");
	}

	@Test
	void setArrayElementInNonexistentIndex() {
		setValueAndExpectError(
			"new org.springframework.expression.spel.testresources.Inventor().inventions[1]", "my invention");
	}

	@Test
	void setArrayElementValueToPrimitiveTypeFromWrapperType() {
		// All primitive values below are auto-boxed into their wrapper types.
		setValue("arrayContainer.booleans[1]", false);
		setValue("arrayContainer.chars[1]", (char) 3);
		setValue("arrayContainer.shorts[1]", (short) 3);
		setValue("arrayContainer.bytes[1]", (byte) 3);
		setValue("arrayContainer.ints[1]", 3);
		setValue("arrayContainer.longs[1]", 3L);
		setValue("arrayContainer.floats[1]", 3.0f);
		setValue("arrayContainer.doubles[1]", 3.4d);
	}

	@Test
	void setArrayElementValueToPrimitiveTypeFromStringResultsInError() {
		String notPrimitiveOrWrapper = "not primitive or wrapper";
		setValueAndExpectError("arrayContainer.booleans[1]", notPrimitiveOrWrapper);
		setValueAndExpectError("arrayContainer.chars[1]", notPrimitiveOrWrapper);
		setValueAndExpectError("arrayContainer.shorts[1]", notPrimitiveOrWrapper);
		setValueAndExpectError("arrayContainer.bytes[1]", notPrimitiveOrWrapper);
		setValueAndExpectError("arrayContainer.ints[1]", notPrimitiveOrWrapper);
		setValueAndExpectError("arrayContainer.longs[1]", notPrimitiveOrWrapper);
		setValueAndExpectError("arrayContainer.floats[1]", notPrimitiveOrWrapper);
		setValueAndExpectError("arrayContainer.doubles[1]", notPrimitiveOrWrapper);
	}

	@Test
	void setArrayElementValueToPrimitiveTypeFromSingleElementList() {
		setValue("arrayContainer.booleans[1]", List.of(false), false);
		setValue("arrayContainer.chars[1]", List.of('a'), 'a');
		setValue("arrayContainer.shorts[1]", List.of((short) 3), (short) 3);
		setValue("arrayContainer.bytes[1]", List.of((byte) 3), (byte) 3);
		setValue("arrayContainer.ints[1]", List.of(42), 42);
		setValue("arrayContainer.longs[1]", List.of(42L), 42L);
		setValue("arrayContainer.floats[1]", List.of(42F), 42F);
		setValue("arrayContainer.doubles[1]", List.of(42D), 42D);
	}

	@Disabled("Disabled due to bug in Indexer.setArrayElement() regarding primitive/wrapper types")
	@Test
	void setArrayElementValueToPrimitiveTypeFromEmptyListResultsInError() {
		List<Object> emptyList = List.of();
		// TODO These fail because CollectionToObjectConverter returns null.
		// It currently throws: java.lang.IllegalStateException: Null conversion result for index [[]].
		// Whereas, it should throw a SpelEvaluationException.
		setValueAndExpectError("arrayContainer.booleans[1]", emptyList);
		setValueAndExpectError("arrayContainer.chars[1]", emptyList);
		setValueAndExpectError("arrayContainer.shorts[1]", emptyList);
		setValueAndExpectError("arrayContainer.bytes[1]", emptyList);
		setValueAndExpectError("arrayContainer.ints[1]", emptyList);
		setValueAndExpectError("arrayContainer.longs[1]", emptyList);
		setValueAndExpectError("arrayContainer.floats[1]", emptyList);
		setValueAndExpectError("arrayContainer.doubles[1]", emptyList);
	}

	@Test  // gh-15239
	void isWritableForInvalidExpressions() {
		// Do NOT reuse super.context!
		StandardEvaluationContext localContext = TestScenarioCreator.getTestEvaluationContext();

		// PROPERTYORFIELDREFERENCE
		// Non-existent field (or property):
		Expression e1 = parser.parseExpression("arrayContainer.wibble");
		assertThat(e1.isWritable(localContext)).as("Should not be writable!").isFalse();

		Expression e2 = parser.parseExpression("arrayContainer.wibble.foo");
		assertThatSpelEvaluationException().isThrownBy(() -> e2.isWritable(localContext));

		// VARIABLE
		// the variable does not exist (but that is OK, we should be writable)
		Expression e3 = parser.parseExpression("#madeup1");
		assertThat(e3.isWritable(localContext)).as("Should be writable!").isTrue();

		Expression e4 = parser.parseExpression("#madeup2.bar"); // compound expression
		assertThat(e4.isWritable(localContext)).as("Should not be writable!").isFalse();

		// INDEXER
		// non-existent indexer (wibble made up)
		Expression e5 = parser.parseExpression("arrayContainer.wibble[99]");
		assertThatSpelEvaluationException().isThrownBy(() -> e5.isWritable(localContext));

		// non-existent indexer (index via a string)
		Expression e6 = parser.parseExpression("arrayContainer.ints['abc']");
		assertThatSpelEvaluationException().isThrownBy(() -> e6.isWritable(localContext));
	}

	@Test
	void setArrayElementNestedValue() {
		setValue("placesLived[0].city", "Wien");
	}

	@Test
	void setListElementValue() {
		setValue("placesLivedList[0]", new PlaceOfBirth("Wien"));
	}

	@Test
	void setGenericListElementValueTypeCoercion() {
		setValue("placesLivedList[0]", "Wien");
	}

	@Test
	void setGenericListElementValueTypeCoercionOK() {
		setValue("booleanList[0]", "true", Boolean.TRUE);
	}

	@Test
	void setListElementNestedValue() {
		setValue("placesLived[0].city", "Wien");
	}

	@Test
	void setArrayElementInvalidIndex() {
		setValueAndExpectError("placesLived[23]", "Wien");
		setValueAndExpectError("placesLivedList[23]", "Wien");
	}

	@Test
	void setMapElements() {
		setValue("testMap['montag']","lundi");
	}

	@Test
	void indexingIntoUnsupportedType() {
		setValueAndExpectError("'hello'[3]", 'p');
	}

	@Test
	void setPropertyTypeCoercion() {
		setValue("publicBoolean", "true", Boolean.TRUE);
	}

	@Test
	void setPropertyTypeCoercionThroughSetter() {
		setValue("SomeProperty", "true", Boolean.TRUE);
	}

	@Test
	void assign() {
		// Do NOT reuse super.context!
		StandardEvaluationContext localContext = TestScenarioCreator.getTestEvaluationContext();
		Expression e = parse("publicName='Andy'");
		assertThat(e.isWritable(localContext)).isFalse();
		assertThat(e.getValue(localContext)).isEqualTo("Andy");
	}

	/**
	 * Testing the coercion of both the keys and the values to the correct type.
	 */
	@Test
	void setGenericMapElementRequiresCoercion() {
		// Do NOT reuse super.context!
		StandardEvaluationContext localContext = TestScenarioCreator.getTestEvaluationContext();
		Expression e = parse("mapOfStringToBoolean[42]");
		assertThat(e.getValue(localContext)).isNull();

		// Key should be coerced to string representation of 42
		e.setValue(localContext, "true");

		// All keys should be strings
		Set<?> keys = parse("mapOfStringToBoolean.keySet()").getValue(localContext, Set.class);
		assertThat(keys).allSatisfy(key -> assertThat(key).isExactlyInstanceOf(String.class));

		// All values should be booleans
		Collection<?> values = parse("mapOfStringToBoolean.values()").getValue(localContext, Collection.class);
		assertThat(values).allSatisfy(key -> assertThat(key).isExactlyInstanceOf(Boolean.class));

		// One final test check coercion on the key for a map lookup
		Object o = e.getValue(localContext);
		assertThat(o).isEqualTo(Boolean.TRUE);
	}


	private Expression parse(String expressionString) {
		return parser.parseExpression(expressionString);
	}

	/**
	 * Call setValue() but expect it to fail.
	 */
	private void setValueAndExpectError(String expression, Object value) {
		Expression e = parser.parseExpression(expression);
		assertThat(e).isNotNull();
		if (DEBUG) {
			SpelUtilities.printAbstractSyntaxTree(System.out, e);
		}
		// Do NOT reuse super.context!
		StandardEvaluationContext localContext = TestScenarioCreator.getTestEvaluationContext();
		assertThatSpelEvaluationException().isThrownBy(() -> e.setValue(localContext, value));
	}

	private void setValue(String expression, Object value) {
		Class<?> expectedType = value.getClass();
		try {
			Expression e = parser.parseExpression(expression);
			assertThat(e).isNotNull();
			if (DEBUG) {
				SpelUtilities.printAbstractSyntaxTree(System.out, e);
			}
			// Do NOT reuse super.context!
			StandardEvaluationContext localContext = TestScenarioCreator.getTestEvaluationContext();
			assertThat(e.isWritable(localContext)).as("Expression is not writeable but should be").isTrue();
			e.setValue(localContext, value);
			assertThat(e.getValue(localContext, expectedType)).as("Retrieved value was not equal to set value").isEqualTo(value);
		}
		catch (EvaluationException | ParseException ex) {
			throw new AssertionError("Unexpected Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * For use when coercion is happening during setValue(). The expectedValue should be
	 * the coerced form of the value.
	 */
	private void setValue(String expression, Object value, Object expectedValue) {
		try {
			Expression e = parser.parseExpression(expression);
			assertThat(e).isNotNull();
			if (DEBUG) {
				SpelUtilities.printAbstractSyntaxTree(System.out, e);
			}
			// Do NOT reuse super.context!
			StandardEvaluationContext localContext = TestScenarioCreator.getTestEvaluationContext();
			assertThat(e.isWritable(localContext)).as("Expression is not writeable but should be").isTrue();
			e.setValue(localContext, value);
			assertThat(expectedValue).isEqualTo(e.getValue(localContext));
		}
		catch (EvaluationException | ParseException ex) {
			throw new AssertionError("Unexpected Exception: " + ex.getMessage(), ex);
		}
	}

	private static ThrowableTypeAssert<SpelEvaluationException> assertThatSpelEvaluationException() {
		return assertThatExceptionOfType(SpelEvaluationException.class);
	}

}
