/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.handler;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;

/**
 * {@link WebHandlerDecorator} that invokes a chain of {@link WebFilter WebFilters}
 * before invoking the delegate {@link WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FilteringWebHandler extends WebHandlerDecorator {

	private final DefaultWebFilterChain chain;


	/**
	 * Constructor.
	 * @param filters the chain of filters
	 */
	public FilteringWebHandler(WebHandler handler, List<WebFilter> filters) {
		super(handler);
		this.chain = new DefaultWebFilterChain(handler, filters);
	}


	/**
	 * Return a read-only list of the configured filters.
	 */
	public List<WebFilter> getFilters() {
		return this.chain.getFilters();
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return this.chain.filter(exchange);
	}

}
