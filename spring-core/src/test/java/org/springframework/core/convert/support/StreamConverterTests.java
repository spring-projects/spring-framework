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

package org.springframework.core.convert.support;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link StreamConverter}.
 *
 * @author Stephane Nicoll
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
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor listOfStrings = new TypeDescriptor(Types.class.getField("listOfStrings"));
		Object result = this.conversionService.convert(stream, listOfStrings);

		assertThat(result).as("Converted object must not be null").isNotNull();
		boolean condition = result instanceof List;
		assertThat(condition).as("Converted object must be a list").isTrue();
		@SuppressWarnings("unchecked")
		List<String> content = (List<String>) result;
		assertThat(content.get(0)).isEqualTo("1");
		assertThat(content.get(1)).isEqualTo("2");
		assertThat(content.get(2)).isEqualTo("3");
		assertThat(content.size()).as("Wrong number of elements").isEqualTo(3);
	}

	@Test
	void convertFromStreamToArray() throws NoSuchFieldException {
		this.conversionService.addConverterFactory(new NumberToNumberConverterFactory());
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor arrayOfLongs = new TypeDescriptor(Types.class.getField("arrayOfLongs"));
		Object result = this.conversionService.convert(stream, arrayOfLongs);

		assertThat(result).as("Converted object must not be null").isNotNull();
		assertThat(result.getClass().isArray()).as("Converted object must be an array").isTrue();
		Long[] content = (Long[]) result;
		assertThat(content[0]).isEqualTo(Long.valueOf(1L));
		assertThat(content[1]).isEqualTo(Long.valueOf(2L));
		assertThat(content[2]).isEqualTo(Long.valueOf(3L));
		assertThat(content.length).as("Wrong number of elements").isEqualTo(3);
	}

	@Test
	void convertFromStreamToRawList() throws NoSuchFieldException {
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor listOfStrings = new TypeDescriptor(Types.class.getField("rawList"));
		Object result = this.conversionService.convert(stream, listOfStrings);

		assertThat(result).as("Converted object must not be null").isNotNull();
		boolean condition = result instanceof List;
		assertThat(condition).as("Converted object must be a list").isTrue();
		@SuppressWarnings("unchecked")
		List<Object> content = (List<Object>) result;
		assertThat(content.get(0)).isEqualTo(1);
		assertThat(content.get(1)).isEqualTo(2);
		assertThat(content.get(2)).isEqualTo(3);
		assertThat(content.size()).as("Wrong number of elements").isEqualTo(3);
	}

	@Test
	void convertFromStreamToArrayNoConverter() throws NoSuchFieldException {
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor arrayOfLongs = new TypeDescriptor(Types.class.getField("arrayOfLongs"));
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				this.conversionService.convert(stream, arrayOfLongs))
			.withCauseInstanceOf(ConverterNotFoundException.class);
	}

	@Test
	@SuppressWarnings("resource")
	void convertFromListToStream() throws NoSuchFieldException {
		this.conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> stream = Arrays.asList("1", "2", "3");
		TypeDescriptor streamOfInteger = new TypeDescriptor(Types.class.getField("streamOfIntegers"));
		Object result = this.conversionService.convert(stream, streamOfInteger);

		assertThat(result).as("Converted object must not be null").isNotNull();
		boolean condition = result instanceof Stream;
		assertThat(condition).as("Converted object must be a stream").isTrue();
		@SuppressWarnings("unchecked")
		Stream<Integer> content = (Stream<Integer>) result;
		assertThat(content.mapToInt(x -> x).sum()).isEqualTo(6);
	}

	@Test
	@SuppressWarnings("resource")
	void convertFromArrayToStream() throws NoSuchFieldException {
		Integer[] stream = new Integer[] {1, 0, 1};
		this.conversionService.addConverter(new Converter<Integer, Boolean>() {
			@Override
			public Boolean convert(Integer source) {
				return source == 1;
			}
		});
		TypeDescriptor streamOfBoolean = new TypeDescriptor(Types.class.getField("streamOfBooleans"));
		Object result = this.conversionService.convert(stream, streamOfBoolean);

		assertThat(result).as("Converted object must not be null").isNotNull();
		boolean condition = result instanceof Stream;
		assertThat(condition).as("Converted object must be a stream").isTrue();
		@SuppressWarnings("unchecked")
		Stream<Boolean> content = (Stream<Boolean>) result;
		assertThat(content.filter(x -> x).count()).isEqualTo(2);
	}

	@Test
	@SuppressWarnings("resource")
	void convertFromListToRawStream() throws NoSuchFieldException {
		List<String> stream = Arrays.asList("1", "2", "3");
		TypeDescriptor streamOfInteger = new TypeDescriptor(Types.class.getField("rawStream"));
		Object result = this.conversionService.convert(stream, streamOfInteger);

		assertThat(result).as("Converted object must not be null").isNotNull();
		boolean condition = result instanceof Stream;
		assertThat(condition).as("Converted object must be a stream").isTrue();
		@SuppressWarnings("unchecked")
		Stream<Object> content = (Stream<Object>) result;
		StringBuilder sb = new StringBuilder();
		content.forEach(sb::append);
		assertThat(sb.toString()).isEqualTo("123");
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
