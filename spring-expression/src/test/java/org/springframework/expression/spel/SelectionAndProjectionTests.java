/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
class SelectionAndProjectionTests {

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(List.class);
		List<Integer> list = (List<Integer>) value;
		assertThat(list).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(0);
	}

	@Test
	void selectLastItemInList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(List.class);
		List<Integer> list = (List<Integer>) value;
		assertThat(list).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(0);
	}

	@Test
	void selectLastItemInSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithIterable() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new IterableTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(List.class);
		List<Integer> list = (List<Integer>) value;
		assertThat(list).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectionWithArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value.getClass().isArray()).isTrue();
		TypedValue typedValue = new TypedValue(value);
		assertThat(typedValue.getTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		Integer[] array = (Integer[]) value;
		assertThat(array).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(0);
	}

	@Test
	void selectLastItemInArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(4);
	}

	@Test
	void selectionWithPrimitiveArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("ints.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value.getClass().isArray()).isTrue();
		TypedValue typedValue = new TypedValue(value);
		assertThat(typedValue.getTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		Integer[] array = (Integer[]) value;
		assertThat(array).containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void selectFirstItemInPrimitiveArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("ints.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(0);
	}

	@Test
	void selectLastItemInPrimitiveArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("ints.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(Integer.class);
		assertThat(value).isEqualTo(4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectionWithMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("colors.?[key.startsWith('b')]");

		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertThat(colorsMap).containsOnlyKeys("beige", "blue", "brown");
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectFirstItemInMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("colors.^[key.startsWith('b')]");
		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertThat(colorsMap.size()).isEqualTo(1);
		assertThat(colorsMap.keySet().iterator().next()).isEqualTo("beige");
	}

	@Test
	@SuppressWarnings("unchecked")
	void selectLastItemInMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("colors.$[key.startsWith('b')]");
		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertThat(colorsMap.size()).isEqualTo(1);
		assertThat(colorsMap.keySet().iterator().next()).isEqualTo("brown");
	}

	@Test
	@SuppressWarnings("unchecked")
	void projectionWithList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createList());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(List.class);
		List<Integer> list = (List<Integer>) value;
		assertThat(list).containsExactly(5, 6, 7);
	}

	@Test
	@SuppressWarnings("unchecked")
	void projectionWithSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createSet());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(List.class);
		List<Integer> list = (List<Integer>) value;
		assertThat(list).containsExactly(5, 6, 7);
	}

	@Test
	@SuppressWarnings("unchecked")
	void projectionWithIterable() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createIterable());
		Object value = expression.getValue(context);
		assertThat(value).isInstanceOf(List.class);
		List<Integer> list = (List<Integer>) value;
		assertThat(list).containsExactly(5, 6, 7);
	}

	@Test
	void projectionWithArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("#testArray.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testArray", IntegerTestBean.createArray());
		Object value = expression.getValue(context);
		assertThat(value.getClass().isArray()).isTrue();
		TypedValue typedValue = new TypedValue(value);
		assertThat(typedValue.getTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Number.class);
		Number[] array = (Number[]) value;
		assertThat(array).containsExactly(5, 5.9f, 7);
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
			return integers::iterator;
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
			return set::iterator;
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
