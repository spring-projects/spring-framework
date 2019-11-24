/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.web.util.UrlPathHelper;

/**
 * A Filter that invokes a delegate {@link Filter} only if the request URL
 * matches the pattern it is mapped to using pattern matching as defined in the
 * Servlet spec.
 *
 * @author Rob Winch
 * @since 3.2
 */
final class PatternMappingFilterProxy implements Filter {

	private static final String EXTENSION_MAPPING_PATTERN = "*.";

	private static final String PATH_MAPPING_PATTERN = "/*";

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

	private final Filter delegate;

	/** Patterns that require an exact match, e.g. "/test" */
	private final List<String> exactMatches = new ArrayList<>();

	/** Patterns that require the URL to have a specific prefix, e.g. "/test/*" */
	private final List<String> startsWithMatches = new ArrayList<>();

	/** Patterns that require the request URL to have a specific suffix, e.g. "*.html" */
	private final List<String> endsWithMatches = new ArrayList<>();


	/**
	 * Creates a new instance.
	 */
	public PatternMappingFilterProxy(Filter delegate, String... urlPatterns) {
		Assert.notNull(delegate, "A delegate Filter is required");
		this.delegate = delegate;
		for (String urlPattern : urlPatterns) {
			addUrlPattern(urlPattern);
		}
	}

	private void addUrlPattern(String urlPattern) {
		Assert.notNull(urlPattern, "Found null URL Pattern");
		if (urlPattern.startsWith(EXTENSION_MAPPING_PATTERN)) {
			this.endsWithMatches.add(urlPattern.substring(1, urlPattern.length()));
		}
		else if (urlPattern.equals(PATH_MAPPING_PATTERN)) {
			this.startsWithMatches.add("");
		}
		else if (urlPattern.endsWith(PATH_MAPPING_PATTERN)) {
			this.startsWithMatches.add(urlPattern.substring(0, urlPattern.length() - 1));
			this.exactMatches.add(urlPattern.substring(0, urlPattern.length() - 2));
		}
		else {
			if (urlPattern.isEmpty()) {
				urlPattern = "/";
			}
			this.exactMatches.add(urlPattern);
		}
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String requestPath = urlPathHelper.getPathWithinApplication(httpRequest);

		if (matches(requestPath)) {
			this.delegate.doFilter(request, response, filterChain);
		}
		else {
			filterChain.doFilter(request, response);
		}
	}

	private boolean matches(String requestPath) {
		for (String pattern : this.exactMatches) {
			if (pattern.equals(requestPath)) {
				return true;
			}
		}
		if (!requestPath.startsWith("/")) {
			return false;
		}
		for (String pattern : this.endsWithMatches) {
			if (requestPath.endsWith(pattern)) {
				return true;
			}
		}
		for (String pattern : this.startsWithMatches) {
			if (requestPath.startsWith(pattern)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.delegate.init(filterConfig);
	}

	@Override
	public void destroy() {
		this.delegate.destroy();
	}

}
