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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@link jakarta.servlet.Filter} that modifies the URL, and then either
 * redirects or wraps the request to effect the change.
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
 * <p>This {@code Filter} should be ordered after {@link ForwardedHeaderFilter},
 * before {@link ServletRequestPathFilter}, and before security filters.
 *
 * @author Rossen Stoyanchev, James Missen
 * @since 6.2
 */
public final class UrlHandlerFilter extends OncePerRequestFilter {

	private static final Log logger = LogFactory.getLog(UrlHandlerFilter.class);


	private final HandlerRegistry handlerRegistry;

	private final boolean excludeContextPath;


	private UrlHandlerFilter(HandlerRegistry handlerRegistry, boolean excludeContextPath) {
		this.handlerRegistry = handlerRegistry;
		this.excludeContextPath = excludeContextPath;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		RequestPath path = (ServletRequestPathUtils.hasParsedRequestPath(request) ?
				ServletRequestPathUtils.getParsedRequestPath(request) :
				ServletRequestPathUtils.parse(request));
		PathContainer lookupPath = excludeContextPath ? path.subPath(path.contextPath().elements().size()) : path;
		Handler handler = handlerRegistry.lookupHandler(path, lookupPath, request);
		if (handler != null) {
			handler.handle(request, response, chain);
			return;
		}
		chain.doFilter(request, response);
	}


	/**
	 * Add a handler that removes the trailing slash from URL paths to ensure
	 * consistent interpretation of paths with or without a trailing slash for
	 * requestion mapping purposes. This is important especially to avoid
	 * misalignment between URL-based authorization decisions and web framework
	 * request mappings.
	 * <p>The root path {@code "/"} is excluded from trailing slash handling.
	 * <p><strong>Note:</strong> A method-level {@code @RequestMapping("/")} adds
	 * a trailing slash to a type-level prefix mapping, and therefore would never
	 * match to a URL with the trailing slash removed. Use {@code @RequestMapping}
	 * without a path instead to avoid the trailing slash in the mapping.
	 * @param patterns path patterns to map the handler to, for example,
	 * <code>"/path/&#42;"</code>, <code>"/path/&#42;&#42;"</code>,
	 * <code>"/path/foo/"</code>.
	 * @return a spec to configure the trailing slash handler with
	 * @see Builder#trailingSlashHandler(String...)
	 */
	public static Builder.TrailingSlashSpec trailingSlashHandler(String... patterns) {
		return new DefaultBuilder().trailingSlashHandler(patterns);
	}


	/**
	 * Builder for {@link UrlHandlerFilter}.
	 */
	public interface Builder {

		/**
		 * Add a handler for URLs with a trailing slash.
		 * @param patterns path patterns to map the handler to, for example,
		 * <code>"/path/&#42;"</code>, <code>"/path/&#42;&#42;"</code>,
		 * <code>"/path/foo/"</code>.
		 * @return a spec to configure the handler with
		 */
		TrailingSlashSpec trailingSlashHandler(String... patterns);

		/**
		 * Specify whether to use path pattern specificity for matching handlers,
		 * with more specific patterns taking precedence.
		 * <p>The default value is {@code false}.
		 * @return the {@link Builder}, which allows adding more
		 * handlers and then building the Filter instance.
		 */
		Builder sortPatternsBySpecificity(boolean sortPatternsBySpecificity);

		/**
		 * Specify whether to exclude the context path when matching paths.
		 * <p>The default value is {@code false}.
		 * @return the {@link Builder}, which allows adding more
		 * handlers and then building the Filter instance.
		 */
		Builder excludeContextPath(boolean excludeContextPath);

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
			 * @param statusCode the redirect status to use
			 * @return the top level {@link Builder}, which allows adding more
			 * handlers and then building the Filter instance.
			 */
			Builder redirect(HttpStatusCode statusCode);

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

		private final MultiValueMap<Handler, String> handlers = new LinkedMultiValueMap<>();

		private boolean sortPatternsBySpecificity = false;

		private boolean excludeContextPath = false;

		@Override
		public TrailingSlashSpec trailingSlashHandler(String... patterns) {
			return new DefaultTrailingSlashSpec(patterns);
		}

