/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public abstract class AbstractHttpRequestFactoryTestCase extends AbstractMockWebServerTestCase {

	protected ClientHttpRequestFactory factory;


	@Before
	public final void createFactory() throws Exception {
		factory = createRequestFactory();
		if (factory instanceof InitializingBean) {
			((InitializingBean) factory).afterPropertiesSet();
		}
	}

	@After
	public final void destroyFactory() throws Exception {
		if (factory instanceof DisposableBean) {
			((DisposableBean) factory).destroy();
		}
	}


	protected abstract ClientHttpRequestFactory createRequestFactory();


	@Test
	public void status() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		ClientHttpRequest request = factory.createRequest(uri, HttpMethod.GET);
		assertEquals("Invalid HTTP method", HttpMethod.GET, request.getMethod());
		assertEquals("Invalid HTTP URI", uri, request.getURI());

		ClientHttpResponse response = request.execute();
		try {
			assertEquals("Invalid status code", HttpStatus.NOT_FOUND, response.getStatusCode());
		}
		finally {
			response.close();
		}
	}

	@Test
	public void echo() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
		assertEquals("Invalid HTTP method", HttpMethod.PUT, request.getMethod());

		String headerName = "MyHeader";
		String headerValue1 = "value1";
		request.getHeaders().add(headerName, headerValue1);
		String headerValue2 = "value2";
		request.getHeaders().add(headerName, headerValue2);
		final byte[] body = "Hello World".getBytes("UTF-8");
		request.getHeaders().setContentLength(body.length);

		if (request instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingRequest = (StreamingHttpOutputMessage) request;
			streamingRequest.setBody(outputStream -> StreamUtils.copy(body, outputStream));
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		ClientHttpResponse response = request.execute();
		try {
			assertEquals("Invalid status code", HttpStatus.OK, response.getStatusCode());
			assertTrue("Header not found", response.getHeaders().containsKey(headerName));
			assertEquals("Header value not found", Arrays.asList(headerValue1, headerValue2),
					response.getHeaders().get(headerName));
			byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
			assertTrue("Invalid body", Arrays.equals(body, result));
		}
		finally {
			response.close();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void multipleWrites() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);

		final byte[] body = "Hello World".getBytes("UTF-8");
		if (request instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingRequest = (StreamingHttpOutputMessage) request;
			streamingRequest.setBody(outputStream -> {
				StreamUtils.copy(body, outputStream);
				outputStream.flush();
				outputStream.close();
			});
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		request.execute();
		FileCopyUtils.copy(body, request.getBody());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void headersAfterExecute() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/status/ok"), HttpMethod.POST);

		request.getHeaders().add("MyHeader", "value");
		byte[] body = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(body, request.getBody());

		ClientHttpResponse response = request.execute();
		try {
			request.getHeaders().add("MyHeader", "value");
		}
		finally {
			response.close();
		}
	}

	@Test
	public void httpMethods() throws Exception {
		assertHttpMethod("get", HttpMethod.GET);
		assertHttpMethod("head", HttpMethod.HEAD);
		assertHttpMethod("post", HttpMethod.POST);
		assertHttpMethod("put", HttpMethod.PUT);
		assertHttpMethod("options", HttpMethod.OPTIONS);
		assertHttpMethod("delete", HttpMethod.DELETE);
	}

	protected void assertHttpMethod(String path, HttpMethod method) throws Exception {
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/methods/" + path), method);
			if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
				// requires a body
				try {
					request.getBody().write(32);
				}
				catch (UnsupportedOperationException ex) {
					// probably a streaming request - let's simply ignore it
				}
			}
			response = request.execute();
			assertEquals("Invalid response status", HttpStatus.OK, response.getStatusCode());
			assertEquals("Invalid method", path.toUpperCase(Locale.ENGLISH), request.getMethod().name());
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

	@Test
	public void queryParameters() throws Exception {
		URI uri = new URI(baseUrl + "/params?param1=value&param2=value1&param2=value2");
		ClientHttpRequest request = factory.createRequest(uri, HttpMethod.GET);

		ClientHttpResponse response = request.execute();
		try {
			assertEquals("Invalid status code", HttpStatus.OK, response.getStatusCode());
		}
		finally {
			response.close();
		}
	}

}
