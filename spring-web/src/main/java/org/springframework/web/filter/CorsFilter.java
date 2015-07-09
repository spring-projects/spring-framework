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

package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationMapping;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;

/**
 * {@link javax.servlet.Filter} that handles CORS preflight requests and intercepts CORS
 * simple and actual requests thanks to a {@link CorsProcessor} implementation
 * ({@link DefaultCorsProcessor} by default) in order to add the relevant CORS response
 * headers (like {@code Access-Control-Allow-Origin}) using the provided
 * {@link CorsConfigurationSource} (for example a {@link CorsConfigurationMapping} instance.
 *
 * <p>This filter could be used in conjunction with {@link DelegatingFilterProxy} in order
 * to help with its initialization.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 */
public class CorsFilter extends OncePerRequestFilter {

	private CorsProcessor processor = new DefaultCorsProcessor();

	private final CorsConfigurationSource source;


	/**
	 * Constructor accepting a {@link CorsConfigurationSource}, this source will be used
	 * by the filter to find the {@link CorsConfiguration} to use for each incoming request.
	 * @see CorsConfigurationMapping
	 */
	public CorsFilter(CorsConfigurationSource source) {
		this.source = source;
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

		if (CorsUtils.isCorsRequest(request)) {
			CorsConfiguration corsConfiguration = this.source.getCorsConfiguration(request);
			if (corsConfiguration != null) {
				boolean isValid = this.processor.processRequest(corsConfiguration, request, response);
				if (!isValid || CorsUtils.isPreFlightRequest(request)) {
					return;
				}
			}
		}
		filterChain.doFilter(request, response);
	}

}