		@Override
		public Builder sortPatternsBySpecificity(boolean sortPatternsBySpecificity) {
			this.sortPatternsBySpecificity = sortPatternsBySpecificity;
			return this;
		}

		@Override
		public Builder excludeContextPath(boolean excludeContextPath) {
			this.excludeContextPath = excludeContextPath;
			return this;
		}

		private Builder addHandler(Handler handler, String... patterns) {
			if (!ObjectUtils.isEmpty(patterns)) {
				this.handlers.addAll(handler, Arrays.stream(patterns).toList());
			}
			return this;
		}

		@Override
		public UrlHandlerFilter build() {
			HandlerRegistry handlerRegistry;
			if (this.sortPatternsBySpecificity) {
				handlerRegistry = new OrderedHandlerRegistry();
			}
			else {
				handlerRegistry = new DefaultHandlerRegistry();
			}
			for (Map.Entry<Handler, List<String>> entry : this.handlers.entrySet()) {
				for (String pattern : entry.getValue()) {
					handlerRegistry.registerHandler(pattern, entry.getKey());
				}
			}

			return new UrlHandlerFilter(handlerRegistry, this.excludeContextPath);
		}

		private final class DefaultTrailingSlashSpec implements TrailingSlashSpec {

			private final String[] pathPatterns;

			private @Nullable Consumer<HttpServletRequest> interceptor;

			private DefaultTrailingSlashSpec(String[] patterns) {
				this.pathPatterns = Arrays.stream(patterns)
						.map(pattern -> pattern.endsWith("**") || pattern.endsWith("/") ? pattern : pattern + "/")
						.toArray(String[]::new);
			}

			@Override
			public TrailingSlashSpec intercept(Consumer<HttpServletRequest> consumer) {
				this.interceptor = (this.interceptor != null ? this.interceptor.andThen(consumer) : consumer);
				return this;
			}

			@Override
			public Builder redirect(HttpStatusCode statusCode) {
				Handler handler = new RedirectTrailingSlashHandler(statusCode, this.interceptor);
				return DefaultBuilder.this.addHandler(handler, this.pathPatterns);
			}

