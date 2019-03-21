/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

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
	 * Return a {@code RequestPredicate} that matches if the request's
	 * HTTP method is equal to the given method.
	 * @param httpMethod the HTTP method to match against
	 * @return a predicate that tests against the given HTTP method
	 */
	public static RequestPredicate method(HttpMethod httpMethod) {
		return new HttpMethodPredicate(httpMethod);
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's
	 * HTTP method is equal to one the of the given methods.
	 * @param httpMethods the HTTP methods to match against
	 * @return a predicate that tests against the given HTTP methods
	 * @since 5.1
	 */
	public static RequestPredicate methods(HttpMethod... httpMethods) {
		return new HttpMethodPredicate(httpMethods);
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request path
	 * against the given path pattern.
	 * @param pattern the pattern to match to
	 * @return a predicate that tests against the given path pattern
	 */
	public static RequestPredicate path(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null");
		return pathPredicates(DEFAULT_PATTERN_PARSER).apply(pattern);
	}

	/**
	 * Return a function that creates new path-matching {@code RequestPredicates}
	 * from pattern Strings using the given {@link PathPatternParser}.
	 * <p>This method can be used to specify a non-default, customized
	 * {@code PathPatternParser} when resolving path patterns.
	 * @param patternParser the parser used to parse patterns given to the returned function
	 * @return a function that resolves a pattern String into a path-matching
	 * {@code RequestPredicates} instance
	 */
	public static Function<String, RequestPredicate> pathPredicates(PathPatternParser patternParser) {
		Assert.notNull(patternParser, "PathPatternParser must not be null");
		return pattern -> new PathPatternPredicate(patternParser.parse(pattern));
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request's headers
	 * against the given headers predicate.
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
		return new ContentTypePredicate(mediaTypes);
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
		return new AcceptPredicate(mediaTypes);
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
		return new PathExtensionPredicate(extension);
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's path matches the given
	 * predicate.
	 * @param extensionPredicate the predicate to test against the request path extension
	 * @return a predicate that matches if the given predicate matches against the request's path
	 * file extension
	 */
	public static RequestPredicate pathExtension(Predicate<String> extensionPredicate) {
		return new PathExtensionPredicate(extensionPredicate);
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's query parameter of the given name
	 * has the given value.
	 * @param name the name of the query parameter to test against
	 * @param value the value of the query parameter to test against
	 * @return a predicate that matches if the query parameter has the given value
	 * @since 5.0.7
	 * @see ServerRequest#queryParam(String)
	 */
	public static RequestPredicate queryParam(String name, String value) {
		return new QueryParamPredicate(name, value);
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
		return new QueryParamPredicate(name, predicate);
	}


	private static void traceMatch(String prefix, Object desired, @Nullable Object actual, boolean match) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s \"%s\" %s against value \"%s\"",
					prefix, desired, match ? "matches" : "does not match", actual));
		}
	}

	private static void restoreAttributes(ServerRequest request, Map<String, Object> attributes) {
		request.attributes().clear();
		request.attributes().putAll(attributes);
	}

	private static Map<String, String> mergePathVariables(Map<String, String> oldVariables,
			Map<String, String> newVariables) {

		if (!newVariables.isEmpty()) {
			Map<String, String> mergedVariables = new LinkedHashMap<>(oldVariables);
			mergedVariables.putAll(newVariables);
			return mergedVariables;
		}
		else {
			return oldVariables;
		}
	}

	private static PathPattern mergePatterns(@Nullable PathPattern oldPattern, PathPattern newPattern) {
		if (oldPattern != null) {
			return oldPattern.combine(newPattern);
		}
		else {
			return newPattern;
		}

	}


	/**
	 * Receives notifications from the logical structure of request predicates.
	 */
	public interface Visitor {

		/**
		 * Receive notification of an HTTP method predicate.
		 * @param methods the HTTP methods that make up the predicate
		 * @see RequestPredicates#method(HttpMethod)
		 */
		void method(Set<HttpMethod> methods);

		/**
		 * Receive notification of an path predicate.
		 * @param pattern the path pattern that makes up the predicate
		 * @see RequestPredicates#path(String)
		 */
		void path(String pattern);

		/**
		 * Receive notification of an path extension predicate.
		 * @param extension the path extension that makes up the predicate
		 * @see RequestPredicates#pathExtension(String)
		 */
		void pathExtension(String extension);

		/**
		 * Receive notification of a HTTP header predicate.
		 * @param name the name of the HTTP header to check
		 * @param value the desired value of the HTTP header
		 * @see RequestPredicates#headers(Predicate)
		 * @see RequestPredicates#contentType(MediaType...)
		 * @see RequestPredicates#accept(MediaType...)
		 */
		void header(String name, String value);

		/**
		 * Receive notification of a query parameter predicate.
		 * @param name the name of the query parameter
		 * @param value the desired value of the parameter
		 * @see RequestPredicates#queryParam(String, String)
		 */
		void queryParam(String name, String value);

		/**
		 * Receive first notification of a logical AND predicate.
		 * The first subsequent notification will contain the left-hand side of the AND-predicate;
		 * followed by {@link #and()}, followed by the right-hand side, followed by {@link #endAnd()}.
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void startAnd();

		/**
		 * Receive "middle" notification of a logical AND predicate.
		 * The following notification contains the right-hand side, followed by {@link #endAnd()}.
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void and();

		/**
		 * Receive last notification of a logical AND predicate.
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void endAnd();

		/**
		 * Receive first notification of a logical OR predicate.
		 * The first subsequent notification will contain the left-hand side of the OR-predicate;
		 * the second notification contains the right-hand side, followed by {@link #endOr()}.
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void startOr();

		/**
		 * Receive "middle" notification of a logical OR predicate.
		 * The following notification contains the right-hand side, followed by {@link #endOr()}.
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void or();

		/**
		 * Receive last notification of a logical OR predicate.
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void endOr();

		/**
		 * Receive first notification of a negated predicate.
		 * The first subsequent notification will contain the negated predicated, followed
		 * by {@link #endNegate()}.
		 * @see RequestPredicate#negate()
		 */
		void startNegate();

		/**
		 * Receive last notification of a negated predicate.
		 * @see RequestPredicate#negate()
		 */
		void endNegate();

		/**
		 * Receive first notification of an unknown predicate.
		 */
		void unknown(RequestPredicate predicate);
	}


	private static class HttpMethodPredicate implements RequestPredicate {

		private final Set<HttpMethod> httpMethods;

		public HttpMethodPredicate(HttpMethod httpMethod) {
			Assert.notNull(httpMethod, "HttpMethod must not be null");
			this.httpMethods = EnumSet.of(httpMethod);
		}

		public HttpMethodPredicate(HttpMethod... httpMethods) {
			Assert.notEmpty(httpMethods, "HttpMethods must not be empty");

			this.httpMethods = EnumSet.copyOf(Arrays.asList(httpMethods));
		}

		@Override
		public boolean test(ServerRequest request) {
			boolean match = this.httpMethods.contains(request.method());
			traceMatch("Method", this.httpMethods, request.method(), match);
			return match;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.method(Collections.unmodifiableSet(this.httpMethods));
		}

		@Override
		public String toString() {
			if (this.httpMethods.size() == 1) {
				return this.httpMethods.iterator().next().toString();
			}
			else {
				return this.httpMethods.toString();
			}
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
			PathContainer pathContainer = request.pathContainer();
			PathPattern.PathMatchInfo info = this.pattern.matchAndExtract(pathContainer);
			traceMatch("Pattern", this.pattern.getPatternString(), request.path(), info != null);
			if (info != null) {
				mergeAttributes(request, info.getUriVariables(), this.pattern);
				return true;
			}
			else {
				return false;
			}
		}

		private static void mergeAttributes(ServerRequest request, Map<String, String> variables,
				PathPattern pattern) {
			Map<String, String> pathVariables = mergePathVariables(request.pathVariables(), variables);
			request.attributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
						Collections.unmodifiableMap(pathVariables));

			pattern = mergePatterns(
					(PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					pattern);
			request.attributes().put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return Optional.ofNullable(this.pattern.matchStartOfPath(request.pathContainer()))
					.map(info -> new SubPathServerRequestWrapper(request, info, this.pattern));
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.path(this.pattern.getPatternString());
		}

		@Override
		public String toString() {
			return this.pattern.getPatternString();
		}
	}


	private static class HeadersPredicate implements RequestPredicate {

		private final Predicate<ServerRequest.Headers> headersPredicate;

		public HeadersPredicate(Predicate<ServerRequest.Headers> headersPredicate) {
			Assert.notNull(headersPredicate, "Predicate must not be null");
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

	private static class ContentTypePredicate extends HeadersPredicate {

		private final Set<MediaType> mediaTypes;

		public ContentTypePredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		private ContentTypePredicate(Set<MediaType> mediaTypes) {
			super(headers -> {
				MediaType contentType =
						headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
				boolean match = mediaTypes.stream()
						.anyMatch(mediaType -> mediaType.includes(contentType));
				traceMatch("Content-Type", mediaTypes, contentType, match);
				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.CONTENT_TYPE,
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}

		@Override
		public String toString() {
			return String.format("Content-Type: %s",
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}
	}

	private static class AcceptPredicate extends HeadersPredicate {

		private final Set<MediaType> mediaTypes;

		public AcceptPredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		private AcceptPredicate(Set<MediaType> mediaTypes) {
			super(headers -> {
				List<MediaType> acceptedMediaTypes = acceptedMediaTypes(headers);
				boolean match = acceptedMediaTypes.stream()
						.anyMatch(acceptedMediaType -> mediaTypes.stream()
								.anyMatch(acceptedMediaType::isCompatibleWith));
				traceMatch("Accept", mediaTypes, acceptedMediaTypes, match);
				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		@NonNull
		private static List<MediaType> acceptedMediaTypes(ServerRequest.Headers headers) {
			List<MediaType> acceptedMediaTypes = headers.accept();
			if (acceptedMediaTypes.isEmpty()) {
				acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
			}
			else {
				MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
			}
			return acceptedMediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.ACCEPT,
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}

		@Override
		public String toString() {
			return String.format("Accept: %s",
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}
	}


	private static class PathExtensionPredicate implements RequestPredicate {

		private final Predicate<String> extensionPredicate;

		@Nullable
		private final String extension;

		public PathExtensionPredicate(Predicate<String> extensionPredicate) {
			Assert.notNull(extensionPredicate, "Predicate must not be null");
			this.extensionPredicate = extensionPredicate;
			this.extension = null;
		}

		public PathExtensionPredicate(String extension) {
			Assert.notNull(extension, "Extension must not be null");

			this.extensionPredicate = s -> {
				boolean match = extension.equalsIgnoreCase(s);
				traceMatch("Extension", extension, s, match);
				return match;
			};
			this.extension = extension;
		}

		@Override
		public boolean test(ServerRequest request) {
			String pathExtension = UriUtils.extractFileExtension(request.path());
			return this.extensionPredicate.test(pathExtension);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.pathExtension(
					(this.extension != null) ?
							this.extension :
							this.extensionPredicate.toString());
		}

		@Override
		public String toString() {
			return String.format("*.%s",
					(this.extension != null) ?
							this.extension :
							this.extensionPredicate);
		}

	}


	private static class QueryParamPredicate implements RequestPredicate {

		private final String name;

		private final Predicate<String> valuePredicate;

		@Nullable
		private final String value;

		public QueryParamPredicate(String name, Predicate<String> valuePredicate) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(valuePredicate, "Predicate must not be null");
			this.name = name;
			this.valuePredicate = valuePredicate;
			this.value = null;
		}

		public QueryParamPredicate(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(value, "Value must not be null");
			this.name = name;
			this.valuePredicate = value::equals;
			this.value = value;
		}

		@Override
		public boolean test(ServerRequest request) {
			Optional<String> s = request.queryParam(this.name);
			return s.filter(this.valuePredicate).isPresent();
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.queryParam(this.name,
					(this.value != null) ?
							this.value :
							this.valuePredicate.toString());
		}

		@Override
		public String toString() {
			return String.format("?%s %s", this.name,
					(this.value != null) ?
							this.value :
							this.valuePredicate);
		}
	}


	/**
	 * {@link RequestPredicate} for where both {@code left} and {@code right} predicates
	 * must match.
	 */
	static class AndRequestPredicate implements RequestPredicate {

		private final RequestPredicate left;

		private final RequestPredicate right;

		public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean test(ServerRequest request) {
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			if (this.left.test(request) && this.right.test(request)) {
				return true;
			}
			restoreAttributes(request, oldAttributes);
			return false;
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return this.left.nest(request).flatMap(this.right::nest);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.startAnd();
			this.left.accept(visitor);
			visitor.and();
			this.right.accept(visitor);
			visitor.endAnd();
		}

		@Override
		public String toString() {
			return String.format("(%s && %s)", this.left, this.right);
		}
	}

	/**
	 * {@link RequestPredicate} that negates a delegate predicate.
	 */
	static class NegateRequestPredicate implements RequestPredicate {
		private final RequestPredicate delegate;

		public NegateRequestPredicate(RequestPredicate delegate) {
			Assert.notNull(delegate, "Delegate must not be null");
			this.delegate = delegate;
		}

		@Override
		public boolean test(ServerRequest request) {
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());
			boolean result = !this.delegate.test(request);
			if (!result) {
				restoreAttributes(request, oldAttributes);
			}
			return result;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.startNegate();
			this.delegate.accept(visitor);
			visitor.endNegate();
		}

		@Override
		public String toString() {
			return "!" + this.delegate.toString();
		}
	}

	/**
	 * {@link RequestPredicate} where either {@code left} or {@code right} predicates
	 * may match.
	 */
	static class OrRequestPredicate implements RequestPredicate {

		private final RequestPredicate left;

		private final RequestPredicate right;

		public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean test(ServerRequest request) {
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			if (this.left.test(request)) {
				return true;
			}
			else {
				restoreAttributes(request, oldAttributes);
				if (this.right.test(request)) {
					return true;
				}
			}
			restoreAttributes(request, oldAttributes);
			return false;
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
		public void accept(Visitor visitor) {
			visitor.startOr();
			this.left.accept(visitor);
			visitor.or();
			this.right.accept(visitor);
			visitor.endOr();
		}


		@Override
		public String toString() {
			return String.format("(%s || %s)", this.left, this.right);
		}
	}


	private static class SubPathServerRequestWrapper implements ServerRequest {

		private final ServerRequest request;

		private final PathContainer pathContainer;

		private final Map<String, Object> attributes;

		public SubPathServerRequestWrapper(ServerRequest request,
				PathPattern.PathRemainingMatchInfo info, PathPattern pattern) {
			this.request = request;
			this.pathContainer = new SubPathContainer(info.getPathRemaining());
			this.attributes = mergeAttributes(request, info.getUriVariables(), pattern);
		}

		private static Map<String, Object> mergeAttributes(ServerRequest request,
		Map<String, String> pathVariables, PathPattern pattern) {
			Map<String, Object> result = new ConcurrentHashMap<>(request.attributes());

			result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
					mergePathVariables(request.pathVariables(), pathVariables));

			pattern = mergePatterns(
					(PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					pattern);
			result.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
			return result;
		}

		@Override
		public HttpMethod method() {
			return this.request.method();
		}

		@Override
		public String methodName() {
			return this.request.methodName();
		}

		@Override
		public URI uri() {
			return this.request.uri();
		}

		@Override
		public UriBuilder uriBuilder() {
			return this.request.uriBuilder();
		}

		@Override
		public String path() {
			return this.pathContainer.value();
		}

		@Override
		public PathContainer pathContainer() {
			return this.pathContainer;
		}

		@Override
		public Headers headers() {
			return this.request.headers();
		}

		@Override
		public MultiValueMap<String, HttpCookie> cookies() {
			return this.request.cookies();
		}

		@Override
		public Optional<InetSocketAddress> remoteAddress() {
			return this.request.remoteAddress();
		}

		@Override
		public List<HttpMessageReader<?>> messageReaders() {
			return this.request.messageReaders();
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
		public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
			return this.request.bodyToMono(typeReference);
		}

		@Override
		public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
			return this.request.bodyToFlux(elementClass);
		}

		@Override
		public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
			return this.request.bodyToFlux(typeReference);
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public Optional<String> queryParam(String name) {
			return this.request.queryParam(name);
		}

		@Override
		public MultiValueMap<String, String> queryParams() {
			return this.request.queryParams();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Map<String, String> pathVariables() {
			return (Map<String, String>) this.attributes.getOrDefault(
					RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());

		}

		@Override
		public Mono<WebSession> session() {
			return this.request.session();
		}

		@Override
		public Mono<? extends Principal> principal() {
			return this.request.principal();
		}

		@Override
		public Mono<MultiValueMap<String, String>> formData() {
			return this.request.formData();
		}

		@Override
		public Mono<MultiValueMap<String, Part>> multipartData() {
			return this.request.multipartData();
		}

		@Override
		public ServerWebExchange exchange() {
			return this.request.exchange();
		}

		@Override
		public String toString() {
			return method() + " " +  path();
		}

		private static class SubPathContainer implements PathContainer {

			private static final PathContainer.Separator SEPARATOR = () -> "/";


			private final String value;

			private final List<Element> elements;

			public SubPathContainer(PathContainer original) {
				this.value = prefixWithSlash(original.value());
				this.elements = prependWithSeparator(original.elements());
			}

			private static String prefixWithSlash(String path) {
				if (!path.startsWith("/")) {
					path = "/" + path;
				}
				return path;
			}

			private static List<Element> prependWithSeparator(List<Element> elements) {
				List<Element> result = new ArrayList<>(elements);
				if (result.isEmpty() || !(result.get(0) instanceof Separator)) {
					result.add(0, SEPARATOR);
				}
				return Collections.unmodifiableList(result);
			}


			@Override
			public String value() {
				return this.value;
			}

			@Override
			public List<Element> elements() {
				return this.elements;
			}
		}
	}

}
