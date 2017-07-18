/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Overrides the {@link HttpServletResponse#sendRedirect(String)} to set the "Location" header and the HTTP Status
 * directly to avoid the Servlet Container from creating an absolute URL. This allows redirects that have a relative
 * "Location" that ensures support for <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">RFC 7231 Section
 * 7.1.2</a>. It should be noted that while relative redirects are more efficient, they may not work with reverse
 * proxies under some configurations.
 *
 * @author Rob Winch
 * @since 4.3.10
 */
public class RelativeRedirectFilter extends OncePerRequestFilter {
	private HttpStatus sendRedirectHttpStatus = HttpStatus.FOUND;

	/**
	 * Sets the HTTP Status to be used when {@code HttpServletResponse#sendRedirect(String)} is invoked.
	 * @param sendRedirectHttpStatus the 3xx HTTP Status to be used when
	 * {@code HttpServletResponse#sendRedirect(String)} is invoked. The default is {@code HttpStatus.FOUND}.
	 */
	public void setSendRedirectHttpStatus(HttpStatus sendRedirectHttpStatus) {
		Assert.notNull(sendRedirectHttpStatus, "HttpStatus is required");
		if(!sendRedirectHttpStatus.is3xxRedirection()) {
			throw new IllegalArgumentException("sendRedirectHttpStatus should be for redirection. Got " + sendRedirectHttpStatus);
		}
		this.sendRedirectHttpStatus = sendRedirectHttpStatus;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		filterChain.doFilter(request, new RelativeRedirectResponse(response));
	}

	/**
	 * Modifies {@link #sendRedirect(String)} to explicitly set the "Location" header and an HTTP status code to avoid
	 * containers from rewriting the location to be an absolute URL.
	 *
	 * @author Rob Winch
	 * @since 4.3.10
	 */
	private class RelativeRedirectResponse extends HttpServletResponseWrapper {

		/**
		 * Constructs a response adaptor wrapping the given response.
		 *
		 * @param response
		 * @throws IllegalArgumentException if the response is null
		 */
		public RelativeRedirectResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void sendRedirect(String location) throws IOException {
			setHeader(HttpHeaders.LOCATION, location);
			setStatus(sendRedirectHttpStatus.value());
		}
	}
}
