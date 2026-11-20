/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
