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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.springframework.expression.spel.SpelMessage.INVALID_TYPE_FOR_SELECTION;
import static org.springframework.expression.spel.SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE;
import static org.springframework.expression.spel.SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Andy Clement
 */
class SelectionAndProjectionTests extends AbstractExpressionTests {

	@Test
	void selectionOnUnsupportedType() {
		evaluateAndCheckError("'abc'.?[#this<5]", INVALID_TYPE_FOR_SELECTION);
		evaluateAndCheckError("null.?[#this<5]", INVALID_TYPE_FOR_SELECTION);
	}

	@Test
	void projectionOnUnsupportedType() {
		evaluateAndCheckError("'abc'.![true]", PROJECTION_NOT_SUPPORTED_ON_TYPE);
		evaluateAndCheckError("null.![true]", PROJECTION_NOT_SUPPORTED_ON_TYPE);
	}

	@Test
	void selectionOnNullWithSafeNavigation() {
		evaluate("null?.?[#this<5]", null, null);
	}

	@Test
	void projectionOnNullWithSafeNavigation() {
		evaluate("null?.![true]", null, null);
	}

	@Test
	void selectionWithNonBooleanSelectionCriteria() {
		evaluateAndCheckError("mapOfNumbersUpToTen.?['hello']", RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
		evaluateAndCheckError("mapOfNumbersUpToTen.keySet().?['hello']", RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
	}

	@Test
	void selectionAST() {
		// select first
		SpelExpression expr = (SpelExpression) parser.parseExpression("'abc'.^[true]");
		assertThat(expr.toStringAST()).isEqualTo("'abc'.^[true]");

		// select all
		expr = (SpelExpression) parser.parseExpression("'abc'.?[true]");
		assertThat(expr.toStringAST()).isEqualTo("'abc'.?[true]");

		// select last
		expr = (SpelExpression) parser.parseExpression("'abc'.$[true]");
		assertThat(expr.toStringAST()).isEqualTo("'abc'.$[true]");
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithList() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(LIST).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInList() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isZero();
	}

	@Test
	void selectLastItemInList() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isEqualTo(4);
	}

	@Test
	void selectionWithSetAndRegex() {
		evaluate("testMap.keySet().?[#this matches '.*o.*']", "[monday]", ArrayList.class);
		evaluate("testMap.keySet().?[#this matches '.*r.*'].contains('saturday')", "true", Boolean.class);
		evaluate("testMap.keySet().?[#this matches '.*r.*'].size()", "3", Integer.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithSet() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(LIST).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInSet() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isZero();
	}

	@Test
	void selectLastItemInSet() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isEqualTo(4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithIterable() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new IterableTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(LIST).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectionWithArray() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value.getClass().isArray()).isTrue();
		TypedValue typedValue = new TypedValue(value);
		assertThat(typedValue.getTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat((Integer[]) value).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInArray() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isZero();
	}

	@Test
	void selectLastItemInArray() {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isEqualTo(4);
	}

	@Test
	void selectionWithPrimitiveArray() {
		Expression expression = new SpelExpressionParser().parseRaw("ints.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value.getClass().isArray()).isTrue();
		TypedValue typedValue = new TypedValue(value);
		assertThat(typedValue.getTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat((Integer[]) value).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInPrimitiveArray() {
		Expression expression = new SpelExpressionParser().parseRaw("ints.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isZero();
	}

	@Test
	void selectLastItemInPrimitiveArray() {
		Expression expression = new SpelExpressionParser().parseRaw("ints.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		assertThat(expression.getValue(context)).asInstanceOf(INTEGER).isEqualTo(4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("colors.?[key.startsWith('b')]");
		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertThat(colorsMap).containsOnlyKeys("beige", "blue", "brown");

		exp = parser.parseExpression("colors.?[key.startsWith('X')]");

		colorsMap = (Map<String, String>) exp.getValue(context);
		assertThat(colorsMap).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectFirstItemInMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("colors.^[key.startsWith('b')]");
		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertThat(colorsMap).containsOnlyKeys("beige");
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectLastItemInMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("colors.$[key.startsWith('b')]");
		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertThat(colorsMap).containsOnlyKeys("brown");
	}

	@Test
	@SuppressWarnings("unchecked")
	void projectionWithList() {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createList());
		assertThat(expression.getValue(context)).asInstanceOf(LIST).containsExactly(5, 6, 7);
	}

	@Test
	void projectionWithMap() {
		evaluate("mapOfNumbersUpToTen.![key > 5 ? value : null]",
				"[null, null, null, null, null, six, seven, eight, nine, ten]", ArrayList.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void projectionWithSet() {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createSet());
		assertThat(expression.getValue(context)).asInstanceOf(LIST).containsExactly(5, 6, 7);
	}

	@Test
	@SuppressWarnings("unchecked")
	void projectionWithIterable() {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createIterable());
		assertThat(expression.getValue(context)).asInstanceOf(LIST).containsExactly(5, 6, 7);
	}

	@Test
	void projectionWithArray() {
		Expression expression = new SpelExpressionParser().parseRaw("#testArray.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testArray", IntegerTestBean.createArray());
		Object value = expression.getValue(context);
		assertThat(value.getClass().isArray()).isTrue();
		TypedValue typedValue = new TypedValue(value);
		assertThat(typedValue.getTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Number.class);
		assertThat((Number[]) value).containsExactly(5, 5.9f, 7);
	}


	static class ListTestBean {

		private final List<Integer> integers = new ArrayList<>();

		ListTestBean() {
			for (int i = 0; i < 10; i++) {
				integers.add(i);
			}
		}

		public List<Integer> getIntegers() {
			return integers;
		}
	}


	static class SetTestBean {

		private final Set<Integer> integers = new LinkedHashSet<>();

		SetTestBean() {
			for (int i = 0; i < 10; i++) {
				integers.add(i);
			}
		}

		public Set<Integer> getIntegers() {
			return integers;
		}
	}


	static class IterableTestBean {

		private final Set<Integer> integers = new LinkedHashSet<>();

		IterableTestBean() {
			for (int i = 0; i < 10; i++) {
				integers.add(i);
			}
		}

		public Iterable<Integer> getIntegers() {
			return integers;
		}
	}


	static class ArrayTestBean {

		private final int[] ints = new int[10];

		private final Integer[] integers = new Integer[10];

		ArrayTestBean() {
			for (int i = 0; i < 10; i++) {
				ints[i] = i;
				integers[i] = i;
			}
		}

		public int[] getInts() {
			return ints;
		}

		public Integer[] getIntegers() {
			return integers;
		}
	}


	static class MapTestBean {

		private final Map<String, String> colors = new TreeMap<>();

		MapTestBean() {
			// colors.put("black", "schwarz");
			colors.put("red", "rot");
			colors.put("brown", "braun");
			colors.put("blue", "blau");
			colors.put("yellow", "gelb");
			colors.put("beige", "beige");
		}

		public Map<String, String> getColors() {
			return colors;
		}
	}


	static class IntegerTestBean {

		private final IntegerWrapper wrapper;

		IntegerTestBean(Number value) {
			this.wrapper = new IntegerWrapper(value);
		}

		public IntegerWrapper getWrapper() {
			return this.wrapper;
		}

		static List<IntegerTestBean> createList() {
			List<IntegerTestBean> list = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				list.add(new IntegerTestBean(i + 5));
			}
			return list;
		}

		static Set<IntegerTestBean> createSet() {
			Set<IntegerTestBean> set = new LinkedHashSet<>();
			for (int i = 0; i < 3; i++) {
				set.add(new IntegerTestBean(i + 5));
			}
			return set;
		}

		static Iterable<IntegerTestBean> createIterable() {
			final Set<IntegerTestBean> set = createSet();
			return set;
		}

		static IntegerTestBean[] createArray() {
			IntegerTestBean[] array = new IntegerTestBean[3];
			for (int i = 0; i < 3; i++) {
				if (i == 1) {
					array[i] = new IntegerTestBean(5.9f);
				}
				else {
					array[i] = new IntegerTestBean(i + 5);
				}
			}
			return array;
		}
	}


	static class IntegerWrapper {

		private final Number value;

		IntegerWrapper(Number value) {
			this.value = value;
		}

		public Number getValue() {
			return this.value;
		}
	}

}
