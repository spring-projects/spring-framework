/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.util.Assert;

/**
 * Wrapper for a {@link ClientHttpRequestFactory} that has support for {@link ClientHttpRequestInterceptor}s.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
public class InterceptingClientHttpRequestFactory implements ClientHttpRequestFactory {

	private final ClientHttpRequestFactory requestFactory;

	private final ClientHttpRequestInterceptor[] interceptors;

	/**
	 * Creates a new instance of the {@code InterceptingClientHttpRequestFactory} with the given parameters.
	 *
	 * @param requestFactory the request factory to wrap
	 * @param interceptors the interceptors that are to be applied. Can be {@code null}.
	 */
	public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
			ClientHttpRequestInterceptor[] interceptors) {
		Assert.notNull(requestFactory, "'requestFactory' must not be null");
		this.requestFactory = requestFactory;
		this.interceptors = interceptors != null ? interceptors : new ClientHttpRequestInterceptor[0];
	}

	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return new InterceptingClientHttpRequest(requestFactory, interceptors, uri, httpMethod);
	}
}
