/*
 * Copyright 2002-2011 the original author or authors.
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Expression evaluation where the TypeConverter plugged in is the
 * {@link org.springframework.core.convert.support.GenericConversionService}.
 *
 * @author Andy Clement
 * @author Dave Syer
 */
public class ExpressionTestsUsingCoreConversionService extends ExpressionTestCase {

	private static List<String> listOfString = new ArrayList<String>();
	private static TypeDescriptor typeDescriptorForListOfString = null;
	private static List<Integer> listOfInteger = new ArrayList<Integer>();
	private static TypeDescriptor typeDescriptorForListOfInteger = null;

	static {
		listOfString.add("1");
		listOfString.add("2");
		listOfString.add("3");
		listOfInteger.add(4);
		listOfInteger.add(5);
		listOfInteger.add(6);
	}

	@Before
	public void setUp() throws Exception {
		ExpressionTestsUsingCoreConversionService.typeDescriptorForListOfString = new TypeDescriptor(ExpressionTestsUsingCoreConversionService.class.getDeclaredField("listOfString"));
		ExpressionTestsUsingCoreConversionService.typeDescriptorForListOfInteger = new TypeDescriptor(ExpressionTestsUsingCoreConversionService.class.getDeclaredField("listOfInteger"));
	}


	/**
	 * Test the service can convert what we are about to use in the expression evaluation tests.
	 */
	@Test
	public void testConversionsAvailable() throws Exception {
		TypeConvertorUsingConversionService tcs = new TypeConvertorUsingConversionService();

		// ArrayList containing List<Integer> to List<String>
		Class<?> clazz = typeDescriptorForListOfString.getElementTypeDescriptor().getType();
		assertEquals(String.class,clazz);
		List l = (List) tcs.convertValue(listOfInteger, TypeDescriptor.forObject(listOfInteger), typeDescriptorForListOfString);
		assertNotNull(l);

		// ArrayList containing List<String> to List<Integer>
		clazz = typeDescriptorForListOfInteger.getElementTypeDescriptor().getType();
		assertEquals(Integer.class,clazz);

		l = (List) tcs.convertValue(listOfString, TypeDescriptor.forObject(listOfString), typeDescriptorForListOfString);
		assertNotNull(l);
	}

	@Test
	public void testSetParameterizedList() throws Exception {
		StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		Expression e = parser.parseExpression("listOfInteger.size()");
		assertEquals(0,e.getValue(context,Integer.class).intValue());
		context.setTypeConverter(new TypeConvertorUsingConversionService());
		// Assign a List<String> to the List<Integer> field - the component elements should be converted
		parser.parseExpression("listOfInteger").setValue(context,listOfString);
		assertEquals(3,e.getValue(context,Integer.class).intValue()); // size now 3
		Class clazz = parser.parseExpression("listOfInteger[1].getClass()").getValue(context,Class.class); // element type correctly Integer
		assertEquals(Integer.class,clazz);
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
		assertTrue(evaluationContext.getTypeConverter()
				.canConvert(TypeDescriptor.valueOf(String.class), collectionType));
		// ... and it can be done successfully
		assertEquals("[1, 2, 3, 4]", evaluationContext.getTypeConverter().convertValue("1,2,3,4", TypeDescriptor.valueOf(String.class), collectionType).toString());

		evaluationContext.setVariable("target", new TestTarget());

		// OK up to here, so the evaluation should be fine...
		// ... but this fails
		int result = (Integer) parser.parseExpression("#target.sum(#root)").getValue(evaluationContext, "1,2,3,4");
		assertEquals("Wrong result: " + result, 10, result);

	}

	public static class Foo {

		private Collection<Foo> foos;

		public final String value;

		public Foo(String value) {
			this.value = value;
		}

		public void setFoos(Collection<Foo> foos) {
			this.foos = foos;
		}

		public Collection<Foo> getFoos() {
			return this.foos;
		}

	}

	@Test
	public void testConvert() {
		Foo root = new Foo("bar");
		StandardEvaluationContext context = new StandardEvaluationContext(root);

		Collection<String> foos = Collections.singletonList("baz");

		// property access, works
		Expression expression = parser.parseExpression("foos");
		expression.setValue(context, foos);
		Foo baz = root.getFoos().iterator().next();
		assertEquals("baz", baz.value);

		// method call, fails (ClassCastException)
		expression = parser.parseExpression("setFoos(#foos)");
		context.setVariable("foos", foos);
		expression.getValue(context);
		baz = root.getFoos().iterator().next();
		assertEquals("baz", baz.value);
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

}
