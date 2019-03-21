/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.convert.converter;

import java.awt.Color;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link DefaultConversionService}.
 *
 * <p>In this package for enforcing accessibility checks to non-public classes outside
 * of the {@code org.springframework.core.convert.support} implementation package.
 * Only in such a scenario, {@code setAccessible(true)} is actually necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class DefaultConversionServiceTests {

	private final DefaultConversionService conversionService = new DefaultConversionService();


	@Test
	public void testStringToCharacter() {
		assertEquals(Character.valueOf('1'), conversionService.convert("1", Character.class));
	}

	@Test
	public void testStringToCharacterEmptyString() {
		assertEquals(null, conversionService.convert("", Character.class));
	}

	@Test(expected = ConversionFailedException.class)
	public void testStringToCharacterInvalidString() {
		conversionService.convert("invalid", Character.class);
	}

	@Test
	public void testCharacterToString() {
		assertEquals("3", conversionService.convert('3', String.class));
	}

	@Test
	public void testStringToBooleanTrue() {
		assertEquals(true, conversionService.convert("true", Boolean.class));
		assertEquals(true, conversionService.convert("on", Boolean.class));
		assertEquals(true, conversionService.convert("yes", Boolean.class));
		assertEquals(true, conversionService.convert("1", Boolean.class));
		assertEquals(true, conversionService.convert("TRUE", Boolean.class));
		assertEquals(true, conversionService.convert("ON", Boolean.class));
		assertEquals(true, conversionService.convert("YES", Boolean.class));
	}

	@Test
	public void testStringToBooleanFalse() {
		assertEquals(false, conversionService.convert("false", Boolean.class));
		assertEquals(false, conversionService.convert("off", Boolean.class));
		assertEquals(false, conversionService.convert("no", Boolean.class));
		assertEquals(false, conversionService.convert("0", Boolean.class));
		assertEquals(false, conversionService.convert("FALSE", Boolean.class));
		assertEquals(false, conversionService.convert("OFF", Boolean.class));
		assertEquals(false, conversionService.convert("NO", Boolean.class));
	}

	@Test
	public void testStringToBooleanEmptyString() {
		assertEquals(null, conversionService.convert("", Boolean.class));
	}

	@Test(expected = ConversionFailedException.class)
	public void testStringToBooleanInvalidString() {
		conversionService.convert("invalid", Boolean.class);
	}

	@Test
	public void testBooleanToString() {
		assertEquals("true", conversionService.convert(true, String.class));
	}

	@Test
	public void testStringToByte() {
		assertEquals(Byte.valueOf("1"), conversionService.convert("1", Byte.class));
	}

	@Test
	public void testByteToString() {
		assertEquals("65", conversionService.convert("A".getBytes()[0], String.class));
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
		assertEquals(Integer.valueOf(1), conversionService.convert("1", Integer.class));
	}

	@Test
	public void testIntegerToString() {
		assertEquals("3", conversionService.convert(3, String.class));
	}

	@Test
	public void testStringToLong() {
		assertEquals(Long.valueOf(1), conversionService.convert("1", Long.class));
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
		assertEquals("1.0", conversionService.convert(Float.valueOf("1.0"), String.class));
	}

	@Test
	public void testStringToDouble() {
		assertEquals(Double.valueOf("1.0"), conversionService.convert("1.0", Double.class));
	}

	@Test
	public void testDoubleToString() {
		assertEquals("1.0", conversionService.convert(Double.valueOf("1.0"), String.class));
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
	public void testStringToEnum() {
		assertEquals(Foo.BAR, conversionService.convert("BAR", Foo.class));
	}

	@Test
	public void testStringToEnumWithSubclass() {
		assertEquals(SubFoo.BAZ, conversionService.convert("BAZ", SubFoo.BAR.getClass()));
	}

	@Test
	public void testStringToEnumEmptyString() {
		assertEquals(null, conversionService.convert("", Foo.class));
	}

	@Test
	public void testEnumToString() {
		assertEquals("BAR", conversionService.convert(Foo.BAR, String.class));
	}

	@Test
	public void testIntegerToEnum() {
		assertEquals(Foo.BAR, conversionService.convert(0, Foo.class));
	}

	@Test
	public void testIntegerToEnumWithSubclass() {
		assertEquals(SubFoo.BAZ, conversionService.convert(1, SubFoo.BAR.getClass()));
	}

	@Test
	public void testIntegerToEnumNull() {
		assertEquals(null, conversionService.convert(null, Foo.class));
	}

	@Test
	public void testEnumToInteger() {
		assertEquals(Integer.valueOf(0), conversionService.convert(Foo.BAR, Integer.class));
	}

	@Test
	public void testStringToEnumSet() throws Exception {
		assertEquals(EnumSet.of(Foo.BAR), conversionService.convert("BAR", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("enumSet"))));
	}

	@Test
	public void testStringToLocale() {
		assertEquals(Locale.ENGLISH, conversionService.convert("en", Locale.class));
	}

	@Test
	public void testStringToCharset() {
		assertEquals(Charset.forName("UTF-8"), conversionService.convert("UTF-8", Charset.class));
	}

	@Test
	public void testCharsetToString() {
		assertEquals("UTF-8", conversionService.convert(Charset.forName("UTF-8"), String.class));
	}

	@Test
	public void testStringToCurrency() {
		assertEquals(Currency.getInstance("EUR"), conversionService.convert("EUR", Currency.class));
	}

	@Test
	public void testCurrencyToString() {
		assertEquals("USD", conversionService.convert(Currency.getInstance("USD"), String.class));
	}

	@Test
	public void testStringToString() {
		String str = "test";
		assertSame(str, conversionService.convert(str, String.class));
	}

	@Test
	public void testUuidToStringAndStringToUuid() {
		UUID uuid = UUID.randomUUID();
		String convertToString = conversionService.convert(uuid, String.class);
		UUID convertToUUID = conversionService.convert(convertToString, UUID.class);
		assertEquals(uuid, convertToUUID);
	}

	@Test
	public void testNumberToNumber() {
		assertEquals(Long.valueOf(1), conversionService.convert(1, Long.class));
	}

	@Test(expected = ConversionFailedException.class)
	public void testNumberToNumberNotSupportedNumber() {
		conversionService.convert(1, CustomNumber.class);
	}

	@Test
	public void testNumberToCharacter() {
		assertEquals(Character.valueOf('A'), conversionService.convert(65, Character.class));
	}

	@Test
	public void testCharacterToNumber() {
		assertEquals(Integer.valueOf(65), conversionService.convert('A', Integer.class));
	}

	// collection conversion

	@Test
	public void convertArrayToCollectionInterface() {
		List<?> result = conversionService.convert(new String[] {"1", "2", "3"}, List.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test
	public void convertArrayToCollectionGenericTypeConversion() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert(new String[] {"1", "2", "3"}, TypeDescriptor
				.valueOf(String[].class), new TypeDescriptor(getClass().getDeclaredField("genericList")));
		assertEquals(Integer.valueOf(1), result.get(0));
		assertEquals(Integer.valueOf(2), result.get(1));
		assertEquals(Integer.valueOf(3), result.get(2));
	}

	@Test
	public void convertArrayToStream() throws Exception {
		String[] source = {"1", "3", "4"};
		@SuppressWarnings("unchecked")
		Stream<Integer> result = (Stream<Integer>) this.conversionService.convert(source,
				TypeDescriptor.valueOf(String[].class),
				new TypeDescriptor(getClass().getDeclaredField("genericStream")));
		assertEquals(8, result.mapToInt((x) -> x).sum());
	}

	@Test
	public void testSpr7766() throws Exception {
		ConverterRegistry registry = (conversionService);
		registry.addConverter(new ColorConverter());
		@SuppressWarnings("unchecked")
		List<Color> colors = (List<Color>) conversionService.convert(new String[] {"ffffff", "#000000"},
				TypeDescriptor.valueOf(String[].class),
				new TypeDescriptor(new MethodParameter(getClass().getMethod("handlerMethod", List.class), 0)));
		assertEquals(2, colors.size());
		assertEquals(Color.WHITE, colors.get(0));
		assertEquals(Color.BLACK, colors.get(1));
	}

	@Test
	public void convertArrayToCollectionImpl() {
		LinkedList<?> result = conversionService.convert(new String[] {"1", "2", "3"}, LinkedList.class);
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test(expected = ConversionFailedException.class)
	public void convertArrayToAbstractCollection() {
		conversionService.convert(new String[]{"1", "2", "3"}, AbstractList.class);
	}

	@Test
	public void convertArrayToString() {
		String result = conversionService.convert(new String[] {"1", "2", "3"}, String.class);
		assertEquals("1,2,3", result);
	}

	@Test
	public void convertArrayToStringWithElementConversion() {
		String result = conversionService.convert(new Integer[] {1, 2, 3}, String.class);
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
		assertEquals(Integer.valueOf(1), result[0]);
		assertEquals(Integer.valueOf(2), result[1]);
		assertEquals(Integer.valueOf(3), result[2]);
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
		Object[] array = new Object[] {3L};
		Object result = conversionService.convert(array, Long.class);
		assertEquals(3L, result);
	}

	@Test
	public void convertArrayToObjectWithElementConversion() {
		String[] array = new String[] {"3"};
		Integer result = conversionService.convert(array, Integer.class);
		assertEquals(Integer.valueOf(3), result);
	}

	@Test
	public void convertArrayToObjectAssignableTargetType() {
		Long[] array = new Long[] {3L};
		Long[] result = (Long[]) conversionService.convert(array, Object.class);
		assertArrayEquals(array, result);
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
		assertEquals(Integer.valueOf(3), result[0]);
	}

	@Test
	public void convertCollectionToArray() {
		List<String> list = new ArrayList<>();
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
		List<String> list = new ArrayList<>();
		list.add("1");
		list.add("2");
		list.add("3");
		Integer[] result = conversionService.convert(list, Integer[].class);
		assertEquals(Integer.valueOf(1), result[0]);
		assertEquals(Integer.valueOf(2), result[1]);
		assertEquals(Integer.valueOf(3), result[2]);
	}

	@Test
	public void convertCollectionToString() {
		List<String> list = Arrays.asList("foo", "bar");
		String result = conversionService.convert(list, String.class);
		assertEquals("foo,bar", result);
	}

	@Test
	public void convertCollectionToStringWithElementConversion() throws Exception {
		List<Integer> list = Arrays.asList(3, 5);
		String result = (String) conversionService.convert(list,
				new TypeDescriptor(getClass().getField("genericList")), TypeDescriptor.valueOf(String.class));
		assertEquals("3,5", result);
	}

	@Test
	public void convertStringToCollection() {
		List<?> result = conversionService.convert("1,2,3", List.class);
		assertEquals(3, result.size());
		assertEquals("1", result.get(0));
		assertEquals("2", result.get(1));
		assertEquals("3", result.get(2));
	}

	@Test
	public void convertStringToCollectionWithElementConversion() throws Exception {
		List<?> result = (List) conversionService.convert("1,2,3", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(3, result.size());
		assertEquals(1, result.get(0));
		assertEquals(2, result.get(1));
		assertEquals(3, result.get(2));
	}

	@Test
	public void convertEmptyStringToCollection() {
		Collection<?> result = conversionService.convert("", Collection.class);
		assertEquals(0, result.size());
	}

	@Test
	public void convertCollectionToObject() {
		List<Long> list = Collections.singletonList(3L);
		Long result = conversionService.convert(list, Long.class);
		assertEquals(Long.valueOf(3), result);
	}

	@Test
	public void convertCollectionToObjectWithElementConversion() {
		List<String> list = Collections.singletonList("3");
		Integer result = conversionService.convert(list, Integer.class);
		assertEquals(Integer.valueOf(3), result);
	}

	@Test
	public void convertCollectionToObjectAssignableTarget() throws Exception {
		Collection<String> source = new ArrayList<>();
		source.add("foo");
		Object result = conversionService.convert(source, new TypeDescriptor(getClass().getField("assignableTarget")));
		assertEquals(source, result);
	}

	@Test
	public void convertCollectionToObjectWithCustomConverter() {
		List<String> source = new ArrayList<>();
		source.add("A");
		source.add("B");
		conversionService.addConverter(List.class, ListWrapper.class, ListWrapper::new);
		ListWrapper result = conversionService.convert(source, ListWrapper.class);
		assertSame(source, result.getList());
	}

	@Test
	public void convertObjectToCollection() {
		List<?> result = conversionService.convert(3L, List.class);
		assertEquals(1, result.size());
		assertEquals(3L, result.get(0));
	}

	@Test
	public void convertObjectToCollectionWithElementConversion() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert(3L, TypeDescriptor.valueOf(Long.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(1, result.size());
		assertEquals(Integer.valueOf(3), result.get(0));
	}

	@Test
	public void convertStringArrayToIntegerArray() {
		Integer[] result = conversionService.convert(new String[] {"1", "2", "3"}, Integer[].class);
		assertEquals(Integer.valueOf(1), result[0]);
		assertEquals(Integer.valueOf(2), result[1]);
		assertEquals(Integer.valueOf(3), result[2]);
	}

	@Test
	public void convertStringArrayToIntArray() {
		int[] result = conversionService.convert(new String[] {"1", "2", "3"}, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertIntegerArrayToIntegerArray() {
		Integer[] result = conversionService.convert(new Integer[] {1, 2, 3}, Integer[].class);
		assertEquals(Integer.valueOf(1), result[0]);
		assertEquals(Integer.valueOf(2), result[1]);
		assertEquals(Integer.valueOf(3), result[2]);
	}

	@Test
	public void convertIntegerArrayToIntArray() {
		int[] result = conversionService.convert(new Integer[] {1, 2, 3}, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertObjectArrayToIntegerArray() {
		Integer[] result = conversionService.convert(new Object[] {1, 2, 3}, Integer[].class);
		assertEquals(Integer.valueOf(1), result[0]);
		assertEquals(Integer.valueOf(2), result[1]);
		assertEquals(Integer.valueOf(3), result[2]);
	}

	@Test
	public void convertObjectArrayToIntArray() {
		int[] result = conversionService.convert(new Object[] {1, 2, 3}, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertByteArrayToWrapperArray() {
		byte[] byteArray = new byte[] {1, 2, 3};
		Byte[] converted = conversionService.convert(byteArray, Byte[].class);
		assertThat(converted, equalTo(new Byte[]{1, 2, 3}));
	}

	@Test
	public void convertArrayToArrayAssignable() {
		int[] result = conversionService.convert(new int[] {1, 2, 3}, int[].class);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void convertListOfNonStringifiable() {
		List<Object> list = Arrays.asList(new TestEntity(1L), new TestEntity(2L));
		assertTrue(conversionService.canConvert(list.getClass(), String.class));
		try {
			conversionService.convert(list, String.class);
		}
		catch (ConversionFailedException ex) {
			assertTrue(ex.getMessage().contains(list.getClass().getName()));
			assertTrue(ex.getCause() instanceof ConverterNotFoundException);
			assertTrue(ex.getCause().getMessage().contains(TestEntity.class.getName()));
		}
	}

	@Test
	public void convertListOfStringToString() {
		List<String> list = Arrays.asList("Foo", "Bar");
		assertTrue(conversionService.canConvert(list.getClass(), String.class));
		String result = conversionService.convert(list, String.class);
		assertEquals("Foo,Bar", result);
	}

	@Test
	public void convertListOfListToString() {
		List<String> list1 = Arrays.asList("Foo", "Bar");
		List<String> list2 = Arrays.asList("Baz", "Boop");
		List<List<String>> list = Arrays.asList(list1, list2);
		assertTrue(conversionService.canConvert(list.getClass(), String.class));
		String result = conversionService.convert(list, String.class);
		assertEquals("Foo,Bar,Baz,Boop", result);
	}

	@Test
	public void convertCollectionToCollection() throws Exception {
		Set<String> foo = new LinkedHashSet<>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		@SuppressWarnings("unchecked")
		List<Integer> bar = (List<Integer>) conversionService.convert(foo, TypeDescriptor.forObject(foo),
				new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(Integer.valueOf(1), bar.get(0));
		assertEquals(Integer.valueOf(2), bar.get(1));
		assertEquals(Integer.valueOf(3), bar.get(2));
	}

	@Test
	public void convertCollectionToCollectionNull() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> bar = (List<Integer>) conversionService.convert(null,
				TypeDescriptor.valueOf(LinkedHashSet.class), new TypeDescriptor(getClass().getField("genericList")));
		assertNull(bar);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void convertCollectionToCollectionNotGeneric() {
		Set<String> foo = new LinkedHashSet<>();
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
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void convertCollectionToCollectionSpecialCaseSourceImpl() throws Exception {
		Map map = new LinkedHashMap();
		map.put("1", "1");
		map.put("2", "2");
		map.put("3", "3");
		Collection values = map.values();
		List<Integer> bar = (List<Integer>) conversionService.convert(values,
				TypeDescriptor.forObject(values), new TypeDescriptor(getClass().getField("genericList")));
		assertEquals(3, bar.size());
		assertEquals(Integer.valueOf(1), bar.get(0));
		assertEquals(Integer.valueOf(2), bar.get(1));
		assertEquals(Integer.valueOf(3), bar.get(2));
	}

	@Test
	public void collection() {
		List<String> strings = new ArrayList<>();
		strings.add("3");
		strings.add("9");
		@SuppressWarnings("unchecked")
		List<Integer> integers = (List<Integer>) conversionService.convert(strings,
				TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
		assertEquals(Integer.valueOf(3), integers.get(0));
		assertEquals(Integer.valueOf(9), integers.get(1));
	}

	@Test
	public void convertMapToMap() throws Exception {
		Map<String, String> foo = new HashMap<>();
		foo.put("1", "BAR");
		foo.put("2", "BAZ");
		@SuppressWarnings("unchecked")
		Map<Integer, Foo> map = (Map<Integer, Foo>) conversionService.convert(foo,
				TypeDescriptor.forObject(foo), new TypeDescriptor(getClass().getField("genericMap")));
		assertEquals(Foo.BAR, map.get(1));
		assertEquals(Foo.BAZ, map.get(2));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void convertHashMapValuesToList() {
		Map<String, Integer> hashMap = new LinkedHashMap<>();
		hashMap.put("1", 1);
		hashMap.put("2", 2);
		List converted = conversionService.convert(hashMap.values(), List.class);
		assertEquals(Arrays.asList(1, 2), converted);
	}

	@Test
	public void map() {
		Map<String, String> strings = new HashMap<>();
		strings.put("3", "9");
		strings.put("6", "31");
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> integers = (Map<Integer, Integer>) conversionService.convert(strings,
				TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(Integer.class)));
		assertEquals(Integer.valueOf(9), integers.get(3));
		assertEquals(Integer.valueOf(31), integers.get(6));
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
	public void convertObjectToStringWithValueOfMethodPresentUsingToString() {
		ISBN.reset();
		assertEquals("123456789", conversionService.convert(new ISBN("123456789"), String.class));

		assertEquals("constructor invocations", 1, ISBN.constructorCount);
		assertEquals("valueOf() invocations", 0, ISBN.valueOfCount);
		assertEquals("toString() invocations", 1, ISBN.toStringCount);
	}

	@Test
	public void convertObjectToObjectUsingValueOfMethod() {
		ISBN.reset();
		assertEquals(new ISBN("123456789"), conversionService.convert("123456789", ISBN.class));

		assertEquals("valueOf() invocations", 1, ISBN.valueOfCount);
		// valueOf() invokes the constructor
		assertEquals("constructor invocations", 2, ISBN.constructorCount);
		assertEquals("toString() invocations", 0, ISBN.toStringCount);
	}

	@Test
	public void convertObjectToStringUsingToString() {
		SSN.reset();
		assertEquals("123456789", conversionService.convert(new SSN("123456789"), String.class));

		assertEquals("constructor invocations", 1, SSN.constructorCount);
		assertEquals("toString() invocations", 1, SSN.toStringCount);
	}

	@Test
	public void convertObjectToObjectUsingObjectConstructor() {
		SSN.reset();
		assertEquals(new SSN("123456789"), conversionService.convert("123456789", SSN.class));

		assertEquals("constructor invocations", 2, SSN.constructorCount);
		assertEquals("toString() invocations", 0, SSN.toStringCount);
	}

	@Test
	public void convertStringToTimezone() {
		assertEquals("GMT+02:00", conversionService.convert("GMT+2", TimeZone.class).getID());
	}

	@Test
	public void convertObjectToStringWithJavaTimeOfMethodPresent() {
		assertTrue(conversionService.convert(ZoneId.of("GMT+1"), String.class).startsWith("GMT+"));
	}

	@Test
	public void convertObjectToStringNotSupported() {
		assertFalse(conversionService.canConvert(TestEntity.class, String.class));
	}

	@Test
	public void convertObjectToObjectWithJavaTimeOfMethod() {
		assertEquals(ZoneId.of("GMT+1"), conversionService.convert("GMT+1", ZoneId.class));
	}

	@Test(expected = ConverterNotFoundException.class)
	public void convertObjectToObjectNoValueOfMethodOrConstructor() {
		conversionService.convert(Long.valueOf(3), SSN.class);
	}

	@Test
	public void convertObjectToObjectFinderMethod() {
		TestEntity e = conversionService.convert(1L, TestEntity.class);
		assertEquals(Long.valueOf(1), e.getId());
	}

	@Test
	public void convertObjectToObjectFinderMethodWithNull() {
		TestEntity entity = (TestEntity) conversionService.convert(null,
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(TestEntity.class));
		assertNull(entity);
	}

	@Test
	public void convertObjectToObjectFinderMethodWithIdConversion() {
		TestEntity entity = conversionService.convert("1", TestEntity.class);
		assertEquals(Long.valueOf(1), entity.getId());
	}

	@Test
	public void convertCharArrayToString() {
		String converted = conversionService.convert(new char[] {'a', 'b', 'c'}, String.class);
		assertThat(converted, equalTo("a,b,c"));
	}

	@Test
	public void convertStringToCharArray() {
		char[] converted = conversionService.convert("a,b,c", char[].class);
		assertThat(converted, equalTo(new char[]{'a', 'b', 'c'}));
	}

	@Test
	public void convertStringToCustomCharArray() {
		conversionService.addConverter(String.class, char[].class, String::toCharArray);
		char[] converted = conversionService.convert("abc", char[].class);
		assertThat(converted, equalTo(new char[] {'a', 'b', 'c'}));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void multidimensionalArrayToListConversionShouldConvertEntriesCorrectly() {
		String[][] grid = new String[][] {new String[] {"1", "2", "3", "4"}, new String[] {"5", "6", "7", "8"},
				new String[] {"9", "10", "11", "12"}};
		List<String[]> converted = conversionService.convert(grid, List.class);
		String[][] convertedBack = conversionService.convert(converted, String[][].class);
		assertArrayEquals(grid, convertedBack);
	}

	@Test
	public void convertCannotOptimizeArray() {
		conversionService.addConverter(Byte.class, Byte.class, source -> (byte) (source + 1));
		byte[] byteArray = new byte[] {1, 2, 3};
		byte[] converted = conversionService.convert(byteArray, byte[].class);
		assertNotSame(byteArray, converted);
		assertArrayEquals(new byte[]{2, 3, 4}, converted);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void convertObjectToOptional() {
		Method method = ClassUtils.getMethod(TestEntity.class, "handleOptionalValue", Optional.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		TypeDescriptor descriptor = new TypeDescriptor(parameter);
		Object actual = conversionService.convert("1,2,3", TypeDescriptor.valueOf(String.class), descriptor);
		assertEquals(Optional.class, actual.getClass());
		assertEquals(Arrays.asList(1, 2, 3), ((Optional<List<Integer>>) actual).get());
	}

	@Test
	public void convertObjectToOptionalNull() {
		assertSame(Optional.empty(), conversionService.convert(null, TypeDescriptor.valueOf(Object.class),
				TypeDescriptor.valueOf(Optional.class)));
		assertSame(Optional.empty(), conversionService.convert(null, Optional.class));
	}

	@Test
	public void convertExistingOptional() {
		assertSame(Optional.empty(), conversionService.convert(Optional.empty(), TypeDescriptor.valueOf(Object.class),
				TypeDescriptor.valueOf(Optional.class)));
		assertSame(Optional.empty(), conversionService.convert(Optional.empty(), Optional.class));
	}

	@Test
	public void testPerformance1() {
		Assume.group(TestGroup.PERFORMANCE);
		StopWatch watch = new StopWatch("integer->string conversionPerformance");
		watch.start("convert 4,000,000 with conversion service");
		for (int i = 0; i < 4000000; i++) {
			conversionService.convert(3, String.class);
		}
		watch.stop();
		watch.start("convert 4,000,000 manually");
		for (int i = 0; i < 4000000; i++) {
			Integer.valueOf(3).toString();
		}
		watch.stop();
		// System.out.println(watch.prettyPrint());
	}


	// test fields and helpers

	public List<Integer> genericList = new ArrayList<>();

	public Stream<Integer> genericStream;

	public Map<Integer, Foo> genericMap = new HashMap<>();

	public EnumSet<Foo> enumSet;

	public Object assignableTarget;


	public void handlerMethod(List<Color> color) {
	}


	public enum Foo {

		BAR, BAZ
	}


	public enum SubFoo {

		BAR {
			@Override
			String s() {
				return "x";
			}
		},
		BAZ {
			@Override
			String s() {
				return "y";
			}
		};

		abstract String s();
	}


	public class ColorConverter implements Converter<String, Color> {

		@Override
		public Color convert(String source) {
			if (!source.startsWith("#")) {
				source = "#" + source;
			}
			return Color.decode(source);
		}
	}


	@SuppressWarnings("serial")
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

		public void handleOptionalValue(Optional<List<Integer>> value) {
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


	private static class SSN {

		static int constructorCount = 0;

		static int toStringCount = 0;

		static void reset() {
			constructorCount = 0;
			toStringCount = 0;
		}

		private final String value;

		public SSN(String value) {
			constructorCount++;
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof SSN)) {
				return false;
			}
			SSN ssn = (SSN) o;
			return this.value.equals(ssn.value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			toStringCount++;
			return value;
		}
	}


	private static class ISBN {

		static int constructorCount = 0;
		static int toStringCount = 0;
		static int valueOfCount = 0;

		static void reset() {
			constructorCount = 0;
			toStringCount = 0;
			valueOfCount = 0;
		}

		private final String value;

		public ISBN(String value) {
			constructorCount++;
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ISBN)) {
				return false;
			}
			ISBN isbn = (ISBN) o;
			return this.value.equals(isbn.value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			toStringCount++;
			return value;
		}

		@SuppressWarnings("unused")
		public static ISBN valueOf(String value) {
			valueOfCount++;
			return new ISBN(value);
		}
	}

}
