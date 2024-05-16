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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@code Filter} that can be configured to trim trailing slashes, and either
 * send a redirect or wrap the request and continue processing.
 *
 * <p>Use the static {@link #trimTrailingSlash(String...)} method to begin to
 * configure and build an instance. For example:
 *
 * <pre>
 * UrlHandlerFilter filter = UrlHandlerFilter
 *    .trimTrailingSlash("/path1/**").andRedirect(HttpStatus.PERMANENT_REDIRECT)
 *    .trimTrailingSlash("/path2/**").andHandleRequest()
 *    .build();
 * </pre>
 *
 * <p>Note that this {@code Filter} should be ordered after
 * {@link ForwardedHeaderFilter} and before the Spring Security filter chain.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public final class UrlHandlerFilter extends OncePerRequestFilter {

	private static final Log logger = LogFactory.getLog(UrlHandlerFilter.class);


	private final Map<PathPattern, Handler> handlers;


	private UrlHandlerFilter(Map<PathPattern, Handler> handlers) {
		this.handlers = new LinkedHashMap<>(handlers);
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
			for (Map.Entry<PathPattern, Handler> entry : this.handlers.entrySet()) {
				Handler handler = entry.getValue();
				if (entry.getKey().matches(path) && handler.shouldHandle(request)) {
					handler.handle(request, response, chain);
					return;
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
	 * Begin to configure and build a {@link UrlHandlerFilter} by adding a
	 * trailing slash handler for the specified paths. For more details, see
	 * {@link Builder#trimTrailingSlash(String...)}.
	 * @param pathPatterns the URL patterns to which trimming applies.
	 * The pattern itself does not need to end with a trailing slash.
	 * @return a spec to continue with configuring the handler
	 */
	public static TrailingSlashHandlerSpec trimTrailingSlash(String... pathPatterns) {
		return new DefaultBuilder().trimTrailingSlash(pathPatterns);
	}


	/**
	 * Builder to configure and build a {@link UrlHandlerFilter}.
	 */
	public interface Builder {

		/**
		 * An entry point to configure a trim trailing slash handler.
		 * @param pathPatterns the URL patterns to which trimming applies.
		 * The pattern itself does not need to end with a trailing slash.
		 * @return a spec to continue with configuring the handler
		 */
		TrailingSlashHandlerSpec trimTrailingSlash(String... pathPatterns);

		/**
		 * Build the {@link UrlHandlerFilter} instance.
		 */
		UrlHandlerFilter build();
	}


	/**
	 * A spec to configure a trailing slash handler.
	 */
	public interface TrailingSlashHandlerSpec {

		/**
		 * A callback to intercept requests with a trailing slash.
		 * @param consumer callback to be invoked for requests with a trailing slash
		 * @return the same spec instance
		 */
		TrailingSlashHandlerSpec intercept(Consumer<HttpServletRequest> consumer);

		/**
		 * Handle by sending a redirect with the given HTTP status and a location
		 * with the trailing slash trimmed.
		 * @param status the status to use
		 * @return to go back to the main {@link Builder} and either add more
		 * handlers or build the {@code Filter} instance.
		 */
		Builder andRedirect(HttpStatus status);

		/**
		 * Handle by wrapping the request with the trimmed trailing slash and
		 * delegating to the rest of the filter chain.
		 * @return to go back to the main {@link Builder} and either add more
		 * handlers or build the {@code Filter} instance.
		 */
		Builder andHandleRequest();
	}


	/**
	 * Default {@link Builder} implementation.
	 */
	private static final class DefaultBuilder implements Builder {

		private final PathPatternParser patternParser = new PathPatternParser();

		private final Map<PathPattern, Handler> handlers = new LinkedHashMap<>();

		@Override
		public TrailingSlashHandlerSpec trimTrailingSlash(String... pathPatterns) {
			return new DefaultTrailingSlashHandlerSpec(this, parseTrailingSlashPatterns(pathPatterns));
		}

		public void addHandler(List<PathPattern> pathPatterns, Handler handler) {
			for (PathPattern pattern : pathPatterns) {
				this.handlers.put(pattern, handler);
			}
		}

		private List<PathPattern> parseTrailingSlashPatterns(String... patternValues) {
			List<PathPattern> patterns = new ArrayList<>(patternValues.length);
			for (String s : patternValues) {
				if (!s.endsWith("**") && s.charAt(s.length() - 1) != '/') {
					s += "/";
				}
				patterns.add(this.patternParser.parse(s));
			}
			return patterns;
		}

		@Override
		public UrlHandlerFilter build() {
			return new UrlHandlerFilter(this.handlers);
		}

	}


	/**
	 * Default {@link TrailingSlashHandlerSpec} implementation.
	 */
	private static final class DefaultTrailingSlashHandlerSpec implements TrailingSlashHandlerSpec {

		private static final Predicate<HttpServletRequest> trailingSlashPredicate =
				request -> request.getRequestURI().endsWith("/");

		private static final Function<String, String> trimTralingSlashFunction = path -> {
			int index = (StringUtils.hasLength(path) ? path.lastIndexOf('/') : -1);
			return (index != -1 ? path.substring(0, index) : path);
		};

		private final DefaultBuilder parent;

		private final List<PathPattern> pathPatterns;

		@Nullable
		private Consumer<HttpServletRequest> interceptors;

		private DefaultTrailingSlashHandlerSpec(DefaultBuilder parent, List<PathPattern> pathPatterns) {
			this.parent = parent;
			this.pathPatterns = pathPatterns;
		}

		@Override
		public TrailingSlashHandlerSpec intercept(Consumer<HttpServletRequest> interceptor) {
			this.interceptors = (this.interceptors != null ? this.interceptors.andThen(interceptor) : interceptor);
			return this;
		}

		@Override
		public Builder andRedirect(HttpStatus status) {
			return addHandler(new RedirectPathHandler(
					trailingSlashPredicate, trimTralingSlashFunction, status, initInterceptor()));
		}

		@Override
		public Builder andHandleRequest() {
			return addHandler(new RequestWrappingPathHandler(
					trailingSlashPredicate, trimTralingSlashFunction, initInterceptor()));
		}

		private Consumer<HttpServletRequest> initInterceptor() {
			if (this.interceptors != null) {
				return this.interceptors;
			}
			return request -> {
				if (logger.isTraceEnabled()) {
					logger.trace("Trimmed trailing slash: " +
							request.getMethod() + " " + request.getRequestURI());
				}
			};
		}

		private DefaultBuilder addHandler(Handler handler) {
			this.parent.addHandler(this.pathPatterns, handler);
			return this.parent;
		}
	}


	/**
	 * Internal handler for {@link UrlHandlerFilter} to delegate to.
	 */
	private interface Handler {

		/**
		 * Whether the handler handles the given request.
		 */
		boolean shouldHandle(HttpServletRequest request);

		/**
		 * Handle the request, possibly delegating to the rest of the filter chain.
		 */
		void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException;
	}


	/**
	 * Base class for handlers that modify the URL path.
	 */
	private abstract static class AbstractPathHandler implements Handler {

		private final Predicate<HttpServletRequest> pathPredicate;

		private final Function<String, String> pathFunction;

		private final Consumer<HttpServletRequest> interceptor;

		AbstractPathHandler(
				Predicate<HttpServletRequest> pathPredicate, Function<String, String> pathFunction,
				Consumer<HttpServletRequest> interceptor) {

			this.pathPredicate = pathPredicate;
			this.pathFunction = pathFunction;
			this.interceptor = interceptor;
		}

		protected Function<String, String> getPathFunction() {
			return this.pathFunction;
		}

		@Override
		public boolean shouldHandle(HttpServletRequest request) {
			return this.pathPredicate.test(request);
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
	}


	/**
	 * Path handler that sends a redirect.
	 */
	private static final class RedirectPathHandler extends AbstractPathHandler {

		private final HttpStatus httpStatus;

		RedirectPathHandler(
				Predicate<HttpServletRequest> pathPredicate, Function<String, String> pathFunction,
				HttpStatus httpStatus, Consumer<HttpServletRequest> interceptor) {

			super(pathPredicate, pathFunction, interceptor);
			this.httpStatus = httpStatus;
		}

		@Override
		public void handleInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws IOException {

			String location = getPathFunction().apply(request.getRequestURI());

			response.resetBuffer();
			response.setStatus(this.httpStatus.value());
			response.setHeader(HttpHeaders.LOCATION, location);
			response.flushBuffer();
		}
	}


	/**
	 * Path handler that wraps the request and continues processing.
	 */
	private static final class RequestWrappingPathHandler extends AbstractPathHandler {

		RequestWrappingPathHandler(
				Predicate<HttpServletRequest> pathPredicate, Function<String, String> pathFunction,
				Consumer<HttpServletRequest> interceptor) {

			super(pathPredicate, pathFunction, interceptor);
		}

		@Override
		public void handleInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {

			request = new PathHttpServletRequestWrapper(request, getPathFunction());
			chain.doFilter(request, response);
		}
	}


	/**
	 * Wraps the request to return modified path information.
	 */
	private static class PathHttpServletRequestWrapper extends HttpServletRequestWrapper {

		private final String requestURI;

		private final StringBuffer requestURL;

		private final String servletPath;

		private final String pathInfo;

		PathHttpServletRequestWrapper(HttpServletRequest request, Function<String, String> pathFunction) {
			super(request);
			this.requestURI = pathFunction.apply(request.getRequestURI());
			this.requestURL = new StringBuffer(pathFunction.apply(request.getRequestURL().toString()));
			if (StringUtils.hasText(request.getPathInfo())) {
				this.servletPath = request.getServletPath();
				this.pathInfo = pathFunction.apply(request.getPathInfo());
			}
			else {
				this.servletPath = pathFunction.apply(request.getServletPath());
				this.pathInfo = request.getPathInfo();
			}
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
