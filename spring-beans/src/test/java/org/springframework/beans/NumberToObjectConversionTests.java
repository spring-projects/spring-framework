/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.beans;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

public class NumberToObjectConversionTests {

	private enum TestEnum {
		  ONE((byte)1, (short)1, 1, (long)1, (float)1, (double)1) 
		, TWO((byte)2, (short)2, 2, (long)2, (float)2, (double)2) 
		// MAX_INT represents the largest possible integer value.  Since it's not 
		// possible to represent this as a byte or short, just set it as null
		, MAX_INT(null, null, Integer.MAX_VALUE, (long)Integer.MAX_VALUE, (float)Integer.MAX_VALUE, (double)Integer.MAX_VALUE)
		;

		private Byte byteValue;
		private Short shortValue;
		private Integer intValue;
		private Long longValue;
		private Float floatValue;
		private Double doubleValue;

		private TestEnum(Byte byteValue, Short shortValue, Integer integerValue, Long longValue, Float floatValue, Double doubleValue) {
			this.byteValue = byteValue;
			this.shortValue = shortValue;
			this.intValue = integerValue;
			this.longValue = longValue;
			this.floatValue = floatValue;
			this.doubleValue = doubleValue;
		}

		public Byte getByteValue() { 
			return byteValue;
		}

		public Short getShortValue() { 
			return shortValue;
		}

		public Integer getIntegerValue() { 
			return intValue;
		}

		public Long getLongValue() { 
			return longValue;
		}

		public Float getFloatValue() { 
			return floatValue;
		}

		public Double getDoubleValue() { 
			return doubleValue;
		}
	}

	@Test
	public void byteToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new Converter<Byte, TestEnum>() {
			public TestEnum convert(Byte source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getByteValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void shortToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new Converter<Short, TestEnum>() {
			public TestEnum convert(Short source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getShortValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void integerToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new Converter<Integer, TestEnum>() {
			public TestEnum convert(Integer source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getIntegerValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}


	@Test
	public void longToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new Converter<Long, TestEnum>() {
			public TestEnum convert(Long source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getLongValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void floatToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new Converter<Float, TestEnum>() {
			public TestEnum convert(Float source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getFloatValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test
	public void doubleToObjectTest() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new Converter<Double, TestEnum>() {
			public TestEnum convert(Double source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getDoubleValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.ONE, converter.convertIfNecessary("1", TestEnum.class));
	}

	@Test(expected = ConversionNotSupportedException.class)
	public void noConverterForString() {
		GenericConversionService service = new GenericConversionService();
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		converter.convertIfNecessary("1", TestEnum.class);
	}

	@Test(expected = ConversionNotSupportedException.class)
	public void noConverterForNumber() {
		GenericConversionService service = new GenericConversionService();
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		converter.convertIfNecessary(1, TestEnum.class);
	}

	/* Test for multiple converters.  Use a byte + long converters, and use a value can 
	 * be parsed as a long but not a byte.
	 */
	@Test
	public void integerToObjectTestMultipleConverters() {
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new Converter<Byte, TestEnum>() {
			public TestEnum convert(Byte source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getByteValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		service.addConverter(new Converter<Integer, TestEnum>() {
			public TestEnum convert(Integer source) {
				for (TestEnum testEnum : EnumSet.allOf(TestEnum.class)) {
					if (testEnum.getIntegerValue().equals(source)) {
						return testEnum;
					}
				}
				return null;
			}
		});
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.setConversionService(service);

		assertEquals(TestEnum.MAX_INT, converter.convertIfNecessary(String.valueOf(Integer.MAX_VALUE), TestEnum.class));
	}

}