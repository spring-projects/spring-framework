/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
class CollectionToCollectionConverterTests {

	private GenericConversionService conversionService = new GenericConversionService();


	@BeforeEach
	void setUp() {
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
	}


	@Test
	void scalarList() throws Exception {
		List<String> list = new ArrayList<>();
		list.add("9");
		list.add("37");
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("scalarListTarget"));
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		try {
			conversionService.convert(list, sourceType, targetType);
		}
		catch (ConversionFailedException ex) {
			boolean condition = ex.getCause() instanceof ConverterNotFoundException;
			assertThat(condition).isTrue();
		}
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		List<Integer> result = (List<Integer>) conversionService.convert(list, sourceType, targetType);
		assertThat(list.equals(result)).isFalse();
		assertThat(result.get(0).intValue()).isEqualTo(9);
		assertThat(result.get(1).intValue()).isEqualTo(37);
	}

	@Test
	void emptyListToList() throws Exception {
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyListTarget"));
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(conversionService.convert(list, sourceType, targetType)).isEqualTo(list);
	}

	@Test
	void emptyListToListDifferentTargetType() throws Exception {
		conversionService.addConverter(new CollectionToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("emptyListDifferentTarget"));
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		@SuppressWarnings("unchecked")
		ArrayList<Integer> result = (ArrayList<Integer>) conversionService.convert(list, sourceType, targetType);
		assertThat(result.getClass()).isEqualTo(ArrayList.class);
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	void collectionToObjectInteraction() throws Exception {
		List<List<String>> list = new ArrayList<>();
		list.add(Arrays.asList("9", "12"));
		list.add(Arrays.asList("37", "23"));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		assertThat(conversionService.canConvert(List.class, List.class)).isTrue();
		assertThat((Object) conversionService.convert(list, List.class)).isSameAs(list);
	}

	@Test
	@SuppressWarnings("unchecked")
	void arrayCollectionToObjectInteraction() throws Exception {
		List<String>[] array = new List[2];
		array[0] = Arrays.asList("9", "12");
		array[1] = Arrays.asList("37", "23");
		conversionService.addConverter(new ArrayToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		assertThat(conversionService.canConvert(String[].class, List.class)).isTrue();
		assertThat(conversionService.convert(array, List.class)).isEqualTo(Arrays.asList(array));
	}

	@Test
	@SuppressWarnings("unchecked")
	void objectToCollection() throws Exception {
		List<List<String>> list = new ArrayList<>();
		list.add(Arrays.asList("9", "12"));
		list.add(Arrays.asList("37", "23"));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		conversionService.addConverter(new ObjectToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("objectToCollection"));
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		List<List<List<Integer>>> result = (List<List<List<Integer>>>) conversionService.convert(list, sourceType, targetType);
		assertThat(result.get(0).get(0).get(0)).isEqualTo((Integer) 9);
		assertThat(result.get(0).get(1).get(0)).isEqualTo((Integer) 12);
		assertThat(result.get(1).get(0).get(0)).isEqualTo((Integer) 37);
		assertThat(result.get(1).get(1).get(0)).isEqualTo((Integer) 23);
	}

	@Test
	@SuppressWarnings("unchecked")
	void stringToCollection() throws Exception {
		List<List<String>> list = new ArrayList<>();
		list.add(Arrays.asList("9,12"));
		list.add(Arrays.asList("37,23"));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		conversionService.addConverter(new StringToCollectionConverter(conversionService));
		conversionService.addConverter(new ObjectToCollectionConverter(conversionService));
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("objectToCollection"));
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		List<List<List<Integer>>> result = (List<List<List<Integer>>>) conversionService.convert(list, sourceType, targetType);
		assertThat(result.get(0).get(0).get(0)).isEqualTo((Integer) 9);
		assertThat(result.get(0).get(0).get(1)).isEqualTo((Integer) 12);
		assertThat(result.get(1).get(0).get(0)).isEqualTo((Integer) 37);
		assertThat(result.get(1).get(0).get(1)).isEqualTo((Integer) 23);
	}

	@Test
	void convertEmptyVector_shouldReturnEmptyArrayList() {
		Vector<String> vector = new Vector<>();
		vector.add("Element");
		testCollectionConversionToArrayList(vector);
	}

	@Test
	void convertNonEmptyVector_shouldReturnNonEmptyArrayList() {
		Vector<String> vector = new Vector<>();
		vector.add("Element");
		testCollectionConversionToArrayList(vector);
	}

	@Test
	void collectionsEmptyList() throws Exception {
		CollectionToCollectionConverter converter = new CollectionToCollectionConverter(new GenericConversionService());
		TypeDescriptor type = new TypeDescriptor(getClass().getField("list"));
		converter.convert(list, type, TypeDescriptor.valueOf(Class.forName("java.util.Collections$EmptyList")));
	}

	@SuppressWarnings("rawtypes")
	private void testCollectionConversionToArrayList(Collection<String> aSource) {
		Object myConverted = (new CollectionToCollectionConverter(new GenericConversionService())).convert(
				aSource, TypeDescriptor.forObject(aSource), TypeDescriptor.forObject(new ArrayList()));
		boolean condition = myConverted instanceof ArrayList<?>;
		assertThat(condition).isTrue();
		assertThat(((ArrayList<?>) myConverted).size()).isEqualTo(aSource.size());
	}

	@Test
	void listToCollectionNoCopyRequired() throws NoSuchFieldException {
		List<?> input = new ArrayList<>(Arrays.asList("foo", "bar"));
		assertThat(conversionService.convert(input, TypeDescriptor.forObject(input),
				new TypeDescriptor(getClass().getField("wildcardCollection")))).isSameAs(input);
	}

	@Test
	void differentImpls() throws Exception {
		List<Resource> resources = new ArrayList<>();
		resources.add(new ClassPathResource("test"));
		resources.add(new FileSystemResource("test"));
		resources.add(new TestResource());
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertThat(conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources")))).isSameAs(resources);
	}

	@Test
	void mixedInNulls() throws Exception {
		List<Resource> resources = new ArrayList<>();
		resources.add(new ClassPathResource("test"));
		resources.add(null);
		resources.add(new FileSystemResource("test"));
		resources.add(new TestResource());
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertThat(conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources")))).isSameAs(resources);
	}

	@Test
	void allNulls() throws Exception {
		List<Resource> resources = new ArrayList<>();
		resources.add(null);
		resources.add(null);
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertThat(conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources")))).isSameAs(resources);
	}

	@Test
	void elementTypesNotConvertible() throws Exception {
		List<String> resources = new ArrayList<>();
		resources.add(null);
		resources.add(null);
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("strings"));
		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}

	@Test
	void nothingInCommon() throws Exception {
		List<Object> resources = new ArrayList<>();
		resources.add(new ClassPathResource("test"));
		resources.add(3);
		TypeDescriptor sourceType = TypeDescriptor.forObject(resources);
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert(resources, sourceType, new TypeDescriptor(getClass().getField("resources"))));
	}

	@Test
	void stringToEnumSet() throws Exception {
		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		List<String> list = new ArrayList<>();
		list.add("A");
		list.add("C");
		assertThat(conversionService.convert(list, TypeDescriptor.forObject(list), new TypeDescriptor(getClass().getField("enumSet")))).isEqualTo(EnumSet.of(MyEnum.A, MyEnum.C));
	}


	public ArrayList<Integer> scalarListTarget;

	public List<Integer> emptyListTarget;

	public ArrayList<Integer> emptyListDifferentTarget;

	public List<List<List<Integer>>> objectToCollection;

	public List<String> strings;

	public List<?> list = Collections.emptyList();

	public Collection<?> wildcardCollection = Collections.emptyList();

	public List<Resource> resources;

	public EnumSet<MyEnum> enumSet;


	public static abstract class BaseResource implements Resource {

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public boolean isReadable() {
			return false;
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public boolean isFile() {
			return false;
		}

		@Override
		public URL getURL() throws IOException {
			return null;
		}

		@Override
		public URI getURI() throws IOException {
			return null;
		}

		@Override
		public File getFile() throws IOException {
			return null;
		}

		@Override
		public long contentLength() throws IOException {
			return 0;
		}

		@Override
		public long lastModified() throws IOException {
			return 0;
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return null;
		}

		@Override
		public String getFilename() {
			return null;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}


	public static class TestResource extends BaseResource {
	}


	public enum MyEnum {A, B, C}

}
