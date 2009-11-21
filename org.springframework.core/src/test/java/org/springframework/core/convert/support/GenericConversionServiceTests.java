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

package org.springframework.core.convert.support;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

public class GenericConversionServiceTests {

	private GenericConversionService conversionService = new GenericConversionService();

	@Test
	public void executeConversion() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertEquals(new Integer(3), conversionService.convert("3", Integer.class));
	}

	@Test
	public void executeConversionNullSource() {
		assertEquals(null, conversionService.convert(null, Integer.class));
	}

	@Test
	public void executeCompatibleSource() {
		assertEquals(Boolean.FALSE, conversionService.convert(false, boolean.class));
	}

	@Test
	public void converterNotFound() {
		try {
			conversionService.convert("3", Integer.class);
			fail("Should have thrown an exception");
		} catch (ConverterNotFoundException e) {
		}
	}

	@Test
	public void addConverterNoSourceTargetClassInfoAvailable() {
		try {
			conversionService.addConverter(new Converter() {
				public Object convert(Object source) {
					return source;
				}
			});
			fail("Should have failed");
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void convertNull() {
		assertNull(conversionService.convert(null, Integer.class));
	}

	public void convertNullTargetClass() {
		assertNull(conversionService.convert("3", (Class<?>) null));
	}

	@Test
	public void convertNullTypeDescriptor() {
		assertNull(conversionService.convert("3", TypeDescriptor.valueOf(String.class), TypeDescriptor.NULL));
	}

	@Test
	public void convertWrongTypeArgument() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		try {
			conversionService.convert("BOGUS", Integer.class);
			fail("Should have failed");
		} catch (ConversionFailedException e) {

		}
	}

	@Test
	public void convertSuperSourceType() {
		conversionService.addConverter(new Converter<CharSequence, Integer>() {
			public Integer convert(CharSequence source) {
				return Integer.valueOf(source.toString());
			}
		});
		Integer result = conversionService.convert("3", Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertObjectToPrimitive() {
		assertFalse(conversionService.canConvert(String.class, boolean.class));
		conversionService.addConverter(new StringToBooleanConverter());
		assertTrue(conversionService.canConvert(String.class, boolean.class));
		Boolean b = conversionService.convert("true", boolean.class);
		assertEquals(Boolean.TRUE, b);
		assertTrue(conversionService.canConvert(TypeDescriptor.valueOf(String.class), TypeDescriptor
				.valueOf(boolean.class)));
		b = (Boolean) conversionService.convert("true", TypeDescriptor.valueOf(String.class), TypeDescriptor
				.valueOf(boolean.class));
		assertEquals(Boolean.TRUE, b);
	}

	@Test
	public void convertObjectToPrimitiveViaConverterFactory() {
		assertFalse(conversionService.canConvert(String.class, int.class));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertTrue(conversionService.canConvert(String.class, int.class));		
		Integer three = conversionService.convert("3", int.class);
		assertEquals(3, three.intValue());
	}
	
	@Test
	public void genericConverterDelegatingBackToConversionServiceConverterNotFound() {
		try {
			conversionService.addGenericConverter(new ObjectToArrayConverter(conversionService));
			conversionService.convert("1", Integer[].class);
			fail("Should hace failed");
		} catch (ConversionFailedException e) {
			assertTrue(e.getCause() instanceof ConverterNotFoundException);
		}
	}

}
