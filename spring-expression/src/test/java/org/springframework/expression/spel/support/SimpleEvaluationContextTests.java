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

package org.springframework.expression.spel.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.spel.CompilableMapAccessor;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link SimpleEvaluationContext}.
 *
 * <p>Some of the use cases in this test class are duplicated elsewhere within the test
 * suite; however, we include them here to consistently focus on related features in this
 * test class.
 *
 * @author Sam Brannen
 */
class SimpleEvaluationContextTests {

	private static final IndexAccessor colorsIndexAccessor =
			new ReflectiveIndexAccessor(Colors.class, int.class, "get", "set");

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final Model model = new Model();


	@Test
	void forReadWriteDataBinding() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding()
				.withIndexAccessors(colorsIndexAccessor)
				.build();

		assertReadWriteMode(context);
	}

	@Test
	void forReadOnlyDataBinding() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
				.withIndexAccessors(colorsIndexAccessor)
				.build();

		assertCommonReadOnlyModeBehavior(context);

		// WRITE -- via assignment operator

		// Variable
		assertAssignmentDisabled(context, "#myVar = 'rejected'");

		// Property
		assertAssignmentDisabled(context, "name = 'rejected'");
		assertIncrementDisabled(context, "count++");
		assertIncrementDisabled(context, "++count");
		assertDecrementDisabled(context, "count--");
		assertDecrementDisabled(context, "--count");

		// Array Index
		assertAssignmentDisabled(context, "array[0] = 'rejected'");
		assertIncrementDisabled(context, "numbers[0]++");
		assertIncrementDisabled(context, "++numbers[0]");
		assertDecrementDisabled(context, "numbers[0]--");
		assertDecrementDisabled(context, "--numbers[0]");

		// List Index
		assertAssignmentDisabled(context, "list[0] = 'rejected'");

		// Map Index -- key as String
		assertAssignmentDisabled(context, "map['red'] = 'rejected'");

		// Map Index -- key as pseudo property name
		assertAssignmentDisabled(context, "map[yellow] = 'rejected'");

		// String Index
		assertAssignmentDisabled(context, "name[0] = 'rejected'");

		// Object Index
		assertAssignmentDisabled(context, "['name'] = 'rejected'");

		// Custom Index
		assertAssignmentDisabled(context, "colors[4] = 'rejected'");
	}

	@Test
	void forPropertyAccessorsInReadWriteMode() {
		SimpleEvaluationContext context = SimpleEvaluationContext
				.forPropertyAccessors(new CompilableMapAccessor(), DataBindingPropertyAccessor.forReadWriteAccess())
				.withIndexAccessors(colorsIndexAccessor)
				.build();

		assertReadWriteMode(context);

		// Map -- with key as property name supported by CompilableMapAccessor

		Expression expression;
		expression = parser.parseExpression("map.yellow");
		expression.setValue(context, model, "pineapple");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("pineapple");

		expression = parser.parseExpression("map.yellow = 'banana'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("banana");
		expression = parser.parseExpression("map.yellow");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("banana");
	}

	/**
	 * We call this "mixed" read-only mode, because write access via PropertyAccessors is
	 * disabled, but write access via the Indexer is not disabled.
	 */
	@Test
	void forPropertyAccessorsInMixedReadOnlyMode() {
		SimpleEvaluationContext context = SimpleEvaluationContext
				.forPropertyAccessors(new CompilableMapAccessor(true), DataBindingPropertyAccessor.forReadOnlyAccess())
				.withIndexAccessors(colorsIndexAccessor)
				.build();

		assertCommonReadOnlyModeBehavior(context);

		// Map -- with key as property name supported by CompilableMapAccessor with allowWrite = true.

		Expression expression;
		expression = parser.parseExpression("map.yellow");
		expression.setValue(context, model, "pineapple");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("pineapple");

		expression = parser.parseExpression("map.yellow = 'banana'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("banana");
		expression = parser.parseExpression("map.yellow");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("banana");

		// WRITE -- via assignment operator

		// Variable
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("#myVar = 'rejected'").getValue(context, model))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED));

		// Property
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("name = 'rejected'").getValue(context, model))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE));

		// Array Index
		expression = parser.parseExpression("array[0] = 'quux'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("quux");
		assertThat(model.array).containsExactly("quux");

		// List Index
		expression = parser.parseExpression("list[0] = 'elephant'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("elephant");
		assertThat(model.list).containsExactly("elephant");

		// Map Index -- key as String
		expression = parser.parseExpression("map['red'] = 'strawberry'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("strawberry");
		assertThat(model.map).containsOnly(entry("red", "strawberry"), entry("yellow", "banana"));

		// Map Index -- key as pseudo property name
		expression = parser.parseExpression("map[yellow] = 'star fruit'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("star fruit");
		assertThat(model.map).containsOnly(entry("red", "strawberry"), entry("yellow", "star fruit"));

		// String Index
		// The Indexer does not support writes when indexing into a String.
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("name[0] = 'rejected'").getValue(context, model))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE));

		// Object Index
		// Although this goes through the Indexer, the PropertyAccessorValueRef actually uses
		// registered PropertyAccessors to perform the write access, and that is disabled here.
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("['name'] = 'rejected'").getValue(context, model))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE));

		// Custom Index
		expression = parser.parseExpression("colors[5] = 'indigo'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("indigo");
		assertThat(model.colors.get(5)).isEqualTo("indigo");

		// WRITE -- via increment and decrement operators

		assertIncrementAndDecrementWritesForIndexedStructures(context);
	}

	@Test
	void forPropertyAccessorsWithAssignmentDisabled() {
		SimpleEvaluationContext context = SimpleEvaluationContext
				.forPropertyAccessors(new CompilableMapAccessor(false), DataBindingPropertyAccessor.forReadOnlyAccess())
				.withIndexAccessors(colorsIndexAccessor)
				.withAssignmentDisabled()
				.build();

		assertCommonReadOnlyModeBehavior(context);

		// WRITE -- via assignment operator

		// Variable
		assertAssignmentDisabled(context, "#myVar = 'rejected'");

		// Property
		assertAssignmentDisabled(context, "name = 'rejected'");
		assertAssignmentDisabled(context, "map.yellow = 'rejected'");
		assertIncrementDisabled(context, "count++");
		assertIncrementDisabled(context, "++count");
		assertDecrementDisabled(context, "count--");
		assertDecrementDisabled(context, "--count");

		// Array Index
		assertAssignmentDisabled(context, "array[0] = 'rejected'");
		assertIncrementDisabled(context, "numbers[0]++");
		assertIncrementDisabled(context, "++numbers[0]");
		assertDecrementDisabled(context, "numbers[0]--");
		assertDecrementDisabled(context, "--numbers[0]");

		// List Index
		assertAssignmentDisabled(context, "list[0] = 'rejected'");

		// Map Index -- key as String
		assertAssignmentDisabled(context, "map['red'] = 'rejected'");

		// Map Index -- key as pseudo property name
		assertAssignmentDisabled(context, "map[yellow] = 'rejected'");

		// String Index
		assertAssignmentDisabled(context, "name[0] = 'rejected'");

		// Object Index
		assertAssignmentDisabled(context, "['name'] = 'rejected'");
	}


	private void assertReadWriteMode(SimpleEvaluationContext context) {
		// Variables can always be set programmatically within an EvaluationContext.
		context.setVariable("myVar", "enigma");

		// WRITE -- via setValue()

		// Property
		parser.parseExpression("name").setValue(context, model, "test");
		assertThat(model.name).isEqualTo("test");
		parser.parseExpression("count").setValue(context, model, 42);
		assertThat(model.count).isEqualTo(42);

		// Array Index
		parser.parseExpression("array[0]").setValue(context, model, "foo");
		assertThat(model.array).containsExactly("foo");

		// List Index
		parser.parseExpression("list[0]").setValue(context, model, "cat");
		assertThat(model.list).containsExactly("cat");

		// Map Index -- key as String
		parser.parseExpression("map['red']").setValue(context, model, "cherry");
		assertThat(model.map).containsOnly(entry("red", "cherry"), entry("yellow", "replace me"));

		// Map Index -- key as pseudo property name
		parser.parseExpression("map[yellow]").setValue(context, model, "lemon");
		assertThat(model.map).containsOnly(entry("red", "cherry"), entry("yellow", "lemon"));

		// Custom Index
		parser.parseExpression("colors[4]").setValue(context, model, "purple");
		assertThat(model.colors.get(4)).isEqualTo("purple");

		// READ
		assertReadAccess(context);

		// WRITE -- via assignment operator

		// Variable assignment is always disabled in a SimpleEvaluationContext.
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("#myVar = 'rejected'").getValue(context, model))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED));

		Expression expression;

		// Property
		expression = parser.parseExpression("name = 'changed'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("changed");
		expression = parser.parseExpression("name");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("changed");

		// Array Index
		expression = parser.parseExpression("array[0] = 'bar'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("bar");
		expression = parser.parseExpression("array[0]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("bar");

		// List Index
		expression = parser.parseExpression("list[0] = 'dog'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("dog");
		expression = parser.parseExpression("list[0]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("dog");

		// Map Index -- key as String
		expression = parser.parseExpression("map['red'] = 'strawberry'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("strawberry");
		expression = parser.parseExpression("map['red']");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("strawberry");

		// Map Index -- key as pseudo property name
		expression = parser.parseExpression("map[yellow] = 'banana'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("banana");
		expression = parser.parseExpression("map[yellow]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("banana");

		// String Index
		// The Indexer does not support writes when indexing into a String.
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("name[0] = 'rejected'").getValue(context, model))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE));

		// Object Index
		expression = parser.parseExpression("['name'] = 'new name'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("new name");
		expression = parser.parseExpression("['name']");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("new name");

		// Custom Index
		expression = parser.parseExpression("colors[5] = 'indigo'");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("indigo");
		expression = parser.parseExpression("colors[5]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("indigo");

		// WRITE -- via increment and decrement operators

		assertIncrementAndDecrementWritesForProperties(context);
		assertIncrementAndDecrementWritesForIndexedStructures(context);
	}

	private void assertCommonReadOnlyModeBehavior(SimpleEvaluationContext context) {
		// Variables can always be set programmatically within an EvaluationContext.
		context.setVariable("myVar", "enigma");

		// WRITE -- via setValue()

		// Note: forReadOnlyDataBinding() disables programmatic writes via setValue() for
		// properties but allows programmatic writes via setValue() for indexed structures.

		// Property
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("name").setValue(context, model, "test"))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE));
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("count").setValue(context, model, 42))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE));

		// Array Index
		parser.parseExpression("array[0]").setValue(context, model, "foo");
		assertThat(model.array).containsExactly("foo");

		// List Index
		parser.parseExpression("list[0]").setValue(context, model, "cat");
		assertThat(model.list).containsExactly("cat");

		// Map Index -- key as String
		parser.parseExpression("map['red']").setValue(context, model, "cherry");
		assertThat(model.map).containsOnly(entry("red", "cherry"), entry("yellow", "replace me"));

		// Map Index -- key as pseudo property name
		parser.parseExpression("map[yellow]").setValue(context, model, "lemon");
		assertThat(model.map).containsOnly(entry("red", "cherry"), entry("yellow", "lemon"));

		// Custom Index
		parser.parseExpression("colors[4]").setValue(context, model, "purple");
		assertThat(model.colors.get(4)).isEqualTo("purple");

		// Since the setValue() attempts for "name" and "count" failed above, we have to set
		// them directly for assertReadAccess().
		model.name = "test";
		model.count = 42;

		// READ
		assertReadAccess(context);
	}

	private void assertReadAccess(SimpleEvaluationContext context) {
		Expression expression;

		// Variable
		expression = parser.parseExpression("#myVar");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("enigma");

		// Property
		expression = parser.parseExpression("name");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("test");
		expression = parser.parseExpression("count");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(42);

		// Array Index
		expression = parser.parseExpression("array[0]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("foo");

		// List Index
		expression = parser.parseExpression("list[0]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("cat");

		// Map Index -- key as String
		expression = parser.parseExpression("map['red']");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("cherry");

		// Map Index -- key as pseudo property name
		expression = parser.parseExpression("map[yellow]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("lemon");

		// String Index
		expression = parser.parseExpression("name[0]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("t");

		// Object Index
		expression = parser.parseExpression("['name']");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("test");

		// Custom Index
		expression = parser.parseExpression("colors[4]");
		assertThat(expression.getValue(context, model, String.class)).isEqualTo("purple");
	}

	private void assertIncrementAndDecrementWritesForProperties(SimpleEvaluationContext context) {
		Expression expression;
		expression = parser.parseExpression("count++");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(42);
		expression = parser.parseExpression("count");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(43);

		expression = parser.parseExpression("++count");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(44);
		expression = parser.parseExpression("count");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(44);

		expression = parser.parseExpression("count--");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(44);
		expression = parser.parseExpression("count");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(43);

		expression = parser.parseExpression("--count");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(42);
		expression = parser.parseExpression("count");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(42);
	}

	private void assertIncrementAndDecrementWritesForIndexedStructures(SimpleEvaluationContext context) {
		Expression expression;
		expression = parser.parseExpression("numbers[0]++");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(99);
		expression = parser.parseExpression("numbers[0]");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(100);

		expression = parser.parseExpression("++numbers[0]");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(101);
		expression = parser.parseExpression("numbers[0]");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(101);

		expression = parser.parseExpression("numbers[0]--");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(101);
		expression = parser.parseExpression("numbers[0]");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(100);

		expression = parser.parseExpression("--numbers[0]");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(99);
		expression = parser.parseExpression("numbers[0]");
		assertThat(expression.getValue(context, model, Integer.class)).isEqualTo(99);
	}

	private ThrowableTypeAssert<SpelEvaluationException> assertThatSpelEvaluationException() {
		return assertThatExceptionOfType(SpelEvaluationException.class);
	}

	private void assertAssignmentDisabled(SimpleEvaluationContext context, String expression) {
		assertEvaluationException(context, expression, SpelMessage.NOT_ASSIGNABLE);
	}

	private void assertIncrementDisabled(SimpleEvaluationContext context, String expression) {
		assertEvaluationException(context, expression, SpelMessage.OPERAND_NOT_INCREMENTABLE);
	}

	private void assertDecrementDisabled(SimpleEvaluationContext context, String expression) {
		assertEvaluationException(context, expression, SpelMessage.OPERAND_NOT_DECREMENTABLE);
	}

	private void assertEvaluationException(SimpleEvaluationContext context, String expression, SpelMessage spelMessage) {
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression(expression).getValue(context, model))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(spelMessage));
	}


	static class Model {

		private String name = "replace me";
		private int count = 0;
		private final String[] array = {"replace me"};
		private final int[] numbers = {99};
		private final List<String> list = new ArrayList<>();
		private final Map<String, String> map = new HashMap<>();
		private final Colors colors = new Colors();

		Model() {
			this.list.add("replace me");
			this.map.put("red", "replace me");
			this.map.put("yellow", "replace me");
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getCount() {
			return this.count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public String[] getArray() {
			return this.array;
		}

		public int[] getNumbers() {
			return this.numbers;
		}

		public List<String> getList() {
			return this.list;
		}

		public Map<String, String> getMap() {
			return this.map;
		}

		public Colors getColors() {
			return this.colors;
		}
	}

	static class Colors {

		private final Map<Integer, String> map = new HashMap<>();

		{
			this.map.put(1, "red");
			this.map.put(2, "green");
			this.map.put(3, "blue");
		}

		public String get(int index) {
			if (!this.map.containsKey(index)) {
				throw new IndexOutOfBoundsException("No color for index " + index);
			}
			return this.map.get(index);
		}

		public void set(int index, String color) {
			this.map.put(index, color);
		}

	}

}
