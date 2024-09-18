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

package org.springframework.web.filter.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@link org.springframework.web.server.WebFilter} that modifies the URL, and
 * then redirects or wraps the request to apply the change.
 *
 * <p>To create an instance, you can use the following:
 *
 * <pre>
 * UrlHandlerFilter filter = UrlHandlerFilter
 *    .trailingSlashHandler("/path1/**").redirect(HttpStatus.PERMANENT_REDIRECT)
 *    .trailingSlashHandler("/path2/**").mutateRequest()
 *    .build();
 * </pre>
 *
 * <p>This {@code WebFilter} should be ordered ahead of security filters.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public final class UrlHandlerFilter implements WebFilter {

	private static final Log logger = LogFactory.getLog(UrlHandlerFilter.class);


	private final MultiValueMap<Handler, PathPattern> handlers;


	private UrlHandlerFilter(MultiValueMap<Handler, PathPattern> handlers) {
		this.handlers = new LinkedMultiValueMap<>(handlers);
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		RequestPath path = exchange.getRequest().getPath();
		for (Map.Entry<Handler, List<PathPattern>> entry : this.handlers.entrySet()) {
			if (!entry.getKey().supports(exchange)) {
				continue;
			}
			for (PathPattern pattern : entry.getValue()) {
				if (pattern.matches(path)) {
					return entry.getKey().handle(exchange, chain);
				}
			}
		}
		return chain.filter(exchange);
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
			 * Configure a request interceptor to be called just before the handler
			 * is invoked when a URL with a trailing slash is matched.
			 */
			TrailingSlashSpec intercept(Function<ServerHttpRequest, Mono<Void>> interceptor);

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
			Builder mutateRequest();
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
			private List<Function<ServerHttpRequest, Mono<Void>>> interceptors;

			private DefaultTrailingSlashSpec(String[] patterns) {
				this.pathPatterns = Arrays.stream(patterns)
						.map(pattern -> pattern.endsWith("**") || pattern.endsWith("/") ? pattern : pattern + "/")
						.map(patternParser::parse)
						.toList();
			}

			@Override
			public TrailingSlashSpec intercept(Function<ServerHttpRequest, Mono<Void>> interceptor) {
				this.interceptors = (this.interceptors != null ? this.interceptors : new ArrayList<>());
				this.interceptors.add(interceptor);
				return this;
			}

			@Override
			public Builder redirect(HttpStatusCode statusCode) {
				Handler handler = new RedirectTrailingSlashHandler(statusCode, this.interceptors);
				return DefaultBuilder.this.addHandler(this.pathPatterns, handler);
			}

			@Override
			public Builder mutateRequest() {
				Handler handler = new RequestWrappingTrailingSlashHandler(this.interceptors);
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
		boolean supports(ServerWebExchange exchange);

		/**
		 * Handle the request, possibly delegating to the rest of the filter chain.
		 */
		Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain);
	}


	/**
	 * Base class for trailing slash {@link Handler} implementations.
	 */
	private abstract static class AbstractTrailingSlashHandler implements Handler {

		private static final List<Function<ServerHttpRequest, Mono<Void>>> defaultInterceptors =
				List.of(request -> {
					if (logger.isTraceEnabled()) {
						logger.trace("Handling trailing slash URL: " + request.getMethod() + " " + request.getURI());
					}
					return Mono.empty();
				});

		private final List<Function<ServerHttpRequest, Mono<Void>>> interceptors;

		protected AbstractTrailingSlashHandler(@Nullable List<Function<ServerHttpRequest, Mono<Void>>> interceptors) {
			this.interceptors = (interceptors != null ? new ArrayList<>(interceptors) : defaultInterceptors);
		}

		@Override
		public boolean supports(ServerWebExchange exchange) {
			ServerHttpRequest request = exchange.getRequest();
			List<PathContainer.Element> elements = request.getPath().pathWithinApplication().elements();
			return (elements.size() > 1 && elements.get(elements.size() - 1).value().equals("/"));
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain) {
			List<Mono<Void>> monos = new ArrayList<>(this.interceptors.size());
			this.interceptors.forEach(interceptor -> monos.add(interceptor.apply(exchange.getRequest())));
			return Flux.concat(monos).then(Mono.defer(() -> handleInternal(exchange, chain)));
		}

		protected abstract Mono<Void> handleInternal(ServerWebExchange exchange, WebFilterChain chain);

		protected String trimTrailingSlash(ServerHttpRequest request) {
			String path = request.getURI().getRawPath();
			int index = (StringUtils.hasLength(path) ? path.lastIndexOf('/') : -1);
			return (index != -1 ? path.substring(0, index) : path);
		}
	}


	/**
	 * Path handler that sends a redirect.
	 */
	private static final class RedirectTrailingSlashHandler extends AbstractTrailingSlashHandler {

		private final HttpStatusCode statusCode;

		RedirectTrailingSlashHandler(
				HttpStatusCode statusCode, @Nullable List<Function<ServerHttpRequest, Mono<Void>>> interceptors) {

			super(interceptors);
			this.statusCode = statusCode;
		}

		@Override
		public Mono<Void> handleInternal(ServerWebExchange exchange, WebFilterChain chain) {
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(this.statusCode);
			response.getHeaders().set(HttpHeaders.LOCATION, trimTrailingSlash(exchange.getRequest()));
			return Mono.empty();
		}
	}


	/**
	 * Path handler that mutates the request and continues processing.
	 */
	private static final class RequestWrappingTrailingSlashHandler extends AbstractTrailingSlashHandler {

		RequestWrappingTrailingSlashHandler(@Nullable List<Function<ServerHttpRequest, Mono<Void>>> interceptors) {
			super(interceptors);
		}

		@Override
		public Mono<Void> handleInternal(ServerWebExchange exchange, WebFilterChain chain) {
			ServerHttpRequest request = exchange.getRequest();
			ServerHttpRequest mutatedRequest = request.mutate().path(trimTrailingSlash(request)).build();
			return chain.filter(exchange.mutate().request(mutatedRequest).build());
		}
	}

}
