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
import java.io.EOFException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.testfixture.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IntrospectingClientHttpResponse}.
 *
 * @author Brian Clozel
 */
class IntrospectingClientHttpResponseTests {


	@ParameterizedTest
	@MethodSource("noBodyHttpStatus")
	void noMessageBodyWhenStatus(HttpStatus status) throws Exception {
		var response = new MockClientHttpResponse(new byte[0], status);
		var wrapped = new IntrospectingClientHttpResponse(response);

		assertThat(wrapped.hasMessageBody()).isFalse();
	}

	static Stream<HttpStatusCode> noBodyHttpStatus() {
		return Stream.of(HttpStatus.NO_CONTENT, HttpStatus.EARLY_HINTS, HttpStatus.NOT_MODIFIED);
	}

	@Test
	void noMessageBodyWhenContentLength0() throws Exception {
		var response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
		response.getHeaders().setContentLength(0);
		var wrapped = new IntrospectingClientHttpResponse(response);

		assertThat(wrapped.hasMessageBody()).isFalse();
	}

	@Test
	void emptyMessageWhenNullInputStream() throws Exception {
		ClientHttpResponse mockResponse = mock();
		given(mockResponse.getBody()).willReturn(null);
		var wrappedMock = new IntrospectingClientHttpResponse(mockResponse);
		assertThat(wrappedMock.hasEmptyMessageBody()).isTrue();
	}

	@Test
	void messageBodyExists() throws Exception {
		var stream = new ByteArrayInputStream("content".getBytes());
		var response = new MockClientHttpResponse(stream, HttpStatus.OK);
		var wrapped = new IntrospectingClientHttpResponse(response);
		assertThat(wrapped.hasEmptyMessageBody()).isFalse();
	}

	@Test
	void emptyMessageWhenEOFException() throws Exception {
		ClientHttpResponse mockResponse = mock();
		InputStream stream = mock();
		given(mockResponse.getBody()).willReturn(stream);
		given(stream.read()).willThrow(new EOFException());
		var wrappedMock = new IntrospectingClientHttpResponse(mockResponse);
		assertThat(wrappedMock.hasEmptyMessageBody()).isTrue();
	}

}
