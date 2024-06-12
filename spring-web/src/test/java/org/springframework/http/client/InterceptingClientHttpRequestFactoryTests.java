/*
 * Copyright 2002-2024 the original author or authors.
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
 * @author Arjen Poutsma
 * @author Juergen Hoeller
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
	void basic() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertThat(((NoOpInterceptor) interceptors.get(0)).invoked).isTrue();
		assertThat(((NoOpInterceptor) interceptors.get(1)).invoked).isTrue();
		assertThat(((NoOpInterceptor) interceptors.get(2)).invoked).isTrue();
		assertThat(requestMock.isExecuted()).isTrue();
		assertThat(response).isSameAs(responseMock);
	}

	@Test
	void noExecution() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add((request, body, execution) -> responseMock);

		interceptors.add(new NoOpInterceptor());
		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, interceptors);

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertThat(((NoOpInterceptor) interceptors.get(1)).invoked).isFalse();
		assertThat(requestMock.isExecuted()).isFalse();
		assertThat(response).isSameAs(responseMock);
	}

	@Test
	void changeHeaders() throws Exception {
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
	void changeAttribute() throws Exception {
		final String attrName = "Foo";
		final String attrValue = "Bar";

		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			System.out.println("interceptor");
			request.getAttributes().put(attrName, attrValue);
			return execution.execute(request, body);
		};

		requestMock = new MockClientHttpRequest() {
			@Override
			protected ClientHttpResponse executeInternal() {
				System.out.println("execute");
				assertThat(getAttributes()).containsEntry(attrName, attrValue);
				return responseMock;
			}
		};

		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	void changeURI() throws Exception {
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
	void changeMethod() throws Exception {
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
	void changeBody() throws Exception {
		final byte[] changedBody = "Foo".getBytes();

		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> execution.execute(request, changedBody);

		requestFactory = new InterceptingClientHttpRequestFactory(requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = requestFactory.createRequest(URI.create("https://example.com"), HttpMethod.GET);
		request.execute();
		assertThat(Arrays.equals(changedBody, requestMock.getBodyAsBytes())).isTrue();
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

}
