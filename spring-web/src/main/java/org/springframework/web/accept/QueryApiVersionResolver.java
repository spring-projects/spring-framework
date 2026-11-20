/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;


import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link ApiVersionResolver} that extract the version from a query parameter.
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
	public @Nullable String resolveVersion(HttpServletRequest request) {
		String query = request.getQueryString();
		if (StringUtils.hasText(query)) {
			UriComponents uri = UriComponentsBuilder.fromUriString("?" + query).build();
			return uri.getQueryParams().getFirst(this.queryParamName);
		}
		return null;
	}

}
