/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expression evaluation where the TypeConverter plugged in is the
 * {@link org.springframework.core.convert.support.GenericConversionService}.
 *
 * @author Andy Clement
 * @author Dave Syer
 */
public class ExpressionWithConversionTests extends AbstractExpressionTests {

	private static List<String> listOfString = new ArrayList<>();
	private static TypeDescriptor typeDescriptorForListOfString = null;
	private static List<Integer> listOfInteger = new ArrayList<>();
	private static TypeDescriptor typeDescriptorForListOfInteger = null;

	static {
		listOfString.add("1");
		listOfString.add("2");
		listOfString.add("3");
		listOfInteger.add(4);
		listOfInteger.add(5);
		listOfInteger.add(6);
	}

	@BeforeEach
	public void setUp() throws Exception {
		ExpressionWithConversionTests.typeDescriptorForListOfString = new TypeDescriptor(ExpressionWithConversionTests.class.getDeclaredField("listOfString"));
		ExpressionWithConversionTests.typeDescriptorForListOfInteger = new TypeDescriptor(ExpressionWithConversionTests.class.getDeclaredField("listOfInteger"));
	}


	/**
	 * Test the service can convert what we are about to use in the expression evaluation tests.
	 */
	@Test
	public void testConversionsAvailable() throws Exception {
		TypeConvertorUsingConversionService tcs = new TypeConvertorUsingConversionService();

		// ArrayList containing List<Integer> to List<String>
		Class<?> clazz = typeDescriptorForListOfString.getElementTypeDescriptor().getType();
		assertThat(clazz).isEqualTo(String.class);
		List<?> l = (List<?>) tcs.convertValue(listOfInteger, TypeDescriptor.forObject(listOfInteger), typeDescriptorForListOfString);
		assertThat(l).isNotNull();

		// ArrayList containing List<String> to List<Integer>
		clazz = typeDescriptorForListOfInteger.getElementTypeDescriptor().getType();
		assertThat(clazz).isEqualTo(Integer.class);

		l = (List<?>) tcs.convertValue(listOfString, TypeDescriptor.forObject(listOfString), typeDescriptorForListOfString);
		assertThat(l).isNotNull();
	}

	@Test
	public void testSetParameterizedList() throws Exception {
		StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		Expression e = parser.parseExpression("listOfInteger.size()");
		assertThat(e.getValue(context, Integer.class)).isZero();
		context.setTypeConverter(new TypeConvertorUsingConversionService());
		// Assign a List<String> to the List<Integer> field - the component elements should be converted
		parser.parseExpression("listOfInteger").setValue(context,listOfString);
		// size now 3
		assertThat(e.getValue(context, Integer.class)).isEqualTo(3);
		Class<?> clazz = parser.parseExpression("listOfInteger[1].getClass()").getValue(context, Class.class); // element type correctly Integer
		assertThat(clazz).isEqualTo(Integer.class);
	}

	@Test
	public void testCoercionToCollectionOfPrimitive() throws Exception {

		class TestTarget {
			@SuppressWarnings("unused")
			public int sum(Collection<Integer> numbers) {
				int total = 0;
				for (int i : numbers) {
					total += i;
				}
				return total;
			}
		}

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

		TypeDescriptor collectionType = new TypeDescriptor(new MethodParameter(TestTarget.class.getDeclaredMethod(
				"sum", Collection.class), 0));
		// The type conversion is possible
		assertThat(evaluationContext.getTypeConverter()
				.canConvert(TypeDescriptor.valueOf(String.class), collectionType)).isTrue();
		// ... and it can be done successfully
		assertThat(evaluationContext.getTypeConverter().convertValue("1,2,3,4", TypeDescriptor.valueOf(String.class), collectionType).toString()).isEqualTo("[1, 2, 3, 4]");

		evaluationContext.setVariable("target", new TestTarget());

		// OK up to here, so the evaluation should be fine...
		// ... but this fails
		int result = (Integer) parser.parseExpression("#target.sum(#root)").getValue(evaluationContext, "1,2,3,4");
		assertThat(result).as("Wrong result: " + result).isEqualTo(10);

	}

	@Test
	public void testConvert() {
		Foo root = new Foo("bar");
		Collection<String> foos = Collections.singletonList("baz");

		StandardEvaluationContext context = new StandardEvaluationContext(root);

		// property access
		Expression expression = parser.parseExpression("foos");
		expression.setValue(context, foos);
		Foo baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("baz");

		// method call
		expression = parser.parseExpression("setFoos(#foos)");
		context.setVariable("foos", foos);
		expression.getValue(context);
		baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("baz");

		// method call with result from method call
		expression = parser.parseExpression("setFoos(getFoosAsStrings())");
		expression.getValue(context);
		baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("baz");

		// method call with result from method call
		expression = parser.parseExpression("setFoos(getFoosAsObjects())");
		expression.getValue(context);
		baz = root.getFoos().iterator().next();
		assertThat(baz.value).isEqualTo("baz");
	}


	/**
	 * Type converter that uses the core conversion service.
	 */
	private static class TypeConvertorUsingConversionService implements TypeConverter {

		private final ConversionService service = new DefaultConversionService();

		@Override
		public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return this.service.canConvert(sourceType, targetType);
		}

		@Override
		public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) throws EvaluationException {
			return this.service.convert(value, sourceType, targetType);
		}
	}


	public static class Foo {

		public final String value;

		private Collection<Foo> foos;

		public Foo(String value) {
			this.value = value;
		}

		public void setFoos(Collection<Foo> foos) {
			this.foos = foos;
		}

		public Collection<Foo> getFoos() {
			return this.foos;
		}

		public Collection<String> getFoosAsStrings() {
			return Collections.singletonList("baz");
		}

		public Collection<?> getFoosAsObjects() {
			return Collections.singletonList("baz");
		}
	}

}
