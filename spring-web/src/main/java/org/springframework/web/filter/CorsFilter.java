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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * {@link jakarta.servlet.Filter} to handle CORS pre-flight requests and intercept
 * CORS simple and actual requests with a {@link CorsProcessor}, and to update
 * the response, for example, with CORS response headers, based on the policy matched
 * through the provided {@link CorsConfigurationSource}.
 *
 * <p>This is an alternative to configuring CORS in the Spring MVC Java config
 * and the Spring MVC XML namespace. It is useful for applications depending
 * only on spring-web (not on spring-webmvc) or for security constraints that
 * require CORS checks to be performed at {@link jakarta.servlet.Filter} level.
 *
 * <p>This filter could be used in conjunction with {@link DelegatingFilterProxy}
 * in order to help with its initialization.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @see UrlBasedCorsConfigurationSource
 */
public class CorsFilter extends OncePerRequestFilter {

	private final CorsConfigurationSource configSource;

	private CorsProcessor processor = new DefaultCorsProcessor();


	/**
	 * Constructor accepting a {@link CorsConfigurationSource} used by the filter
	 * to find the {@link CorsConfiguration} to use for each incoming request.
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsFilter(CorsConfigurationSource configSource) {
		Assert.notNull(configSource, "CorsConfigurationSource must not be null");
		this.configSource = configSource;
	}


	/**
	 * Configure a custom {@link CorsProcessor} to use to apply the matched
	 * {@link CorsConfiguration} for a request.
	 * <p>By default {@link DefaultCorsProcessor} is used.
	 */
	public void setCorsProcessor(CorsProcessor processor) {
		Assert.notNull(processor, "CorsProcessor must not be null");
		this.processor = processor;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(request);
		boolean isValid = this.processor.processRequest(corsConfiguration, request, response);
		if (!isValid || CorsUtils.isPreFlightRequest(request)) {
			return;
		}
		filterChain.doFilter(request, response);
	}

}
