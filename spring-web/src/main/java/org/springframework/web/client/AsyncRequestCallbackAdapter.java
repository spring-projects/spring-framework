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

package org.springframework.web.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adapts a {@link RequestCallback} to the {@link AsyncRequestCallback} interface.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class AsyncRequestCallbackAdapter implements AsyncRequestCallback {

	private final RequestCallback adaptee;

	/**
	 * Creates a new {@code AsyncRequestCallbackAdapter} from the given
	 * {@link RequestCallback}.
	 *
	 * @param requestCallback the callback to base this adapter on
	 */
	public AsyncRequestCallbackAdapter(RequestCallback requestCallback) {
		this.adaptee = requestCallback;
	}

	@Override
	public void doWithRequest(final AsyncClientHttpRequest request) throws IOException {
		if (adaptee != null) {
			adaptee.doWithRequest(new ClientHttpRequest() {
				@Override
				public ClientHttpResponse execute() throws IOException {
					throw new UnsupportedOperationException("execute not supported");
				}

				@Override
				public OutputStream getBody() throws IOException {
					return request.getBody();
				}

				@Override
				public HttpMethod getMethod() {
					return request.getMethod();
				}

				@Override
				public URI getURI() {
					return request.getURI();
				}

				@Override
				public HttpHeaders getHeaders() {
					return request.getHeaders();
				}

			});
		}
	}
}
