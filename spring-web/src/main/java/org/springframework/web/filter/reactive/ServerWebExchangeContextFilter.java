/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.filter.reactive;

import java.util.Optional;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Inserts an attribute in the Reactor {@link Context} that makes the current
 * {@link ServerWebExchange} available under the attribute name
 * {@link #EXCHANGE_CONTEXT_ATTRIBUTE}. This is useful for access to the
 * exchange without explicitly passing it to components that participate in
 * request processing.
 *
 * <p>The convenience method {@link #getExchange(ContextView)} looks up the exchange.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ServerWebExchangeContextFilter implements WebFilter {

	/** Attribute name under which the exchange is saved in the context. */
	public static final String EXCHANGE_CONTEXT_ATTRIBUTE =
			ServerWebExchangeContextFilter.class.getName() + ".EXCHANGE_CONTEXT";


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return chain.filter(exchange)
				.contextWrite(context -> context.put(EXCHANGE_CONTEXT_ATTRIBUTE, exchange));
	}


	/**
	 * Access the {@link ServerWebExchange} from a Reactor {@link ContextView},
	 * if available, which is generally the case when
	 * {@link ServerWebExchangeContextFilter} is present in the filter chain.
	 * @param contextView the contextView to get the exchange from
	 * @return an {@link Optional} with the exchange if found
	 * @since 6.0.6
	 */
	public static Optional<ServerWebExchange> getExchange(ContextView contextView) {
		return contextView.getOrEmpty(EXCHANGE_CONTEXT_ATTRIBUTE);
	}

}
