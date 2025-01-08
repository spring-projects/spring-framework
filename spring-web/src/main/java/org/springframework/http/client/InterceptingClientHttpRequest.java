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
import java.util.List;
import java.util.function.BiPredicate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

/**
 * Wrapper for a {@link ClientHttpRequest} that has support for {@link ClientHttpRequestInterceptor
 * ClientHttpRequestInterceptors}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 3.1
 */
class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final ClientHttpRequestFactory requestFactory;

	private final List<ClientHttpRequestInterceptor> interceptors;

	private final HttpMethod method;

	private final URI uri;

	private final BiPredicate<URI, HttpMethod> bufferingPredicate;


	protected InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
			List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method,
			BiPredicate<URI, HttpMethod> bufferingPredicate) {

		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.method = method;
		this.uri = uri;
		this.bufferingPredicate = bufferingPredicate;
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		return getExecution().execute(this, bufferedOutput);
	}

	private ClientHttpRequestExecution getExecution() {
		ClientHttpRequestExecution execution = new EndOfChainRequestExecution(this.requestFactory);
		return this.interceptors.stream()
				.reduce(ClientHttpRequestInterceptor::andThen)
				.map(interceptor -> interceptor.apply(execution))
				.orElse(execution);
	}

	private boolean shouldBufferResponse(HttpRequest request) {
		return this.bufferingPredicate.test(request.getURI(), request.getMethod());
	}


	private class EndOfChainRequestExecution implements ClientHttpRequestExecution {

		private final ClientHttpRequestFactory requestFactory;

		public EndOfChainRequestExecution(ClientHttpRequestFactory requestFactory) {
			this.requestFactory = requestFactory;
		}

		@Override
		public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
			ClientHttpRequest delegate = this.requestFactory.createRequest(request.getURI(), request.getMethod());
			request.getHeaders().forEach((key, value) -> delegate.getHeaders().addAll(key, value));
			request.getAttributes().forEach((key, value) -> delegate.getAttributes().put(key, value));
			return executeWithRequest(delegate, body, shouldBufferResponse(request));
		}
	}

}
