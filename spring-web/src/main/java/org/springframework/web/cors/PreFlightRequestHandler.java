/*
 * Copyright 2002-2024 the original author or authors.
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