			@Override
			public Builder wrapRequest() {
				Handler handler = new RequestWrappingTrailingSlashHandler(this.interceptor);
				return DefaultBuilder.this.addHandler(handler, this.pathPatterns);
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
		boolean supports(HttpServletRequest request, RequestPath path);

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
		public boolean supports(HttpServletRequest request, RequestPath path) {
			List<PathContainer.Element> elements = path.pathWithinApplication().elements();
			return (elements.size() > 1 && elements.get(elements.size() - 1).value().equals("/"));
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

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}


	/**
	 * Path handler that sends a redirect.
	 */
	private static final class RedirectTrailingSlashHandler extends AbstractTrailingSlashHandler {

		private final HttpStatusCode statusCode;

		RedirectTrailingSlashHandler(HttpStatusCode statusCode, @Nullable Consumer<HttpServletRequest> interceptor) {
			super(interceptor);
			Assert.isTrue(statusCode.is3xxRedirection(), "HTTP status code for redirect handlers " +
					"must be in the Redirection class (3xx)");
			this.statusCode = statusCode;
		}

		@Override
		public void handleInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws IOException {

			String location = trimTrailingSlash(request.getRequestURI());
			if (StringUtils.hasText(request.getQueryString())) {
				location += "?" + request.getQueryString();
			}

			response.resetBuffer();
			response.setStatus(this.statusCode.value());
			response.setHeader(HttpHeaders.LOCATION, location);
			response.flushBuffer();
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + " {statusCode=" + this.statusCode.value() + "}";
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

			RequestPath previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
			ServletRequestPathUtils.clearParsedRequestPath(request);
			try {
				chain.doFilter(request, response);
			}
			finally {
				if (previousPath != null) {
					ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
				}
			}
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
			return (isForward() ? getDelegate().getRequestURI() : this.requestURI);
		}

		@Override
		public StringBuffer getRequestURL() {
			return (isForward() ? getDelegate().getRequestURL() : this.requestURL);
		}

		@Override
		public String getServletPath() {
			return (isForward() ? getDelegate().getServletPath() : this.servletPath);
		}

		@Override
		public String getPathInfo() {
			return (isForward() ? getDelegate().getPathInfo() : this.pathInfo);
		}

		private boolean isForward() {
			return (getDispatcherType() == DispatcherType.FORWARD);
		}

		private HttpServletRequest getDelegate() {
			return (HttpServletRequest) getRequest();
		}
	}


	/**
	 * Internal registry to encapsulate different ways to select a handler for a request.
	 */
	private interface HandlerRegistry {

		/**
		 * Register the specified handler for the given path pattern.
		 * @param pattern the path pattern the handler should be mapped to
		 * @param handler the handler instance to register
		 * @throws IllegalStateException if there is a conflicting handler registered
		 */
		void registerHandler(String pattern, Handler handler);

		/**
		 * Look up a handler instance for the given URL lookup path.
		 * @param path the parsed RequestPath
		 * @param lookupPath the URL path the handler is mapped to
		 * @param request the current request
		 * @return the associated handler instance, or {@code null} if not found
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		@Nullable Handler lookupHandler(RequestPath path, PathContainer lookupPath, HttpServletRequest request);
	}


	/**
	 * Base class for {@link HandlerRegistry} implementations.
	 */
	private static abstract class AbstractHandlerRegistry implements HandlerRegistry {

		private final PathPatternParser patternParser = new PathPatternParser();

		@Override
		public final void registerHandler(String pattern, Handler handler) {
			Assert.notNull(pattern, "Pattern must not be null");
			Assert.notNull(handler, "Handler must not be null");

			// Parse path pattern
			pattern = patternParser.initFullPathPattern(pattern);
			PathPattern pathPattern = patternParser.parse(pattern);

			// Register handler
			registerHandlerInternal(pathPattern, handler);
			if (logger.isTraceEnabled()) {
				logger.trace("Mapped [" + pattern + "] onto " + handler);
			}
		}

		protected abstract void registerHandlerInternal(PathPattern pathPattern, Handler handler);
	}


	/**
	 * Default {@link HandlerRegistry} implementation.
	 */
	private static final class DefaultHandlerRegistry extends AbstractHandlerRegistry {

		private final MultiValueMap<Handler, PathPattern> handlerMap = new LinkedMultiValueMap<>();

		@Override
		protected void registerHandlerInternal(PathPattern pathPattern, Handler handler) {
			this.handlerMap.add(handler, pathPattern);
		}

		@Override
		public @Nullable Handler lookupHandler(RequestPath path, PathContainer lookupPath, HttpServletRequest request) {
			for (Map.Entry<Handler, List<PathPattern>> entry : this.handlerMap.entrySet()) {
				if (!entry.getKey().supports(request, path)) {
					continue;
				}
				for (PathPattern pattern : entry.getValue()) {
					if (pattern.matches(lookupPath)) {
						return entry.getKey();
					}
				}
			}
			return null;
		}
	}


	/**
	 * Handler registry that selects the handler mapped to the best-matching
	 * (i.e. most specific) path pattern.
	 */
	private static final class OrderedHandlerRegistry extends AbstractHandlerRegistry {

		private final Map<PathPattern, Handler> handlerMap = new TreeMap<>();

		@Override
		protected void registerHandlerInternal(PathPattern pathPattern, Handler handler) {
			if (this.handlerMap.containsKey(pathPattern)) {
				Handler existingHandler = this.handlerMap.get(pathPattern);
				if (existingHandler != null && existingHandler != handler) {
					throw new IllegalStateException(
							"Cannot map " + handler + " to [" + pathPattern + "]: there is already " +
									existingHandler + " mapped.");
				}
			}
			this.handlerMap.put(pathPattern, handler);
		}

		@Override
		public @Nullable Handler lookupHandler(RequestPath path, PathContainer lookupPath, HttpServletRequest request) {
			for (Map.Entry<PathPattern, Handler> entry : this.handlerMap.entrySet()) {
				if (!entry.getKey().matches(lookupPath)) {
					continue;
				}
				if (entry.getValue().supports(request, path)) {
					return entry.getValue();
				}
				return null; // only match one path pattern
			}
			return null;
		}
	}

}
