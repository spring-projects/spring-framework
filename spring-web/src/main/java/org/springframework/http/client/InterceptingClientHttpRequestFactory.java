/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.StreamUtils;

/**
 * Wrapper for a {@link ClientHttpRequestFactory} that has support for {@link ClientHttpRequestInterceptor}s.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class InterceptingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	private final List<ClientHttpRequestInterceptor> interceptors;


	/**
	 * Create a new instance of the {@code InterceptingClientHttpRequestFactory} with the given parameters.
	 * @param requestFactory the request factory to wrap
	 * @param interceptors the interceptors that are to be applied (can be {@code null})
	 */
	public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
			List<ClientHttpRequestInterceptor> interceptors) {

		super(requestFactory);
		this.interceptors = (interceptors != null ? interceptors : Collections.<ClientHttpRequestInterceptor>emptyList());
	}

	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {
		return new InterceptingClientHttpRequest(requestFactory, this.interceptors, uri, httpMethod);
	}

	private static class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

		private final ClientHttpRequestFactory requestFactory;

		private final List<ClientHttpRequestInterceptor> interceptors;

		private HttpMethod method;

		private URI uri;


		protected InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory,
				List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method) {

			this.requestFactory = requestFactory;
			this.interceptors = interceptors;
			this.method = method;
			this.uri = uri;
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
		protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws
				IOException {
			InterceptingRequestExecution
					requestExecution = new InterceptingRequestExecution();
			return requestExecution.execute(this, bufferedOutput);
		}


		private class InterceptingRequestExecution implements ClientHttpRequestExecution {

			private final Iterator<ClientHttpRequestInterceptor> iterator;

			public InterceptingRequestExecution() {
				this.iterator = interceptors.iterator();
			}

			@Override
			public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
				if (this.iterator.hasNext()) {
					ClientHttpRequestInterceptor nextInterceptor = this.iterator.next();
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
}
