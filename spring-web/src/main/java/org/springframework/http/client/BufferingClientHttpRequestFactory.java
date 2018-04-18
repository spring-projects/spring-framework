/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.http.HttpMethod;

/**
 * Wrapper for a {@link ClientHttpRequestFactory} that buffers
 * all outgoing and incoming streams in memory.
 *
 * <p>Using this wrapper allows for multiple reads of the
 * @linkplain ClientHttpResponse#getBody() response body}.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class BufferingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	/**
	 * Create a buffering wrapper for the given {@link ClientHttpRequestFactory}.
	 * @param requestFactory the target request factory to wrap
	 */
	public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
		super(requestFactory);
	}


	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
			throws IOException {

		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		if (shouldBuffer(uri, httpMethod)) {
			return new BufferingClientHttpRequestWrapper(request);
		}
		else {
			return request;
		}
	}

	/**
	 * Indicates whether the request/response exchange for the given URI and method
	 * should be buffered in memory.
	 * <p>The default implementation returns {@code true} for all URIs and methods.
	 * Subclasses can override this method to change this behavior.
	 * @param uri the URI
	 * @param httpMethod the method
	 * @return {@code true} if the exchange should be buffered; {@code false} otherwise
	 */
	protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
		return true;
	}

}
