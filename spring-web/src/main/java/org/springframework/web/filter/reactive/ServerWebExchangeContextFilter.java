/*
 * Copyright 2002-2023 the original author or authors.
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

	/**
	 * Access the {@link ServerWebExchange} from a Reactor {@link Context},
	 * if available, which is generally the case when
	 * {@link ServerWebExchangeContextFilter} is present in the filter chain.
	 * @param context the context to get the exchange from
	 * @return an {@link Optional} with the exchange if found
	 * @deprecated in favor of using {@link #getExchange(ContextView)} which
	 * accepts a {@link ContextView} instead of {@link Context}, reflecting the
	 * fact that the {@code ContextView} is needed only for reading.
	 */
	@Deprecated(since = "6.0.6", forRemoval = true)
	public static Optional<ServerWebExchange> get(Context context) {
		return context.getOrEmpty(EXCHANGE_CONTEXT_ATTRIBUTE);
	}

}
