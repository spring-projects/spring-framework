/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import org.springframework.web.server.ServerWebExchange;

/**
 * Contract to add handling of requests with a deprecated API version. Typically,
 * this involves use of response headers to send hints and information about
 * the deprecated version to clients.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface ApiVersionDeprecationHandler {

	/**
	 * Check if the requested API version is deprecated, and if so handle it
	 * accordingly, e.g. by setting response headers to signal the deprecation,
	 * to specify relevant dates and provide links to further details.
	 * @param version the resolved and parsed request version
	 * @param handler the handler chosen for the exchange
	 * @param exchange the current exchange
	 */
	void handleVersion(Comparable<?> version, Object handler, ServerWebExchange exchange);

}
