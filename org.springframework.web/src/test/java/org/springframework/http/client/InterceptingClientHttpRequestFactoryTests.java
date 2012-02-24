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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.HttpRequestWrapper;

import static org.junit.Assert.*;

/** @author Arjen Poutsma */
public class InterceptingClientHttpRequestFactoryTests {

	private InterceptingClientHttpRequestFactory requestFactory;

	private RequestFactoryMock requestFactoryMock;

	private RequestMock requestMock;

	private ResponseMock responseMock;

	@Before
	public void setUp() throws Exception {

		requestFactoryMock = new RequestFactoryMock();
		requestMock = new RequestMock();
		responseMock = new ResponseMock();

	}

	@Test
	public void basic() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertTrue(((NoOpInterceptor) interceptors.get(0)).invoked);
		assertTrue(((NoOpInterceptor) interceptors.get(1)).invoked);
		assertTrue(((NoOpInterceptor) interceptors.get(2)).invoked);
		assertTrue(requestMock.executed);
		assertSame(responseMock, response);
	}

	@Test
	public void noExecution() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
		interceptors.add(new ClientHttpRequestInterceptor() {
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				return responseMock;
			}
		});

		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertFalse(((NoOpInterceptor) interceptors.get(1)).invoked);
		assertFalse(requestMock.executed);
		assertSame(responseMock, response);
	}

	@Test
	public void changeHeaders() throws Exception {
		final String headerName = "Foo";
		final String headerValue = "Bar";
		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {

				return execution.execute(new HttpRequestWrapper(request) {
					@Override
					public HttpHeaders getHeaders() {
						HttpHeaders headers = new HttpHeaders();
						headers.set(headerName, headerValue);
						return headers;
					}
				}, body);
			}
		};

		requestMock = new RequestMock() {
			@Override
			public ClientHttpResponse execute() throws IOException {
				assertEquals(headerValue, getHeaders().getFirst(headerName));
				return super.execute();
			}
		};

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeURI() throws Exception {
		final URI changedUri = new URI("http://example.com/2");
		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {

				return execution.execute(new HttpRequestWrapper(request) {
					@Override
					public URI getURI() {
						return changedUri;
					}

				}, body);
			}
		};

		requestFactoryMock = new RequestFactoryMock() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				assertEquals(changedUri, uri);
				return super.createRequest(uri, httpMethod);
			}
		};

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeMethod() throws Exception {
		final HttpMethod changedMethod = HttpMethod.POST;
		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {

				return execution.execute(new HttpRequestWrapper(request) {
					@Override
					public HttpMethod getMethod() {
						return changedMethod;
					}

				}, body);
			}
		};

		requestFactoryMock = new RequestFactoryMock() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				assertEquals(changedMethod, httpMethod);
				return super.createRequest(uri, httpMethod);
			}
		};

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeBody() throws Exception {
		final byte[] changedBody = "Foo".getBytes();
		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {

				return execution.execute(request, changedBody);
			}
		};

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
		assertTrue(Arrays.equals(changedBody, requestMock.body.toByteArray()));
	}

	private static class NoOpInterceptor implements ClientHttpRequestInterceptor {

		private boolean invoked = false;

		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			invoked = true;
			return execution.execute(request, body);
		}
	}

	private class RequestFactoryMock implements ClientHttpRequestFactory {

		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
			requestMock.setURI(uri);
			requestMock.setMethod(httpMethod);
			return requestMock;
		}

	}

	private class RequestMock implements ClientHttpRequest {

		private URI uri;

		private HttpMethod method;

		private HttpHeaders headers = new HttpHeaders();

		private ByteArrayOutputStream body = new ByteArrayOutputStream();

		private boolean executed = false;

		private RequestMock() {
		}

		public URI getURI() {
			return uri;
		}

		public void setURI(URI uri) {
			this.uri = uri;
		}

		public HttpMethod getMethod() {
			return method;
		}

		public void setMethod(HttpMethod method) {
			this.method = method;
		}

		public HttpHeaders getHeaders() {
			return headers;
		}

		public OutputStream getBody() throws IOException {
			return body;
		}

		public ClientHttpResponse execute() throws IOException {
			executed = true;
			return responseMock;
		}
	}

	private static class ResponseMock implements ClientHttpResponse {

		private HttpStatus statusCode = HttpStatus.OK;

		private String statusText = "";

		private HttpHeaders headers = new HttpHeaders();

		public HttpStatus getStatusCode() throws IOException {
			return statusCode;
		}

		public int getRawStatusCode() throws IOException {
			return statusCode.value();
		}

		public String getStatusText() throws IOException {
			return statusText;
		}

		public HttpHeaders getHeaders() {
			return headers;
		}

		public InputStream getBody() throws IOException {
			return null;
		}

		public void close() {
		}
	}
}
