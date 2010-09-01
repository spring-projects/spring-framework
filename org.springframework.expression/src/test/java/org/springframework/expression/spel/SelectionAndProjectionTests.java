/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class SelectionAndProjectionTests {

	@Test
	public void selectionWithList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof List);
		List list = (List) value;
		assertEquals(5, list.size());
		assertEquals(0, list.get(0));
		assertEquals(1, list.get(1));
		assertEquals(2, list.get(2));
		assertEquals(3, list.get(3));
		assertEquals(4, list.get(4));
	}

	@Test
	public void selectFirstItemInList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(0, value);
	}

	@Test
	public void selectLastItemInList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ListTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(4, value);
	}

	@Test
	public void selectionWithSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof List);
		List list = (List) value;
		assertEquals(5, list.size());
		assertEquals(0, list.get(0));
		assertEquals(1, list.get(1));
		assertEquals(2, list.get(2));
		assertEquals(3, list.get(3));
		assertEquals(4, list.get(4));
	}

	@Test
	public void selectFirstItemInSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(0, value);
	}

	@Test
	public void selectLastItemInSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new SetTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(4, value);
	}

	@Test
	public void selectionWithArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertTrue(value.getClass().isArray());
		TypedValue typedValue = new TypedValue(value);
		assertEquals(Integer.class, typedValue.getTypeDescriptor().getElementType());
		Integer[] array = (Integer[]) value;
		assertEquals(5, array.length);
		assertEquals(new Integer(0), array[0]);
		assertEquals(new Integer(1), array[1]);
		assertEquals(new Integer(2), array[2]);
		assertEquals(new Integer(3), array[3]);
		assertEquals(new Integer(4), array[4]);
	}

	@Test
	public void selectFirstItemInArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(0, value);
	}

	@Test
	public void selectLastItemInArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("integers.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(4, value);
	}

	@Test
	public void selectionWithPrimitiveArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("ints.?[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertTrue(value.getClass().isArray());
		TypedValue typedValue = new TypedValue(value);
		assertEquals(Integer.class, typedValue.getTypeDescriptor().getElementType());
		Integer[] array = (Integer[]) value;
		assertEquals(5, array.length);
		assertEquals(new Integer(0), array[0]);
		assertEquals(new Integer(1), array[1]);
		assertEquals(new Integer(2), array[2]);
		assertEquals(new Integer(3), array[3]);
		assertEquals(new Integer(4), array[4]);
	}

	@Test
	public void selectFirstItemInPrimitiveArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("ints.^[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(0, value);
	}

	@Test
	public void selectLastItemInPrimitiveArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("ints.$[#this<5]");
		EvaluationContext context = new StandardEvaluationContext(new ArrayTestBean());
		Object value = expression.getValue(context);
		assertTrue(value instanceof Integer);
		assertEquals(4, value);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void selectionWithMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("colors.?[key.startsWith('b')]");

		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertEquals(3, colorsMap.size());
		assertTrue(colorsMap.containsKey("beige"));
		assertTrue(colorsMap.containsKey("blue"));
		assertTrue(colorsMap.containsKey("brown"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void selectFirstItemInMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("colors.^[key.startsWith('b')]");
		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertEquals(1, colorsMap.size());
		assertEquals("beige", colorsMap.keySet().iterator().next());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void selectLastItemInMap() {
		EvaluationContext context = new StandardEvaluationContext(new MapTestBean());
		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("colors.$[key.startsWith('b')]");
		Map<String, String> colorsMap = (Map<String, String>) exp.getValue(context);
		assertEquals(1, colorsMap.size());
		assertEquals("brown", colorsMap.keySet().iterator().next());
	}

	@Test
	public void projectionWithList() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createList());
		Object value = expression.getValue(context);
		assertTrue(value instanceof List);
		List list = (List) value;
		assertEquals(3, list.size());
		assertEquals(5, list.get(0));
		assertEquals(6, list.get(1));
		assertEquals(7, list.get(2));
	}

	@Test
	public void projectionWithSet() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("#testList.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testList", IntegerTestBean.createSet());
		Object value = expression.getValue(context);
		assertTrue(value instanceof List);
		List list = (List) value;
		assertEquals(3, list.size());
		assertEquals(5, list.get(0));
		assertEquals(6, list.get(1));
		assertEquals(7, list.get(2));
	}

	@Test
	public void projectionWithArray() throws Exception {
		Expression expression = new SpelExpressionParser().parseRaw("#testArray.![wrapper.value]");
		EvaluationContext context = new StandardEvaluationContext();
		context.setVariable("testArray", IntegerTestBean.createArray());
		Object value = expression.getValue(context);
		assertTrue(value.getClass().isArray());
		TypedValue typedValue = new TypedValue(value);
		assertEquals(Number.class, typedValue.getTypeDescriptor().getElementType());
		Number[] array = (Number[]) value;
		assertEquals(3, array.length);
		assertEquals(new Integer(5), array[0]);
		assertEquals(5.9f, array[1]);
		assertEquals(new Integer(7), array[2]);
	}

	static class MapTestBean {

		private final Map<String, String> colors = new TreeMap<String, String>();

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

	static class ListTestBean {

		private final List<Integer> integers = new ArrayList<Integer>();

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

		private final Set<Integer> integers = new LinkedHashSet<Integer>();

		SetTestBean() {
			for (int i = 0; i < 10; i++) {
				integers.add(i);
			}
		}

		public Set<Integer> getIntegers() {
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

	static class IntegerTestBean {

		private final IntegerWrapper wrapper;

		IntegerTestBean(Number value) {
			this.wrapper = new IntegerWrapper(value);
		}

		public IntegerWrapper getWrapper() {
			return this.wrapper;
		}

		static List<IntegerTestBean> createList() {
			List<IntegerTestBean> list = new ArrayList<IntegerTestBean>();
			for (int i = 0; i < 3; i++) {
				list.add(new IntegerTestBean(i + 5));
			}
			return list;
		}

		static Set<IntegerTestBean> createSet() {
			Set<IntegerTestBean> set = new LinkedHashSet<IntegerTestBean>();
			for (int i = 0; i < 3; i++) {
				set.add(new IntegerTestBean(i + 5));
			}
			return set;
		}

		static IntegerTestBean[] createArray() {
			IntegerTestBean[] array = new IntegerTestBean[3];
			for (int i = 0; i < 3; i++) {
				if (i == 1) {
					array[i] = new IntegerTestBean(5.9f);
				} else {
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
