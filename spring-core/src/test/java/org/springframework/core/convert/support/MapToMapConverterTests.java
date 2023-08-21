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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class MapToMapConverterTests {

	private final GenericConversionService conversionService = new GenericConversionService();


	@BeforeEach
	void setup() {
		conversionService.addConverter(new MapToMapConverter(conversionService));
	}


	@Test
	void scalarMap() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("1", "9");
		map.put("2", "37");
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarMapTarget"));

		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		try {
			conversionService.convert(map, sourceType, targetType);
		}
		catch (ConversionFailedException ex) {
			assertThat(ex.getCause() instanceof ConverterNotFoundException).isTrue();
		}

		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> result = (Map<Integer, Integer>) conversionService.convert(map, sourceType, targetType);
		assertThat(map.equals(result)).isFalse();
		assertThat((int) result.get(1)).isEqualTo(9);
		assertThat((int) result.get(2)).isEqualTo(37);
	}

	@Test
	void scalarMapNotGenericTarget() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("1", "9");
		map.put("2", "37");

		assertThat(conversionService.canConvert(Map.class, Map.class)).isTrue();
		assertThat((Map<?, ?>) conversionService.convert(map, Map.class)).isSameAs(map);
	}

	@Test
	void scalarMapNotGenericSourceField() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("1", "9");
		map.put("2", "37");
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("notGenericMapSource"));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarMapTarget"));

		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		try {
			conversionService.convert(map, sourceType, targetType);
		}
		catch (ConversionFailedException ex) {
			assertThat(ex.getCause() instanceof ConverterNotFoundException).isTrue();
		}

		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		Map<Integer, Integer> result = (Map<Integer, Integer>) conversionService.convert(map, sourceType, targetType);
		assertThat(map.equals(result)).isFalse();
		assertThat((int) result.get(1)).isEqualTo(9);
		assertThat((int) result.get(2)).isEqualTo(37);
	}

	@Test
	void collectionMap() throws Exception {
		Map<String, List<String>> map = new HashMap<>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("collectionMapTarget"));

		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		try {
			conversionService.convert(map, sourceType, targetType);
		}
		catch (ConversionFailedException ex) {
			assertThat(ex.getCause() instanceof ConverterNotFoundException).isTrue();
		}

		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		Map<Integer, List<Integer>> result = (Map<Integer, List<Integer>>) conversionService.convert(map, sourceType, targetType);
		assertThat(map.equals(result)).isFalse();
		assertThat(result.get(1)).isEqualTo(Arrays.asList(9, 12));
		assertThat(result.get(2)).isEqualTo(Arrays.asList(37, 23));
	}

	@Test
	void collectionMapSourceTarget() throws Exception {
		Map<String, List<String>> map = new HashMap<>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("sourceCollectionMapTarget"));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("collectionMapTarget"));

		assertThat(conversionService.canConvert(sourceType, targetType)).isFalse();
		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert(map, sourceType, targetType));

		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		Map<Integer, List<Integer>> result = (Map<Integer, List<Integer>>) conversionService.convert(map, sourceType, targetType);
		assertThat(map.equals(result)).isFalse();
		assertThat(result.get(1)).isEqualTo(Arrays.asList(9, 12));
		assertThat(result.get(2)).isEqualTo(Arrays.asList(37, 23));
	}

	@Test
	void collectionMapNotGenericTarget() throws Exception {
		Map<String, List<String>> map = new HashMap<>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));

		assertThat(conversionService.canConvert(Map.class, Map.class)).isTrue();
		assertThat((Map<?, ?>) conversionService.convert(map, Map.class)).isSameAs(map);
	}

	@Test
	void collectionMapNotGenericTargetCollectionToObjectInteraction() throws Exception {
		Map<String, List<String>> map = new HashMap<>();
		map.put("1", Arrays.asList("9", "12"));
		map.put("2", Arrays.asList("37", "23"));
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));

		assertThat(conversionService.canConvert(Map.class, Map.class)).isTrue();
		assertThat((Map<?, ?>) conversionService.convert(map, Map.class)).isSameAs(map);
	}

	@Test
	void emptyMap() throws Exception {
		Map<String, String> map = new HashMap<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyMapTarget"));

		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(conversionService.convert(map, sourceType, targetType)).isSameAs(map);
	}

	@Test
	void emptyMapNoTargetGenericInfo() throws Exception {
		Map<String, String> map = new HashMap<>();

		assertThat(conversionService.canConvert(Map.class, Map.class)).isTrue();
		assertThat((Map<?, ?>) conversionService.convert(map, Map.class)).isSameAs(map);
	}

	@Test
	void emptyMapDifferentTargetImplType() throws Exception {
		Map<String, String> map = new HashMap<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(map);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyMapDifferentTarget"));

		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, String> result = (LinkedHashMap<String, String>) conversionService.convert(map, sourceType, targetType);
		assertThat(result).isEqualTo(map);
		assertThat(result.getClass()).isEqualTo(LinkedHashMap.class);
	}

	@Test
	void noDefaultConstructorCopyNotRequired() throws Exception {
		// SPR-9284
		NoDefaultConstructorMap<String, Integer> map = new NoDefaultConstructorMap<>(
				Collections.<String, Integer>singletonMap("1", 1));
		TypeDescriptor sourceType = TypeDescriptor.map(NoDefaultConstructorMap.class,
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
		TypeDescriptor targetType = TypeDescriptor.map(NoDefaultConstructorMap.class,
				TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));

		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		Map<String, Integer> result = (Map<String, Integer>) conversionService.convert(map, sourceType, targetType);
		assertThat(result).isEqualTo(map);
		assertThat(result.getClass()).isEqualTo(NoDefaultConstructorMap.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void multiValueMapToMultiValueMap() throws Exception {
		DefaultConversionService.addDefaultConverters(conversionService);
		MultiValueMap<String, Integer> source = new LinkedMultiValueMap<>();
		source.put("a", Arrays.asList(1, 2, 3));
		source.put("b", Arrays.asList(4, 5, 6));
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("multiValueMapTarget"));

		MultiValueMap<String, String> converted = (MultiValueMap<String, String>) conversionService.convert(source, targetType);
		assertThat(converted.size()).isEqualTo(2);
		assertThat(converted.get("a")).isEqualTo(Arrays.asList("1", "2", "3"));
		assertThat(converted.get("b")).isEqualTo(Arrays.asList("4", "5", "6"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void mapToMultiValueMap() throws Exception {
		DefaultConversionService.addDefaultConverters(conversionService);
		Map<String, Integer> source = new HashMap<>();
		source.put("a", 1);
		source.put("b", 2);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("multiValueMapTarget"));

		MultiValueMap<String, String> converted = (MultiValueMap<String, String>) conversionService.convert(source, targetType);
		assertThat(converted.size()).isEqualTo(2);
		assertThat(converted.get("a")).isEqualTo(Arrays.asList("1"));
		assertThat(converted.get("b")).isEqualTo(Arrays.asList("2"));
	}

	@Test
	void stringToEnumMap() throws Exception {
		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		Map<String, Integer> source = new HashMap<>();
		source.put("A", 1);
		source.put("C", 2);
		EnumMap<MyEnum, Integer> result = new EnumMap<>(MyEnum.class);
		result.put(MyEnum.A, 1);
		result.put(MyEnum.C, 2);

		assertThat(conversionService.convert(source,
				TypeDescriptor.forObject(source), new TypeDescriptor(getClass().getField("enumMap")))).isEqualTo(result);
	}


	public Map<Integer, Integer> scalarMapTarget;

	public Map<Integer, List<Integer>> collectionMapTarget;

	public Map<String, List<String>> sourceCollectionMapTarget;

	public Map<String, String> emptyMapTarget;

	public LinkedHashMap<String, String> emptyMapDifferentTarget;

	public MultiValueMap<String, String> multiValueMapTarget;

	@SuppressWarnings("rawtypes")
	public Map notGenericMapSource;

	public EnumMap<MyEnum, Integer> enumMap;


	@SuppressWarnings("serial")
	public static class NoDefaultConstructorMap<K, V> extends HashMap<K, V> {

		public NoDefaultConstructorMap(Map<? extends K, ? extends V> map) {
			super(map);
		}
	}


	public enum MyEnum {A, B, C}

}
