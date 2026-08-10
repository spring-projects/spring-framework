/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.PreFlightRequestHandler;

/**
 * Servlet Filter that handles pre-flight requests through a
 * {@link PreFlightRequestHandler} and bypasses the rest of the chain.
 *
 * <p>The {@code @EnableWebMvc} config declares a bean of type
 * {@code PreFlightRequestHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0.7
 */
public class PreFlightRequestFilter extends OncePerRequestFilter {

	private final PreFlightRequestHandler handler;


	public PreFlightRequestFilter(PreFlightRequestHandler handler) {
		Assert.notNull(handler, "PreFlightRequestHandler is required");
		this.handler = handler;
	}


	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		if (!CorsUtils.isPreFlightRequest(request)) {
			chain.doFilter(request, response);
			return;
		}

		try {
			this.handler.handlePreFlight(request, response);
		}
		catch (ServletException | IOException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new ServletException("Pre-flight request handling failed: " + ex, ex);
		}
	}

}
