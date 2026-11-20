/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import org.jspecify.annotations.Nullable;

import org.springframework.web.server.ServerWebExchange;

/**
 * {@link ApiVersionResolver} that extracts the version from a query parameter.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class QueryApiVersionResolver implements ApiVersionResolver {

	private final String queryParamName;


	public QueryApiVersionResolver(String queryParamName) {
		this.queryParamName = queryParamName;
	}


	@Override
	public @Nullable String resolveVersion(ServerWebExchange exchange) {
		return exchange.getRequest().getQueryParams().getFirst(this.queryParamName);
	}

}
