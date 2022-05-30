/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("rawtypes")
class IndexingTests {

	@Test
	@SuppressWarnings("unchecked")
	void indexIntoGenericPropertyContainingMap() {
		Map<String, String> property = new HashMap<>();
		property.put("foo", "bar");
		this.property = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.HashMap<?, ?>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		assertThat(expression.getValue(this, Map.class)).isEqualTo(property);
		expression = parser.parseExpression("property['foo']");
		assertThat(expression.getValue(this)).isEqualTo("bar");
	}

	@FieldAnnotation
	public Object property;

	@Test
	@SuppressWarnings("unchecked")
	void indexIntoGenericPropertyContainingMapObject() {
		Map<String, Map<String, String>> property = new HashMap<>();
		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");
		property.put("property", map);
		SpelExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new MapAccessor());
		context.setRootObject(property);
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(context).toString()).isEqualTo("java.util.HashMap<?, ?>");
		assertThat(expression.getValue(context)).isEqualTo(map);
		assertThat(expression.getValue(context, Map.class)).isEqualTo(map);
		expression = parser.parseExpression("property['foo']");
		assertThat(expression.getValue(context)).isEqualTo("bar");
	}

	public static class MapAccessor implements PropertyAccessor {

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (((Map<?, ?>) target).containsKey(name));
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(((Map<?, ?>) target).get(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			((Map) target).put(name, newValue);
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return new Class<?>[] {Map.class};
		}

	}

	@Test
	void setGenericPropertyContainingMap() {
		Map<String, String> property = new HashMap<>();
		property.put("foo", "bar");
		this.property = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.HashMap<?, ?>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("property['foo']");
		assertThat(expression.getValue(this)).isEqualTo("bar");
		expression.setValue(this, "baz");
		assertThat(expression.getValue(this)).isEqualTo("baz");
	}

	@Test
	void setPropertyContainingMap() {
		Map<Integer, Integer> property = new HashMap<>();
		property.put(9, 3);
		this.parameterizedMap = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("parameterizedMap");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("java.util.HashMap<java.lang.Integer, java.lang.Integer>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("parameterizedMap['9']");
		assertThat(expression.getValue(this)).isEqualTo(3);
		expression.setValue(this, "37");
		assertThat(expression.getValue(this)).isEqualTo(37);
	}

	public Map<Integer, Integer> parameterizedMap;

	@Test
	void setPropertyContainingMapAutoGrow() {
		SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, false));
		Expression expression = parser.parseExpression("parameterizedMap");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("java.util.Map<java.lang.Integer, java.lang.Integer>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("parameterizedMap['9']");
		assertThat(expression.getValue(this)).isNull();
		expression.setValue(this, "37");
		assertThat(expression.getValue(this)).isEqualTo(37);
	}

	@Test
	void indexIntoGenericPropertyContainingList() {
		List<String> property = new ArrayList<>();
		property.add("bar");
		this.property = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.ArrayList<?>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("property[0]");
		assertThat(expression.getValue(this)).isEqualTo("bar");
	}

	@Test
	void setGenericPropertyContainingList() {
		List<Integer> property = new ArrayList<>();
		property.add(3);
		this.property = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.ArrayList<?>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("property[0]");
		assertThat(expression.getValue(this)).isEqualTo(3);
		expression.setValue(this, "4");
		assertThat(expression.getValue(this)).isEqualTo("4");
	}

	@Test
	void setGenericPropertyContainingListAutogrow() {
		List<Integer> property = new ArrayList<>();
		this.property = property;
		SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.ArrayList<?>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("property[0]");
		try {
			expression.setValue(this, "4");
		}
		catch (EvaluationException ex) {
			assertThat(ex.getMessage().startsWith("EL1053E")).isTrue();
		}
	}

	public List<BigDecimal> decimals;

	@Test
	void autoGrowListOfElementsWithoutDefaultConstructor() {
		this.decimals = new ArrayList<>();
		SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		parser.parseExpression("decimals[0]").setValue(this, "123.4");
		assertThat(decimals).containsExactly(BigDecimal.valueOf(123.4));
	}

	@Test
	void indexIntoPropertyContainingListContainingNullElement() {
		this.decimals = new ArrayList<>();
		this.decimals.add(null);
		this.decimals.add(BigDecimal.ONE);
		SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		parser.parseExpression("decimals[0]").setValue(this, "9876.5");
		assertThat(decimals).containsExactly(BigDecimal.valueOf(9876.5), BigDecimal.ONE);
	}

	@Test
	void indexIntoPropertyContainingList() {
		List<Integer> property = new ArrayList<>();
		property.add(3);
		this.parameterizedList = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("parameterizedList");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("java.util.ArrayList<java.lang.Integer>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("parameterizedList[0]");
		assertThat(expression.getValue(this)).isEqualTo(3);
	}

	public List<Integer> parameterizedList;

	@Test
	void indexIntoPropertyContainingListOfList() {
		List<List<Integer>> property = new ArrayList<>();
		property.add(Arrays.asList(3));
		this.parameterizedListOfList = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("parameterizedListOfList[0]");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("java.util.Arrays$ArrayList<java.lang.Integer>");
		assertThat(expression.getValue(this)).isEqualTo(property.get(0));
		expression = parser.parseExpression("parameterizedListOfList[0][0]");
		assertThat(expression.getValue(this)).isEqualTo(3);
	}

	public List<List<Integer>> parameterizedListOfList;

	@Test
	void setPropertyContainingList() {
		List<Integer> property = new ArrayList<>();
		property.add(3);
		this.parameterizedList = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("parameterizedList");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("java.util.ArrayList<java.lang.Integer>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("parameterizedList[0]");
		assertThat(expression.getValue(this)).isEqualTo(3);
		expression.setValue(this, "4");
		assertThat(expression.getValue(this)).isEqualTo(4);
	}

	@Test
	void indexIntoGenericPropertyContainingNullList() {
		SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
		SpelExpressionParser parser = new SpelExpressionParser(configuration);
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.lang.Object");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("property[0]");
		try {
			assertThat(expression.getValue(this)).isEqualTo("bar");
		}
		catch (EvaluationException ex) {
			assertThat(ex.getMessage().startsWith("EL1027E")).isTrue();
		}
	}

	@Test
	void indexIntoGenericPropertyContainingGrowingList() {
		List<String> property = new ArrayList<>();
		this.property = property;
		SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
		SpelExpressionParser parser = new SpelExpressionParser(configuration);
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.ArrayList<?>");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("property[0]");
		try {
			assertThat(expression.getValue(this)).isEqualTo("bar");
		}
		catch (EvaluationException ex) {
			assertThat(ex.getMessage().startsWith("EL1053E")).isTrue();
		}
	}

	@Test
	void indexIntoGenericPropertyContainingGrowingList2() {
		List<String> property2 = new ArrayList<>();
		this.property2 = property2;
		SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
		SpelExpressionParser parser = new SpelExpressionParser(configuration);
		Expression expression = parser.parseExpression("property2");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("java.util.ArrayList<?>");
		assertThat(expression.getValue(this)).isEqualTo(property2);
		expression = parser.parseExpression("property2[0]");
		try {
			assertThat(expression.getValue(this)).isEqualTo("bar");
		}
		catch (EvaluationException ex) {
			assertThat(ex.getMessage().startsWith("EL1053E")).isTrue();
		}
	}

	public List property2;

	@Test
	void indexIntoGenericPropertyContainingArray() {
		String[] property = new String[] { "bar" };
		this.property = property;
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("property");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.lang.String[]");
		assertThat(expression.getValue(this)).isEqualTo(property);
		expression = parser.parseExpression("property[0]");
		assertThat(expression.getValue(this)).isEqualTo("bar");
	}

	@Test
	void emptyList() {
		listOfScalarNotGeneric = new ArrayList();
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listOfScalarNotGeneric");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("java.util.ArrayList<?>");
		assertThat(expression.getValue(this, String.class)).isEqualTo("");
	}

	@SuppressWarnings("unchecked")
	@Test
	void resolveCollectionElementType() {
		listNotGeneric = new ArrayList(2);
		listNotGeneric.add(5);
		listNotGeneric.add(6);
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listNotGeneric");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.ArrayList<?>");
		assertThat(expression.getValue(this, String.class)).isEqualTo("5,6");
	}

	@Test
	void resolveCollectionElementTypeNull() {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listNotGeneric");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.List<?>");
	}

	@FieldAnnotation
	public List listNotGeneric;

	@Target({ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FieldAnnotation {

	}

	@SuppressWarnings("unchecked")
	@Test
	void resolveMapKeyValueTypes() {
		mapNotGeneric = new HashMap();
		mapNotGeneric.put("baseAmount", 3.11);
		mapNotGeneric.put("bonusAmount", 7.17);
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("mapNotGeneric");
		assertThat(expression.getValueTypeDescriptor(this).toString()).isEqualTo("@org.springframework.expression.spel.IndexingTests$FieldAnnotation java.util.HashMap<?, ?>");
	}

	@FieldAnnotation
	public Map mapNotGeneric;

	@SuppressWarnings("unchecked")
	@Test
	void testListOfScalar() {
		listOfScalarNotGeneric = new ArrayList(1);
		listOfScalarNotGeneric.add("5");
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listOfScalarNotGeneric[0]");
		assertThat(expression.getValue(this, Integer.class)).isEqualTo(Integer.valueOf(5));
	}

	public List listOfScalarNotGeneric;


	@SuppressWarnings("unchecked")
	@Test
	void testListsOfMap() {
		listOfMapsNotGeneric = new ArrayList();
		Map map = new HashMap();
		map.put("fruit", "apple");
		listOfMapsNotGeneric.add(map);
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("listOfMapsNotGeneric[0]['fruit']");
		assertThat(expression.getValue(this, String.class)).isEqualTo("apple");
	}

	public List listOfMapsNotGeneric;

}
