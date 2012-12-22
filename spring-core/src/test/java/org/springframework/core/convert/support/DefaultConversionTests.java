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

package org.springframework.core.convert.support;

import static org.junit.Assert.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class DefaultConversionTests {

	private DefaultConversionService conversionService = new DefaultConversionService();

	@Test
	public void testStringToCharacter() {
		assertEquals(Character.valueOf('1'), conversionService.convert("1", Character.class));
	}

	@Test
	public void testStringToCharacterEmptyString() {
		assertEquals(null, conversionService.convert("", Character.class));
	}

	@Test(expected=ConversionFailedException.class)
	public void testStringToCharacterInvalidString() {
		conversionService.convert("invalid", Character.class);
	}

	@Test
	public void testCharacterToString() {
		assertEquals("3", conversionService.convert('3', String.class));
	}

	@Test
	public void testStringToBooleanTrue() {
		assertEquals(Boolean.valueOf(true), conversionService.convert("true", Boolean.class));
		assertEquals(Boolean.valueOf(true), conversionService.convert("on", Boolean.class));
		assertEquals(Boolean.valueOf(true), conversionService.convert("yes", Boolean.class));
		assertEquals(Boolean.valueOf(true), conversionService.convert("1", Boolean.class));
		assertEquals(Boolean.valueOf(true), conversionService.convert("TRUE", Boolean.class));
		assertEquals(Boolean.valueOf(true), conversionService.convert("ON", Boolean.class));
		assertEquals(Boolean.valueOf(true), conversionService.convert("YES", Boolean.class));
	}

	@Test
	public void testStringToBooleanFalse() {
		assertEquals(Boolean.valueOf(false), conversionService.convert("false", Boolean.class));
		assertEquals(Boolean.valueOf(false), conversionService.convert("off", Boolean.class));
		assertEquals(Boolean.valueOf(false), conversionService.convert("no", Boolean.class));
		assertEquals(Boolean.valueOf(false), conversionService.convert("0", Boolean.class));
		assertEquals(Boolean.valueOf(false), conversionService.convert("FALSE", Boolean.class));
		assertEquals(Boolean.valueOf(false), conversionService.convert("OFF", Boolean.class));
		assertEquals(Boolean.valueOf(false), conversionService.convert("NO", Boolean.class));
	}

	@Test
	public void testStringToBooleanEmptyString() {
		assertEquals(null, conversionService.convert("", Boolean.class));
	}

	@Test(expected=ConversionFailedException.class)
	public void testStringToBooleanInvalidString() {
		conversionService.convert("invalid", Boolean.class);
	}

	@Test
	public void testBooleanToString() {
		assertEquals("true", conversionService.convert(true, String.class));
	}

	@Test
	public void testStringToByte() throws Exception {
		assertEquals(Byte.valueOf("1"), conversionService.convert("1", Byte.class));
	}

	@Test
	public void testByteToString() {
		assertEquals("65", conversionService.convert(new String("A").getBytes()[0], String.class));
	}

	@Test
	public void testStringToShort() {
		assertEquals(Short.valueOf("1"), conversionService.convert("1", Short.class));
	}

	@Test
	public void testShortToString() {
		short three = 3;
		assertEquals("3", conversionService.convert(three, String.class));
	}

	@Test
	public void testStringToInteger() {
		assertEquals(Integer.valueOf("1"), conversionService.convert("1", Integer.class));
	}

	@Test
	public void testIntegerToString() {
		assertEquals("3", conversionService.convert(3, String.class));
	}

	@Test
	public void testStringToLong() {
		assertEquals(Long.valueOf("1"), conversionService.convert("1", Long.class));
	}

	@Test
	public void testLongToString() {
		assertEquals("3", conversionService.convert(3L, String.class));
	}

	@Test
	public void testStringToFloat() {
		assertEquals(Float.valueOf("1.0"), conversionService.convert("1.0", Float.class));
	}

	@Test
	public void testFloatToString() {
		assertEquals("1.0", conversionService.convert(new Float("1.0"), String.class));
	}

	@Test
	public void testStringToDouble() {
		assertEquals(Double.valueOf("1.0"), conversionService.convert("1.0", Double.class));
	}

	@Test
	public void testDoubleToString() {
		assertEquals("1.0", conversionService.convert(new Double("1.0"), String.class));
	}

	@Test
	public void testStringToBigInteger() {
		assertEquals(new BigInteger("1"), conversionService.convert("1", BigInteger.class));
	}

	@Test
	public void testBigIntegerToString() {
		assertEquals("100", conversionService.convert(new BigInteger("100"), String.class));
	}

	@Test
	public void testStringToBigDecimal() {
		assertEquals(new BigDecimal("1.0"), conversionService.convert("1.0", BigDecimal.class));
	}

	@Test
	public void testBigDecimalToString() {
		assertEquals("100.00", conversionService.convert(new BigDecimal("100.00"), String.class));
	}

	@Test
	public void testStringToNumber() {
		assertEquals(new BigDecimal("1.0"), conversionService.convert("1.0", Number.class));
	}

	@Test
	public void testStringToNumberEmptyString() {
		assertEquals(null, conversionService.convert("", Number.class));
	}

	@Test
	public void testStringToEnum() throws Exception {
		assertEquals(Foo.BAR, conversionService.convert("BAR", Foo.class));
	}

	@Test
	public void testStringToEnumEmptyString() {
		assertEquals(null, conversionService.convert("", Foo.class));
	}

	@Test
	public void testEnumToString() {
		assertEquals("BAR", conversionService.convert(Foo.BAR, String.class));
	}

	public static enum Foo {
		BAR, BAZ;
	}

	@Test
	public void testStringToLocale() {
		assertEquals(Locale.ENGLISH, conversionService.convert("en", Locale.class));
	}

	@Test
	public void testStringToString() {
		String str = "test";
		assertSame(str, conversionService.convert(str, String.class));
	}

	@Test
	public void testNumberToNumber() {
		assertEquals(Long.valueOf(1), conversionService.convert(Integer.valueOf(1), Long.class));
	}

	@Test(expected=ConversionFailedException.class)
	public void testNumberToNumberNotSupportedNumber() {
		conversionService.convert(Integer.valueOf(1), CustomNumber.class);
	}

	public static class CustomNumber extends Number {

		@Override
		public double doubleValue() {
			return 0;
		}

		@Override
		public float floatValue() {
			return 0;
		}

		@Override
		public int intValue() {
			return 0;
		}

		@Override
		public long longValue() {
			return 0;
		}

	}

	@Test
	public void testNumberToCharacter() {
		assertEquals(Character.valueOf('A'), conversionService.convert(Integer.valueOf(65), Character.class));
	}

	@Test
	public void testCharacterToNumber() {
		assertEquals(new Integer(65), conversionService.convert('A', Integer.class));
	}

	// collection conversion

	@Test
	public void convertArrayToCollectionInterface() {
		List<?> result = conversionService.convert(new String[] { "1", "2", "3" }, List.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	public List<Integer> genericList = new ArrayList<Integer>();

	@Test
	public void convertArrayToCollectionGenericTypeConversion() throws Exception {
		List<Integer> result = (List<Integer>) conversionService.convert(new String[] { "1", "2", "3" }, TypeDescriptor
				.valueOf(String[].class), new TypeDescriptor(getClass().getDeclaredField("genericList")));
		assertEquals(new Integer("1"), result.get(0));
		assertEquals(new Integer("2"), result.get(1));
		assertEquals(new Integer("3"), result.get(2));
	}

	@Test
	public void testSpr7766() throws Exception {
		ConverterRegistry registry = ((ConverterRegistry) conversionService);
		registry.addConverter(new ColorConverter());
		List<Color> colors = (List<Color>) conversionService.convert(new String[] { "ffffff", "#000000" }, TypeDescriptor.valueOf(String[].class), new TypeDescriptor(new MethodParameter(getClass().getMethod("handlerMethod", List.class), 0)));
		assertEquals(2, colors.size());
		assertEquals(Color.WHITE, colors.get(0));
		assertEquals(Color.BLACK, colors.get(1));
	}

	public class ColorConverter implements Converter<String, Color> {
		public Color convert(String source) { if (!source.startsWith("#")) source = "#" + source; return Color.decode(source); }
	}

	public void handlerMethod(List<Color> color) {

	}

	@Test
	public void convertArrayToCollectionImpl() {
		LinkedList<?> result = conversionService.convert(new String[] { "1", "2", "3" }, LinkedList.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test(expected = ConversionFailedException.class)
	public void convertArrayToAbstractCollection() {
		conversionService.convert(new String[] { "1", "2", "3" }, AbstractList.class);
	}

	public static enum FooEnum {
		BAR, BAZ
	}

	@Test
	public void convertArrayToString() {
		String result = conversionService.convert(new String[] { "1", "2", "3" }, String.class);
		assertEquals("1,2,3", result);
	}

	@Test
	public void convertArrayToStringWithElementConversion() {
		String result = conversionService.convert(new Integer[] { 1, 2, 3 }, String.class);
		assertEquals("1,2,3", result);
	}

	@Test
	public void convertEmptyArrayToString() {
		String result = conversionService.convert(new String[0], String.class);
		assertEquals("", result);
	}

	@Test
	public void convertStringToArray() {
		String[] result = conversionService.convert("1,2,3", String[].class);
		assertEquals(3, result.length);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	@Test
	public void convertStringToArrayWithElementConversion() {
		Integer[] result = conversionService.convert("1,2,3", Integer[].class);
		assertEquals(3, result.length);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void convertStringToPrimitiveArrayWithElementConversion() {
		int[] result = conversionService.convert("1,2,3", int[].class);
		assertEquals(3, result.length);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertEmptyStringToArray() {
		String[] result = conversionService.convert("", String[].class);
		assertEquals(0, result.length);
	}

	@Test
	public void convertArrayToObject() {
		Object[] array = new Object[] { 3L };
		Object result = conversionService.convert(array, Long.class);
		assertEquals(3L, result);
	}

	@Test
	public void convertArrayToObjectWithElementConversion() {
		String[] array = new String[] { "3" };
		Integer result = conversionService.convert(array, Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertArrayToObjectAssignableTargetType() {
		Long[] array = new Long[] { 3L };
		Long[] result = (Long[]) conversionService.convert(array, Object.class);
		assertEquals(array, result);
	}

	@Test
	public void convertObjectToArray() {
		Object[] result = conversionService.convert(3L, Object[].class);
		assertEquals(1, result.length);
		assertEquals(3L, result[0]);
	}

	@Test
	public void convertObjectToArrayWithElementConversion() {
		Integer[] result = conversionService.convert(3L, Integer[].class);
		assertEquals(1, result.length);
		assertEquals(new Integer(3), result[0]);
	}

	@Test
	public void convertCollectionToArray() {
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		String[] result = conversionService.convert(list, String[].class);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	@Test
	public void convertCollectionToArrayWithElementConversion() {
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = conversionService.convert(list, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void convertCollectionToString() {
		List<String> list = Arrays.asList(new String[] { "foo", "bar" });
		String result = conversionService.convert(list, String.class);
		assertEquals("foo,bar", result);
	}

	@Test
	public void convertCollectionToStringWithElementConversion() throws Exception {
		List<Integer> list = Arrays.asList(new Integer[] { 3, 5 });
		String result = (String) conversionService.convert(list,
				new TypeDescriptor(getClass().getField("genericList")), TypeDescriptor.valueOf(String.class));
		assertEquals("3,5", result);
	}

	@Test
	public void convertStringToCollection() {
		List result = conversionService.convert("1,2,3", List.class);
		assertEquals(3, result.size());
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test
	public void convertStringToCollectionWithElementConversion() throws Exception {
		List result = (List) conversionService.convert("1,2,3", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(3, result.size());
		assertEquals(new Integer(1), result.get(0));
		assertEquals(new Integer(2), result.get(1));
		assertEquals(new Integer(3), result.get(2));
	}

	@Test
	public void convertEmptyStringToCollection() {
		Collection result = conversionService.convert("", Collection.class);
		assertEquals(0, result.size());
	}

	@Test
	public void convertCollectionToObject() {
		List<Long> list = Collections.singletonList(3L);
		Long result = conversionService.convert(list, Long.class);
		assertEquals(new Long(3), result);
	}

	@Test
	public void convertCollectionToObjectWithElementConversion() {
		List<String> list = Collections.singletonList("3");
		Integer result = conversionService.convert(list, Integer.class);
		assertEquals(new Integer(3), result);
	}

	@Test
	public void convertCollectionToObjectAssignableTarget() throws Exception {
		Collection<String> source = new ArrayList<String>();
		source.add("foo");
		Object result = conversionService.convert(source, new TypeDescriptor(getClass().getField("assignableTarget")));
		assertEquals(source, result);
	}

	@Test
	public void convertCollectionToObjectWithCustomConverter() throws Exception {
		List<String> source = new ArrayList<String>();
		source.add("A");
		source.add("B");
		conversionService.addConverter(new Converter<List, ListWrapper>() {
			@Override
			public ListWrapper convert(List source) {
				return new ListWrapper(source);
			}
		});
		ListWrapper result = conversionService.convert(source, ListWrapper.class);
		assertSame(source, result.getList());
	}

	@Test
	public void convertObjectToCollection() {
		List<String> result = (List<String>) conversionService.convert(3L, List.class);
		assertEquals(1, result.size());
		assertEquals(3L, result.get(0));
	}

	@Test
	public void convertObjectToCollectionWithElementConversion() throws Exception {
		List<Integer> result = (List<Integer>) conversionService.convert(3L, TypeDescriptor.valueOf(Long.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(1, result.size());
		assertEquals(new Integer(3), result.get(0));
	}

	@Test
	public void convertArrayToArray() {
		Integer[] result = conversionService.convert(new String[] { "1", "2", "3" }, Integer[].class);
		assertEquals(new Integer(1), result[0]);
		assertEquals(new Integer(2), result[1]);
		assertEquals(new Integer(3), result[2]);
	}

	@Test
	public void convertArrayToPrimitiveArray() {
		int[] result = conversionService.convert(new String[] { "1", "2", "3" }, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertArrayToArrayAssignable() {
		int[] result = conversionService.convert(new int[] { 1, 2, 3 }, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertCollectionToCollection() throws Exception {
		Set<String> foo = new LinkedHashSet<String>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		List<Integer> bar = (List<Integer>) conversionService.convert(foo, TypeDescriptor.forObject(foo),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(new Integer(1), bar.get(0));
		assertEquals(new Integer(2), bar.get(1));
		assertEquals(new Integer(3), bar.get(2));
	}

	@Test
	public void convertCollectionToCollectionNull() throws Exception {
		List<Integer> bar = (List<Integer>) conversionService.convert(null,
				TypeDescriptor.valueOf(LinkedHashSet.class), new TypeDescriptor(getClass().getField("genericList")));
		assertNull(bar);
	}

	@Test
	public void convertCollectionToCollectionNotGeneric() throws Exception {
		Set<String> foo = new LinkedHashSet<String>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		List bar = (List) conversionService.convert(foo, TypeDescriptor.valueOf(LinkedHashSet.class), TypeDescriptor
				.valueOf(List.class));
		assertEquals("1", bar.get(0));
		assertEquals("2", bar.get(1));
		assertEquals("3", bar.get(2));
	}

	@Test
	public void convertCollectionToCollectionSpecialCaseSourceImpl() throws Exception {
		Map map = new LinkedHashMap();
		map.put("1", "1");
		map.put("2", "2");
		map.put("3", "3");
		Collection values = map.values();
		List<Integer> bar = (List<Integer>) conversionService.convert(values,
				TypeDescriptor.forObject(values), new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(3, bar.size());
		assertEquals(new Integer(1), bar.get(0));
		assertEquals(new Integer(2), bar.get(1));
		assertEquals(new Integer(3), bar.get(2));
	}

	@Test
	public void collection() {
		List<String> strings = new ArrayList<String>();
		strings.add("3");
		strings.add("9");
		List<Integer> integers = (List<Integer>) conversionService.convert(strings, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
		assertEquals(new Integer(3), integers.get(0));
		assertEquals(new Integer(9), integers.get(1));
	}

	public Map<Integer, FooEnum> genericMap = new HashMap<Integer, FooEnum>();

	@Test
	public void convertMapToMap() throws Exception {
		Map<String, String> foo = new HashMap<String, String>();
		foo.put("1", "BAR");
		foo.put("2", "BAZ");
		Map<String, FooEnum> map = (Map<String, FooEnum>) conversionService.convert(foo,
				TypeDescriptor.forObject(foo), new TypeDescriptor(getClass().getField("genericMap")));
		assertEquals(FooEnum.BAR, map.get(1));
		assertEquals(FooEnum.BAZ, map.get(2));
	}

	@Test
	public void map() {
		Map<String, String> strings = new HashMap<String, String>();
		strings.put("3", "9");
		strings.put("6", "31");
		Map<Integer, Integer> integers = (Map<Integer, Integer>) conversionService.convert(strings, TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(Integer.class)));
		assertEquals(new Integer(9), integers.get(3));
		assertEquals(new Integer(31), integers.get(6));
	}

	@Test
	public void convertPropertiesToString() {
		Properties foo = new Properties();
		foo.setProperty("1", "BAR");
		foo.setProperty("2", "BAZ");
		String result = conversionService.convert(foo, String.class);
		assertTrue(result.contains("1=BAR"));
		assertTrue(result.contains("2=BAZ"));
	}

	@Test
	public void convertStringToProperties() {
		Properties result = conversionService.convert("a=b\nc=2\nd=", Properties.class);
		assertEquals(3, result.size());
		assertEquals("b", result.getProperty("a"));
		assertEquals("2", result.getProperty("c"));
		assertEquals("", result.getProperty("d"));
	}

	@Test
	public void convertStringToPropertiesWithSpaces() {
		Properties result = conversionService.convert("   foo=bar\n   bar=baz\n    baz=boop", Properties.class);
		assertEquals("bar", result.get("foo"));
		assertEquals("baz", result.get("bar"));
		assertEquals("boop", result.get("baz"));
	}

	// generic object conversion

	@Test
	public void convertObjectToStringValueOfMethodPresent() {
		assertEquals("123456789", conversionService.convert(ISBN.valueOf("123456789"), String.class));
	}

	@Test
	public void convertObjectToStringStringConstructorPresent() {
		assertEquals("123456789", conversionService.convert(new SSN("123456789"), String.class));
	}

	@Test
	public void convertObjectToStringNotSupported() {
		assertFalse(conversionService.canConvert(TestEntity.class, String.class));
	}

	@Test
	public void convertObjectToObjectValueOfMethod() {
		assertEquals(ISBN.valueOf("123456789"), conversionService.convert("123456789", ISBN.class));
	}

	@Test
	public void convertObjectToObjectConstructor() {
		assertEquals(new SSN("123456789"), conversionService.convert("123456789", SSN.class));
		assertEquals("123456789", conversionService.convert(new SSN("123456789"), String.class));
	}

	@Test(expected=ConverterNotFoundException.class)
	public void convertObjectToObjectNoValueOFMethodOrConstructor() {
		conversionService.convert(new Long(3), SSN.class);
	}

	public Object assignableTarget;

	private static class SSN {

		private String value;

		public SSN(String value) {
			this.value = value;
		}

		public boolean equals(Object o) {
			if (!(o instanceof SSN)) {
				return false;
			}
			SSN ssn = (SSN) o;
			return this.value.equals(ssn.value);
		}

		public int hashCode() {
			return value.hashCode();
		}

		public String toString() {
			return value;
		}
	}

	private static class ISBN {

		private String value;

		private ISBN(String value) {
			this.value = value;
		}

		public boolean equals(Object o) {
			if (!(o instanceof ISBN)) {
				return false;
			}
			ISBN isbn = (ISBN) o;
			return this.value.equals(isbn.value);
		}

		public int hashCode() {
			return value.hashCode();
		}

		public String toString() {
			return value;
		}

		public static ISBN valueOf(String value) {
			return new ISBN(value);
		}
	}

	@Test
	public void convertObjectToObjectFinderMethod() {
		TestEntity e = conversionService.convert(1L, TestEntity.class);
		assertEquals(new Long(1), e.getId());
	}

	@Test
	public void convertObjectToObjectFinderMethodWithNull() {
		TestEntity e = (TestEntity) conversionService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(TestEntity.class));
		assertNull(e);
	}

	@Test
	public void convertObjectToObjectFinderMethodWithIdConversion() {
		TestEntity e = conversionService.convert("1", TestEntity.class);
		assertEquals(new Long(1), e.getId());
	}

	public static class TestEntity {

		private Long id;

		public TestEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public static TestEntity findTestEntity(Long id) {
			return new TestEntity(id);
		}
	}

	private static class ListWrapper {

		private List<?> list;

		public ListWrapper(List<?> list) {
			this.list = list;
		}

		public List<?> getList() {
			return list;
		}
	}

}
