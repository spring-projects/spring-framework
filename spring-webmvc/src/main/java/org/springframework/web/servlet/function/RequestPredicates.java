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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Implementations of {@link RequestPredicate} that implement various useful
 * request matching operations, such as matching based on path, HTTP method, etc.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.2
 */
public abstract class RequestPredicates {

	private static final Log logger = LogFactory.getLog(RequestPredicates.class);


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
		Assert.notNull(httpMethod, "HttpMethod must not be null");
		return new SingleHttpMethodPredicate(httpMethod);
	}

	/**
	 * Return a {@code RequestPredicate} that matches if the request's
	 * HTTP method is equal to one the of the given methods.
	 * @param httpMethods the HTTP methods to match against
	 * @return a predicate that tests against the given HTTP methods
	 */
	public static RequestPredicate methods(HttpMethod... httpMethods) {
		Assert.notEmpty(httpMethods, "HttpMethods must not be empty");
		if (httpMethods.length == 1) {
			return new SingleHttpMethodPredicate(httpMethods[0]);
		}
		else {
			return new MultipleHttpMethodsPredicate(httpMethods);
		}
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request path
	 * against the given path pattern.
	 * @param pattern the pattern to match to
	 * @return a predicate that tests against the given path pattern
	 * @see org.springframework.web.util.pattern.PathPattern
	 */
	public static RequestPredicate path(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null");
		PathPatternParser parser = PathPatternParser.defaultInstance;
		pattern = parser.initFullPathPattern(pattern);
		return pathPredicates(parser).apply(pattern);
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
		if (mediaTypes.length == 1) {
			return new SingleContentTypePredicate(mediaTypes[0]);
		}
		else {
			return new MultipleContentTypesPredicate(mediaTypes);
		}
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
		if (mediaTypes.length == 1) {
			return new SingleAcceptPredicate(mediaTypes[0]);
		}
		else {
			return new MultipleAcceptsPredicate(mediaTypes);
		}
	}

	/**
	 * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code GET}
	 * and the given {@code pattern} matches against the request path.
	 * @param pattern the path pattern to match against
	 * @return a predicate that matches if the request method is GET and if the given pattern
	 * matches against the request path
	 * @see org.springframework.web.util.pattern.PathPattern
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
	 * @see org.springframework.web.util.pattern.PathPattern
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
	 * @see org.springframework.web.util.pattern.PathPattern
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
	 * @see org.springframework.web.util.pattern.PathPattern
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
	 * @see org.springframework.web.util.pattern.PathPattern
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
	 * @see org.springframework.web.util.pattern.PathPattern
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
	 * @see org.springframework.web.util.pattern.PathPattern
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
	 * Return a {@code RequestPredicate} that matches if the request's parameter of the given name
	 * has the given value.
	 * @param name the name of the parameter to test against
	 * @param value the value of the parameter to test against
	 * @return a predicate that matches if the parameter has the given value
	 * @see ServerRequest#param(String)
	 */
	public static RequestPredicate param(String name, String value) {
		return new ParamPredicate(name, value);
	}

	/**
	 * Return a {@code RequestPredicate} that tests the request's parameter of the given name
	 * against the given predicate.
	 * @param name the name of the parameter to test against
	 * @param predicate the predicate to test against the parameter value
	 * @return a predicate that matches the given predicate against the parameter of the given name
	 * @see ServerRequest#param(String)
	 */
	public static RequestPredicate param(String name, Predicate<String> predicate) {
		return new ParamPredicate(name, predicate);
	}


	private static void traceMatch(String prefix, Object desired, @Nullable Object actual, boolean match) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s \"%s\" %s against value \"%s\"",
					prefix, desired, match ? "matches" : "does not match", actual));
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
		 * Receive notification of a path predicate.
		 * @param pattern the path pattern that makes up the predicate
		 * @see RequestPredicates#path(String)
		 * @see org.springframework.web.util.pattern.PathPattern
		 */
		void path(String pattern);

		/**
		 * Receive notification of a path extension predicate.
		 * @param extension the path extension that makes up the predicate
		 * @see RequestPredicates#pathExtension(String)
		 */
		void pathExtension(String extension);

		/**
		 * Receive notification of an HTTP header predicate.
		 * @param name the name of the HTTP header to check
		 * @param value the desired value of the HTTP header
		 * @see RequestPredicates#headers(Predicate)
		 * @see RequestPredicates#contentType(MediaType...)
		 * @see RequestPredicates#accept(MediaType...)
		 */
		void header(String name, String value);

		/**
		 * Receive notification of a parameter predicate.
		 * @param name the name of the parameter
		 * @param value the desired value of the parameter
		 * @see RequestPredicates#param(String, String)
		 */
		void param(String name, String value);

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


	/**
	 * Extension of {@code RequestPredicate} that can modify the {@code ServerRequest}.
	 */
	private abstract static class RequestModifyingPredicate implements RequestPredicate {


		public static RequestModifyingPredicate of(RequestPredicate requestPredicate) {
			if (requestPredicate instanceof RequestModifyingPredicate modifyingPredicate) {
				return modifyingPredicate;
			}
			else {
				return new RequestModifyingPredicate() {
					@Override
					protected Result testInternal(ServerRequest request) {
						return Result.of(requestPredicate.test(request));
					}
				};
			}
		}


		@Override
		public final boolean test(ServerRequest request) {
			Result result = testInternal(request);
			boolean value = result.value();
			if (value) {
				result.modifyAttributes(request.attributes());
			}
			return value;
		}

		protected abstract Result testInternal(ServerRequest request);


		protected static final class Result {

			private static final Result TRUE = new Result(true, null);

			private static final Result FALSE = new Result(false, null);


			private final boolean value;

			@Nullable
			private final Consumer<Map<String, Object>> modifyAttributes;


			private Result(boolean value, @Nullable Consumer<Map<String, Object>> modifyAttributes) {
				this.value = value;
				this.modifyAttributes = modifyAttributes;
			}


			public static Result of(boolean value) {
				return of(value, null);
			}

			public static Result of(boolean value, @Nullable Consumer<Map<String, Object>> modifyAttributes) {
				if (modifyAttributes == null) {
					return value ? TRUE : FALSE;
				}
				else {
					return new Result(value, modifyAttributes);
				}
			}


			public boolean value() {
				return this.value;
			}

			public void modifyAttributes(Map<String, Object> attributes) {
				if (this.modifyAttributes != null) {
					this.modifyAttributes.accept(attributes);
				}
			}

			public boolean modifiesAttributes() {
				return this.modifyAttributes != null;
			}
		}

	}


	private static class SingleHttpMethodPredicate implements RequestPredicate {

		private final HttpMethod httpMethod;

		public SingleHttpMethodPredicate(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public boolean test(ServerRequest request) {
			HttpMethod method = method(request);
			boolean match = this.httpMethod.equals(method);
			traceMatch("Method", this.httpMethod, method, match);
			return match;
		}

		static HttpMethod method(ServerRequest request) {
			if (CorsUtils.isPreFlightRequest(request.servletRequest())) {
				String accessControlRequestMethod =
						request.headers().firstHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
				if (accessControlRequestMethod != null) {
					return HttpMethod.valueOf(accessControlRequestMethod);
				}
			}
			return request.method();
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.method(Set.of(this.httpMethod));
		}

		@Override
		public String toString() {
			return this.httpMethod.toString();
		}
	}


	private static class MultipleHttpMethodsPredicate implements RequestPredicate {

		private final Set<HttpMethod> httpMethods;

		public MultipleHttpMethodsPredicate(HttpMethod[] httpMethods) {
			this.httpMethods = new LinkedHashSet<>(Arrays.asList(httpMethods));
		}

		@Override
		public boolean test(ServerRequest request) {
			HttpMethod method = SingleHttpMethodPredicate.method(request);
			boolean match = this.httpMethods.contains(method);
			traceMatch("Method", this.httpMethods, method, match);
			return match;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.method(Collections.unmodifiableSet(this.httpMethods));
		}

		@Override
		public String toString() {
			return this.httpMethods.toString();
		}
	}


	private static class PathPatternPredicate extends RequestModifyingPredicate
			implements ChangePathPatternParserVisitor.Target {

		private PathPattern pattern;


		public PathPatternPredicate(PathPattern pattern) {
			Assert.notNull(pattern, "'pattern' must not be null");
			this.pattern = pattern;
		}


		@Override
		protected Result testInternal(ServerRequest request) {
			PathContainer pathContainer = request.requestPath().pathWithinApplication();
			PathPattern.PathMatchInfo info = this.pattern.matchAndExtract(pathContainer);
			traceMatch("Pattern", this.pattern.getPatternString(), request.path(), info != null);
			if (info != null) {
				return Result.of(true, attributes -> modifyAttributes(attributes, request, info.getUriVariables()));
			}
			else {
				return Result.of(false);
			}
		}

		private void modifyAttributes(Map<String, Object> attributes, ServerRequest request,
				Map<String, String> variables) {

			Map<String, String> pathVariables = CollectionUtils.compositeMap(request.pathVariables(), variables);

			attributes.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
					Collections.unmodifiableMap(pathVariables));

			PathPattern pattern = mergePatterns(
					(PathPattern) attributes.get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					this.pattern);

			attributes.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return Optional.ofNullable(this.pattern.matchStartOfPath(request.requestPath().pathWithinApplication()))
					.map(info -> new NestedPathPatternServerRequestWrapper(request, info, this.pattern));
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.path(this.pattern.getPatternString());
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			String patternString = this.pattern.getPatternString();
			this.pattern = parser.parse(patternString);
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
			if (CorsUtils.isPreFlightRequest(request.servletRequest())) {
				return true;
			}
			else {
				return this.headersPredicate.test(request.headers());
			}
		}

		@Override
		public String toString() {
			return this.headersPredicate.toString();
		}
	}


	private static class SingleContentTypePredicate extends HeadersPredicate {

		private final MediaType mediaType;

		public SingleContentTypePredicate(MediaType mediaType) {
			super(headers -> {
				MediaType contentType = headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
				boolean match = mediaType.includes(contentType);
				traceMatch("Content-Type", mediaType, contentType, match);
				return match;
			});
			this.mediaType = mediaType;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.CONTENT_TYPE, this.mediaType.toString());
		}

		@Override
		public String toString() {
			return "Content-Type: " + this.mediaType;
		}
	}


	private static class MultipleContentTypesPredicate extends HeadersPredicate {

		private final MediaType[] mediaTypes;

		public MultipleContentTypesPredicate(MediaType[] mediaTypes) {
			super(headers -> {
				MediaType contentType = headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
				boolean match = false;
				for (MediaType mediaType : mediaTypes) {
					if (mediaType.includes(contentType)) {
						match = true;
						break;
					}
				}
				traceMatch("Content-Type", mediaTypes, contentType, match);
				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.CONTENT_TYPE, Arrays.toString(this.mediaTypes));
		}

		@Override
		public String toString() {
			return "Content-Type: " + Arrays.toString(this.mediaTypes);
		}
	}


	private static class SingleAcceptPredicate extends HeadersPredicate {

		private final MediaType mediaType;

		public SingleAcceptPredicate(MediaType mediaType) {
			super(headers -> {
				List<MediaType> acceptedMediaTypes = acceptedMediaTypes(headers);
				boolean match = false;
				for (MediaType acceptedMediaType : acceptedMediaTypes) {
					if (acceptedMediaType.isCompatibleWith(mediaType)) {
						match = true;
						break;
					}
				}
				traceMatch("Accept", mediaType, acceptedMediaTypes, match);
				return match;
			});
			this.mediaType = mediaType;
		}

		static List<MediaType> acceptedMediaTypes(ServerRequest.Headers headers) {
			List<MediaType> acceptedMediaTypes = headers.accept();
			if (acceptedMediaTypes.isEmpty()) {
				acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
			}
			else {
				MimeTypeUtils.sortBySpecificity(acceptedMediaTypes);
			}
			return acceptedMediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.ACCEPT, this.mediaType.toString());
		}

		@Override
		public String toString() {
			return "Accept: " + this.mediaType;
		}
	}


	private static class MultipleAcceptsPredicate extends HeadersPredicate {

		private final MediaType[] mediaTypes;

		public MultipleAcceptsPredicate(MediaType[] mediaTypes) {
			super(headers -> {
				List<MediaType> acceptedMediaTypes = SingleAcceptPredicate.acceptedMediaTypes(headers);
				boolean match = false;
				outer:
				for (MediaType acceptedMediaType : acceptedMediaTypes) {
					for (MediaType mediaType : mediaTypes) {
						if (acceptedMediaType.isCompatibleWith(mediaType)) {
							match = true;
							break outer;
						}
					}
				}
				traceMatch("Accept", mediaTypes, acceptedMediaTypes, match);
				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.ACCEPT, Arrays.toString(this.mediaTypes));
		}

		@Override
		public String toString() {
			return "Accept: " + Arrays.toString(this.mediaTypes);
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
			return (pathExtension != null && this.extensionPredicate.test(pathExtension));
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


	private static class ParamPredicate implements RequestPredicate {

		private final String name;

		private final Predicate<String> valuePredicate;

		@Nullable
		private final String value;

		public ParamPredicate(String name, Predicate<String> valuePredicate) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(valuePredicate, "Predicate must not be null");
			this.name = name;
			this.valuePredicate = valuePredicate;
			this.value = null;
		}

		public ParamPredicate(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(value, "Value must not be null");
			this.name = name;
			this.valuePredicate = value::equals;
			this.value = value;
		}

		@Override
		public boolean test(ServerRequest request) {
			Optional<String> s = request.param(this.name);
			return s.filter(this.valuePredicate).isPresent();
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.param(this.name,
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
	static class AndRequestPredicate extends RequestModifyingPredicate
			implements ChangePathPatternParserVisitor.Target {

		private final RequestPredicate left;

		private final RequestModifyingPredicate leftModifying;

		private final RequestPredicate right;

		private final RequestModifyingPredicate rightModifying;


		public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.leftModifying = of(left);
			this.right = right;
			this.rightModifying = of(right);
		}


		@Override
		protected Result testInternal(ServerRequest request) {
			Result leftResult = this.leftModifying.testInternal(request);
			if (!leftResult.value()) {
				return leftResult;
			}
			// ensure that attributes (and uri variables) set in left and available in right
			ServerRequest rightRequest;
			if (leftResult.modifiesAttributes()) {
				Map<String, Object> leftAttributes = new LinkedHashMap<>(2);
				leftResult.modifyAttributes(leftAttributes);
				rightRequest = new ExtendedAttributesServerRequestWrapper(request, leftAttributes);
			}
			else {
				rightRequest = request;
			}
			Result rightResult = this.rightModifying.testInternal(rightRequest);
			if (!rightResult.value()) {
				return rightResult;
			}
			return Result.of(true, attributes -> {
				leftResult.modifyAttributes(attributes);
				rightResult.modifyAttributes(attributes);
			});
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
		public void changeParser(PathPatternParser parser) {
			if (this.left instanceof ChangePathPatternParserVisitor.Target target) {
				target.changeParser(parser);
			}
			if (this.right instanceof ChangePathPatternParserVisitor.Target target) {
				target.changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s && %s)", this.left, this.right);
		}
	}


	/**
	 * {@link RequestPredicate} that negates a delegate predicate.
	 */
	static class NegateRequestPredicate extends RequestModifyingPredicate
			implements ChangePathPatternParserVisitor.Target {

		private final RequestPredicate delegate;

		private final RequestModifyingPredicate delegateModifying;


		public NegateRequestPredicate(RequestPredicate delegate) {
			Assert.notNull(delegate, "Delegate must not be null");
			this.delegate = delegate;
			this.delegateModifying = of(delegate);
		}


		@Override
		protected Result testInternal(ServerRequest request) {
			Result result = this.delegateModifying.testInternal(request);
			return Result.of(!result.value(), result::modifyAttributes);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.startNegate();
			this.delegate.accept(visitor);
			visitor.endNegate();
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			if (this.delegate instanceof ChangePathPatternParserVisitor.Target target) {
				target.changeParser(parser);
			}
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
	static class OrRequestPredicate extends RequestModifyingPredicate
			implements ChangePathPatternParserVisitor.Target {

		private final RequestPredicate left;

		private final RequestModifyingPredicate leftModifying;

		private final RequestPredicate right;

		private final RequestModifyingPredicate rightModifying;


		public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.leftModifying = of(left);
			this.right = right;
			this.rightModifying = of(right);
		}

		@Override
		protected Result testInternal(ServerRequest request) {
			Result leftResult = this.leftModifying.testInternal(request);
			if (leftResult.value()) {
				return leftResult;
			}
			else {
				return this.rightModifying.testInternal(request);
			}
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
		public void changeParser(PathPatternParser parser) {
			if (this.left instanceof ChangePathPatternParserVisitor.Target target) {
				target.changeParser(parser);
			}
			if (this.right instanceof ChangePathPatternParserVisitor.Target target) {
				target.changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s || %s)", this.left, this.right);
		}
	}



	private abstract static class DelegatingServerRequest implements ServerRequest {

		private final ServerRequest delegate;


		protected DelegatingServerRequest(ServerRequest delegate) {
			Assert.notNull(delegate, "Delegate must not be null");
			this.delegate = delegate;
		}

		@Override
		public HttpMethod method() {
			return this.delegate.method();
		}

		@Override
		@Deprecated
		public String methodName() {
			return this.delegate.methodName();
		}

		@Override
		public URI uri() {
			return this.delegate.uri();
		}

		@Override
		public UriBuilder uriBuilder() {
			return this.delegate.uriBuilder();
		}

		@Override
		public String path() {
			return this.delegate.path();
		}

		@Override
		@Deprecated
		public PathContainer pathContainer() {
			return this.delegate.pathContainer();
		}

		@Override
		public RequestPath requestPath() {
			return this.delegate.requestPath();
		}

		@Override
		public Headers headers() {
			return this.delegate.headers();
		}

		@Override
		public MultiValueMap<String, Cookie> cookies() {
			return this.delegate.cookies();
		}

		@Override
		public Optional<InetSocketAddress> remoteAddress() {
			return this.delegate.remoteAddress();
		}

		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return this.delegate.messageConverters();
		}

		@Override
		public <T> T body(Class<T> bodyType) throws ServletException, IOException {
			return this.delegate.body(bodyType);
		}

		@Override
		public <T> T body(ParameterizedTypeReference<T> bodyType) throws ServletException, IOException {
			return this.delegate.body(bodyType);
		}

		@Override
		public <T> T bind(Class<T> bindType) throws BindException {
			return this.delegate.bind(bindType);
		}

		@Override
		public <T> T bind(Class<T> bindType, Consumer<WebDataBinder> dataBinderCustomizer) throws BindException {
			return this.delegate.bind(bindType, dataBinderCustomizer);
		}

		@Override
		public Optional<Object> attribute(String name) {
			return this.delegate.attribute(name);
		}

		@Override
		public Map<String, Object> attributes() {
			return this.delegate.attributes();
		}

		@Override
		public Optional<String> param(String name) {
			return this.delegate.param(name);
		}

		@Override
		public MultiValueMap<String, String> params() {
			return this.delegate.params();
		}

		@Override
		public MultiValueMap<String, Part> multipartData() throws IOException, ServletException {
			return this.delegate.multipartData();
		}

		@Override
		public String pathVariable(String name) {
			return this.delegate.pathVariable(name);
		}

		@Override
		public Map<String, String> pathVariables() {
			return this.delegate.pathVariables();
		}

		@Override
		public HttpSession session() {
			return this.delegate.session();
		}

		@Override
		public Optional<Principal> principal() {
			return this.delegate.principal();
		}

		@Override
		public HttpServletRequest servletRequest() {
			return this.delegate.servletRequest();
		}

		@Override
		public Optional<ServerResponse> checkNotModified(Instant lastModified) {
			return this.delegate.checkNotModified(lastModified);
		}

		@Override
		public Optional<ServerResponse> checkNotModified(String etag) {
			return this.delegate.checkNotModified(etag);
		}

		@Override
		public Optional<ServerResponse> checkNotModified(Instant lastModified, String etag) {
			return this.delegate.checkNotModified(lastModified, etag);
		}

		@Override
		public String toString() {
			return this.delegate.toString();
		}
	}


	private static class ExtendedAttributesServerRequestWrapper extends DelegatingServerRequest {

		private final Map<String, Object> attributes;


		public ExtendedAttributesServerRequestWrapper(ServerRequest delegate, Map<String, Object> newAttributes) {
			super(delegate);
			Assert.notNull(newAttributes, "NewAttributes must not be null");
			Map<String, Object> oldAttributes = delegate.attributes();
			this.attributes = CollectionUtils.compositeMap(newAttributes, oldAttributes, newAttributes::put,
					newAttributes::putAll);
		}

		@Override
		public Optional<Object> attribute(String name) {
			return Optional.ofNullable(this.attributes.get(name));
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public String pathVariable(String name) {
			Map<String, String> pathVariables = pathVariables();
			if (pathVariables.containsKey(name)) {
				return pathVariables.get(name);
			}
			else {
				throw new IllegalArgumentException("No path variable with name \"" + name + "\" available");
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public Map<String, String> pathVariables() {
			return (Map<String, String>) this.attributes.getOrDefault(
					RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
		}
	}


	private static class NestedPathPatternServerRequestWrapper extends ExtendedAttributesServerRequestWrapper {

		private final RequestPath requestPath;


		public NestedPathPatternServerRequestWrapper(ServerRequest request,
				PathPattern.PathRemainingMatchInfo info, PathPattern pattern) {
			super(request, mergeAttributes(request, info.getUriVariables(), pattern));
			this.requestPath = requestPath(request.requestPath(), info);
		}

		private static Map<String, Object> mergeAttributes(ServerRequest request, Map<String, String> newPathVariables,
				PathPattern newPathPattern) {


			Map<String, String> oldPathVariables = request.pathVariables();
			Map<String, String> pathVariables;
			if (oldPathVariables.isEmpty()) {
				pathVariables = newPathVariables;
			}
			else {
				pathVariables = CollectionUtils.compositeMap(oldPathVariables, newPathVariables);
			}

			PathPattern oldPathPattern = (PathPattern) request.attribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE)
					.orElse(null);
			PathPattern pathPattern = mergePatterns(oldPathPattern, newPathPattern);

			Map<String, Object> result = CollectionUtils.newLinkedHashMap(2);
			result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);
			result.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pathPattern);
			return result;
		}

		private static RequestPath requestPath(RequestPath original, PathPattern.PathRemainingMatchInfo info) {
			StringBuilder contextPath = new StringBuilder(original.contextPath().value());
			contextPath.append(info.getPathMatched().value());
			int length = contextPath.length();
			if (length > 0 && contextPath.charAt(length - 1) == '/') {
				contextPath.setLength(length - 1);
			}
			return original.modifyContextPath(contextPath.toString());
		}


		@Override
		public RequestPath requestPath() {
			return this.requestPath;
		}

		@Override
		public String path() {
			return this.requestPath.pathWithinApplication().value();
		}

		@Override
		@Deprecated
		public PathContainer pathContainer() {
			return this.requestPath;
		}
	}
}
