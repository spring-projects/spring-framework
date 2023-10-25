/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockFilterConfig;
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
final class MockMvcFilterDecorator implements Filter {

	private static final String ALL_MAPPING_PATTERN = "*";

	private static final String EXTENSION_MAPPING_PATTERN = "*.";

	private static final String PATH_MAPPING_PATTERN = "/*";

	private final Filter delegate;

	@Nullable
	private final Function<ServletContext, FilterConfig> filterConfigInitializer;

	@Nullable
	private final EnumSet<DispatcherType> dispatcherTypes;

	private final boolean hasPatterns;

	/** Patterns that require an exact match, e.g. "/test" */
	private final List<String> exactMatches = new ArrayList<>();

	/** Patterns that require the URL to have a specific prefix, e.g. "/test/*" */
	private final List<String> startsWithMatches = new ArrayList<>();

	/** Patterns that require the request URL to have a specific suffix, e.g. "*.html" */
	private final List<String> endsWithMatches = new ArrayList<>();


	/**
	 * Create instance with URL patterns only.
	 * <p>Note: when this constructor is used, the Filter is not initialized.
	 */
	public MockMvcFilterDecorator(Filter delegate, String[] urlPatterns) {
		Assert.notNull(delegate, "filter cannot be null");
		Assert.notNull(urlPatterns, "urlPatterns cannot be null");
		this.delegate = delegate;
		this.filterConfigInitializer = null;
		this.dispatcherTypes = null;
		this.hasPatterns = initPatterns(urlPatterns);
	}

	/**
	 * Create instance with init parameters to initialize the filter with,
	 * as well as dispatcher types and URL patterns to match.
	 */
	public MockMvcFilterDecorator(
			Filter delegate, @Nullable String filterName, @Nullable Map<String, String> initParams,
			@Nullable EnumSet<DispatcherType> dispatcherTypes, String... urlPatterns) {

		Assert.notNull(delegate, "filter cannot be null");
		Assert.notNull(urlPatterns, "urlPatterns cannot be null");
		this.delegate = delegate;
		this.filterConfigInitializer = getFilterConfigInitializer(filterName, initParams);
		this.dispatcherTypes = dispatcherTypes;
		this.hasPatterns = initPatterns(urlPatterns);
	}

	private static Function<ServletContext, FilterConfig> getFilterConfigInitializer(
			@Nullable String filterName, @Nullable Map<String, String> initParams) {

		return servletContext -> {
			MockFilterConfig filterConfig = (filterName != null ?
					new MockFilterConfig(servletContext, filterName) : new MockFilterConfig(servletContext));
			if (initParams != null) {
				initParams.forEach(filterConfig::addInitParameter);
			}
			return filterConfig;
		};
	}

	private boolean initPatterns(String... urlPatterns) {
		for (String urlPattern : urlPatterns) {
			Assert.notNull(urlPattern, "Found null URL Pattern");
			if (urlPattern.startsWith(EXTENSION_MAPPING_PATTERN)) {
				this.endsWithMatches.add(urlPattern.substring(1));
			}
			else if (urlPattern.equals(PATH_MAPPING_PATTERN) || urlPattern.equals(ALL_MAPPING_PATTERN)) {
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
		return (urlPatterns.length != 0);
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String requestPath = UrlPathHelper.defaultInstance.getPathWithinApplication(httpRequest);

		if (matchDispatcherType(httpRequest.getDispatcherType()) && matchRequestPath(requestPath)) {
			this.delegate.doFilter(request, response, filterChain);
		}
		else {
			filterChain.doFilter(request, response);
		}
	}

	private boolean matchDispatcherType(DispatcherType dispatcherType) {
		return (this.dispatcherTypes == null ||
				this.dispatcherTypes.stream().anyMatch(type -> type == dispatcherType));
	}

	private boolean matchRequestPath(String requestPath) {
		if (!this.hasPatterns) {
			return true;
		}
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

	public void initIfRequired(@Nullable ServletContext servletContext) throws ServletException {
		if (this.filterConfigInitializer != null) {
			FilterConfig filterConfig = this.filterConfigInitializer.apply(servletContext);
			this.delegate.init(filterConfig);
		}
	}

}
