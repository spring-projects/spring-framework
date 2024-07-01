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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.SmartHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link HttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Sam Brannen
 */
class HttpMessageConverterExtractorTests {

	private final HttpMessageConverter<String> converter = mock();
	private final HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor<>(String.class, List.of(converter));
	private final MediaType contentType = MediaType.TEXT_PLAIN;
	private final HttpHeaders responseHeaders = new HttpHeaders();
	private final ClientHttpResponse response = mock();


	@Test
	void constructorPreconditions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpMessageConverterExtractor<>(String.class, null))
				.withMessage("'messageConverters' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HttpMessageConverterExtractor<>(String.class, Arrays.asList(null, this.converter)))
				.withMessage("'messageConverters' must not contain null elements");
	}

	@Test
	void noContent() throws IOException {
		given(response.getStatusCode()).willReturn(HttpStatus.NO_CONTENT);

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void notModified() throws IOException {
		given(response.getStatusCode()).willReturn(HttpStatus.NOT_MODIFIED);

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void informational() throws IOException {
		given(response.getStatusCode()).willReturn(HttpStatus.CONTINUE);

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void zeroContentLength() throws IOException {
		responseHeaders.setContentLength(0);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void emptyMessageBody() throws IOException {
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("".getBytes()));

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test // gh-22265
	void nullMessageBody() throws IOException {
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(null);

		Object result = extractor.extractData(response);
		assertThat(result).isNull();
	}

	@Test
	void normal() throws IOException {
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expected);

		Object result = extractor.extractData(response);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void cannotRead() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(false);
		assertThatExceptionOfType(RestClientException.class).isThrownBy(() -> extractor.extractData(response));
	}

	@Test
	void generics() throws IOException {
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		ParameterizedTypeReference<List<String>> reference = new ParameterizedTypeReference<>() {};
		Type type = reference.getType();

		GenericHttpMessageConverter<String> converter = mock();
		HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor<List<String>>(type, List.of(converter));

		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(type, null, contentType)).willReturn(true);
		given(converter.read(eq(type), eq(null), any(HttpInputMessage.class))).willReturn(expected);

		Object result = extractor.extractData(response);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void smartConverter() throws IOException {
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		ParameterizedTypeReference<List<String>> reference = new ParameterizedTypeReference<>() {};
		ResolvableType resolvableType = ResolvableType.forType(reference.getType());

		SmartHttpMessageConverter<String> converter = mock();
		HttpMessageConverterExtractor<?> extractor = new HttpMessageConverterExtractor<List<String>>(resolvableType.getType(), List.of(converter));

		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(resolvableType, contentType)).willReturn(true);
		given(converter.read(eq(resolvableType), any(HttpInputMessage.class), isNull())).willReturn(expected);

		Object result = extractor.extractData(response);
		assertThat(result).isEqualTo(expected);
	}

	@Test  // SPR-13592
	void converterThrowsIOException() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willThrow(IOException.class);
		assertThatExceptionOfType(RestClientException.class).isThrownBy(() -> extractor.extractData(response))
			.withMessageContaining("Error while extracting response for type [class java.lang.String] and content type [text/plain]")
			.withCauseInstanceOf(IOException.class);
	}

	@Test  // SPR-13592
	void converterThrowsHttpMessageNotReadableException() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willThrow(HttpMessageNotReadableException.class);
		assertThatExceptionOfType(RestClientException.class).isThrownBy(() -> extractor.extractData(response))
			.withMessageContaining("Error while extracting response for type [class java.lang.String] and content type [text/plain]")
			.withCauseInstanceOf(HttpMessageNotReadableException.class);
	}

	@Test
	void unknownContentTypeExceptionContainsCorrectResponseBody() throws IOException {
		responseHeaders.setContentType(contentType);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()) {
			@Override
			public boolean markSupported() {
				return false;
			}
		});
		given(converter.canRead(String.class, contentType)).willReturn(false);

		assertThatExceptionOfType(UnknownContentTypeException.class).isThrownBy(() -> extractor.extractData(response))
			.satisfies(exception -> assertThat(exception.getResponseBodyAsString()).isEqualTo("Foobar"));
	}

}
