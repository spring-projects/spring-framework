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

package org.springframework.core.convert.converter;

import java.awt.Color;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DefaultConversionService}.
 *
 * <p>In this package for enforcing accessibility checks to non-public classes outside
 * the {@code org.springframework.core.convert.support} implementation package.
 * Only in such a scenario, {@code setAccessible(true)} is actually necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class DefaultConversionServiceTests {

	private final DefaultConversionService conversionService = new DefaultConversionService();


	@Test
	void stringToCharacter() {
		assertThat(conversionService.convert("1", Character.class)).isEqualTo(Character.valueOf('1'));
	}

	@Test
	void stringToCharacterEmptyString() {
		assertThat(conversionService.convert("", Character.class)).isNull();
	}

	@Test
	void stringToCharacterInvalidString() {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert("invalid", Character.class));
	}

	@Test
	void characterToString() {
		assertThat(conversionService.convert('3', String.class)).isEqualTo("3");
	}

	@Test
	void stringToBooleanTrue() {
		assertThat(conversionService.convert("true", Boolean.class)).isTrue();
		assertThat(conversionService.convert("on", Boolean.class)).isTrue();
		assertThat(conversionService.convert("yes", Boolean.class)).isTrue();
		assertThat(conversionService.convert("1", Boolean.class)).isTrue();
		assertThat(conversionService.convert("TRUE", Boolean.class)).isTrue();
		assertThat(conversionService.convert("ON", Boolean.class)).isTrue();
		assertThat(conversionService.convert("YES", Boolean.class)).isTrue();
	}

	@Test
	void stringToBooleanFalse() {
		assertThat(conversionService.convert("false", Boolean.class)).isFalse();
		assertThat(conversionService.convert("off", Boolean.class)).isFalse();
		assertThat(conversionService.convert("no", Boolean.class)).isFalse();
		assertThat(conversionService.convert("0", Boolean.class)).isFalse();
		assertThat(conversionService.convert("FALSE", Boolean.class)).isFalse();
		assertThat(conversionService.convert("OFF", Boolean.class)).isFalse();
		assertThat(conversionService.convert("NO", Boolean.class)).isFalse();
	}

	@Test
	void stringToBooleanEmptyString() {
		assertThat(conversionService.convert("", Boolean.class)).isNull();
	}

	@Test
	void stringToBooleanInvalidString() {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert("invalid", Boolean.class));
	}

	@Test
	void booleanToString() {
		assertThat(conversionService.convert(true, String.class)).isEqualTo("true");
	}

	@Test
	void stringToByte() {
		assertThat(conversionService.convert("1", Byte.class)).isEqualTo((byte) 1);
	}

	@Test
	void byteToString() {
		assertThat(conversionService.convert("A".getBytes()[0], String.class)).isEqualTo("65");
	}

	@Test
	void stringToShort() {
		assertThat(conversionService.convert("1", Short.class)).isEqualTo((short) 1);
	}

	@Test
	void shortToString() {
		short three = 3;
		assertThat(conversionService.convert(three, String.class)).isEqualTo("3");
	}

	@Test
	void stringToInteger() {
		assertThat(conversionService.convert("1", Integer.class)).isEqualTo(1);
	}

	@Test
	void integerToString() {
		assertThat(conversionService.convert(3, String.class)).isEqualTo("3");
	}

	@Test
	void stringToLong() {
		assertThat(conversionService.convert("1", Long.class)).isEqualTo(Long.valueOf(1));
	}

	@Test
	void longToString() {
		assertThat(conversionService.convert(3L, String.class)).isEqualTo("3");
	}

	@Test
	void stringToFloat() {
		assertThat(conversionService.convert("1.0", Float.class)).isEqualTo(Float.valueOf("1.0"));
	}

	@Test
	void floatToString() {
		assertThat(conversionService.convert(Float.valueOf("1.0"), String.class)).isEqualTo("1.0");
	}

	@Test
	void stringToDouble() {
		assertThat(conversionService.convert("1.0", Double.class)).isEqualTo(Double.valueOf("1.0"));
	}

	@Test
	void doubleToString() {
		assertThat(conversionService.convert(Double.valueOf("1.0"), String.class)).isEqualTo("1.0");
	}

	@Test
	void stringToBigInteger() {
		assertThat(conversionService.convert("1", BigInteger.class)).isEqualTo(new BigInteger("1"));
	}

	@Test
	void bigIntegerToString() {
		assertThat(conversionService.convert(new BigInteger("100"), String.class)).isEqualTo("100");
	}

	@Test
	void stringToBigDecimal() {
		assertThat(conversionService.convert("1.0", BigDecimal.class)).isEqualTo(new BigDecimal("1.0"));
	}

	@Test
	void bigDecimalToString() {
		assertThat(conversionService.convert(new BigDecimal("100.00"), String.class)).isEqualTo("100.00");
	}

	@Test
	void stringToNumber() {
		assertThat(conversionService.convert("1.0", Number.class)).isEqualTo(new BigDecimal("1.0"));
	}

	@Test
	void stringToNumberEmptyString() {
		assertThat(conversionService.convert("", Number.class)).isNull();
	}

	@Test
	void stringToEnum() {
		assertThat(conversionService.convert("BAR", Foo.class)).isEqualTo(Foo.BAR);
	}

	@Test
	void stringToEnumWithSubclass() {
		assertThat(conversionService.convert("BAZ", SubFoo.BAR.getClass())).isEqualTo(SubFoo.BAZ);
	}

	@Test
	void stringToEnumEmptyString() {
		assertThat(conversionService.convert("", Foo.class)).isNull();
	}

	@Test
	void enumToString() {
		assertThat(conversionService.convert(Foo.BAR, String.class)).isEqualTo("BAR");
	}

	@Test
	void integerToEnum() {
		assertThat(conversionService.convert(0, Foo.class)).isEqualTo(Foo.BAR);
	}

	@Test
	void integerToEnumWithSubclass() {
		assertThat(conversionService.convert(1, SubFoo.BAR.getClass())).isEqualTo(SubFoo.BAZ);
	}

	@Test
	void integerToEnumNull() {
		assertThat(conversionService.convert(null, Foo.class)).isNull();
	}

	@Test
	void enumToInteger() {
		assertThat(conversionService.convert(Foo.BAR, Integer.class)).isEqualTo(0);
	}

	@Test
	void stringToEnumSet() throws Exception {
		assertThat(conversionService.convert("BAR", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("enumSet")))).isEqualTo(EnumSet.of(Foo.BAR));
	}

	@Test
	void stringToLocale() {
		assertThat(conversionService.convert("en", Locale.class)).isEqualTo(Locale.ENGLISH);
	}

	@Test
	void stringToLocaleWithCountry() {
		assertThat(conversionService.convert("en_US", Locale.class)).isEqualTo(Locale.US);
	}

	@Test
	void stringToLocaleWithLanguageTag() {
		assertThat(conversionService.convert("en-US", Locale.class)).isEqualTo(Locale.US);
	}

	@Test
	void stringToCharset() {
		assertThat(conversionService.convert("UTF-8", Charset.class)).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	void charsetToString() {
		assertThat(conversionService.convert(StandardCharsets.UTF_8, String.class)).isEqualTo("UTF-8");
	}

	@Test
	void stringToCurrency() {
		assertThat(conversionService.convert("EUR", Currency.class)).isEqualTo(Currency.getInstance("EUR"));
	}

	@Test
	void currencyToString() {
		assertThat(conversionService.convert(Currency.getInstance("USD"), String.class)).isEqualTo("USD");
	}

	@Test
	void stringToString() {
		String str = "test";
		assertThat(conversionService.convert(str, String.class)).isSameAs(str);
	}

	@Test
	void uuidToStringAndStringToUuid() {
		UUID uuid = UUID.randomUUID();
		String convertToString = conversionService.convert(uuid, String.class);
		UUID convertToUUID = conversionService.convert(convertToString, UUID.class);
		assertThat(convertToUUID).isEqualTo(uuid);
	}

	@Test
	void stringToPatternEmptyString() {
		assertThat(conversionService.convert("", Pattern.class)).isNull();
	}

	@Test
	void stringToPattern() {
		String pattern = "\\s";
		assertThat(conversionService.convert(pattern, Pattern.class))
				.isInstanceOfSatisfying(Pattern.class, regex -> assertThat(regex.pattern()).isEqualTo(pattern));
	}

	@Test
	void patternToString() {
		String regex = "\\d";
		assertThat(conversionService.convert(Pattern.compile(regex), String.class)).isEqualTo(regex);
	}

	@Test
	void numberToNumber() {
		assertThat(conversionService.convert(1, Long.class)).isEqualTo(Long.valueOf(1));
	}

	@Test
	void numberToNumberNotSupportedNumber() {
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> conversionService.convert(1, CustomNumber.class));
	}

	@Test
	void numberToCharacter() {
		assertThat(conversionService.convert(65, Character.class)).isEqualTo(Character.valueOf('A'));
	}

	@Test
	void characterToNumber() {
		assertThat(conversionService.convert('A', Integer.class)).isEqualTo(65);
	}

	// collection conversion

	@Test
	void convertArrayToCollectionInterface() {
		@SuppressWarnings("unchecked")
		Collection<String> result = conversionService.convert(new String[] {"1", "2", "3"}, Collection.class);
		assertThat(result).isEqualTo(List.of("1", "2", "3"));
		assertThat(result).isExactlyInstanceOf(ArrayList.class).containsExactly("1", "2", "3");
	}

	@Test
	void convertArrayToSetInterface() {
		@SuppressWarnings("unchecked")
		Collection<String> result = conversionService.convert(new String[] {"1", "2", "3"}, Set.class);
		assertThat(result).isExactlyInstanceOf(LinkedHashSet.class).containsExactly("1", "2", "3");
	}

	@Test
	void convertArrayToListInterface() {
		@SuppressWarnings("unchecked")
		List<String> result = conversionService.convert(new String[] {"1", "2", "3"}, List.class);
		assertThat(result).isExactlyInstanceOf(ArrayList.class).containsExactly("1", "2", "3");
	}

	@Test
	void convertArrayToCollectionGenericTypeConversion() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert(new String[] {"1", "2", "3"},
				TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getDeclaredField("genericList")));
		assertThat(result).isExactlyInstanceOf(ArrayList.class).containsExactly(1, 2, 3);
	}

	@Test
	void convertArrayToStream() throws Exception {
		String[] source = {"1", "3", "4"};
		@SuppressWarnings("unchecked")
		Stream<Integer> result = (Stream<Integer>) this.conversionService.convert(source,
				TypeDescriptor.valueOf(String[].class), new TypeDescriptor(getClass().getDeclaredField("genericStream")));
		assertThat(result).containsExactly(1, 3, 4);
	}

	@Test
	void spr7766() throws Exception {
		conversionService.addConverter(new ColorConverter());
		@SuppressWarnings("unchecked")
		List<Color> colors = (List<Color>) conversionService.convert(new String[] {"ffffff", "#000000"},
				TypeDescriptor.valueOf(String[].class),
				new TypeDescriptor(new MethodParameter(getClass().getMethod("handlerMethod", List.class), 0)));
		assertThat(colors).containsExactly(Color.WHITE, Color.BLACK);
	}

	@Test
	void convertArrayToCollectionImpl() {
		@SuppressWarnings("unchecked")
		ArrayList<String> result = conversionService.convert(new String[] {"1", "2", "3"}, ArrayList.class);
		assertThat(result).isExactlyInstanceOf(ArrayList.class).containsExactly("1", "2", "3");
	}

	@Test
	void convertArrayToAbstractCollection() {
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> conversionService.convert(new String[]{"1", "2", "3"}, AbstractList.class));
	}

	@Test
	void convertArrayToString() {
		String result = conversionService.convert(new String[] {"1", "2", "3"}, String.class);
		assertThat(result).isEqualTo("1,2,3");
	}

	@Test
	void convertArrayToStringWithElementConversion() {
		String result = conversionService.convert(new Integer[] {1, 2, 3}, String.class);
		assertThat(result).isEqualTo("1,2,3");
	}

	@Test
	void convertEmptyArrayToString() {
		String result = conversionService.convert(new String[0], String.class);
		assertThat(result).isEmpty();
	}

	@Test
	void convertStringToArray() {
		String[] result = conversionService.convert("1,2,3", String[].class);
		assertThat(result).containsExactly("1", "2", "3");
	}

	@Test
	void convertStringToArrayWithElementConversion() {
		Integer[] result = conversionService.convert("1,2,3", Integer[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertStringToPrimitiveArrayWithElementConversion() {
		int[] result = conversionService.convert("1,2,3", int[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertEmptyStringToArray() {
		String[] result = conversionService.convert("", String[].class);
		assertThat(result).isEmpty();
	}

	@Test
	void convertArrayToObject() {
		Object[] array = new Object[] {3L};
		Object result = conversionService.convert(array, Long.class);
		assertThat(result).isEqualTo(3L);
	}

	@Test
	void convertArrayToObjectWithElementConversion() {
		String[] array = new String[] {"3"};
		Integer result = conversionService.convert(array, Integer.class);
		assertThat(result).isEqualTo(3);
	}

	@Test
	void convertArrayToObjectAssignableTargetType() {
		Long[] array = new Long[] {3L};
		Long[] result = (Long[]) conversionService.convert(array, Object.class);
		assertThat(result).isEqualTo(array);
	}

	@Test
	void convertObjectToArray() {
		Object[] result = conversionService.convert(3L, Object[].class);
		assertThat(result).containsExactly(3L);
	}

	@Test
	void convertObjectToArrayWithElementConversion() {
		Integer[] result = conversionService.convert(3L, Integer[].class);
		assertThat(result).containsExactly(3);
	}

	@Test
	void convertCollectionToArray() {
		List<String> list = List.of("1", "2", "3");
		String[] result = conversionService.convert(list, String[].class);
		assertThat(result).containsExactly("1", "2", "3");
	}

	@Test
	void convertCollectionToArrayWithElementConversion() {
		List<String> list = List.of("1", "2", "3");
		Integer[] result = conversionService.convert(list, Integer[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertCollectionToString() {
		List<String> list = List.of("foo", "bar");
		String result = conversionService.convert(list, String.class);
		assertThat(result).isEqualTo("foo,bar");
	}

	@Test
	void convertCollectionToStringWithElementConversion() throws Exception {
		List<Integer> list = List.of(3, 5);
		String result = (String) conversionService.convert(list,
				new TypeDescriptor(getClass().getField("genericList")), TypeDescriptor.valueOf(String.class));
		assertThat(result).isEqualTo("3,5");
	}

	@Test
	void convertStringToCollection() {
		@SuppressWarnings("unchecked")
		List<String> result = conversionService.convert("1,2,3", List.class);
		assertThat(result).containsExactly("1", "2", "3");
	}

	@Test
	void convertStringToCollectionWithElementConversion() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert("1,2,3", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertEmptyStringToCollection() {
		Collection<?> result = conversionService.convert("", Collection.class);
		assertThat(result).isEmpty();
	}

	@Test
	void convertCollectionToObject() {
		List<Long> list = Collections.singletonList(3L);
		Long result = conversionService.convert(list, Long.class);
		assertThat(result).isEqualTo(3L);
	}

	@Test
	void convertCollectionToObjectWithElementConversion() {
		List<String> list = Collections.singletonList("3");
		Integer result = conversionService.convert(list, Integer.class);
		assertThat(result).isEqualTo(3);
	}

	@Test
	void convertCollectionToObjectAssignableTarget() throws Exception {
		Collection<String> source = List.of("foo");
		Object result = conversionService.convert(source, new TypeDescriptor(getClass().getField("assignableTarget")));
		assertThat(result).isSameAs(source);
	}

	@Test
	void convertCollectionToObjectWithCustomConverter() {
		List<String> source = List.of("A", "B");
		conversionService.addConverter(List.class, ListWrapper.class, ListWrapper::new);
		ListWrapper result = conversionService.convert(source, ListWrapper.class);
		assertThat(result.getList()).isSameAs(source);
	}

	@Test
	void convertObjectToCollection() {
		@SuppressWarnings("unchecked")
		List<Long> result = conversionService.convert(3L, List.class);
		assertThat(result).containsExactly(3L);
	}

	@Test
	void convertObjectToCollectionWithElementConversion() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert(3L, TypeDescriptor.valueOf(Long.class),
				new TypeDescriptor(getClass().getField("genericList")));
		assertThat(result).containsExactly(3);
	}

	@Test
	void convertStringArrayToIntegerArray() {
		Integer[] result = conversionService.convert(new String[] {"1", "2", "3"}, Integer[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertStringArrayToIntArray() {
		int[] result = conversionService.convert(new String[] {"1", "2", "3"}, int[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertIntArrayToStringArray() {
		String[] result = conversionService.convert(new int[] {1, 2, 3}, String[].class);
		assertThat(result).containsExactly("1", "2", "3");
	}

	@Test
	void convertIntegerArrayToIntegerArray() {
		Integer[] result = conversionService.convert(new Integer[] {1, 2, 3}, Integer[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertIntegerArrayToIntArray() {
		int[] result = conversionService.convert(new Integer[] {1, 2, 3}, int[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertIntArrayToIntegerArray() {
		Integer[] result = conversionService.convert(new int[] {1, 2}, Integer[].class);
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	void convertObjectArrayToIntegerArray() {
		Integer[] result = conversionService.convert(new Object[] {1, 2, 3}, Integer[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertObjectArrayToIntArray() {
		int[] result = conversionService.convert(new Object[] {1, 2, 3}, int[].class);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test  // gh-33212
	void convertIntArrayToObjectArray() {
		Object[] result = conversionService.convert(new int[] {1, 2}, Object[].class);
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	void convertIntArrayToFloatArray() {
		Float[] result = conversionService.convert(new int[] {1, 2}, Float[].class);
		assertThat(result).containsExactly(1.0F, 2.0F);
	}

	@Test
	void convertIntArrayToPrimitiveFloatArray() {
		float[] result = conversionService.convert(new int[] {1, 2}, float[].class);
		assertThat(result).containsExactly(1.0F, 2.0F);
	}

	@Test
	void convertPrimitiveByteArrayToByteWrapperArray() {
		byte[] byteArray = {1, 2, 3};
		Byte[] converted = conversionService.convert(byteArray, Byte[].class);
		assertThat(converted).isEqualTo(new Byte[]{1, 2, 3});
	}

	@Test  // gh-14200, SPR-9566
	void convertPrimitiveByteArrayToPrimitiveByteArray() {
		byte[] byteArray = new byte[] {1, 2, 3};
		byte[] result = conversionService.convert(byteArray, byte[].class);
		assertThat(result).isSameAs(byteArray);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test  // gh-14200, SPR-9566
	void convertIntArrayToIntArray() {
		int[] intArray = new int[] {1, 2, 3};
		int[] result = conversionService.convert(intArray, int[].class);
		assertThat(result).isSameAs(intArray);
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void convertListOfNonStringifiable() {
		List<Object> list = List.of(new TestEntity(1L), new TestEntity(2L));
		assertThat(conversionService.canConvert(list.getClass(), String.class)).isTrue();
		try {
			conversionService.convert(list, String.class);
		}
		catch (ConversionFailedException ex) {
			assertThat(ex.getMessage()).contains(list.getClass().getName());
			assertThat(ex.getCause()).isInstanceOf(ConverterNotFoundException.class);
			assertThat(ex.getCause().getMessage()).contains(TestEntity.class.getName());
		}
	}

	@Test
	void convertListOfStringToString() {
		List<String> list = List.of("Foo", "Bar");
		assertThat(conversionService.canConvert(list.getClass(), String.class)).isTrue();
		String result = conversionService.convert(list, String.class);
		assertThat(result).isEqualTo("Foo,Bar");
	}

	@Test
	void convertListOfListToString() {
		List<String> list1 = List.of("Foo", "Bar");
		List<String> list2 = List.of("Baz", "Boop");
		List<List<String>> list = List.of(list1, list2);
		assertThat(conversionService.canConvert(list.getClass(), String.class)).isTrue();
		String result = conversionService.convert(list, String.class);
		assertThat(result).isEqualTo("Foo,Bar,Baz,Boop");
	}

	@Test
	void convertCollectionToCollection() throws Exception {
		Set<String> foo = new LinkedHashSet<>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		@SuppressWarnings("unchecked")
		List<Integer> bar = (List<Integer>) conversionService.convert(foo,
				new TypeDescriptor(getClass().getField("genericList")));
		assertThat(bar).containsExactly(1, 2, 3);
	}

	@Test
	void convertCollectionToCollectionNull() throws Exception {
		@SuppressWarnings("unchecked")
		List<Integer> bar = (List<Integer>) conversionService.convert(null,
				TypeDescriptor.valueOf(LinkedHashSet.class), new TypeDescriptor(getClass().getField("genericList")));
		assertThat(bar).isNull();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void convertCollectionToCollectionNotGeneric() {
		Set<String> foo = new LinkedHashSet<>();
		foo.add("1");
		foo.add("2");
		foo.add("3");
		List bar = (List) conversionService.convert(foo, TypeDescriptor.valueOf(LinkedHashSet.class), TypeDescriptor.valueOf(List.class));
		assertThat(bar).containsExactly("1", "2", "3");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void convertCollectionToCollectionSpecialCaseSourceImpl() throws Exception {
		Map map = new LinkedHashMap();
		map.put("1", "1");
		map.put("2", "2");
		map.put("3", "3");
		Collection values = map.values();
		List<Integer> bar = (List<Integer>) conversionService.convert(values,
				TypeDescriptor.forObject(values), new TypeDescriptor(getClass().getField("genericList")));
		assertThat(bar).containsExactly(1, 2, 3);
	}

	@Test
	void collection() {
		List<String> strings = List.of("3", "9");
		@SuppressWarnings("unchecked")
		List<Integer> integers = (List<Integer>) conversionService.convert(strings,
				TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
		assertThat(integers).containsExactly(3, 9);
	}

	@Test
	void convertMapToMap() throws Exception {
		Map<String, String> foo = Map.of("1", "BAR", "2", "BAZ");
		@SuppressWarnings("unchecked")
		Map<Integer, Foo> map = (Map<Integer, Foo>) conversionService.convert(foo,
				TypeDescriptor.forObject(foo), new TypeDescriptor(getClass().getField("genericMap")));
		assertThat(map).contains(entry(1, Foo.BAR), entry(2, Foo.BAZ));
	}

	@Test
	void convertHashMapValuesToList() {
		Map<String, Integer> hashMap = new LinkedHashMap<>();
		hashMap.put("1", 1);
		hashMap.put("2", 2);
		@SuppressWarnings("unchecked")
		List<Integer> converted = conversionService.convert(hashMap.values(), List.class);
		assertThat(converted).containsExactly(1, 2);
	}

	@Test
	void map() {
		Map<String, String> strings = new HashMap<>();
		strings.put("3", "9");
		strings.put("6", "31");
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> integers = (Map<Integer, Integer>) conversionService.convert(strings,
				TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(Integer.class)));
		assertThat(integers).contains(entry(3, 9), entry(6, 31));
	}

	@Test
	void convertPropertiesToString() {
		Properties foo = new Properties();
		foo.setProperty("1", "BAR");
		foo.setProperty("2", "BAZ");
		String result = conversionService.convert(foo, String.class);
		assertThat(result).contains("1=BAR", "2=BAZ");
	}

	@Test
	void convertStringToProperties() {
		Properties result = conversionService.convert("""
				a=b
				c=2
				d=""", Properties.class);
		assertThat(result).contains(entry("a", "b"), entry("c", "2"), entry("d", ""));
	}

	@Test
	void convertStringToPropertiesWithLeadingSpaces() {
		Properties result = conversionService.convert("""
				\s  foo=bar
				\s   bar=baz
				\s    baz=boo""", Properties.class);
		assertThat(result).contains(entry("foo", "bar"), entry("bar", "baz"), entry("baz", "boo"));
	}

	// generic object conversion

	@Test
	void convertObjectToStringWithValueOfMethodPresentUsingToString() {
		ISBN.reset();
		assertThat(conversionService.convert(new ISBN("123456789"), String.class)).isEqualTo("123456789");

		assertThat(ISBN.constructorCount).as("constructor invocations").isEqualTo(1);
		assertThat(ISBN.valueOfCount).as("valueOf() invocations").isEqualTo(0);
		assertThat(ISBN.toStringCount).as("toString() invocations").isEqualTo(1);
	}

	/**
	 * @see org.springframework.core.convert.support.ObjectToObjectConverterTests
	 */
	@Test
	void convertObjectToObjectUsingValueOfMethod() {
		ISBN.reset();
		assertThat(conversionService.convert("123456789", ISBN.class)).isEqualTo(new ISBN("123456789"));

		assertThat(ISBN.valueOfCount).as("valueOf() invocations").isEqualTo(1);
		// valueOf() invokes the constructor
		assertThat(ISBN.constructorCount).as("constructor invocations").isEqualTo(2);
		assertThat(ISBN.toStringCount).as("toString() invocations").isEqualTo(0);
	}

	@Test
	void convertObjectToStringUsingToString() {
		SSN.reset();
		assertThat(conversionService.convert(new SSN("123456789"), String.class)).isEqualTo("123456789");

		assertThat(SSN.constructorCount).as("constructor invocations").isEqualTo(1);
		assertThat(SSN.toStringCount).as("toString() invocations").isEqualTo(1);
	}

	@Test
	void convertObjectToObjectUsingObjectConstructor() {
		SSN.reset();
		assertThat(conversionService.convert("123456789", SSN.class)).isEqualTo(new SSN("123456789"));

		assertThat(SSN.constructorCount).as("constructor invocations").isEqualTo(2);
		assertThat(SSN.toStringCount).as("toString() invocations").isEqualTo(0);
	}

	@Test
	void convertStringToTimezone() {
		assertThat(conversionService.convert("GMT+2", TimeZone.class).getID()).isEqualTo("GMT+02:00");
	}

	@Test
	void convertObjectToStringWithJavaTimeOfMethodPresent() {
		assertThat(conversionService.convert(ZoneId.of("GMT+1"), String.class)).startsWith("GMT+");
	}

	@Test
	void convertObjectToStringNotSupported() {
		assertThat(conversionService.canConvert(TestEntity.class, String.class)).isFalse();
	}

	@Test
	void convertObjectToObjectWithJavaTimeOfMethod() {
		assertThat(conversionService.convert("GMT+1", ZoneId.class)).isEqualTo(ZoneId.of("GMT+1"));
	}

	@Test
	void convertObjectToObjectNoValueOfMethodOrConstructor() {
		assertThatExceptionOfType(ConverterNotFoundException.class)
				.isThrownBy(() -> conversionService.convert(3L, SSN.class));
	}

	@Test
	void convertObjectToObjectFinderMethod() {
		TestEntity e = conversionService.convert(1L, TestEntity.class);
		assertThat(e.getId()).isEqualTo(Long.valueOf(1));
	}

	@Test
	void convertObjectToObjectFinderMethodWithNull() {
		TestEntity entity = (TestEntity) conversionService.convert(null,
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(TestEntity.class));
		assertThat(entity).isNull();
	}

	@Test
	void convertObjectToObjectFinderMethodWithIdConversion() {
		TestEntity entity = conversionService.convert("1", TestEntity.class);
		assertThat(entity.getId()).isEqualTo(Long.valueOf(1));
	}

	@Test
	void convertCharArrayToString() {
		String converted = conversionService.convert(new char[] {'a', 'b', 'c'}, String.class);
		assertThat(converted).isEqualTo("a,b,c");
	}

	@Test
	void convertStringToCharArray() {
		char[] converted = conversionService.convert("a,b,c", char[].class);
		assertThat(converted).containsExactly('a', 'b', 'c');
	}

	@Test
	void convertStringToCustomCharArray() {
		conversionService.addConverter(String.class, char[].class, String::toCharArray);
		char[] converted = conversionService.convert("abc", char[].class);
		assertThat(converted).containsExactly('a', 'b', 'c');
	}

	@Test
	@SuppressWarnings("unchecked")
	void multidimensionalArrayToListConversionShouldConvertEntriesCorrectly() {
		String[][] grid = new String[][] {{"1", "2", "3", "4"}, {"5", "6", "7", "8"}, {"9", "10", "11", "12"}};
		List<String[]> converted = conversionService.convert(grid, List.class);
		String[][] convertedBack = conversionService.convert(converted, String[][].class);
		assertThat(convertedBack).isEqualTo(grid);
	}

	@Test
	void convertCannotOptimizeArray() {
		conversionService.addConverter(Byte.class, Byte.class, source -> (byte) (source + 1));
		byte[] byteArray = {1, 2, 3};
		byte[] converted = conversionService.convert(byteArray, byte[].class);
		assertThat(converted).isNotSameAs(byteArray);
		assertThat(converted).containsExactly(2, 3, 4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void convertObjectToOptional() {
		Method method = ClassUtils.getMethod(TestEntity.class, "handleOptionalValue", Optional.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		TypeDescriptor descriptor = new TypeDescriptor(parameter);
		Object actual = conversionService.convert("1,2,3", TypeDescriptor.valueOf(String.class), descriptor);
		assertThat(actual.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional<List<Integer>>) actual)).contains(List.of(1, 2, 3));
	}

	@Test
	void convertObjectToOptionalNull() {
		assertThat(conversionService.convert(null, TypeDescriptor.valueOf(Object.class),
				TypeDescriptor.valueOf(Optional.class))).isSameAs(Optional.empty());
		assertThat((Object) conversionService.convert(null, Optional.class)).isSameAs(Optional.empty());
	}

	@Test
	void convertExistingOptional() {
		assertThat(conversionService.convert(Optional.empty(), TypeDescriptor.valueOf(Object.class),
				TypeDescriptor.valueOf(Optional.class))).isSameAs(Optional.empty());
		assertThat((Object) conversionService.convert(Optional.empty(), Optional.class)).isSameAs(Optional.empty());
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
		public boolean equals(@Nullable Object o) {
			if (!(o instanceof SSN ssn)) {
				return false;
			}
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
		public boolean equals(@Nullable Object o) {
			if (!(o instanceof ISBN isbn)) {
				return false;
			}
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
