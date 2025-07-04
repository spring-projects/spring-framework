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

package org.springframework.web.client.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapterDecorator;
import org.springframework.web.service.invoker.HttpRequestValues;

/**
 * {@code HttpExchangeAdapterDecorator} that suppresses the
 * {@link HttpClientErrorException.NotFound} exception raised on a 404 response
 * and returns a {@code ResponseEntity} with the status set to
 * {@link org.springframework.http.HttpStatus#NOT_FOUND} status, or
 * {@code null} from {@link #exchangeForBody}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class NotFoundRestClientAdapterDecorator extends HttpExchangeAdapterDecorator {


	public NotFoundRestClientAdapterDecorator(HttpExchangeAdapter delegate) {
		super(delegate);
	}


	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		try {
			return super.exchangeForBody(values, bodyType);
		}
		catch (HttpClientErrorException.NotFound ex) {
			return null;
		}
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		try {
			return super.exchangeForBodilessEntity(values);
		}
		catch (HttpClientErrorException.NotFound ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		try {
			return super.exchangeForEntity(values, bodyType);
		}
		catch (HttpClientErrorException.NotFound ex) {
			return ResponseEntity.notFound().build();
		}
	}

}
