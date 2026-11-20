/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
