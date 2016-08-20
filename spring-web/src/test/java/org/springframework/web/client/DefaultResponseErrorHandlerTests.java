/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link DefaultResponseErrorHandler}.
 *
 * @author Arjen Poutsma
 */
public class DefaultResponseErrorHandlerTests {

	private DefaultResponseErrorHandler handler;

	private ClientHttpResponse response;

	@Before
	public void setUp() throws Exception {
		handler = new DefaultResponseErrorHandler();
		response = mock(ClientHttpResponse.class);
	}

	@Test
	public void hasErrorTrue() throws Exception {
		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		assertTrue(handler.hasError(response));
	}

	@Test
	public void hasErrorFalse() throws Exception {
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		assertFalse(handler.hasError(response));
	}

	@Test
	public void handleError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Hello World".getBytes("UTF-8")));

		try {
			handler.handleError(response);
			fail("expected HttpClientErrorException");
		}
		catch (HttpClientErrorException e) {
			assertSame(headers, e.getResponseHeaders());
		}
	}

	@Test(expected = HttpClientErrorException.class)
	public void handleErrorIOException() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willThrow(new IOException());

		handler.handleError(response);
	}

	@Test(expected = HttpClientErrorException.class)
	public void handleErrorNullResponse() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);

		handler.handleError(response);
	}

	// SPR-9406

	@Test(expected = UnknownHttpStatusCodeException.class)
	public void unknownStatusCode() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willThrow(new IllegalArgumentException("No matching constant for 999"));
		given(response.getRawStatusCode()).willReturn(999);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		handler.handleError(response);
	}
}
