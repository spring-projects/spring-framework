/*
 * Copyright 2002-present the original author or authors.
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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequestFactory} wrapper with support for
 * {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see ClientHttpRequestFactory
 * @see ClientHttpRequestInterceptor
 */
public class InterceptingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	private final List<ClientHttpRequestInterceptor> interceptors;

	private final BiPredicate<URI, HttpMethod> bufferingPredicate;


	/**
	 * Create a new instance with the given parameters.
	 * @param requestFactory the request factory to wrap
	 * @param interceptors the interceptors that are to be applied (can be {@code null})
	 */
	public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
			@Nullable List<ClientHttpRequestInterceptor> interceptors) {

		this(requestFactory, interceptors, null);
	}

	/**
	 * Constructor variant with an additional predicate to decide whether to
	 * buffer the response.
	 * @since 7.0
	 */
	public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
			@Nullable List<ClientHttpRequestInterceptor> interceptors,
			@Nullable BiPredicate<URI, HttpMethod> bufferingPredicate) {

		super(requestFactory);
		this.interceptors = (interceptors != null ? interceptors : Collections.emptyList());
		this.bufferingPredicate = (bufferingPredicate != null ? bufferingPredicate : (uri, method) -> false);
	}


	@Override
	protected ClientHttpRequest createRequest(
			URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {

		return new InterceptingClientHttpRequest(
				requestFactory, this.interceptors, uri, httpMethod, this.bufferingPredicate);
	}

}
