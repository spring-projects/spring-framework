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

package org.springframework.web.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@link jakarta.servlet.Filter} that modifies the URL, and then redirects or
 * wraps the request to apply the change.
 *
 * <p>To create an instance, you can use the following:
 *
 * <pre>
 * UrlHandlerFilter filter = UrlHandlerFilter
 *    .trailingSlashHandler("/path1/**").redirect(HttpStatus.PERMANENT_REDIRECT)
 *    .trailingSlashHandler("/path2/**").wrapRequest()
 *    .build();
 * </pre>
 *
 * <p>This {@code Filter} should be ordered after {@link ForwardedHeaderFilter}
 * and before any security filters.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public final class UrlHandlerFilter extends OncePerRequestFilter {

	private static final Log logger = LogFactory.getLog(UrlHandlerFilter.class);


	private final MultiValueMap<Handler, PathPattern> handlers;


	private UrlHandlerFilter(MultiValueMap<Handler, PathPattern> handlers) {
		this.handlers = new LinkedMultiValueMap<>(handlers);
	}


	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		RequestPath previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
		RequestPath path = previousPath;
		try {
			if (path == null) {
				path = ServletRequestPathUtils.parseAndCache(request);
			}
			for (Map.Entry<Handler, List<PathPattern>> entry : this.handlers.entrySet()) {
				if (!entry.getKey().canHandle(request, path)) {
					continue;
				}
				for (PathPattern pattern : entry.getValue()) {
					if (pattern.matches(path)) {
						entry.getKey().handle(request, response, chain);
						return;
					}
				}
			}
		}
		finally {
			if (previousPath != null) {
				ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
			}
		}

		chain.doFilter(request, response);
	}


	/**
	 * Create a builder by adding a handler for URL's with a trailing slash.
	 * @param pathPatterns path patterns to map the handler to, e.g.
	 * <code>"/path/&#42;"</code>, <code>"/path/&#42;&#42;"</code>,
	 * <code>"/path/foo/"</code>.
	 * @return a spec to configure the trailing slash handler with
	 * @see Builder#trailingSlashHandler(String...)
	 */
	public static Builder.TrailingSlashSpec trailingSlashHandler(String... pathPatterns) {
		return new DefaultBuilder().trailingSlashHandler(pathPatterns);
	}


	/**
	 * Builder for {@link UrlHandlerFilter}.
	 */
	public interface Builder {

		/**
		 * Add a handler for URL's with a trailing slash.
		 * @param pathPatterns path patterns to map the handler to, e.g.
		 * <code>"/path/&#42;"</code>, <code>"/path/&#42;&#42;"</code>,
		 * <code>"/path/foo/"</code>.
		 * @return a spec to configure the handler with
		 */
		TrailingSlashSpec trailingSlashHandler(String... pathPatterns);

		/**
		 * Build the {@link UrlHandlerFilter} instance.
		 */
		UrlHandlerFilter build();


		/**
		 * A spec to configure a trailing slash handler.
		 */
		interface TrailingSlashSpec {

			/**
			 * Configure a request consumer to be called just before the handler
			 * is invoked when a URL with a trailing slash is matched.
			 */
			TrailingSlashSpec intercept(Consumer<HttpServletRequest> consumer);

			/**
			 * Handle requests by sending a redirect to the same URL but the
			 * trailing slash trimmed.
			 * @param status the redirect status to use
			 * @return the top level {@link Builder}, which allows adding more
			 * handlers and then building the Filter instance.
			 */
			Builder redirect(HttpStatus status);

			/**
			 * Handle the request by wrapping it in order to trim the trailing
			 * slash, and delegating to the rest of the filter chain.
			 * @return the top level {@link Builder}, which allows adding more
			 * handlers and then building the Filter instance.
			 */
			Builder wrapRequest();
		}
	}


	/**
	 * Default {@link Builder} implementation.
	 */
	private static final class DefaultBuilder implements Builder {

		private final PathPatternParser patternParser = new PathPatternParser();

		private final MultiValueMap<Handler, PathPattern> handlers = new LinkedMultiValueMap<>();

		@Override
		public TrailingSlashSpec trailingSlashHandler(String... patterns) {
			return new DefaultTrailingSlashSpec(patterns);
		}

		private DefaultBuilder addHandler(List<PathPattern> pathPatterns, Handler handler) {
			pathPatterns.forEach(pattern -> this.handlers.add(handler, pattern));
			return this;
		}

		@Override
		public UrlHandlerFilter build() {
			return new UrlHandlerFilter(this.handlers);
		}

		private final class DefaultTrailingSlashSpec implements TrailingSlashSpec {

			private final List<PathPattern> pathPatterns;

			@Nullable
			private Consumer<HttpServletRequest> interceptor;

			private DefaultTrailingSlashSpec(String[] patterns) {
				this.pathPatterns = Arrays.stream(patterns)
						.map(pattern -> pattern.endsWith("**") || pattern.endsWith("/") ? pattern : pattern + "/")
						.map(patternParser::parse)
						.toList();
			}

			@Override
			public TrailingSlashSpec intercept(Consumer<HttpServletRequest> consumer) {
				this.interceptor = (this.interceptor != null ? this.interceptor.andThen(consumer) : consumer);
				return this;
			}

			@Override
			public Builder redirect(HttpStatus status) {
				Handler handler = new RedirectTrailingSlashHandler(status, this.interceptor);
				return DefaultBuilder.this.addHandler(this.pathPatterns, handler);
			}

			@Override
			public Builder wrapRequest() {
				Handler handler = new RequestWrappingTrailingSlashHandler(this.interceptor);
				return DefaultBuilder.this.addHandler(this.pathPatterns, handler);
			}
		}
	}



	/**
	 * Internal handler to encapsulate different ways to handle a request.
	 */
	private interface Handler {

		/**
		 * Whether the handler handles the given request.
		 */
		boolean canHandle(HttpServletRequest request, RequestPath path);

		/**
		 * Handle the request, possibly delegating to the rest of the filter chain.
		 */
		void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException;
	}


	/**
	 * Base class for trailing slash {@link Handler} implementations.
	 */
	private abstract static class AbstractTrailingSlashHandler implements Handler {

		private static final Consumer<HttpServletRequest> defaultInterceptor = request -> {
			if (logger.isTraceEnabled()) {
				logger.trace("Handling trailing slash URL: " +
						request.getMethod() + " " + request.getRequestURI());
			}
		};

		private final Consumer<HttpServletRequest> interceptor;

		protected AbstractTrailingSlashHandler(@Nullable Consumer<HttpServletRequest> interceptor) {
			this.interceptor = (interceptor != null ? interceptor : defaultInterceptor);
		}

		@Override
		public boolean canHandle(HttpServletRequest request, RequestPath path) {
			List<PathContainer.Element> elements = path.elements();
			return (!elements.isEmpty() && elements.get(elements.size() - 1).value().equals("/"));
		}

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {

			this.interceptor.accept(request);
			handleInternal(request, response, chain);
		}

		protected abstract void handleInternal(
				HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException;

		protected String trimTrailingSlash(String path) {
			int index = (StringUtils.hasLength(path) ? path.lastIndexOf('/') : -1);
			return (index != -1 ? path.substring(0, index) : path);
		}
	}


	/**
	 * Path handler that sends a redirect.
	 */
	private static final class RedirectTrailingSlashHandler extends AbstractTrailingSlashHandler {

		private final HttpStatus httpStatus;

		RedirectTrailingSlashHandler(HttpStatus httpStatus, @Nullable Consumer<HttpServletRequest> interceptor) {
			super(interceptor);
			this.httpStatus = httpStatus;
		}

		@Override
		public void handleInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws IOException {

			response.resetBuffer();
			response.setStatus(this.httpStatus.value());
			response.setHeader(HttpHeaders.LOCATION, trimTrailingSlash(request.getRequestURI()));
			response.flushBuffer();
		}
	}


	/**
	 * Path handler that wraps the request and continues processing.
	 */
	private static final class RequestWrappingTrailingSlashHandler extends AbstractTrailingSlashHandler {

		RequestWrappingTrailingSlashHandler(@Nullable Consumer<HttpServletRequest> interceptor) {
			super(interceptor);
		}

		@Override
		public void handleInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {

			String servletPath = request.getServletPath();
			String pathInfo = request.getPathInfo();
			boolean hasPathInfo = StringUtils.hasText(pathInfo);

			request = new TrailingSlashHttpServletRequest(
					request,
					trimTrailingSlash(request.getRequestURI()),
					trimTrailingSlash(request.getRequestURL().toString()),
					hasPathInfo ? servletPath : trimTrailingSlash(servletPath),
					hasPathInfo ? trimTrailingSlash(pathInfo) : pathInfo);

			chain.doFilter(request, response);
		}
	}


	/**
	 * Wraps the request to return modified path information.
	 */
	private static class TrailingSlashHttpServletRequest extends HttpServletRequestWrapper {

		private final String requestURI;

		private final StringBuffer requestURL;

		private final String servletPath;

		private final String pathInfo;

		TrailingSlashHttpServletRequest(HttpServletRequest request,
				String requestURI, String requestURL, String servletPath, String pathInfo) {

			super(request);
			this.requestURI = requestURI;
			this.requestURL = new StringBuffer(requestURL);
			this.servletPath = servletPath;
			this.pathInfo = pathInfo;
		}

		@Override
		public String getRequestURI() {
			return this.requestURI;
		}

		@Override
		public StringBuffer getRequestURL() {
			return this.requestURL;
		}

		@Override
		public String getServletPath() {
			return this.servletPath;
		}

		@Override
		public String getPathInfo() {
			return this.pathInfo;
		}
	}

}
