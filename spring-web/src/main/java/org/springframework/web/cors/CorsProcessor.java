/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.cors;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Contract for handling CORS preflight requests and intercepting CORS simple
 * and actual requests.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommandation</a>
 */
public interface CorsProcessor {

	/**
	 * Process a preflight CORS request given a {@link CorsConfiguration}.
	 * If the request is not a valid CORS pre-flight request or if it does not
	 * comply with the configuration it should be rejected.
	 * If the request is valid and complies with the configuration, CORS headers
	 * should be added to the response.
	 */
	boolean processPreFlightRequest(CorsConfiguration conf, HttpServletRequest request,
			HttpServletResponse response) throws IOException;

	/**
	 * Process a simple or actual CORS request given a {@link CorsConfiguration}.
	 * If the request is not a valid CORS simple or actual request or if it does
	 * not comply with the configuration, it should be rejected.
	 * If the request is valid and comply with the configuration, this method adds the related
	 * CORS headers to the response.
	 */
	boolean processActualRequest(CorsConfiguration conf, HttpServletRequest request,
			HttpServletResponse response) throws IOException;

}
