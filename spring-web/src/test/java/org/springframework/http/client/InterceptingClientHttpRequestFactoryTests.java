/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.HttpRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class InterceptingClientHttpRequestFactoryTests {

	private RequestFactoryMock requestFactoryMock = new RequestFactoryMock();

	private RequestMock requestMock = new RequestMock();

	private ResponseMock responseMock = new ResponseMock();

	private InterceptingClientHttpRequestFactory requestFactory;


	@Test
	public void basic() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(new URI("https://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertThat(((NoOpInterceptor) interceptors.get(0)).invoked).isTrue();
		assertThat(((NoOpInterceptor) interceptors.get(1)).invoked).isTrue();
		assertThat(((NoOpInterceptor) interceptors.get(2)).invoked).isTrue();
		assertThat(requestMock.executed).isTrue();
		assertThat(response).isSameAs(responseMock);
	}

	@Test
	public void noExecution() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				return responseMock;
			}
		});

		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(new URI("https://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertThat(((NoOpInterceptor) interceptors.get(1)).invoked).isFalse();
		assertThat(requestMock.executed).isFalse();
		assertThat(response).isSameAs(responseMock);
	}

	@Test
	public void changeHeaders() throws Exception {
		final String headerName = "Foo";
		final String headerValue = "Bar";
		final String otherValue = "Baz";

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
				wrapper.getHeaders().add(headerName, otherValue);
				return execution.execute(wrapper, body);
			}
		};

		requestMock = new RequestMock() {
			@Override
			public ClientHttpResponse execute() throws IOException {
				List<String> headerValues = getHeaders().get(headerName);
				assertThat(headerValues.size()).isEqualTo(2);
				assertThat(headerValues.get(0)).isEqualTo(headerValue);
				assertThat(headerValues.get(1)).isEqualTo(otherValue);
				return super.execute();
			}
		};
		requestMock.getHeaders().add(headerName, headerValue);

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeURI() throws Exception {
		final URI changedUri = new URI("https://example.com/2");

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
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
				assertThat(uri).isEqualTo(changedUri);
				return super.createRequest(uri, httpMethod);
			}
		};

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeMethod() throws Exception {
		final HttpMethod changedMethod = HttpMethod.POST;

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
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
				assertThat(httpMethod).isEqualTo(changedMethod);
				return super.createRequest(uri, httpMethod);
			}
		};

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeBody() throws Exception {
		final byte[] changedBody = "Foo".getBytes();

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				return execution.execute(request, changedBody);
			}
		};

		requestFactory =
				new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(new URI("https://example.com"), HttpMethod.GET);
		request.execute();
		assertThat(Arrays.equals(changedBody, requestMock.body.toByteArray())).isTrue();
	}


	private static class NoOpInterceptor implements ClientHttpRequestInterceptor {

		private boolean invoked = false;

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			invoked = true;
			return execution.execute(request, body);
		}
	}


	private class RequestFactoryMock implements ClientHttpRequestFactory {

		@Override
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

		@Override
		public URI getURI() {
			return uri;
		}

		public void setURI(URI uri) {
			this.uri = uri;
		}

		@Override
		public HttpMethod getMethod() {
			return method;
		}

		@Override
		public String getMethodValue() {
			return method.name();
		}

		public void setMethod(HttpMethod method) {
			this.method = method;
		}

		@Override
		public HttpHeaders getHeaders() {
			return headers;
		}

		@Override
		public OutputStream getBody() throws IOException {
			return body;
		}

		@Override
		public ClientHttpResponse execute() throws IOException {
			executed = true;
			return responseMock;
		}
	}


	private static class ResponseMock implements ClientHttpResponse {

		private HttpStatus statusCode = HttpStatus.OK;

		private String statusText = "";

		private HttpHeaders headers = new HttpHeaders();

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return statusCode;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return statusCode.value();
		}

		@Override
		public String getStatusText() throws IOException {
			return statusText;
		}

		@Override
		public HttpHeaders getHeaders() {
			return headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			return null;
		}

		@Override
		public void close() {
		}
	}

}
