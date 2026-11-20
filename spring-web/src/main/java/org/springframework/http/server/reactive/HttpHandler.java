/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import reactor.core.publisher.Mono;

/**
 * Lowest level contract for reactive HTTP request handling that serves as a
 * common denominator across different runtimes.
 *
 * <p>Higher-level, but still generic, building blocks for applications such as
 * {@code WebFilter}, {@code WebSession}, {@code ServerWebExchange}, and others
 * are available in the {@code org.springframework.web.server} package.
 *
 * <p>Application level programming models such as annotated controllers and
 * functional handlers are available in the {@code spring-webflux} module.
 *
 * <p>Typically an {@link HttpHandler} represents an entire application with
 * higher-level programming models bridged via
 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder}.
 * Multiple applications at unique context paths can be plugged in with the
 * help of the {@link ContextPathCompositeHandler}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ContextPathCompositeHandler
 */
public interface HttpHandler {

	/**
	 * Handle the given request and write to the response.
	 * @param request current request
	 * @param response current response
	 * @return indicates completion of request handling
	 */
	Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response);

}
