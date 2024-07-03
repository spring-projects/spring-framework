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

package org.springframework.test.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link HttpMessageContentConverter}.
 *
 * @author Stephane Nicoll
 */
class HttpMessageContentConverterTests {

	private static final MediaType JSON = MediaType.APPLICATION_JSON;

	private static final ResolvableType listOfIntegers = ResolvableType.forClassWithGenerics(List.class, Integer.class);

	private static final MappingJackson2HttpMessageConverter jacksonMessageConverter =
			new MappingJackson2HttpMessageConverter(new ObjectMapper());

	@Test
	void createInstanceWithEmptyIterable() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageContentConverter.of(List.of()))
				.withMessage("At least one message converter needs to be specified");
	}

	@Test
	void createInstanceWithEmptyVarArg() {
		assertThatIllegalArgumentException()
				.isThrownBy(HttpMessageContentConverter::of)
				.withMessage("At least one message converter needs to be specified");
	}

	@Test
	void convertInvokesFirstMatchingConverter() throws IOException {
		HttpInputMessage message = createMessage("1,2,3");
		SmartHttpMessageConverter<?> firstConverter = mockSmartConverterForRead(
				listOfIntegers, JSON, message, List.of(1, 2, 3));
		SmartHttpMessageConverter<?> secondConverter = mockSmartConverterForRead(
				listOfIntegers, JSON, message, List.of(3, 2, 1));
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(firstConverter, secondConverter));
		List<Integer> data = contentConverter.convert(message, JSON, listOfIntegers);
		assertThat(data).containsExactly(1, 2, 3);
		verify(firstConverter).canRead(listOfIntegers, JSON);
		verifyNoInteractions(secondConverter);
	}

	@Test
	void convertInvokesGenericHttpMessageConverter() throws IOException {
		GenericHttpMessageConverter<?> firstConverter = mock(GenericHttpMessageConverter.class);
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(firstConverter, jacksonMessageConverter));
		List<Integer> data = contentConverter.convert(createMessage("[2,3,4]"), JSON, listOfIntegers);
		assertThat(data).containsExactly(2, 3, 4);
		verify(firstConverter).canRead(listOfIntegers.getType(), List.class, JSON);
	}

	@Test
	void convertInvokesSmartHttpMessageConverter() throws IOException {
		HttpInputMessage message = createMessage("dummy");
		GenericHttpMessageConverter<?> firstConverter = mock(GenericHttpMessageConverter.class);
		SmartHttpMessageConverter<?> smartConverter = mockSmartConverterForRead(
				listOfIntegers, JSON, message, List.of(1, 2, 3));
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(firstConverter, smartConverter));
		List<Integer> data = contentConverter.convert(message, JSON, listOfIntegers);
		assertThat(data).containsExactly(1, 2, 3);
		verify(smartConverter).canRead(listOfIntegers, JSON);
	}

	@Test
	void convertInvokesHttpMessageConverter() throws IOException {
		HttpInputMessage message = createMessage("1,2,3");
		SmartHttpMessageConverter<?> secondConverter = mockSmartConverterForRead(
				listOfIntegers, JSON, message, List.of(1, 2, 3));
		HttpMessageConverter<?> thirdConverter = mockSimpleConverterForRead(
				List.class, MediaType.TEXT_PLAIN, message, List.of(1, 2, 3));
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(jacksonMessageConverter, secondConverter, thirdConverter));
		List<Integer> data = contentConverter.convert(message, MediaType.TEXT_PLAIN, listOfIntegers);
		assertThat(data).containsExactly(1, 2, 3);
		verify(secondConverter).canRead(listOfIntegers, MediaType.TEXT_PLAIN);
		verify(thirdConverter).canRead(List.class, MediaType.TEXT_PLAIN);
	}

	@Test
	void convertFailsIfNoMatchingConverterIsFound() throws IOException {
		HttpInputMessage message = createMessage("[1,2,3]");
		SmartHttpMessageConverter<?> textConverter = mockSmartConverterForRead(
				listOfIntegers, MediaType.TEXT_PLAIN, message, List.of(1, 2, 3));
		SmartHttpMessageConverter<?> htmlConverter = mockSmartConverterForRead(
				listOfIntegers, MediaType.TEXT_HTML, message, List.of(3, 2, 1));
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(textConverter, htmlConverter));
		assertThatIllegalStateException()
				.isThrownBy(() -> contentConverter.convert(message, JSON, listOfIntegers))
				.withMessage("No converter found to read [application/json] to [java.util.List<java.lang.Integer>]");
		verify(textConverter).canRead(listOfIntegers, JSON);
		verify(htmlConverter).canRead(listOfIntegers, JSON);
	}

	@Test
	void convertViaJsonInvokesFirstMatchingConverter() throws IOException {
		String value = "1,2,3";
		ResolvableType valueType = ResolvableType.forInstance(value);
		SmartHttpMessageConverter<?> readConverter = mockSmartConverterForRead(listOfIntegers, JSON, null, List.of(1, 2, 3));
		SmartHttpMessageConverter<?> firstWriteJsonConverter = mockSmartConverterForWritingJson(value, valueType, "[1,2,3]");
		SmartHttpMessageConverter<?> secondWriteJsonConverter = mockSmartConverterForWritingJson(value, valueType, "[3,2,1]");
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(readConverter, firstWriteJsonConverter, secondWriteJsonConverter));
		List<Integer> data = contentConverter.convertViaJson(value, listOfIntegers);
		assertThat(data).containsExactly(1, 2, 3);
		verify(readConverter).canRead(listOfIntegers, JSON);
		verify(firstWriteJsonConverter).canWrite(valueType, String.class, JSON);
		verifyNoInteractions(secondWriteJsonConverter);
	}

	@Test
	void convertViaJsonInvokesGenericHttpMessageConverter() throws IOException {
		String value = "1,2,3";
		ResolvableType valueType = ResolvableType.forInstance(value);
		SmartHttpMessageConverter<?> readConverter = mockSmartConverterForRead(listOfIntegers, JSON, null, List.of(1, 2, 3));
		GenericHttpMessageConverter<?> writeConverter = mockGenericConverterForWritingJson(value, valueType, "[3,2,1]");
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(readConverter, writeConverter, jacksonMessageConverter));
		List<Integer> data = contentConverter.convertViaJson("[1, 2, 3]", listOfIntegers);
		assertThat(data).containsExactly(1, 2, 3);
		verify(readConverter).canRead(listOfIntegers, JSON);
		verify(writeConverter).canWrite(valueType.getType(), value.getClass(), JSON);
	}

	@Test
	void convertViaJsonInvokesSmartHttpMessageConverter() throws IOException {
		String value = "1,2,3";
		ResolvableType valueType = ResolvableType.forInstance(value);
		SmartHttpMessageConverter<?> readConverter = mockSmartConverterForRead(listOfIntegers, JSON, null, List.of(1, 2, 3));
		SmartHttpMessageConverter<?> writeConverter = mockSmartConverterForWritingJson(value, valueType, "[3,2,1]");
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(readConverter, writeConverter, jacksonMessageConverter));
		List<Integer> data = contentConverter.convertViaJson("[1, 2, 3]", listOfIntegers);
		assertThat(data).containsExactly(1, 2, 3);
		verify(readConverter).canRead(listOfIntegers, JSON);
		verify(writeConverter).canWrite(valueType, value.getClass(), JSON);
	}

	@Test
	void convertViaJsonInvokesHttpMessageConverter() throws IOException {
		String value = "1,2,3";
		SmartHttpMessageConverter<?> readConverter = mockSmartConverterForRead(listOfIntegers, JSON, null, List.of(1, 2, 3));
		HttpMessageConverter<?> writeConverter = mockSimpleConverterForWritingJson(value, "[3,2,1]");
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(
				List.of(readConverter, writeConverter, jacksonMessageConverter));
		List<Integer> data = contentConverter.convertViaJson("[1, 2, 3]", listOfIntegers);
		assertThat(data).containsExactly(1, 2, 3);
		verify(readConverter).canRead(listOfIntegers, JSON);
		verify(writeConverter).canWrite(value.getClass(), JSON);
	}

	@Test
	void convertViaJsonFailsIfNoMatchingConverterIsFound() throws IOException {
		String value = "1,2,3";
		ResolvableType valueType = ResolvableType.forInstance(value);
		SmartHttpMessageConverter<?> readConverter = mockSmartConverterForRead(listOfIntegers, JSON, null, List.of(1, 2, 3));
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(List.of(readConverter));
		assertThatIllegalStateException()
				.isThrownBy(() -> contentConverter.convertViaJson(value, listOfIntegers))
				.withMessage("No converter found to convert [java.lang.String] to JSON");
		verify(readConverter).canWrite(valueType, value.getClass(), JSON);
	}

	@SuppressWarnings("unchecked")
	private static SmartHttpMessageConverter<?> mockSmartConverterForRead(
			ResolvableType type, MediaType mediaType, @Nullable HttpInputMessage message, Object value) throws IOException {
		SmartHttpMessageConverter<Object> converter = mock(SmartHttpMessageConverter.class);
		given(converter.canRead(type, mediaType)).willReturn(true);
		given(converter.read(eq(type), (message != null ? eq(message) : any()), any())).willReturn(value);
		return converter;
	}

	@SuppressWarnings("unchecked")
	private static SmartHttpMessageConverter<?> mockSmartConverterForWritingJson(Object value, ResolvableType valueType, String json) throws IOException {
		SmartHttpMessageConverter<Object> converter = mock(SmartHttpMessageConverter.class);
		given(converter.canWrite(valueType, value.getClass(), JSON)).willReturn(true);
		willAnswer(invocation -> {
			MockHttpOutputMessage out = invocation.getArgument(3, MockHttpOutputMessage.class);
			StreamUtils.copy(json, StandardCharsets.UTF_8, out.getBody());
			return null;
		}).given(converter).write(eq(value), eq(valueType), eq(JSON), any(), any());
		return converter;
	}

	@SuppressWarnings("unchecked")
	private static GenericHttpMessageConverter<?> mockGenericConverterForWritingJson(Object value, ResolvableType valueType, String json) throws IOException {
		GenericHttpMessageConverter<Object> converter = mock(GenericHttpMessageConverter.class);
		given(converter.canWrite(valueType.getType(), value.getClass(), JSON)).willReturn(true);
		willAnswer(invocation -> {
			MockHttpOutputMessage out = invocation.getArgument(4, MockHttpOutputMessage.class);
			StreamUtils.copy(json, StandardCharsets.UTF_8, out.getBody());
			return null;
		}).given(converter).write(eq(value), eq(valueType.getType()), eq(JSON), any());
		return converter;
	}

	@SuppressWarnings("unchecked")
	private static HttpMessageConverter<?> mockSimpleConverterForRead(
			Class<?> rawType, MediaType mediaType, HttpInputMessage message, Object value) throws IOException {
		HttpMessageConverter<Object> converter = mock(HttpMessageConverter.class);
		given(converter.canRead(rawType, mediaType)).willReturn(true);
		given(converter.read(rawType, message)).willReturn(value);
		return converter;
	}

	@SuppressWarnings("unchecked")
	private static HttpMessageConverter<?> mockSimpleConverterForWritingJson(Object value, String json) throws IOException {
		HttpMessageConverter<Object> converter = mock(HttpMessageConverter.class);
		given(converter.canWrite(value.getClass(), JSON)).willReturn(true);
		willAnswer(invocation -> {
			MockHttpOutputMessage out = invocation.getArgument(2, MockHttpOutputMessage.class);
			StreamUtils.copy(json, StandardCharsets.UTF_8, out.getBody());
			return null;
		}).given(converter).write(eq(value), eq(JSON), any());
		return converter;
	}

	private static HttpInputMessage createMessage(String content) {
		return new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
	}

}
