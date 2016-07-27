/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class EmptyResponseTests {

	@Test
	public void statusCode() throws Exception {
		HttpStatus statusCode = HttpStatus.ACCEPTED;
		EmptyResponse emptyResponse = new EmptyResponse(statusCode.value(), new HttpHeaders());
		assertSame(statusCode, emptyResponse.statusCode());
	}

	@Test
	public void headers() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		EmptyResponse emptyResponse = new EmptyResponse(200, headers);
		assertEquals(headers, emptyResponse.headers());
	}

	@Test
	public void body() throws Exception {
		EmptyResponse emptyResponse = new EmptyResponse(200, new HttpHeaders());
		assertNull(emptyResponse.body());
	}

	@Test
	public void writeTo() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("MyKey", "MyValue");
		EmptyResponse emptyResponse = new EmptyResponse(201, headers);

		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);


		emptyResponse.writeTo(exchange).block();
		assertEquals(201, response.getStatusCode().value());
		assertEquals("MyValue", response.getHeaders().getFirst("MyKey"));
		assertNull(response.getBody());
	}
}