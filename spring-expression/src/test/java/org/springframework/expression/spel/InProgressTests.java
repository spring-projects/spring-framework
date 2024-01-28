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

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.standard.SpelExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.expression.spel.SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST;
import static org.springframework.expression.spel.SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE;
import static org.springframework.expression.spel.SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN;

/**
 * These are tests for language features that are not yet considered 'live':
 * either missing implementation or documentation.
 *
 * @author Andy Clement
 */
class InProgressTests extends AbstractExpressionTests {

	// BETWEEN

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

	// PROJECTION

	@Test
	void projectionOnList() {
		evaluate("listOfNumbersUpToTen.![#this<5?'y':'n']", "[y, y, y, y, n, n, n, n, n, n]", ArrayList.class);
	}

	@Test
	void projectionOnInlineList() {
		evaluate("{1,2,3,4,5,6,7,8,9,10}.![#this<5?'y':'n']", "[y, y, y, y, n, n, n, n, n, n]", ArrayList.class);
	}

	@Test
	void projectionOnMap() {
		evaluate("mapOfNumbersUpToTen.![key > 5 ? value : null]",
				"[null, null, null, null, null, six, seven, eight, nine, ten]", ArrayList.class);
	}

	@Test
	void projectionOnUnsupportedType() {
		evaluateAndCheckError("'abc'.![true]", PROJECTION_NOT_SUPPORTED_ON_TYPE);
		evaluateAndCheckError("null.![true]", PROJECTION_NOT_SUPPORTED_ON_TYPE);
	}

	@Test
	void projectionOnNullWithSafeNavigation() {
		evaluate("null?.![true]", null, null);
	}

	// SELECTION

	@Test
	void selectionWithNonBooleanSelectionCriteria() {
		evaluateAndCheckError("listOfNumbersUpToTen.?['nonboolean']", RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
		evaluateAndCheckError("mapOfNumbersUpToTen.?['hello'].size()", RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
	}

	@Test
	void selectionOnSet() {
		evaluate("testMap.keySet().?[#this matches '.*o.*']", "[monday]", ArrayList.class);
		evaluate("testMap.keySet().?[#this matches '.*r.*'].contains('saturday')", "true", Boolean.class);
		evaluate("testMap.keySet().?[#this matches '.*r.*'].size()", "3", Integer.class);
	}

	@Test
	void selectionOnMap() {
		evaluate("mapOfNumbersUpToTen.?[key>5].size()", "5", Integer.class);
		evaluate("mapOfNumbersUpToTen.?[key>11].size()", "0", Integer.class);
		evaluate("mapOfNumbersUpToTen.^[key>11]", null, null);
		evaluate("mapOfNumbersUpToTen.$[key>11]", null, null);
		evaluate("null?.$[key>11]", null, null);
		evaluateAndCheckError("null.?[key>11]", SpelMessage.INVALID_TYPE_FOR_SELECTION);
		evaluateAndCheckError("'abc'.?[key>11]", SpelMessage.INVALID_TYPE_FOR_SELECTION);
	}

	@Test
	void selectFirstOnList() {
		evaluate("listOfNumbersUpToTen.^[#isEven(#this) == 'y']", "2", Integer.class);
	}

	@Test
	void selectFirstOnMap() {
		evaluate("mapOfNumbersUpToTen.^[key>5].size()", "1", Integer.class);
	}

	@Test
	void selectLastOnList() {
		evaluate("listOfNumbersUpToTen.$[#isEven(#this) == 'y']", "10", Integer.class);
	}

	@Test
	void selectLastOnMap() {
		evaluate("mapOfNumbersUpToTen.$[key>5]", "{10=ten}", HashMap.class);
		evaluate("mapOfNumbersUpToTen.$[key>5].size()", "1", Integer.class);
	}

	@Test
	void selectionAST() {
		SpelExpression expr = (SpelExpression) parser.parseExpression("'abc'.^[true]");
		assertThat(expr.toStringAST()).isEqualTo("'abc'.^[true]");
		expr = (SpelExpression) parser.parseExpression("'abc'.?[true]");
		assertThat(expr.toStringAST()).isEqualTo("'abc'.?[true]");
		expr = (SpelExpression) parser.parseExpression("'abc'.$[true]");
		assertThat(expr.toStringAST()).isEqualTo("'abc'.$[true]");
	}

	// Constructor invocation

	@Test
	void constructorInvocationMethodInvocationAndInlineList() {
		evaluate("new java.util.HashSet().addAll({'a','b','c'})", "true", Boolean.class);
	}

}
