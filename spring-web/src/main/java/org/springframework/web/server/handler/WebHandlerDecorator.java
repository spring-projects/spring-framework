/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.handler;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * {@link WebHandler} that decorates and delegates to another {@code WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebHandlerDecorator implements WebHandler {

	private final WebHandler delegate;


	/**
	 * Create a {@code WebHandlerDecorator} for the given delegate.
	 * @param delegate the WebHandler delegate
	 */
	public WebHandlerDecorator(WebHandler delegate) {
		Assert.notNull(delegate, "'delegate' must not be null");
		this.delegate = delegate;
	}


	/**
	 * Return the wrapped delegate.
	 */
	public WebHandler getDelegate() {
		return this.delegate;
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return this.delegate.handle(exchange);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
	}

}
