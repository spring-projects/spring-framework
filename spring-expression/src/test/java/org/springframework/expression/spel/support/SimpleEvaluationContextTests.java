/*
 * Copyright 2002-present the original author or authors.
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

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.core.SpringProperties;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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

	private static final List<String> conversions = new ArrayList<>();

	private static final IndexAccessor colorsIndexAccessor =
			new ReflectiveIndexAccessor(Colors.class, int.class, "get", "set");

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final Model model = new Model();

	private final Catalog catalog = new Catalog();


	@BeforeEach
	void clearConversions() {
		conversions.clear();
	}

	@Test
	void compilationNotSupportedByDefault() {
		assertThat(SimpleEvaluationContext.forReadOnlyDataBinding().build().isCompilationSupported()).isFalse();
		assertThat(SimpleEvaluationContext.forReadWriteDataBinding().build().isCompilationSupported()).isFalse();
		assertThat(SimpleEvaluationContext.forPropertyAccessors(DataBindingPropertyAccessor.forReadOnlyAccess())
				.build().isCompilationSupported()).isFalse();
	}

	@Test
	void compilationSupportedViaBuilder() {
		assertThat(SimpleEvaluationContext.forReadOnlyDataBinding()
				.withCompilationSupported().build().isCompilationSupported()).isTrue();
		assertThat(SimpleEvaluationContext.forReadWriteDataBinding()
				.withCompilationSupported().build().isCompilationSupported()).isTrue();
		assertThat(SimpleEvaluationContext.forPropertyAccessors(DataBindingPropertyAccessor.forReadOnlyAccess())
				.withCompilationSupported().build().isCompilationSupported()).isTrue();
	}

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
				.forPropertyAccessors(new MapAccessor(), DataBindingPropertyAccessor.forReadWriteAccess())
				.withIndexAccessors(colorsIndexAccessor)
				.build();

		assertReadWriteMode(context);

		// Map -- with key as property name supported by MapAccessor

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
				.forPropertyAccessors(new MapAccessor(true), DataBindingPropertyAccessor.forReadOnlyAccess())
				.withIndexAccessors(colorsIndexAccessor)
				.build();

		assertCommonReadOnlyModeBehavior(context);

		// Map -- with key as property name supported by MapAccessor with allowWrite = true.

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
				.forPropertyAccessors(new MapAccessor(false), DataBindingPropertyAccessor.forReadOnlyAccess())
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

	@Test
	void mapKeyConversionViaConstructorNotSupportedByDefault() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("skus['ABC-123']").getValue(context, catalog))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.TYPE_CONVERSION_ERROR))
				.withRootCauseInstanceOf(ConverterNotFoundException.class);
		assertThat(conversions).isEmpty();
	}

	@Test
	void propertyConversionViaConstructorNotSupportedByDefault() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();

		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("sku = 'ABC-123'").getValue(context, catalog))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE))
				.withRootCauseInstanceOf(ConverterNotFoundException.class);
		assertThat(conversions).isEmpty();
	}

	@Test
	void methodArgumentConversionViaConstructorNotSupportedByDefault() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
				.withInstanceMethods()
				.build();

		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("contains('ABC-123')").getValue(context, catalog))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.METHOD_NOT_FOUND));
		assertThat(conversions).isEmpty();
	}

	/**
	 * Each conversion is not supported with the default {@code ConversionService}
	 * but is supported with a standard {@link DefaultConversionService}.
	 */
	@ParameterizedTest
	@CsvSource(quoteCharacter = '"', delimiterString = "->", textBlock = """
		skus['ABC-123']     -> new Sku(ABC-123)
		sku = 'ABC-123'     -> new Sku(ABC-123)
		contains('ABC-123') -> new Sku(ABC-123)
		isbns['ABC-123']    -> Isbn.valueOf(ABC-123)
		products['ABC-123'] -> Product.findProduct(ABC-123)
		listing = draft     -> Draft.toListing()
		""")
	void reflectiveConversionNotSupportedByDefault(String expression, String conversion) {
		SimpleEvaluationContext defaultContext = SimpleEvaluationContext.forReadWriteDataBinding()
				.withInstanceMethods()
				.build();

		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression(expression).getValue(defaultContext, catalog));
		assertThat(conversions).isEmpty();

		SimpleEvaluationContext customContext = SimpleEvaluationContext.forReadWriteDataBinding()
				.withInstanceMethods()
				.withConversionService(new DefaultConversionService())
				.build();

		parser.parseExpression(expression).getValue(customContext, catalog);
		assertThat(conversions).containsExactly(conversion);
	}

	@Test
	void customTypeConverterIsUsedAsIs() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
				.withTypeConverter(new StandardTypeConverter())
				.build();

		assertThat(parser.parseExpression("skus['ABC-123']").getValue(context, catalog)).isNull();
		assertThat(conversions).containsExactly("new Sku(ABC-123)");
	}

	@Test
	void commonConversionsSupportedByDefault() {
		SimpleEvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
		ConversionModel conversionModel = new ConversionModel();

		parser.parseExpression("number = '42'").getValue(context, conversionModel);
		assertThat(conversionModel.getNumber()).isEqualTo(42);

		parser.parseExpression("flag = 'true'").getValue(context, conversionModel);
		assertThat(conversionModel.getFlag()).isTrue();

		parser.parseExpression("booleans[0]").setValue(context, conversionModel, "false");
		assertThat(conversionModel.getBooleans()).containsExactly(false);

		parser.parseExpression("color = 'BLUE'").getValue(context, conversionModel);
		assertThat(conversionModel.getColor()).isEqualTo(Color.BLUE);

		UUID uuid = UUID.randomUUID();
		parser.parseExpression("uuid = '" + uuid + "'").getValue(context, conversionModel);
		assertThat(conversionModel.getUuid()).isEqualTo(uuid);

		// text and builder are both properties on the root context object.
		parser.parseExpression("text = builder").getValue(context, conversionModel);
		assertThat(conversionModel.getText()).isEqualTo("built");

		assertThat(parser.parseExpression("numberKeys['1']").getValue(context, conversionModel)).isEqualTo("one");
		assertThat(parser.parseExpression("colorKeys['RED']").getValue(context, conversionModel)).isEqualTo("red");
		assertThat(parser.parseExpression("colorKeys[RED]").getValue(context, conversionModel)).isEqualTo("red");
	}

	@Test
	void withConverter() {
		ConversionModel conversionModel = new ConversionModel();
		SimpleEvaluationContext defaultContext = SimpleEvaluationContext.forReadWriteDataBinding().build();

		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("zoneId = 'Europe/Paris'").getValue(defaultContext, conversionModel))
				.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE))
				.withRootCauseInstanceOf(ConverterNotFoundException.class);

		SimpleEvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding()
				.withConverter(String.class, ZoneId.class, ZoneId::of)
				.build();

		parser.parseExpression("zoneId = 'Europe/Paris'").getValue(context, conversionModel);
		assertThat(conversionModel.getZoneId()).isEqualTo(ZoneId.of("Europe/Paris"));

		// Common conversions are still supported.
		parser.parseExpression("number = '42'").getValue(context, conversionModel);
		assertThat(conversionModel.getNumber()).isEqualTo(42);

		// Reflective conversions are still not supported.
		assertThatSpelEvaluationException()
				.isThrownBy(() -> parser.parseExpression("skus['ABC-123']").getValue(context, catalog))
				.withRootCauseInstanceOf(ConverterNotFoundException.class);
		assertThat(conversions).isEmpty();
	}

	@Test
	void withConverterCannotBeCombinedWithCustomConversionServiceOrTypeConverter() {
		assertThatIllegalStateException().isThrownBy(() -> SimpleEvaluationContext.forReadOnlyDataBinding()
				.withConverter(String.class, ZoneId.class, ZoneId::of)
				.withConversionService(new DefaultConversionService())
				.build());

		assertThatIllegalStateException().isThrownBy(() -> SimpleEvaluationContext.forReadOnlyDataBinding()
				.withTypeConverter(new StandardTypeConverter())
				.withConverter(String.class, ZoneId.class, ZoneId::of)
				.build());
	}

	@Test
	void reflectiveConversionProperty() {
		SpringProperties.setFlag(SimpleEvaluationContext.REFLECTIVE_CONVERSION_PROPERTY_NAME);
		try {
			SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

			assertThat(parser.parseExpression("skus['ABC-123']").getValue(context, catalog)).isNull();
			assertThat(conversions).containsExactly("new Sku(ABC-123)");

			conversions.clear();
			ConversionModel conversionModel = new ConversionModel();
			context = SimpleEvaluationContext.forReadWriteDataBinding()
					.withConverter(String.class, ZoneId.class, ZoneId::of)
					.build();

			parser.parseExpression("zoneId = 'Europe/Paris'").getValue(context, conversionModel);
			assertThat(conversionModel.getZoneId()).isEqualTo(ZoneId.of("Europe/Paris"));
			assertThat(parser.parseExpression("skus['ABC-123']").getValue(context, catalog)).isNull();
			assertThat(conversions).containsExactly("new Sku(ABC-123)");
		}
		finally {
			SpringProperties.setProperty(SimpleEvaluationContext.REFLECTIVE_CONVERSION_PROPERTY_NAME, null);
		}
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

	static class Catalog {

		private final Map<Sku, String> skus = new HashMap<>();
		private final Map<Isbn, String> isbns = new HashMap<>();
		private final Map<Product, String> products = new HashMap<>();
		private Sku sku;
		private final Draft draft = new Draft();
		private Listing listing;

		public Map<Sku, String> getSkus() {
			return this.skus;
		}

		public Map<Isbn, String> getIsbns() {
			return this.isbns;
		}

		public Map<Product, String> getProducts() {
			return this.products;
		}

		public Sku getSku() {
			return this.sku;
		}

		public void setSku(Sku sku) {
			this.sku = sku;
		}

		public Draft getDraft() {
			return this.draft;
		}

		public Listing getListing() {
			return this.listing;
		}

		public void setListing(Listing listing) {
			this.listing = listing;
		}

		public boolean contains(Sku sku) {
			return this.skus.containsKey(sku);
		}
	}

	static class Sku {

		public Sku(String code) {
			conversions.add("new Sku(" + code + ")");
		}
	}

	static class Isbn {

		private Isbn() {
		}

		public static Isbn valueOf(String code) {
			conversions.add("Isbn.valueOf(" + code + ")");
			return new Isbn();
		}
	}

	// Must be public, since IdToEntityConverter does not make the finder method accessible.
	public static class Product {

		private Product() {
		}

		public static Product findProduct(String id) {
			conversions.add("Product.findProduct(" + id + ")");
			return new Product();
		}
	}

	static class Draft {

		public Listing toListing() {
			conversions.add("Draft.toListing()");
			return new Listing();
		}
	}

	static class Listing {
	}

	enum Color {
		RED, BLUE
	}

	static class ConversionModel {

		private Integer number;
		private Boolean flag;
		private final List<Boolean> booleans = new ArrayList<>();
		private Color color;
		private UUID uuid;
		private ZoneId zoneId;
		private String text;
		private final StringBuilder builder = new StringBuilder("built");
		private final Map<Integer, String> numberKeys = new HashMap<>();
		private final Map<Color, String> colorKeys = new HashMap<>();

		ConversionModel() {
			this.booleans.add(true);
			this.numberKeys.put(1, "one");
			this.colorKeys.put(Color.RED, "red");
		}

		public Integer getNumber() {
			return this.number;
		}

		public void setNumber(Integer number) {
			this.number = number;
		}

		public Boolean getFlag() {
			return this.flag;
		}

		public void setFlag(Boolean flag) {
			this.flag = flag;
		}

		public List<Boolean> getBooleans() {
			return this.booleans;
		}

		public Color getColor() {
			return this.color;
		}

		public void setColor(Color color) {
			this.color = color;
		}

		public UUID getUuid() {
			return this.uuid;
		}

		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}

		public ZoneId getZoneId() {
			return this.zoneId;
		}

		public void setZoneId(ZoneId zoneId) {
			this.zoneId = zoneId;
		}

		public String getText() {
			return this.text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public StringBuilder getBuilder() {
			return this.builder;
		}

		public Map<Integer, String> getNumberKeys() {
			return this.numberKeys;
		}

		public Map<Color, String> getColorKeys() {
			return this.colorKeys;
		}
	}

}
