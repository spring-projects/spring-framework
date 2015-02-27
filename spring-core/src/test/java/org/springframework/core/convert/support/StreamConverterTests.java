/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Tests for {@link StreamConverter}.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
public class StreamConverterTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final GenericConversionService conversionService = new GenericConversionService();

	private final StreamConverter streamConverter = new StreamConverter(this.conversionService);

	@Before
	public void setup() {
		this.conversionService.addConverter(new CollectionToCollectionConverter(this.conversionService));
		this.conversionService.addConverter(new ArrayToCollectionConverter(this.conversionService));
		this.conversionService.addConverter(new CollectionToArrayConverter(this.conversionService));
		this.conversionService.addConverter(this.streamConverter);
	}

	@Test
	public void convertFromStreamToList() throws NoSuchFieldException {
		this.conversionService.addConverter(Number.class, String.class, new ObjectToStringConverter());
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor listOfStrings = new TypeDescriptor(Types.class.getField("listOfStrings")); ;
		Object result = this.conversionService.convert(stream, listOfStrings);
		assertNotNull("converted object must not be null", result);
		assertTrue("Converted object must be a list", result instanceof List);
		@SuppressWarnings("unchecked")
		List<String> content = (List<String>) result;
		assertEquals("1", content.get(0));
		assertEquals("2", content.get(1));
		assertEquals("3", content.get(2));
		assertEquals("Wrong number of elements", 3, content.size());
	}

	@Test
	public void convertFromStreamToArray() throws NoSuchFieldException {
		this.conversionService.addConverterFactory(new NumberToNumberConverterFactory());
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor arrayOfLongs = new TypeDescriptor(Types.class.getField("arrayOfLongs")); ;
		Object result = this.conversionService.convert(stream, arrayOfLongs);
		assertNotNull("converted object must not be null", result);
		assertTrue("Converted object must be an array", result.getClass().isArray());
		Long[] content = (Long[]) result;
		assertEquals(Long.valueOf(1L), content[0]);
		assertEquals(Long.valueOf(2L), content[1]);
		assertEquals(Long.valueOf(3L), content[2]);
		assertEquals("Wrong number of elements", 3, content.length);
	}

	@Test
	public void convertFromStreamToRawList() throws NoSuchFieldException {
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor listOfStrings = new TypeDescriptor(Types.class.getField("rawList")); ;
		Object result = this.conversionService.convert(stream, listOfStrings);
		assertNotNull("converted object must not be null", result);
		assertTrue("Converted object must be a list", result instanceof List);
		@SuppressWarnings("unchecked")
		List<Object> content = (List<Object>) result;
		assertEquals(1, content.get(0));
		assertEquals(2, content.get(1));
		assertEquals(3, content.get(2));
		assertEquals("Wrong number of elements", 3, content.size());
	}

	@Test
	public void convertFromStreamToArrayNoConverter() throws NoSuchFieldException {
		Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
		TypeDescriptor arrayOfLongs = new TypeDescriptor(Types.class.getField("arrayOfLongs")); ;

		thrown.expect(ConversionFailedException.class);
		thrown.expectCause(is(instanceOf(ConverterNotFoundException.class)));
		this.conversionService.convert(stream, arrayOfLongs);
	}

	@Test
	@SuppressWarnings("resource")
	public void convertFromListToStream() throws NoSuchFieldException {
		this.conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> stream = Arrays.asList("1", "2", "3");
		TypeDescriptor streamOfInteger = new TypeDescriptor(Types.class.getField("streamOfIntegers")); ;
		Object result = this.conversionService.convert(stream, streamOfInteger);
		assertNotNull("converted object must not be null", result);
		assertTrue("Converted object must be a stream", result instanceof Stream);
		@SuppressWarnings("unchecked")
		Stream<Integer> content = (Stream<Integer>) result;
		assertEquals(6, content.mapToInt((x) -> x).sum());
	}

	@Test
	@SuppressWarnings("resource")
	public void convertFromArrayToStream() throws NoSuchFieldException {
		Integer[] stream = new Integer[] {1, 0, 1};
		this.conversionService.addConverter(new Converter<Integer, Boolean>() {
			@Override
			public Boolean convert(Integer source) {
				return source == 1;
			}
		});
		TypeDescriptor streamOfBoolean = new TypeDescriptor(Types.class.getField("streamOfBooleans")); ;
		Object result = this.conversionService.convert(stream, streamOfBoolean);
		assertNotNull("converted object must not be null", result);
		assertTrue("Converted object must be a stream", result instanceof Stream);
		@SuppressWarnings("unchecked")
		Stream<Boolean> content = (Stream<Boolean>) result;
		assertEquals(2, content.filter(x -> x).count());
	}

	@Test
	@SuppressWarnings("resource")
	public void convertFromListToRawStream() throws NoSuchFieldException {
		List<String> stream = Arrays.asList("1", "2", "3");
		TypeDescriptor streamOfInteger = new TypeDescriptor(Types.class.getField("rawStream")); ;
		Object result = this.conversionService.convert(stream, streamOfInteger);
		assertNotNull("converted object must not be null", result);
		assertTrue("Converted object must be a stream", result instanceof Stream);
		@SuppressWarnings("unchecked")
		Stream<Object> content = (Stream<Object>) result;
		StringBuilder sb = new StringBuilder();
		content.forEach(sb::append);
		assertEquals("123", sb.toString());
	}

	@Test
	public void doesNotMatchIfNoStream() throws NoSuchFieldException {
		assertFalse("Should not match non stream type", this.streamConverter.matches(
				new TypeDescriptor(Types.class.getField("listOfStrings")),
				new TypeDescriptor(Types.class.getField("arrayOfLongs"))));
	}

	@Test
	public void shouldFailToConvertIfNoStream() throws NoSuchFieldException {
		thrown.expect(IllegalStateException.class);
		this.streamConverter.convert(new Object(), new TypeDescriptor(Types.class.getField("listOfStrings")),
				new TypeDescriptor(Types.class.getField("arrayOfLongs")));
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