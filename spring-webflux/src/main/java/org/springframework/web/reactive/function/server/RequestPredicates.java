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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.patterns.PathPattern;
import org.springframework.web.util.patterns.PathPatternParser;

/**
 * Implementations of {@link RequestPredicate} that implement various useful
 * request matching operations, such as matching based on path, HTTP method, etc.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class RequestPredicates {

	private static final Log logger = LogFactory.getLog(RequestPredicates.class);

	private static final PathPatternParser DEFAULT_PATTERN_PARSER = new PathPatternParser();


	/**
	 * Return a {@code RequestPredicate} that always matches.
	 * @return a predicate that always matches
	 */
	public static RequestPredicate all() {
		return request -> true;
	}


	/**
	 * Return a {@code RequestPredicate} that tests against the given HTTP method.
	 * @param httpMethod the HTTP method to match to
	 * @return a predicate that tests against the given HTTP method
	 */
	public static RequestPredicate method(HttpMethod httpMethod) {
		return new HttpMethodPredicate(httpMethod);
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request path against the given path pattern.
	 * @param pattern the pattern to match to
	 * @return a predicate that tests against the given path pattern
	 */
	public static RequestPredicate path(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null");
		return pathPredicates(DEFAULT_PATTERN_PARSER).apply(pattern);
	}

	/**
	 * Return a function that creates new path-matching {@code RequestPredicates} from pattern
	 * Strings using the given {@link PathPatternParser}. This method can be used to specify a
	 * non-default, customized {@code PathPatternParser} when resolving path patterns.
	 * @param patternParser the parser used to parse patterns given to the returned function
	 * @return a function that resolves patterns Strings into path-matching
	 * {@code RequestPredicate}s
	 */
	public static Function<String, RequestPredicate> pathPredicates(PathPatternParser patternParser) {
		Assert.notNull(patternParser, "'patternParser' must not be null");
		return pattern -> {
			synchronized (patternParser) {
				return new PathPatternPredicate(patternParser.parse(pattern));
			}
		};
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request's headers against the given headers predicate.
	 * @param headersPredicate a predicate that tests against the request headers
	 * @return a predicate that tests against the given header predicate
	 */
	public static RequestPredicate headers(Predicate<ServerRequest.Headers> headersPredicate) {
		return new HeadersPredicate(headersPredicate);
	}

	/**
	 * Return a {@code RequestPredicate} that tests if the request's
	 * {@linkplain ServerRequest.Headers#contentType() content type} is
	 * {@linkplain MediaType#includes(MediaType) included} by any of the given media types.
	 * @param mediaTypes the media types to match the request's content type against
	 * @return a predicate that tests the request's content type against the given media types
	 */
	public static RequestPredicate contentType(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		Set<MediaType> mediaTypeSet = new HashSet<>(Arrays.asList(mediaTypes));

		return headers(new Predicate<ServerRequest.Headers>() {
			@Override
			public boolean test(ServerRequest.Headers headers) {
				MediaType contentType =
						headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
				boolean match = mediaTypeSet.stream()
						.anyMatch(mediaType -> mediaType.includes(contentType));
				traceMatch("Content-Type", mediaTypeSet, contentType, match);
				return match;
			}

			@Override
			public String toString() {
				return String.format("Content-Type: %s", mediaTypeSet);
			}
		});
	}

	/**
	 * Return a {@code RequestPredicate} that tests if the request's
	 * {@linkplain ServerRequest.Headers#accept() accept} header is
	 * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with any of the given media types.
	 * @param mediaTypes the media types to match the request's accept header against
	 * @return a predicate that tests the request's accept header against the given media types
	 */
	public static RequestPredicate accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		Set<MediaType> mediaTypeSet = new HashSet<>(Arrays.asList(mediaTypes));

		return headers(new Predicate<ServerRequest.Headers>() {
			@Override
			public boolean test(ServerRequest.Headers headers) {
				List<MediaType> acceptedMediaTypes = headers.accept();
				if (acceptedMediaTypes.isEmpty()) {
					acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
				}
				else {
					MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
				}
				boolean match = acceptedMediaTypes.stream()
						.anyMatch(acceptedMediaType -> mediaTypeSet.stream()
								.anyMatch(acceptedMediaType::isCompatibleWith));
				traceMatch("Accept", mediaTypeSet, acceptedMediaTypes, match);
				return match;
			}
			@Override
			public String toString() {
				return String.format("Accept: %s", mediaTypeSet);
			}
		});
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code GET}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is GET and if the given pattern
	 * matches against the request path
	 */
	public static RequestPredicate GET(String pattern) {
		return method(HttpMethod.GET).and(path(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code HEAD}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is HEAD and if the given pattern
	 * matches against the request path
	 */
	public static RequestPredicate HEAD(String pattern) {
		return method(HttpMethod.HEAD).and(path(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code POST}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is POST and if the given pattern
	 * matches against the request path
	 */
	public static RequestPredicate POST(String pattern) {
		return method(HttpMethod.POST).and(path(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code PUT}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is PUT and if the given pattern
	 * matches against the request path
	 */
	public static RequestPredicate PUT(String pattern) {
		return method(HttpMethod.PUT).and(path(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code PATCH}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is PATCH and if the given pattern
	 * matches against the request path
	 */
	public static RequestPredicate PATCH(String pattern) {
		return method(HttpMethod.PATCH).and(path(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code DELETE}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is DELETE and if the given pattern
	 * matches against the request path
	 */
	public static RequestPredicate DELETE(String pattern) {
		return method(HttpMethod.DELETE).and(path(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code OPTIONS}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is OPTIONS and if the given pattern
	 * matches against the request path
	 */
	public static RequestPredicate OPTIONS(String pattern) {
		return method(HttpMethod.OPTIONS).and(path(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's path has the given extension.
	 * @param extension the path extension to match against, ignoring case
	 * @return a predicate that matches if the request's path has the given file extension
	 */
	public static RequestPredicate pathExtension(String extension) {
		Assert.notNull(extension, "'extension' must not be null");
		return pathExtension(pathExtension -> {
			boolean match = extension.equalsIgnoreCase(pathExtension);
			traceMatch("Extension", extension, pathExtension, match);
			return match;
		});
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's path matches the given
	 * predicate.
	 * @param extensionPredicate the predicate to test against the request path extension
	 * @return a predicate that matches if the given predicate matches against the request's path
	 * file extension
	 */
	public static RequestPredicate pathExtension(Predicate<String> extensionPredicate) {
		Assert.notNull(extensionPredicate, "'extensionPredicate' must not be null");
		return request -> {
			String pathExtension = UriUtils.extractFileExtension(request.path());
			return extensionPredicate.test(pathExtension);
		};
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request's query parameter of the given name
	 * against the given predicate.
	 * @param name the name of the query parameter to test against
	 * @param predicate predicate to test against the query parameter value
	 * @return a predicate that matches the given predicate against the query parameter of the given name
	 * @see ServerRequest#queryParam(String)
	 */
	public static RequestPredicate queryParam(String name, Predicate<String> predicate) {
		return request -> {
			Optional<String> s = request.queryParam(name);
			return s.filter(predicate).isPresent();
		};
	}


	private static void traceMatch(String prefix, Object desired, Object actual, boolean match) {
		if (logger.isTraceEnabled()) {
			String message = String.format("%s \"%s\" %s against value \"%s\"",
					prefix, desired, match ? "matches" : "does not match", actual);
			logger.trace(message);
		}
	}


	private static class HttpMethodPredicate implements RequestPredicate {

		private final HttpMethod httpMethod;

		public HttpMethodPredicate(HttpMethod httpMethod) {
			Assert.notNull(httpMethod, "'httpMethod' must not be null");
			this.httpMethod = httpMethod;
		}

		@Override
		public boolean test(ServerRequest request) {
			boolean match = this.httpMethod == request.method();
			traceMatch("Method", this.httpMethod, request.method(), match);
			return match;
		}

		@Override
		public String toString() {
			return this.httpMethod.toString();
		}
	}


	private static class PathPatternPredicate implements RequestPredicate {

		private final PathPattern pattern;

		public PathPatternPredicate(PathPattern pattern) {
			Assert.notNull(pattern, "'pattern' must not be null");
			this.pattern = pattern;
		}

		@Override
		public boolean test(ServerRequest request) {
			String path = request.path();
			boolean match = this.pattern.matches(path);
			traceMatch("Pattern", this.pattern.getPatternString(), path, match);
			if (match) {
				mergeTemplateVariables(request);
				return true;
			}
			else {
				return false;
			}
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			String remainingPath = this.pattern.getPathRemaining(request.path());
			return Optional.ofNullable(remainingPath)
					.map(path -> !path.startsWith("/") ? "/" + path : path)
					.map(path -> {
						// TODO: re-enable when SPR-15419 has been fixed.
						// mergeTemplateVariables(request);
						return new SubPathServerRequestWrapper(request, path);
					});
		}

		private void mergeTemplateVariables(ServerRequest request) {
			Map<String, String> newVariables = this.pattern.matchAndExtract(request.path());
			if (!newVariables.isEmpty()) {
				Map<String, String> oldVariables = request.pathVariables();
				Map<String, String> variables = new LinkedHashMap<>(oldVariables);
				variables.putAll(newVariables);
				request.attributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
						Collections.unmodifiableMap(variables));
			}
		}

		@Override
		public String toString() {
			return this.pattern.getPatternString();
		}
	}


	private static class HeadersPredicate implements RequestPredicate {

		private final Predicate<ServerRequest.Headers> headersPredicate;

		public HeadersPredicate(Predicate<ServerRequest.Headers> headersPredicate) {
			Assert.notNull(headersPredicate, "'headersPredicate' must not be null");
			this.headersPredicate = headersPredicate;
		}

		@Override
		public boolean test(ServerRequest request) {
			return this.headersPredicate.test(request.headers());
		}

		@Override
		public String toString() {
			return this.headersPredicate.toString();
		}
	}

	static class AndRequestPredicate implements RequestPredicate {

		private final RequestPredicate left;

		private final RequestPredicate right;

		public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean test(ServerRequest t) {
			return this.left.test(t) && this.right.test(t);
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return this.left.nest(request).flatMap(this.right::nest);
		}

		@Override
		public String toString() {
			return String.format("(%s && %s)", this.left, this.right);
		}
	}

	static class OrRequestPredicate implements RequestPredicate {

		private final RequestPredicate left;

		private final RequestPredicate right;

		public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
			this.left = left;
			this.right = right;
		}
		@Override
		public boolean test(ServerRequest t) {
			return this.left.test(t) || this.right.test(t);
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			Optional<ServerRequest> leftResult = this.left.nest(request);
			if (leftResult.isPresent()) {
				return leftResult;
			}
			else {
				return this.right.nest(request);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s || %s)", this.left, this.right);
		}
	}

	private static class SubPathServerRequestWrapper implements ServerRequest {

		private final ServerRequest request;

		private final String subPath;


		public SubPathServerRequestWrapper(ServerRequest request, String subPath) {
			this.request = request;
			this.subPath = subPath;
		}

		@Override
		public HttpMethod method() {
			return this.request.method();
		}

		@Override
		public URI uri() {
			return this.request.uri();
		}

		@Override
		public String path() {
			return this.subPath;
		}

		@Override
		public Headers headers() {
			return this.request.headers();
		}

		@Override
		public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor) {
			return this.request.body(extractor);
		}

		@Override
		public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {
			return this.request.body(extractor, hints);
		}

		@Override
		public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
			return this.request.bodyToMono(elementClass);
		}

		@Override
		public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
			return this.request.bodyToFlux(elementClass);
		}

		@Override
		public <T> Optional<T> attribute(String name) {
			return this.request.attribute(name);
		}

		@Override
		public Map<String, Object> attributes() {
			return this.request.attributes();
		}

		@Override
		public Optional<String> queryParam(String name) {
			return this.request.queryParam(name);
		}

		@Override
		public List<String> queryParams(String name) {
			return this.request.queryParams(name);
		}

		@Override
		public String pathVariable(String name) {
			return this.request.pathVariable(name);
		}

		@Override
		public Map<String, String> pathVariables() {
			return this.request.pathVariables();
		}

		@Override
		public Mono<WebSession> session() {
			return this.request.session();
		}

		@Override
		public String toString() {
			return method() + " " +  path();
		}
	}

}
