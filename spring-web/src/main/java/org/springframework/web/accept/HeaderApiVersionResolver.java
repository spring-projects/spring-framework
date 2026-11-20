/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

/**
 * {@link ApiVersionResolver} that extracts the version from a header.
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
	public @Nullable String resolveVersion(HttpServletRequest request) {
		return request.getHeader(this.headerName);
	}

}
