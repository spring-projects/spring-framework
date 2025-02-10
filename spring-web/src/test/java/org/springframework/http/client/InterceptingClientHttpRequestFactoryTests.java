/*
 * Copyright 2002-2025 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.testfixture.http.client.MockClientHttpRequest;
import org.springframework.web.testfixture.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InterceptingClientHttpRequestFactory}
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Brian Clozel
 */
class InterceptingClientHttpRequestFactoryTests {

	private RequestFactoryMock requestFactoryMock = new RequestFactoryMock();

	private MockClientHttpRequest requestMock = new MockClientHttpRequest();

	private MockClientHttpResponse responseMock = new MockClientHttpResponse();

	private InterceptingClientHttpRequestFactory requestFactory;

	@BeforeEach
	void beforeEach() {
		this.requestMock.setResponse(this.responseMock);
	}

	@Test
	void invokeInterceptors() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertThat(((NoOpInterceptor) interceptors.get(0)).invocationCount).isEqualTo(1);
		assertThat(((NoOpInterceptor) interceptors.get(1)).invocationCount).isEqualTo(1);
		assertThat(((NoOpInterceptor) interceptors.get(2)).invocationCount).isEqualTo(1);
		assertThat(requestMock.isExecuted()).isTrue();
		assertThat(response).isSameAs(responseMock);
	}

	@Test
	void skipInterceptor() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add((request, body, execution) -> responseMock);
		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertThat(((NoOpInterceptor) interceptors.get(1)).invocationCount).isZero();
		assertThat(requestMock.isExecuted()).isFalse();
		assertThat(response).isSameAs(responseMock);
	}

	@Test
	void updateRequestHeader() throws Exception {
		final String headerName = "Foo";
		final String headerValue = "Bar";
		final String otherValue = "Baz";

		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
			wrapper.getHeaders().add(headerName, otherValue);
			return execution.execute(wrapper, body);
		};
		requestMock = new MockClientHttpRequest() {
			@Override
			protected ClientHttpResponse executeInternal() {
				List<String> headerValues = getHeaders().get(headerName);
				assertThat(headerValues).containsExactly(headerValue, otherValue);
				return responseMock;
			}
		};
		requestMock.getHeaders().add(headerName, headerValue);

		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));
		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	void updateRequestAttribute() throws Exception {
		final String attrName = "Foo";
		final String attrValue = "Bar";

		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getAttributes().put(attrName, attrValue);
			return execution.execute(request, body);
		};
		requestMock = new MockClientHttpRequest() {
			@Override
			protected ClientHttpResponse executeInternal() {
				assertThat(getAttributes()).containsEntry(attrName, attrValue);
				return responseMock;
			}
		};
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	void updateRequestURI() throws Exception {
		final URI changedUri = URI.create("https://example.com/2");

		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> execution.execute(new HttpRequestWrapper(request) {
			@Override
			public URI getURI() {
				return changedUri;
			}

		}, body);
		requestFactoryMock = new RequestFactoryMock() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				assertThat(uri).isEqualTo(changedUri);
				return super.createRequest(uri, httpMethod);
			}
		};
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	void updateRequestMethod() throws Exception {
		final HttpMethod changedMethod = HttpMethod.POST;

		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> execution.execute(new HttpRequestWrapper(request) {
			@Override
			public HttpMethod getMethod() {
				return changedMethod;
			}

		}, body);
		requestFactoryMock = new RequestFactoryMock() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				assertThat(httpMethod).isEqualTo(changedMethod);
				return super.createRequest(uri, httpMethod);
			}
		};
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	void updateRequestBody() throws Exception {
		final byte[] changedBody = "Foo".getBytes();
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> execution.execute(request, changedBody);
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));
		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		request.execute();

		assertThat(Arrays.equals(changedBody, requestMock.getBodyAsBytes())).isTrue();
		assertThat(requestMock.getHeaders().getContentLength()).isEqualTo(changedBody.length);
	}

	@Test
	void multipleExecutions() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new MultipleExecutionInterceptor());
		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertThat(((NoOpInterceptor) interceptors.get(1)).invocationCount).isEqualTo(2);
		assertThat(requestMock.isExecuted()).isTrue();
		assertThat(response).isSameAs(responseMock);
	}


	private static class NoOpInterceptor implements ClientHttpRequestInterceptor {

		private int invocationCount = 0;

		@Override
		public ClientHttpResponse intercept(
				HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			invocationCount++;
			return execution.execute(request, body);
		}
	}


	private static class MultipleExecutionInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(
				HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			// execute another request first
			execution.execute(new MockClientHttpRequest(), body);
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

}
