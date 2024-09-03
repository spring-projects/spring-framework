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

package org.springframework.core.convert.support;

import java.awt.Color;
import java.awt.SystemColor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link GenericConversionService}.
 *
 * <p>In this package for access to package-local converter implementations.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author David Haraburda
 * @author Sam Brannen
 */
class GenericConversionServiceTests {

	private final GenericConversionService conversionService = new GenericConversionService();


	@Test
	void canConvert() {
		assertThat(conversionService.canConvert(String.class, Integer.class)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(String.class, Integer.class)).isTrue();
	}

	@Test
	void canConvertAssignable() {
		assertThat(conversionService.canConvert(String.class, String.class)).isTrue();
		assertThat(conversionService.canConvert(Integer.class, Number.class)).isTrue();
		assertThat(conversionService.canConvert(boolean.class, boolean.class)).isTrue();
		assertThat(conversionService.canConvert(boolean.class, Boolean.class)).isTrue();
	}

	@Test
	void canConvertFromClassSourceTypeToNullTargetType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(String.class, null));
	}

	@Test
	void canConvertFromTypeDescriptorSourceTypeToNullTargetType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(TypeDescriptor.valueOf(String.class), null));
	}

	@Test
	void canConvertNullSourceType() {
		assertThat(conversionService.canConvert(null, Integer.class)).isTrue();
		assertThat(conversionService.canConvert(null, TypeDescriptor.valueOf(Integer.class))).isTrue();
	}

	@Test
	void convert() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.convert("3", Integer.class)).isEqualTo(3);
	}

	@Test
	void convertNullSource() {
		assertThat(conversionService.convert(null, Integer.class)).isNull();
	}

	@Test
	void convertNullSourcePrimitiveTarget() {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert(null, int.class));
	}

	@Test
	void convertNullSourcePrimitiveTargetTypeDescriptor() {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(int.class)));
	}

	@Test
	void convertNotNullSourceNullSourceTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", null, TypeDescriptor.valueOf(int.class)));
	}

	@Test
	void convertAssignableSource() {
		assertThat(conversionService.convert(false, boolean.class)).isFalse();
		assertThat(conversionService.convert(false, Boolean.class)).isFalse();
	}

	@Test
	void converterNotFound() {
		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("3", Integer.class));
	}

	@Test
	void addConverterNoSourceTargetClassInfoAvailable() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.addConverter(new UntypedConverter()));
	}

	@Test
	void sourceTypeIsVoid() {
		assertThat(conversionService.canConvert(void.class, String.class)).isFalse();
	}

	@Test
	void targetTypeIsVoid() {
		assertThat(conversionService.canConvert(String.class, void.class)).isFalse();
	}

	@Test
	void convertNull() {
		assertThat(conversionService.convert(null, Integer.class)).isNull();
	}

	@Test
	void convertToNullTargetClass() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", (Class<?>) null));
	}

	@Test
	void convertToNullTargetTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", TypeDescriptor.valueOf(String.class), null));
	}

	@Test
	void convertWrongSourceTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(Long.class)));
	}

	@Test
	void convertWrongTypeArgument() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert("BOGUS", Integer.class));
	}

	@Test
	void convertSuperSourceType() {
		conversionService.addConverter(CharSequence.class, Integer.class, source -> Integer.valueOf(source.toString()));
		Integer result = conversionService.convert("3", Integer.class);
		assertThat(result).isEqualTo(3);
	}

	// SPR-8718
	@Test
	void convertSuperTarget() {
		conversionService.addConverter(new ColorConverter());
		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("#000000", SystemColor.class));
	}

	@Test
	void convertObjectToPrimitive() {
		assertThat(conversionService.canConvert(String.class, boolean.class)).isFalse();
		conversionService.addConverter(new StringToBooleanConverter());
		assertThat(conversionService.canConvert(String.class, boolean.class)).isTrue();
		Boolean b = conversionService.convert("true", boolean.class);
		assertThat(b).isTrue();
		assertThat(conversionService.canConvert(TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(boolean.class))).isTrue();
		b = (Boolean) conversionService.convert("true", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(boolean.class));
		assertThat(b).isTrue();
	}

	@Test
	void convertObjectToPrimitiveViaConverterFactory() {
		assertThat(conversionService.canConvert(String.class, int.class)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(String.class, int.class)).isTrue();
		Integer three = conversionService.convert("3", int.class);
		assertThat(three).isEqualTo(3);
	}

	@Test
	void genericConverterDelegatingBackToConversionServiceConverterNotFound() {
		conversionService.addConverter(new ObjectToArrayConverter(conversionService));
		assertThat(conversionService.canConvert(String.class, Integer[].class)).isFalse();
		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("3,4,5", Integer[].class));
	}

	@Test
	void listToIterableConversion() {
		List<Object> raw = List.of("one", "two");
		Object converted = conversionService.convert(raw, Iterable.class);
		assertThat(converted).isSameAs(raw);
	}

	@Test
	void listToObjectConversion() {
		List<Object> raw = List.of("one", "two");
		Object converted = conversionService.convert(raw, Object.class);
		assertThat(converted).isSameAs(raw);
	}

	@Test
	void mapToObjectConversion() {
		Map<Object, Object> raw = Map.of("key", "value");
		Object converted = conversionService.convert(raw, Object.class);
		assertThat(converted).isSameAs(raw);
	}

	@Test
	void interfaceToString() {
		conversionService.addConverter(new MyBaseInterfaceToStringConverter());
		conversionService.addConverter(new ObjectToStringConverter());
		Object converted = conversionService.convert(new MyInterfaceImplementer(), String.class);
		assertThat(converted).isEqualTo("RESULT");
	}

	@Test
	void interfaceArrayToStringArray() {
		conversionService.addConverter(new MyBaseInterfaceToStringConverter());
		conversionService.addConverter(new ArrayToArrayConverter(conversionService));
		String[] converted = conversionService.convert(new MyInterface[] {new MyInterfaceImplementer()}, String[].class);
		assertThat(converted[0]).isEqualTo("RESULT");
	}

	@Test
	void objectArrayToStringArray() {
		conversionService.addConverter(new MyBaseInterfaceToStringConverter());
		conversionService.addConverter(new ArrayToArrayConverter(conversionService));
		String[] converted = conversionService.convert(new MyInterfaceImplementer[] {new MyInterfaceImplementer()}, String[].class);
		assertThat(converted[0]).isEqualTo("RESULT");
	}

	@Test
	void stringArrayToResourceArray() {
		conversionService.addConverter(new MyStringArrayToResourceArrayConverter());
		Resource[] converted = conversionService.convert(new String[] { "x1", "z3" }, Resource[].class);
		List<String> descriptions = Arrays.stream(converted).map(Resource::getDescription).sorted(naturalOrder()).collect(toList());
		assertThat(descriptions).isEqualTo(Arrays.asList("1", "3"));
	}

	@Test
	void stringArrayToIntegerArray() {
		conversionService.addConverter(new MyStringArrayToIntegerArrayConverter());
		Integer[] converted = conversionService.convert(new String[] {"x1", "z3"}, Integer[].class);
		assertThat(converted).isEqualTo(new Integer[] { 1, 3 });
	}

	@Test
	void stringToIntegerArray() {
		conversionService.addConverter(new MyStringToIntegerArrayConverter());
		Integer[] converted = conversionService.convert("x1,z3", Integer[].class);
		assertThat(converted).isEqualTo(new Integer[] { 1, 3 });
	}

	@Test
	void wildcardMap() throws Exception {
		Map<String, String> input = new LinkedHashMap<>();
		input.put("key", "value");
		Object converted = conversionService.convert(input, new TypeDescriptor(getClass().getField("wildcardMap")));
		assertThat(converted).isEqualTo(input);
	}

	@Test
	void stringToString() {
		String value = "myValue";
		String result = conversionService.convert(value, String.class);
		assertThat(result).isSameAs(value);
	}

	@Test
	void stringToObject() {
		String value = "myValue";
		Object result = conversionService.convert(value, Object.class);
		assertThat(result).isSameAs(value);
	}

	@Test
	void ignoreCopyConstructor() {
		WithCopyConstructor value = new WithCopyConstructor();
		Object result = conversionService.convert(value, WithCopyConstructor.class);
		assertThat(result).isSameAs(value);
	}

	@Test
	void emptyListToArray() {
		conversionService.addConverter(new CollectionToArrayConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = TypeDescriptor.valueOf(String[].class);
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(((String[]) conversionService.convert(list, sourceType, targetType))).isEmpty();
	}

	@Test
	void emptyListToObject() {
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = TypeDescriptor.valueOf(Integer.class);
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(conversionService.convert(list, sourceType, targetType)).isNull();
	}

	@Test
	void stringToArrayCanConvert() {
		conversionService.addConverter(new StringToArrayConverter(conversionService));
		assertThat(conversionService.canConvert(String.class, Integer[].class)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(String.class, Integer[].class)).isTrue();
	}

	@Test
	void stringToCollectionCanConvert() throws Exception {
		conversionService.addConverter(new StringToCollectionConverter(conversionService));
		assertThat(conversionService.canConvert(String.class, Collection.class)).isTrue();
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("integerCollection"));
		assertThat(conversionService.canConvert(TypeDescriptor.valueOf(String.class), targetType)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(TypeDescriptor.valueOf(String.class), targetType)).isTrue();
	}

	@Test
	void convertiblePairsInSet() {
		Set<GenericConverter.ConvertiblePair> set = new HashSet<>();
		set.add(new GenericConverter.ConvertiblePair(Number.class, String.class));
		assert set.contains(new GenericConverter.ConvertiblePair(Number.class, String.class));
	}

	@Test
	void convertiblePairEqualsAndHash() {
		GenericConverter.ConvertiblePair pair = new GenericConverter.ConvertiblePair(Number.class, String.class);
		GenericConverter.ConvertiblePair pairEqual = new GenericConverter.ConvertiblePair(Number.class, String.class);
		assertThat(pairEqual).isEqualTo(pair);
		assertThat(pairEqual.hashCode()).isEqualTo(pair.hashCode());
	}

	@Test
	void convertiblePairDifferentEqualsAndHash() {
		GenericConverter.ConvertiblePair pair = new GenericConverter.ConvertiblePair(Number.class, String.class);
		GenericConverter.ConvertiblePair pairOpposite = new GenericConverter.ConvertiblePair(String.class, Number.class);
		assertThat(pair.equals(pairOpposite)).isFalse();
		assertThat(pair.hashCode()).isNotEqualTo(pairOpposite.hashCode());
	}

	@Test
	void canConvertIllegalArgumentNullTargetTypeFromClass() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(String.class, null));
	}

	@Test
	void canConvertIllegalArgumentNullTargetTypeFromTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(TypeDescriptor.valueOf(String.class), null));
	}

	@Test
	void removeConvertible() {
		conversionService.addConverter(new ColorConverter());
		assertThat(conversionService.canConvert(String.class, Color.class)).isTrue();
		conversionService.removeConvertible(String.class, Color.class);
		assertThat(conversionService.canConvert(String.class, Color.class)).isFalse();
	}

	@Test
	void conditionalConverter() {
		MyConditionalConverter converter = new MyConditionalConverter();
		conversionService.addConverter(new ColorConverter());
		conversionService.addConverter(converter);
		assertThat(conversionService.convert("#000000", Color.class)).isEqualTo(Color.BLACK);
		assertThat(converter.getMatchAttempts()).isGreaterThan(0);
	}

	@Test
	void conditionalConverterFactory() {
		MyConditionalConverterFactory converter = new MyConditionalConverterFactory();
		conversionService.addConverter(new ColorConverter());
		conversionService.addConverterFactory(converter);
		assertThat(conversionService.convert("#000000", Color.class)).isEqualTo(Color.BLACK);
		assertThat(converter.getMatchAttempts()).isGreaterThan(0);
		assertThat(converter.getNestedMatchAttempts()).isGreaterThan(0);
	}

	@Test
	void conditionalConverterCachingForDifferentAnnotationAttributes() throws Exception {
		conversionService.addConverter(new ColorConverter());
		conversionService.addConverter(new MyConditionalColorConverter());

		assertThat(conversionService.convert("000000xxxx",
				new TypeDescriptor(getClass().getField("activeColor")))).isEqualTo(Color.BLACK);
		assertThat(conversionService.convert(" #000000 ",
				new TypeDescriptor(getClass().getField("inactiveColor")))).isEqualTo(Color.BLACK);
		assertThat(conversionService.convert("000000yyyy",
				new TypeDescriptor(getClass().getField("activeColor")))).isEqualTo(Color.BLACK);
		assertThat(conversionService.convert("  #000000  ",
				new TypeDescriptor(getClass().getField("inactiveColor")))).isEqualTo(Color.BLACK);
	}

	@Test
	void shouldNotSupportNullConvertibleTypesFromNonConditionalGenericConverter() {
		GenericConverter converter = new NonConditionalGenericConverter();
		assertThatIllegalStateException().isThrownBy(() ->
				conversionService.addConverter(converter))
			.withMessage("Only conditional converters may return null convertible types");
	}

	@Test
	void conditionalConversionForAllTypes() {
		MyConditionalGenericConverter converter = new MyConditionalGenericConverter();
		conversionService.addConverter(converter);
		assertThat(conversionService.convert(3, Integer.class)).isEqualTo(3);
		assertThat(converter.getSourceTypes()).hasSizeGreaterThan(2);
		assertThat(converter.getSourceTypes().stream().allMatch(td -> Integer.class.equals(td.getType()))).isTrue();
	}

	@Test
	void convertOptimizeArray() {
		// SPR-9566
		byte[] byteArray = new byte[] { 1, 2, 3 };
		byte[] converted = conversionService.convert(byteArray, byte[].class);
		assertThat(converted).isSameAs(byteArray);
	}

	@Test
	void enumToStringConversion() {
		conversionService.addConverter(new EnumToStringConverter(conversionService));
		assertThat(conversionService.convert(MyEnum.A, String.class)).isEqualTo("A");
	}

	@Test
	void subclassOfEnumToString() throws Exception {
		conversionService.addConverter(new EnumToStringConverter(conversionService));
		assertThat(conversionService.convert(EnumWithSubclass.FIRST, String.class)).isEqualTo("FIRST");
	}

	@Test
	void enumWithInterfaceToStringConversion() {
		// SPR-9692
		conversionService.addConverter(new EnumToStringConverter(conversionService));
		conversionService.addConverter(new MyEnumInterfaceToStringConverter<MyEnum>());
		assertThat(conversionService.convert(MyEnum.A, String.class)).isEqualTo("1");
	}

	@Test
	void stringToEnumWithInterfaceConversion() {
		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		conversionService.addConverterFactory(new StringToMyEnumInterfaceConverterFactory());
		assertThat(conversionService.convert("1", MyEnum.class)).isEqualTo(MyEnum.A);
	}

	@Test
	void stringToEnumWithBaseInterfaceConversion() {
		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		conversionService.addConverterFactory(new StringToMyEnumBaseInterfaceConverterFactory());
		assertThat(conversionService.convert("base1", MyEnum.class)).isEqualTo(MyEnum.A);
	}

	@Test
	void convertNullAnnotatedStringToString() throws Exception {
		String source = null;
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("annotatedString"));
		TypeDescriptor targetType = TypeDescriptor.valueOf(String.class);
		conversionService.convert(source, sourceType, targetType);
	}

	@Test
	void multipleCollectionTypesFromSameSourceType() throws Exception {
		conversionService.addConverter(new MyStringToRawCollectionConverter());
		conversionService.addConverter(new MyStringToGenericCollectionConverter());
		conversionService.addConverter(new MyStringToStringCollectionConverter());
		conversionService.addConverter(new MyStringToIntegerCollectionConverter());

		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("integerCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
	}

	@Test
	void adaptedCollectionTypesFromSameSourceType() throws Exception {
		conversionService.addConverter(new MyStringToStringCollectionConverter());

		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));

		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("integerCollection"))));
	}

	@Test
	void genericCollectionAsSource() throws Exception {
		conversionService.addConverter(new MyStringToGenericCollectionConverter());

		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));

		// The following is unpleasant but a consequence of the generic collection converter above...
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("integerCollection")))).isEqualTo(Collections.singleton("testX"));
	}

	@Test
	void rawCollectionAsSource() throws Exception {
		conversionService.addConverter(new MyStringToRawCollectionConverter());

		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));

		// The following is unpleasant but a consequence of the raw collection converter above...
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("integerCollection")))).isEqualTo(Collections.singleton("testX"));
	}


	@ExampleAnnotation(active = true)
	public String annotatedString;

	@ExampleAnnotation(active = true)
	public Color activeColor;

	@ExampleAnnotation(active = false)
	public Color inactiveColor;

	public Map<String, ?> wildcardMap;

	@SuppressWarnings("rawtypes")
	public Collection rawCollection;

	public Collection<?> genericCollection;

	public Collection<String> stringCollection;

	public Collection<Integer> integerCollection;


	@Retention(RetentionPolicy.RUNTIME)
	private @interface ExampleAnnotation {

		boolean active();
	}


	private interface MyBaseInterface {
	}


	private interface MyInterface extends MyBaseInterface {
	}


	private static class MyInterfaceImplementer implements MyInterface {
	}


	private static class MyBaseInterfaceToStringConverter implements Converter<MyBaseInterface, String> {

		@Override
		public String convert(MyBaseInterface source) {
			return "RESULT";
		}
	}


	private static class MyStringArrayToResourceArrayConverter implements Converter<String[], Resource[]> {

		@Override
		public Resource[] convert(String[] source) {
			return Arrays.stream(source).map(s -> s.substring(1)).map(DescriptiveResource::new).toArray(Resource[]::new);
		}
	}


	private static class MyStringArrayToIntegerArrayConverter implements Converter<String[], Integer[]> {

		@Override
		public Integer[] convert(String[] source) {
			return Arrays.stream(source).map(s -> s.substring(1)).map(Integer::valueOf).toArray(Integer[]::new);
		}
	}


	private static class MyStringToIntegerArrayConverter implements Converter<String, Integer[]>	{

		@Override
		public Integer[] convert(String source) {
			String[] srcArray = StringUtils.commaDelimitedListToStringArray(source);
			return Arrays.stream(srcArray).map(s -> s.substring(1)).map(Integer::valueOf).toArray(Integer[]::new);
		}
	}


	private static class WithCopyConstructor {

		WithCopyConstructor() {}

		@SuppressWarnings("unused")
		WithCopyConstructor(WithCopyConstructor value) {}
	}


	private static class MyConditionalConverter implements Converter<String, Color>, ConditionalConverter {

		private int matchAttempts = 0;

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			matchAttempts++;
			return false;
		}

		@Override
		public Color convert(String source) {
			throw new IllegalStateException();
		}

		public int getMatchAttempts() {
			return matchAttempts;
		}
	}


	private static class NonConditionalGenericConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return null;
		}
	}


	private static class MyConditionalGenericConverter implements GenericConverter, ConditionalConverter {

		private final List<TypeDescriptor> sourceTypes = new ArrayList<>();

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			sourceTypes.add(sourceType);
			return false;
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return null;
		}

		public List<TypeDescriptor> getSourceTypes() {
			return sourceTypes;
		}
	}


	private static class MyConditionalConverterFactory implements ConverterFactory<String, Color>, ConditionalConverter {

		private MyConditionalConverter converter = new MyConditionalConverter();

		private int matchAttempts = 0;

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			matchAttempts++;
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Color> Converter<String, T> getConverter(Class<T> targetType) {
			return (Converter<String, T>) converter;
		}

		public int getMatchAttempts() {
			return matchAttempts;
		}

		public int getNestedMatchAttempts() {
			return converter.getMatchAttempts();
		}
	}

	private interface MyEnumBaseInterface {
		String getBaseCode();
	}


	private interface MyEnumInterface extends MyEnumBaseInterface {
		String getCode();
	}


	private enum MyEnum implements MyEnumInterface {

		A("1"),
		B("2"),
		C("3");

		private final String code;

		MyEnum(String code) {
			this.code = code;
		}

		@Override
		public String getCode() {
			return code;
		}

		@Override
		public String getBaseCode() {
			return "base" + code;
		}
	}


	private enum EnumWithSubclass {

		FIRST {
			@Override
			public String toString() {
				return "1st";
			}
		}
	}


	@SuppressWarnings("rawtypes")
	private static class MyStringToRawCollectionConverter implements Converter<String, Collection> {

		@Override
		public Collection convert(String source) {
			return Collections.singleton(source + "X");
		}
	}


	private static class MyStringToGenericCollectionConverter implements Converter<String, Collection<?>> {

		@Override
		public Collection<?> convert(String source) {
			return Collections.singleton(source + "X");
		}
	}


	private static class MyEnumInterfaceToStringConverter<T extends MyEnumInterface> implements Converter<T, String> {

		@Override
		public String convert(T source) {
			return source.getCode();
		}
	}


	private static class StringToMyEnumInterfaceConverterFactory implements ConverterFactory<String, MyEnumInterface> {

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T extends MyEnumInterface> Converter<String, T> getConverter(Class<T> targetType) {
			return new StringToMyEnumInterfaceConverter(targetType);
		}

		private static class StringToMyEnumInterfaceConverter<T extends Enum<?> & MyEnumInterface> implements Converter<String, T> {

			private final Class<T> enumType;

			public StringToMyEnumInterfaceConverter(Class<T> enumType) {
				this.enumType = enumType;
			}

			@Override
			public T convert(String source) {
				for (T value : enumType.getEnumConstants()) {
					if (value.getCode().equals(source)) {
						return value;
					}
				}
				return null;
			}
		}
	}


	private static class StringToMyEnumBaseInterfaceConverterFactory implements ConverterFactory<String, MyEnumBaseInterface> {

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T extends MyEnumBaseInterface> Converter<String, T> getConverter(Class<T> targetType) {
			return new StringToMyEnumBaseInterfaceConverter(targetType);
		}

		private static class StringToMyEnumBaseInterfaceConverter<T extends Enum<?> & MyEnumBaseInterface> implements Converter<String, T> {

			private final Class<T> enumType;

			public StringToMyEnumBaseInterfaceConverter(Class<T> enumType) {
				this.enumType = enumType;
			}

			@Override
			public T convert(String source) {
				for (T value : enumType.getEnumConstants()) {
					if (value.getBaseCode().equals(source)) {
						return value;
					}
				}
				return null;
			}
		}
	}


	private static class MyStringToStringCollectionConverter implements Converter<String, Collection<String>> {

		@Override
		public Collection<String> convert(String source) {
			return Collections.singleton(source + "X");
		}
	}


	private static class MyStringToIntegerCollectionConverter implements Converter<String, Collection<Integer>> {

		@Override
		public Collection<Integer> convert(String source) {
			return Collections.singleton(source.length());
		}
	}


	@SuppressWarnings("rawtypes")
	private static class UntypedConverter implements Converter {

		@Override
		public Object convert(Object source) {
			return source;
		}
	}


	private static class ColorConverter implements Converter<String, Color> {

		@Override
		public Color convert(String source) {
			return Color.decode(source.trim());
		}
	}


	private static class MyConditionalColorConverter implements Converter<String, Color>, ConditionalConverter {

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			ExampleAnnotation ann = targetType.getAnnotation(ExampleAnnotation.class);
			return (ann != null && ann.active());
		}

		@Override
		public Color convert(String source) {
			return Color.decode(source.substring(0, 6));
		}
	}
}
