/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
