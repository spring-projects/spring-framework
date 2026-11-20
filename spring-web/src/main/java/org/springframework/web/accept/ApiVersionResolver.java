/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

/**
 * Contract to extract the version from a request.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
@FunctionalInterface
public
interface ApiVersionResolver {

	/**
	 * Resolve the version for the given request.
	 * @param request the current request
	 * @return the version value, or {@code null} if not found
	 */
	@Nullable String resolveVersion(HttpServletRequest request);

}
