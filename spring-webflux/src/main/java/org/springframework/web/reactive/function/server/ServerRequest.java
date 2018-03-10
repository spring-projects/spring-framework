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

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;

/**
 * Represents a server-side HTTP request, as handled by a {@code HandlerFunction}.
 * Access to headers and body is offered by {@link Headers} and
 * {@link #body(BodyExtractor)}, respectively.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerRequest {

	/**
	 * Return the HTTP method.
	 * @return the HTTP method as an HttpMethod enum value, or {@code null}
	 * if not resolvable (e.g. in case of a non-standard HTTP method)
	 */
	@Nullable
	default HttpMethod method() {
		return HttpMethod.resolve(methodName());
	}

	/**
	 * Return the name of the HTTP method.
	 * @return the HTTP method as a String
	 */
	String methodName();

	/**
	 * Return the request URI.
	 */
	URI uri();

	/**
	 * Return a {@code UriBuilderComponents}  from the URI associated with this
	 * {@code ServerRequest}, while also overlaying with values from the headers
	 * "Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 * @return a URI builder
	 */
	UriBuilder uriBuilder();

	/**
	 * Return the request path.
	 */
	default String path() {
		return uri().getRawPath();
	}

	/**
	 * Return the request path as {@code PathContainer}.
	 */
	default PathContainer pathContainer() {
		return PathContainer.parsePath(path());
	}

	/**
	 * Return the headers of this request.
	 */
	Headers headers();

	/**
	 * Return the cookies of this request.
	 */
	MultiValueMap<String, HttpCookie> cookies();

	/**
	 * Extract the body with the given {@code BodyExtractor}.
	 * @param extractor the {@code BodyExtractor} that reads from the request
	 * @param <T> the type of the body returned
	 * @return the extracted body
	 * @see #body(BodyExtractor, Map)
	 */
	<T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor);

	/**
	 * Extract the body with the given {@code BodyExtractor} and hints.
	 * @param extractor the {@code BodyExtractor} that reads from the request
	 * @param hints the map of hints like {@link Jackson2CodecSupport#JSON_VIEW_HINT}
	 * to use to customize body extraction
	 * @param <T> the type of the body returned
	 * @return the extracted body
	 */
	<T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints);

	/**
	 * Extract the body to a {@code Mono}.
	 * @param elementClass the class of element in the {@code Mono}
	 * @param <T> the element type
	 * @return the body as a mono
	 */
	<T> Mono<T> bodyToMono(Class<? extends T> elementClass);

	/**
	 * Extract the body to a {@code Mono}.
	 * @param typeReference a type reference describing the expected response request type
	 * @param <T> the element type
	 * @return a mono containing the body of the given type {@code T}
	 */
	<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference);

	/**
	 * Extract the body to a {@code Flux}.
	 * @param elementClass the class of element in the {@code Flux}
	 * @param <T> the element type
	 * @return the body as a flux
	 */
	<T> Flux<T> bodyToFlux(Class<? extends T> elementClass);

	/**
	 * Extract the body to a {@code Flux}.
	 * @param typeReference a type reference describing the expected request body type
	 * @param <T> the element type
	 * @return a flux containing the body of the given type {@code T}
	 */
	<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference);

	/**
	 * Return the request attribute value if present.
	 * @param name the attribute name
	 * @return the attribute value
	 */
	default Optional<Object> attribute(String name) {
		Map<String, Object> attributes = attributes();
		if (attributes.containsKey(name)) {
			return Optional.of(attributes.get(name));
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Return a mutable map of request attributes.
	 * @return the request attributes
	 */
	Map<String, Object> attributes();

	/**
	 * Return the first query parameter with the given name, if present.
	 * @param name the parameter name
	 * @return the parameter value
	 */
	default Optional<String> queryParam(String name) {
		List<String> queryParamValues = queryParams().get(name);
		if (CollectionUtils.isEmpty(queryParamValues)) {
			return Optional.empty();
		}
		else {
			String value = queryParamValues.get(0);
			if (value == null) {
				value = "";
			}
			return Optional.of(value);
		}
	}

	/**
	 * Return all query parameters for this request.
	 */
	MultiValueMap<String, String> queryParams();

	/**
	 * Return the path variable with the given name, if present.
	 * @param name the variable name
	 * @return the variable value
	 * @throws IllegalArgumentException if there is no path variable with the given name
	 */
	default String pathVariable(String name) {
		Map<String, String> pathVariables = pathVariables();
		if (pathVariables.containsKey(name)) {
			return pathVariables().get(name);
		}
		else {
			throw new IllegalArgumentException("No path variable with name \"" + name + "\" available");
		}
	}

	/**
	 * Return all path variables for this request.
	 */
	Map<String, String> pathVariables();

	/**
	 * Return the web session for this request. Always guaranteed to
	 * return an instance either matching to the session id requested by the
	 * client, or with a new session id either because the client did not
	 * specify one or because the underlying session had expired. Use of this
	 * method does not automatically create a session.
	 */
	Mono<WebSession> session();

	/**
	 * Return the authenticated user for the request, if any.
	 */
	Mono<? extends Principal> principal();


	/**
	 * Create a new {@code ServerRequest} based on the given {@code ServerWebExchange} and
	 * message readers.
	 * @param exchange the exchange
	 * @param messageReaders the message readers
	 * @return the created {@code ServerRequest}
	 */
	static ServerRequest create(ServerWebExchange exchange,
			List<HttpMessageReader<?>> messageReaders) {

		return new DefaultServerRequest(exchange, messageReaders);
	}

	/**
	 * Represents the headers of the HTTP request.
	 * @see ServerRequest#headers()
	 */
	interface Headers {

		/**
		 * Return the list of acceptable {@linkplain MediaType media types},
		 * as specified by the {@code Accept} header.
		 * <p>Returns an empty list when the acceptable media types are unspecified.
		 */
		List<MediaType> accept();

		/**
		 * Return the list of acceptable {@linkplain Charset charsets},
		 * as specified by the {@code Accept-Charset} header.
		 */
		List<Charset> acceptCharset();

		/**
		 * Return the list of acceptable {@linkplain Locale.LanguageRange languages},
		 * as specified by the {@code Accept-Language} header.
		 */
		List<Locale.LanguageRange> acceptLanguage();

		/**
		 * Return the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 */
		OptionalLong contentLength();

		/**
		 * Return the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 */
		Optional<MediaType> contentType();

		/**
		 * Return the value of the required {@code Host} header.
		 * <p>If the header value does not contain a port, the returned
		 * {@linkplain InetSocketAddress#getPort() port} will be {@code 0}.
		 */
		@Nullable
		InetSocketAddress host();

		/**
		 * Return the value of the {@code Range} header.
		 * <p>Returns an empty list when the range is unknown.
		 */
		List<HttpRange> range();

		/**
		 * Return the header value(s), if any, for the header of the given name.
		 * <p>Return an empty list if no header values are found.
		 * @param headerName the header name
		 */
		List<String> header(String headerName);

		/**
		 * Return the headers as a {@link HttpHeaders} instance.
		 */
		HttpHeaders asHttpHeaders();
	}

}
