/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.List;

import org.springframework.core.convert.ConversionContext;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.core.convert.support.GenericTypeConverter;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Expression evaluation where the TypeConverter plugged in is the {@link GenericTypeConverter}
 * 
 * @author Andy Clement
 */
public class ExpressionTestsUsingCoreConversionService extends ExpressionTestCase {

	private static List<String> listOfString = new ArrayList<String>();
	private static ConversionContext typeDescriptorForListOfString = null;
	private static List<Integer> listOfInteger = new ArrayList<Integer>();
	private static ConversionContext typeDescriptorForListOfInteger = null;
	
	static {
		listOfString.add("1");
		listOfString.add("2");
		listOfString.add("3");
		listOfInteger.add(4);
		listOfInteger.add(5);
		listOfInteger.add(6);
	}
	
	public void setUp() throws Exception {
		super.setUp();
		typeDescriptorForListOfString = new ConversionContext(ExpressionTestsUsingCoreConversionService.class.getDeclaredField("listOfString"));
		typeDescriptorForListOfInteger = new ConversionContext(ExpressionTestsUsingCoreConversionService.class.getDeclaredField("listOfInteger"));
	}
		
	
	/**
	 * Test the service can convert what we are about to use in the expression evaluation tests.
	 */
	public void testConversionsAvailable() throws Exception {
		TypeConvertorUsingConversionService tcs = new TypeConvertorUsingConversionService();
		
		// ArrayList containing List<Integer> to List<String>
		Class<?> clazz = typeDescriptorForListOfString.getElementType();
		assertEquals(String.class,clazz);
		List l = (List) tcs.convertValue(listOfInteger, typeDescriptorForListOfString);
		assertNotNull(l); 

		// ArrayList containing List<String> to List<Integer>
		clazz = typeDescriptorForListOfInteger.getElementType();
		assertEquals(Integer.class,clazz);
		
		l = (List) tcs.convertValue(listOfString, typeDescriptorForListOfString);
		assertNotNull(l);
	}
	
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
	

	/**
	 * Type converter that uses the core conversion service.
	 */
	private static class TypeConvertorUsingConversionService implements TypeConverter {

		private final DefaultTypeConverter service = new DefaultTypeConverter();

		public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
			return this.service.canConvert(sourceType, ConversionContext.valueOf(targetType));
		}

		public boolean canConvert(Class<?> sourceType, ConversionContext typeDescriptor) {
			return this.service.canConvert(sourceType, typeDescriptor);
		}

		@SuppressWarnings("unchecked")
		public <T> T convertValue(Object value, Class<T> targetType) throws EvaluationException {
			return (T) this.service.convert(value,ConversionContext.valueOf(targetType));
		}

		@SuppressWarnings("unchecked")
		public Object convertValue(Object value, ConversionContext typeDescriptor) throws EvaluationException {
			return this.service.convert(value, typeDescriptor);
		}
	}

}
