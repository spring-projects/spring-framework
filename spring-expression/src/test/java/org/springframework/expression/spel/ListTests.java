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
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.ast.InlineList;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.expression.spel.SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN;

/**
 * Test usage of inline lists.
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0.4
 */
class ListTests extends AbstractExpressionTests {

	// if the list is full of literals then it will be of the type unmodifiableClass
	// rather than ArrayList
	Class<?> unmodifiableClass = Collections.unmodifiableList(new ArrayList<>()).getClass();


	@Test
	void testInlineListCreation01() {
		evaluate("{1, 2, 3, 4, 5}", "[1, 2, 3, 4, 5]", unmodifiableClass);
	}

	@Test
	void testInlineListCreation02() {
		evaluate("{'abc', 'xyz'}", "[abc, xyz]", unmodifiableClass);
	}

	@Test
	void testInlineListCreation03() {
		evaluate("{}", "[]", unmodifiableClass);
	}

	@Test
	void testInlineListCreation04() {
		evaluate("{'abc'=='xyz'}", "[false]", ArrayList.class);
	}

	@Test
	void testInlineListAndNesting() {
		evaluate("{{1,2,3},{4,5,6}}", "[[1, 2, 3], [4, 5, 6]]", unmodifiableClass);
		evaluate("{{1,'2',3},{4,{'a','b'},5,6}}", "[[1, 2, 3], [4, [a, b], 5, 6]]", unmodifiableClass);
	}

	@Test
	void testInlineListError() {
		parseAndCheckError("{'abc'", SpelMessage.OOD);
	}

	@Test
	void testRelOperatorsIs02() {
		evaluate("{1, 2, 3, 4, 5} instanceof T(java.util.List)", "true", Boolean.class);
	}

	@Test
	void testInlineListCreation05() {
		evaluate("3 between {1,5}", "true", Boolean.class);
	}

	@Test
	void testInlineListCreation06() {
		evaluate("8 between {1,5}", "false", Boolean.class);
	}

	@Test
	void projectionOnList() {
		evaluate("listOfNumbersUpToTen.![#this<5?'y':'n']", "[y, y, y, y, n, n, n, n, n, n]", ArrayList.class);
	}

	@Test
	void projectionOnInlineList() {
		evaluate("{1,2,3,4,5,6}.![#this>3]", "[false, false, false, true, true, true]", ArrayList.class);
		evaluate("{1,2,3,4,5,6,7,8,9,10}.![#this<5?'y':'n']", "[y, y, y, y, n, n, n, n, n, n]", ArrayList.class);
	}

	@Test
	void selectionOnInlineList() {
		evaluate("{1,2,3,4,5,6}.?[#this>3]", "[4, 5, 6]", ArrayList.class);
		evaluate("{1,2,3,4,5,6,7,8,9,10}.?[#isEven(#this) == 'y']", "[2, 4, 6, 8, 10]", ArrayList.class);
	}

	@Test
	void selectionOnListWithNonBooleanSelectionCriteria() {
		evaluateAndCheckError("listOfNumbersUpToTen.?['nonboolean']", RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
	}

	@Test
	void selectFirstOnList() {
		evaluate("listOfNumbersUpToTen.^[#isEven(#this) == 'y']", "2", Integer.class);
	}

	@Test
	void selectLastOnList() {
		evaluate("listOfNumbersUpToTen.$[#isEven(#this) == 'y']", "10", Integer.class);
	}

	@Test
	void setConstructionWithInlineList() {
		evaluate("new java.util.HashSet().addAll({'a','b','c'})", "true", Boolean.class);
	}

	@Test
	void testRelOperatorsBetween01() {
		evaluate("32 between {32, 42}", "true", Boolean.class);
	}

	@Test
	void testRelOperatorsBetween02() {
		evaluate("'efg' between {'abc', 'xyz'}", "true", Boolean.class);
	}

	@Test
	void testRelOperatorsBetween03() {
		evaluate("42 between {32, 42}", "true", Boolean.class);
	}

	@Test
	void testRelOperatorsBetween04() {
		evaluate("new java.math.BigDecimal('1') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
			"true", Boolean.class);
		evaluate("new java.math.BigDecimal('3') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
			"true", Boolean.class);
		evaluate("new java.math.BigDecimal('5') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
			"true", Boolean.class);
		evaluate("new java.math.BigDecimal('8') between {new java.math.BigDecimal('1'),new java.math.BigDecimal('5')}",
			"false", Boolean.class);
	}

	@Test
	void testRelOperatorsBetweenErrors02() {
		evaluateAndCheckError("'abc' between {5,7}", SpelMessage.NOT_COMPARABLE, 6);
	}

	@Test
	void testConstantRepresentation1() {
		checkConstantList("{1,2,3,4,5}", true);
		checkConstantList("{'abc'}", true);
		checkConstantList("{}", true);
		checkConstantList("{#a,2,3}", false);
		checkConstantList("{1,2,Integer.valueOf(4)}", false);
		checkConstantList("{1,2,{#a}}", false);
	}

	private void checkConstantList(String expressionText, boolean expectedToBeConstant) {
		SpelExpressionParser parser = new SpelExpressionParser();
		SpelExpression expression = (SpelExpression) parser.parseExpression(expressionText);
		SpelNode node = expression.getAST();
		boolean condition = node instanceof InlineList;
		assertThat(condition).isTrue();
		InlineList inlineList = (InlineList) node;
		if (expectedToBeConstant) {
			assertThat(inlineList.isConstant()).isTrue();
		}
		else {
			assertThat(inlineList.isConstant()).isFalse();
		}
	}

	@Test
	void testInlineListWriting() {
		// list should be unmodifiable
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				evaluate("{1, 2, 3, 4, 5}[0]=6", "[1, 2, 3, 4, 5]", unmodifiableClass));
	}
}
