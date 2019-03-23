/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.Iterator;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An {@link AsyncClientHttpRequest} wrapper that enriches it proceeds the actual
 * request execution with calling the registered interceptors.
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @see InterceptingAsyncClientHttpRequestFactory
 */
class InterceptingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private AsyncClientHttpRequestFactory requestFactory;

	private List<AsyncClientHttpRequestInterceptor> interceptors;

	private URI uri;

	private HttpMethod httpMethod;


	/**
	 * Create new instance of {@link InterceptingAsyncClientHttpRequest}.
	 * @param requestFactory the async request factory
	 * @param interceptors the list of interceptors
	 * @param uri the request URI
	 * @param httpMethod the HTTP method
	 */
	public InterceptingAsyncClientHttpRequest(AsyncClientHttpRequestFactory requestFactory,
			List<AsyncClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod httpMethod) {

		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.uri = uri;
		this.httpMethod = httpMethod;
	}


	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] body)
			throws IOException {

		return new AsyncRequestExecution().executeAsync(this, body);
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return uri;
	}


	private class AsyncRequestExecution implements AsyncClientHttpRequestExecution {

		private Iterator<AsyncClientHttpRequestInterceptor> iterator;

		public AsyncRequestExecution() {
			this.iterator = interceptors.iterator();
		}

		@Override
		public ListenableFuture<ClientHttpResponse> executeAsync(HttpRequest request, byte[] body)
				throws IOException {

			if (this.iterator.hasNext()) {
				AsyncClientHttpRequestInterceptor interceptor = this.iterator.next();
				return interceptor.intercept(request, body, this);
			}
			else {
				URI uri = request.getURI();
				HttpMethod method = request.getMethod();
				HttpHeaders headers = request.getHeaders();

				AsyncClientHttpRequest delegate = requestFactory.createAsyncRequest(uri, method);
				delegate.getHeaders().putAll(headers);
				if (body.length > 0) {
					StreamUtils.copy(body, delegate.getBody());
				}

				return delegate.executeAsync();
			}
		}
	}

}
