/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** @author Arjen Poutsma */
public class DefaultResponseErrorHandlerTests {

	private DefaultResponseErrorHandler handler;

	private ClientHttpResponse response;

	@Before
	public void setUp() throws Exception {
		handler = new DefaultResponseErrorHandler();
		response = createMock(ClientHttpResponse.class);
	}

	@Test
	public void hasErrorTrue() throws Exception {
		expect(response.getStatusCode()).andReturn(HttpStatus.NOT_FOUND);

		replay(response);
		assertTrue(handler.hasError(response));

		verify(response);
	}

	@Test
	public void hasErrorFalse() throws Exception {
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);

		replay(response);
		assertFalse(handler.hasError(response));

		verify(response);
	}

	@Test
	public void handleError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		expect(response.getStatusCode()).andReturn(HttpStatus.NOT_FOUND);
		expect(response.getStatusText()).andReturn("Not Found");
		expect(response.getHeaders()).andReturn(headers);
		expect(response.getBody()).andReturn(new ByteArrayInputStream("Hello World".getBytes("UTF-8")));

		replay(response);

		try {
			handler.handleError(response);
			fail("expected HttpClientErrorException");
		}
		catch (HttpClientErrorException e) {
			assertSame(headers, e.getResponseHeaders());
		}

		verify(response);
	}

	@Test(expected = HttpClientErrorException.class)
	public void handleErrorIOException() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		expect(response.getStatusCode()).andReturn(HttpStatus.NOT_FOUND);
		expect(response.getStatusText()).andReturn("Not Found");
		expect(response.getHeaders()).andReturn(headers);
		expect(response.getBody()).andThrow(new IOException());

		replay(response);

		handler.handleError(response);

		verify(response);
	}

	@Test(expected = HttpClientErrorException.class)
	public void handleErrorNullResponse() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		expect(response.getStatusCode()).andReturn(HttpStatus.NOT_FOUND);
		expect(response.getStatusText()).andReturn("Not Found");
		expect(response.getHeaders()).andReturn(headers);
		expect(response.getBody()).andReturn(null);

		replay(response);

		handler.handleError(response);

		verify(response);
	}
}
