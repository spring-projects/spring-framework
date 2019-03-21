/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Future;

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
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

public abstract class AbstractAsyncHttpRequestFactoryTestCase extends AbstractMockWebServerTestCase {

	protected AsyncClientHttpRequestFactory factory;


	@Before
	public final void createFactory() throws Exception {
		this.factory = createRequestFactory();
		if (this.factory instanceof InitializingBean) {
			((InitializingBean) this.factory).afterPropertiesSet();
		}
	}

	@After
	public final void destroyFactory() throws Exception {
		if (this.factory instanceof DisposableBean) {
			((DisposableBean) this.factory).destroy();
		}
	}

	protected abstract AsyncClientHttpRequestFactory createRequestFactory();


	@Test
	public void status() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(uri, HttpMethod.GET);
		assertEquals("Invalid HTTP method", HttpMethod.GET, request.getMethod());
		assertEquals("Invalid HTTP URI", uri, request.getURI());
		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		ClientHttpResponse response = futureResponse.get();
		try {
			assertEquals("Invalid status code", HttpStatus.NOT_FOUND, response.getStatusCode());
		}
		finally {
			response.close();
		}
	}

	@Test
	public void statusCallback() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(uri, HttpMethod.GET);
		assertEquals("Invalid HTTP method", HttpMethod.GET, request.getMethod());
		assertEquals("Invalid HTTP URI", uri, request.getURI());
		ListenableFuture<ClientHttpResponse> listenableFuture = request.executeAsync();
		listenableFuture.addCallback(new ListenableFutureCallback<ClientHttpResponse>() {
			@Override
			public void onSuccess(ClientHttpResponse result) {
				try {
					assertEquals("Invalid status code", HttpStatus.NOT_FOUND, result.getStatusCode());
				}
				catch (IOException ex) {
					fail(ex.getMessage());
				}
			}
			@Override
			public void onFailure(Throwable ex) {
				fail(ex.getMessage());
			}
		});
		ClientHttpResponse response = listenableFuture.get();
		try {
			assertEquals("Invalid status code", HttpStatus.NOT_FOUND, response.getStatusCode());
		}
		finally {
			response.close();
		}
	}

	@Test
	public void echo() throws Exception {
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
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

		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		ClientHttpResponse response = futureResponse.get();
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

	@Test
	public void multipleWrites() throws Exception {
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
		final byte[] body = "Hello World".getBytes("UTF-8");

		if (request instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingRequest = (StreamingHttpOutputMessage) request;
			streamingRequest.setBody(outputStream -> StreamUtils.copy(body, outputStream));
		}
		else {
			StreamUtils.copy(body, request.getBody());
		}

		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		ClientHttpResponse response = futureResponse.get();
		try {
			FileCopyUtils.copy(body, request.getBody());
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException ex) {
			// expected
		}
		finally {
			response.close();
		}
	}

	@Test
	public void headersAfterExecute() throws Exception {
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
		request.getHeaders().add("MyHeader", "value");
		byte[] body = "Hello World".getBytes("UTF-8");
		FileCopyUtils.copy(body, request.getBody());

		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		ClientHttpResponse response = futureResponse.get();
		try {
			request.getHeaders().add("MyHeader", "value");
			fail("UnsupportedOperationException expected");
		}
		catch (UnsupportedOperationException ex) {
			// expected
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
			AsyncClientHttpRequest request = this.factory.createAsyncRequest(new URI(baseUrl + "/methods/" + path), method);
			if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
				// requires a body
				request.getBody().write(32);
			}
			Future<ClientHttpResponse> futureResponse = request.executeAsync();
			response = futureResponse.get();
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
	public void cancel() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		AsyncClientHttpRequest request = this.factory.createAsyncRequest(uri, HttpMethod.GET);
		Future<ClientHttpResponse> futureResponse = request.executeAsync();
		futureResponse.cancel(true);
		assertTrue(futureResponse.isCancelled());
	}


}
