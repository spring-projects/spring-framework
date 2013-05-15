/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.StreamUtils;

/**
 * Wrapper for a {@link ClientHttpRequest} that has support for {@link ClientHttpRequestInterceptor}s.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final ClientHttpRequestFactory requestFactory;

	private final List<ClientHttpRequestInterceptor> interceptors;

	private HttpMethod method;

	private URI uri;

	protected InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
			List<ClientHttpRequestInterceptor> interceptors,
			URI uri,
			HttpMethod method) {
		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.method = method;
		this.uri = uri;
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		RequestExecution requestExecution = new RequestExecution();

		return requestExecution.execute(this, bufferedOutput);
	}

	private class RequestExecution implements ClientHttpRequestExecution {

		private final Iterator<ClientHttpRequestInterceptor> iterator;

		private RequestExecution() {
			this.iterator = interceptors.iterator();
		}

		@Override
		public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
			if (iterator.hasNext()) {
				ClientHttpRequestInterceptor nextInterceptor = iterator.next();
				return nextInterceptor.intercept(request, body, this);
			}
			else {
				ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), request.getMethod());

				delegate.getHeaders().putAll(request.getHeaders());

				if (body.length > 0) {
					StreamUtils.copy(body, delegate.getBody());
				}
				return delegate.execute();
			}
		}
	}

}
