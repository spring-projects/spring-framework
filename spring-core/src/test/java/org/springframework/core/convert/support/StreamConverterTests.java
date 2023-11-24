/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.stream;

/**
 * Tests for {@link StreamConverter}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 */
class StreamConverterTests {

	private final GenericConversionService conversionService = new GenericConversionService();

	private final StreamConverter streamConverter = new StreamConverter(this.conversionService);


	@BeforeEach
	void setup() {
		this.conversionService.addConverter(new CollectionToCollectionConverter(this.conversionService));
		this.conversionService.addConverter(new ArrayToCollectionConverter(this.conversionService));
		this.conversionService.addConverter(new CollectionToArrayConverter(this.conversionService));
		this.conversionService.addConverter(this.streamConverter);
	}


	@Test
	void convertFromStreamToList() throws NoSuchFieldException {
		this.conversionService.addConverter(Number.class, String.class, new ObjectToStringConverter());
		Stream<Integer> stream = Stream.of(1, 2, 3);
		TypeDescriptor listOfStrings = new TypeDescriptor(Types.class.getField("listOfStrings"));
		Object result = this.conversionService.convert(stream, listOfStrings);

		assertThat(result).asInstanceOf(list(String.class)).containsExactly("1", "2", "3");
	}

	@Test
	void convertFromStreamToArray() throws NoSuchFieldException {
		this.conversionService.addConverterFactory(new NumberToNumberConverterFactory());
		Stream<Integer> stream = Stream.of(1, 2, 3);
		TypeDescriptor arrayOfLongs = new TypeDescriptor(Types.class.getField("arrayOfLongs"));
		Object result = this.conversionService.convert(stream, arrayOfLongs);

		assertThat(result).as("Converted object must not be null").isNotNull();
		assertThat(result.getClass().isArray()).as("Converted object must be an array").isTrue();
		Long[] content = (Long[]) result;
		assertThat(content).containsExactly(1L, 2L, 3L);
	}

	@Test
	void convertFromStreamToRawList() throws NoSuchFieldException {
		Stream<Integer> stream = Stream.of(1, 2, 3);
		TypeDescriptor listOfStrings = new TypeDescriptor(Types.class.getField("rawList"));
		Object result = this.conversionService.convert(stream, listOfStrings);

		assertThat(result).asInstanceOf(list(Object.class)).containsExactly(1, 2, 3);
	}

	@Test
	void convertFromStreamToArrayNoConverter() throws NoSuchFieldException {
		Stream<Integer> stream = Stream.of(1, 2, 3);
		TypeDescriptor arrayOfLongs = new TypeDescriptor(Types.class.getField("arrayOfLongs"));
		assertThatExceptionOfType(ConversionFailedException.class)
			.isThrownBy(() -> this.conversionService.convert(stream, arrayOfLongs))
			.withCauseInstanceOf(ConverterNotFoundException.class);
	}

	@Test
	@SuppressWarnings("resource")
	void convertFromListToStream() throws NoSuchFieldException {
		this.conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = Arrays.asList("1", "2", "3");
		TypeDescriptor streamOfInteger = new TypeDescriptor(Types.class.getField("streamOfIntegers"));
		Object result = this.conversionService.convert(list, streamOfInteger);

		assertThat(result).as("Converted object must be a stream").isInstanceOf(Stream.class);
		@SuppressWarnings("unchecked")
		Stream<Integer> content = (Stream<Integer>) result;
		assertThat(content.mapToInt(x -> x).sum()).isEqualTo(6);
	}

	@Test
	@SuppressWarnings("resource")
	void convertFromArrayToStream() throws NoSuchFieldException {
		Integer[] array = new Integer[] {1, 0, 1};
		this.conversionService.addConverter(Integer.class, Boolean.class, source -> source == 1);
		TypeDescriptor streamOfBoolean = new TypeDescriptor(Types.class.getField("streamOfBooleans"));
		Object result = this.conversionService.convert(array, streamOfBoolean);

		assertThat(result).asInstanceOf(stream(Boolean.class)).filteredOn(x -> x).hasSize(2);
	}

	@Test
	@SuppressWarnings("resource")
	void convertFromListToRawStream() throws NoSuchFieldException {
		List<String> list = Arrays.asList("1", "2", "3");
		TypeDescriptor streamOfInteger = new TypeDescriptor(Types.class.getField("rawStream"));
		Object result = this.conversionService.convert(list, streamOfInteger);

		assertThat(result).as("Converted object must be a stream").isInstanceOf(Stream.class);
		@SuppressWarnings("unchecked")
		Stream<Object> content = (Stream<Object>) result;
		assertThat(content).containsExactly("1", "2", "3");
	}

	@Test
	void doesNotMatchIfNoStream() throws NoSuchFieldException {
		assertThat(this.streamConverter.matches(
				new TypeDescriptor(Types.class.getField("listOfStrings")),
				new TypeDescriptor(Types.class.getField("arrayOfLongs")))).as("Should not match non stream type").isFalse();
	}

	@Test
	void shouldFailToConvertIfNoStream() throws NoSuchFieldException {
		TypeDescriptor sourceType = new TypeDescriptor(Types.class.getField("listOfStrings"));
		TypeDescriptor targetType = new TypeDescriptor(Types.class.getField("arrayOfLongs"));
		assertThatIllegalStateException().isThrownBy(() ->
			this.streamConverter.convert(new Object(), sourceType, targetType));
	}


	@SuppressWarnings({ "rawtypes" })
	static class Types {

		public List<String> listOfStrings;

		public Long[] arrayOfLongs;

		public Stream<Integer> streamOfIntegers;

		public Stream<Boolean> streamOfBooleans;

		public Stream rawStream;

		public List rawList;
	}

}
