/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.cors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler for CORS pre-flight requests.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public interface PreFlightRequestHandler {

	/**
	 * Handle a pre-flight request by finding and applying the CORS configuration
	 * that matches the expected actual request. As a result of handling, the
	 * response should be updated with CORS headers or rejected with
	 * {@link org.springframework.http.HttpStatus#FORBIDDEN}.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 */
	void handlePreFlight(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
