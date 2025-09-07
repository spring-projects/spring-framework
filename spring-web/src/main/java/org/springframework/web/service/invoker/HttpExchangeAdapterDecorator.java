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

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * {@link HttpExchangeAdapter} that wraps and delegates to another adapter instance.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class HttpExchangeAdapterDecorator implements HttpExchangeAdapter {

	private final HttpExchangeAdapter delegate;


	public HttpExchangeAdapterDecorator(HttpExchangeAdapter delegate) {
		this.delegate = delegate;
	}


	/**
	 * Return the wrapped delgate {@code HttpExchangeAdapter}.
	 */
	public HttpExchangeAdapter getHttpExchangeAdapter() {
		return this.delegate;
	}


	@Override
	public boolean supportsRequestAttributes() {
		return this.delegate.supportsRequestAttributes();
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		this.delegate.exchange(requestValues);
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
		return this.delegate.exchangeForHeaders(requestValues);
	}

	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return this.delegate.exchangeForBody(requestValues, bodyType);
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
		return this.delegate.exchangeForBodilessEntity(requestValues);
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return this.delegate.exchangeForEntity(requestValues, bodyType);
	}

}
