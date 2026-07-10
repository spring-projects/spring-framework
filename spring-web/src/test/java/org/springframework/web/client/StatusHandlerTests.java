/*
 * Copyright 2002-present the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.converter.SmartHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StatusHandler}.
 *
 * @author Sakshi Chitnis
 */
class StatusHandlerTests {

	private static final MediaType CONTENT_TYPE = MediaType.TEXT_PLAIN;

	private static final String BODY = "Foo";

	private final ClientHttpResponse response = mock();


	@BeforeEach
	void setUp() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(CONTENT_TYPE);
		headers.setContentLength(BODY.length());
		given(this.response.getStatusCode()).willReturn(HttpStatus.BAD_REQUEST);
		given(this.response.getStatusText()).willReturn(HttpStatus.BAD_REQUEST.getReasonPhrase());
		given(this.response.getHeaders()).willReturn(headers);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(BODY.getBytes(StandardCharsets.UTF_8)));
	}


	@Test
	void convertBodyWithHttpMessageConverter() throws IOException {
		HttpMessageConverter<String> converter = mock();
		given(converter.canRead(String.class, CONTENT_TYPE)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(BODY);

		RestClientResponseException exception = createException(converter);

		assertThat(exception.getResponseBodyAs(String.class)).isEqualTo(BODY);
	}

	@Test
	void convertBodyWithGenericHttpMessageConverter() throws IOException {
		ParameterizedTypeReference<List<String>> targetType = new ParameterizedTypeReference<>() {};
		Type type = targetType.getType();
		GenericHttpMessageConverter<List<String>> converter = mock();
		given(converter.canRead(type, null, CONTENT_TYPE)).willReturn(true);
		given(converter.read(eq(type), isNull(), any(HttpInputMessage.class))).willReturn(List.of(BODY));

		RestClientResponseException exception = createException(converter);

		assertThat(exception.getResponseBodyAs(targetType)).containsExactly(BODY);
	}

	@Test
	void convertBodyWithSmartHttpMessageConverter() throws IOException {
		ParameterizedTypeReference<List<String>> targetType = new ParameterizedTypeReference<>() {};
		ResolvableType resolvableType = ResolvableType.forType(targetType.getType());
		SmartHttpMessageConverter<List<String>> converter = mock();
		given(converter.canRead(resolvableType, CONTENT_TYPE)).willReturn(true);
		given(converter.read(eq(resolvableType), any(HttpInputMessage.class), isNull())).willReturn(List.of(BODY));

		RestClientResponseException exception = createException(converter);

		assertThat(exception.getResponseBodyAs(targetType)).containsExactly(BODY);
	}

	@Test
	void emptyBodyIsNotConverted() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentLength(0);
		given(this.response.getHeaders()).willReturn(headers);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(new byte[0]));
		HttpMessageConverter<String> converter = mock();

		RestClientResponseException exception = createException(converter);

		assertThat(exception.getResponseBodyAs(String.class)).isNull();
	}

	@Test
	void cannotConvertBody() throws IOException {
		HttpMessageConverter<String> converter = mock();
		given(converter.canRead(String.class, CONTENT_TYPE)).willReturn(false);

		RestClientResponseException exception = createException(converter);

		assertThatExceptionOfType(UnknownContentTypeException.class)
				.isThrownBy(() -> exception.getResponseBodyAs(String.class))
				.satisfies(ex -> assertThat(ex.getResponseBodyAsString()).isEqualTo(BODY));
	}

	@Test
	void converterThrowsIOException() throws IOException {
		HttpMessageConverter<String> converter = mock();
		given(converter.canRead(String.class, CONTENT_TYPE)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willThrow(IOException.class);

		RestClientResponseException exception = createException(converter);

		assertThatExceptionOfType(RestClientException.class)
				.isThrownBy(() -> exception.getResponseBodyAs(String.class))
				.withMessageContaining("Error while extracting response for type [class java.lang.String] " +
						"and content type [text/plain]")
				.withCauseInstanceOf(IOException.class);
	}

	private RestClientResponseException createException(HttpMessageConverter<?> converter) throws IOException {
		return StatusHandler.createException(this.response, List.of(converter));
	}

}
