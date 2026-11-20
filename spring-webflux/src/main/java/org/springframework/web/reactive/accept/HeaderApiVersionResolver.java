/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import org.jspecify.annotations.Nullable;

import org.springframework.web.server.ServerWebExchange;

/**
 * {@link ApiVersionResolver} that extracts the version from a request header.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class HeaderApiVersionResolver implements ApiVersionResolver {

	private final String headerName;


	public HeaderApiVersionResolver(String headerName) {
		this.headerName = headerName;
	}


	@Override
	public @Nullable String resolveVersion(ServerWebExchange exchange) {
		return exchange.getRequest().getHeaders().getFirst(this.headerName);
	}

}
