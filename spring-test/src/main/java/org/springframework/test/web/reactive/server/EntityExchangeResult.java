/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import org.jspecify.annotations.Nullable;

/**
 * {@code ExchangeResult} sub-class that exposes the response body fully
 * extracted to a representation of type {@code <T>}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the response body type
 * @see FluxExchangeResult
 */
public class EntityExchangeResult<T> extends ExchangeResult {

	private final @Nullable T body;


	EntityExchangeResult(ExchangeResult result, @Nullable T body) {
		super(result);
		this.body = body;
	}


	/**
	 * Return the entity extracted from the response body.
	 */
	public @Nullable T getResponseBody() {
		return this.body;
	}

}
