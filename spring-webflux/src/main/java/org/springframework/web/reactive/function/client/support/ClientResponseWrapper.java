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

package org.springframework.web.reactive.function.client.support;

import guru.mocker.annotation.mixin.Mixin;

import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Implementation of the {@link ClientResponse} interface that can be subclassed
 * to adapt the request in a
 * {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction exchange filter function}.
 * All methods default to calling through to the wrapped request.
 *
 * @author Arjen Poutsma
 * @since 5.0.5
 */
@Mixin
public class ClientResponseWrapper extends ClientResponseWrapperForwarder implements ClientResponse {

	/**
	 * Create a new {@code ClientResponseWrapper} that wraps the given response.
	 * @param delegate the response to wrap
	 */
	public ClientResponseWrapper(ClientResponse delegate) {
		super(delegate);
	}

	/**
	 * Return the wrapped request.
	 */
	public ClientResponse response() {
		return this.delegate;
	}

	/**
	 * Implementation of the {@code Headers} interface that can be subclassed
	  * to adapt the headers in a
	  * {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction exchange filter function}.
	  * All methods default to calling through to the wrapped request.
	 */
	@Mixin
	public static class HeadersWrapper extends HeadersWrapperForwarder implements ClientResponse.Headers {

		/**
		 * Create a new {@code HeadersWrapper} that wraps the given request.
		 *
		 * @param headers the headers to wrap
		 */
		public HeadersWrapper(Headers headers) {
			super(headers);
		}
	}
}
