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

package org.springframework.http.client;

import java.net.URI;
import java.util.Arrays;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;

public class BufferingClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTestCase {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory());
	}

	@Test
	public void repeatableRead() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
		assertEquals("Invalid HTTP method", HttpMethod.PUT, request.getMethod());
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		request.getHeaders().add(headerName, headerValue1);
		String headerValue2 = "value2";
		request.getHeaders().add(headerName, headerValue2);
		byte[] body = "Hello World".getBytes("UTF-8");
		request.getHeaders().setContentLength(body.length);
		FileCopyUtils.copy(body, request.getBody());
		ClientHttpResponse response = request.execute();
		try {
			assertEquals("Invalid status code", HttpStatus.OK, response.getStatusCode());
			assertEquals("Invalid status code", HttpStatus.OK, response.getStatusCode());

			assertTrue("Header not found", response.getHeaders().containsKey(headerName));
			assertTrue("Header not found", response.getHeaders().containsKey(headerName));

			assertEquals("Header value not found", Arrays.asList(headerValue1, headerValue2),
					response.getHeaders().get(headerName));
			assertEquals("Header value not found", Arrays.asList(headerValue1, headerValue2),
					response.getHeaders().get(headerName));

			byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
			assertTrue("Invalid body", Arrays.equals(body, result));
			FileCopyUtils.copyToByteArray(response.getBody());
			assertTrue("Invalid body", Arrays.equals(body, result));
		}
		finally {
			response.close();
		}
	}

}
