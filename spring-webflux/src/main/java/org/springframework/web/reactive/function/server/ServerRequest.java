/*
 * Copyright 2002-2018 the original author or authors.
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
import org.springframework.http.codec.multipart.Part;
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
 *
 * <p>Access to headers and body is offered by {@link Headers} and
 * {@link #body(BodyExtractor)}, respectively.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerRequest {

	/**
	 * Get the HTTP method.
	 * @return the HTTP method as an HttpMethod enum value, or {@code null}
	 * if not resolvable (e.g. in case of a non-standard HTTP method)
	 */
	@Nullable
	default HttpMethod method() {
		return HttpMethod.resolve(methodName());
	}

	/**
	 * Get the name of the HTTP method.
	 * @return the HTTP method as a String
	 */
	String methodName();

	/**
	 * Get the request URI.
	 */
	URI uri();

	/**
	 * Get a {@code UriBuilderComponents}  from the URI associated with this
	 * {@code ServerRequest}, while also overlaying with values from the headers
	 * "Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 * @return a URI builder
	 */
	UriBuilder uriBuilder();

	/**
	 * Get the request path.
	 */
	default String path() {
		return uri().getRawPath();
	}

	/**
	 * Get the request path as a {@code PathContainer}.
	 */
	default PathContainer pathContainer() {
		return PathContainer.parsePath(path());
	}

	/**
	 * Get the headers of this request.
	 */
	Headers headers();

	/**
	 * Get the cookies of this request.
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
	 * Get the request attribute value if present.
	 * @param name the attribute name
	 * @return the attribute value
	 */
	default Optional<Object> attribute(String name) {
		return Optional.ofNullable(attributes().get(name));
	}

	/**
	 * Get a mutable map of request attributes.
	 * @return the request attributes
	 */
	Map<String, Object> attributes();

	/**
	 * Get the first query parameter with the given name, if present.
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
	 * Get all query parameters for this request.
	 */
	MultiValueMap<String, String> queryParams();

	/**
	 * Get the path variable with the given name, if present.
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
	 * Get all path variables for this request.
	 */
	Map<String, String> pathVariables();

	/**
	 * Get the web session for this request.
	 * <p>Always guaranteed to return an instance either matching the session id
	 * requested by the client, or with a new session id either because the client
	 * did not specify one or because the underlying session had expired.
	 * <p>Use of this method does not automatically create a session.
	 */
	Mono<WebSession> session();

	/**
	 * Get the authenticated user for the request, if any.
	 */
	Mono<? extends Principal> principal();

	/**
	 * Get the form data from the body of the request if the Content-Type is
	 * {@code "application/x-www-form-urlencoded"} or an empty map otherwise.
	 * <p><strong>Note:</strong> calling this method causes the request body to
	 * be read and parsed in full, and the resulting {@code MultiValueMap} is
	 * cached so that this method is safe to call more than once.
	 */
	Mono<MultiValueMap<String, String>> formData();

	/**
	 * Get the parts of a multipart request if the Content-Type is
	 * {@code "multipart/form-data"} or an empty map otherwise.
	 * <p><strong>Note:</strong> calling this method causes the request body to
	 * be read and parsed in full, and the resulting {@code MultiValueMap} is
	 * cached so that this method is safe to call more than once.
	 */
	Mono<MultiValueMap<String, Part>> multipartData();


	// Static builder methods

	/**
	 * Create a new {@code ServerRequest} based on the given {@code ServerWebExchange} and
	 * message readers.
	 * @param exchange the exchange
	 * @param messageReaders the message readers
	 * @return the created {@code ServerRequest}
	 */
	static ServerRequest create(ServerWebExchange exchange, List<HttpMessageReader<?>> messageReaders) {
		return new DefaultServerRequest(exchange, messageReaders);
	}


	/**
	 * Represents the headers of the HTTP request.
	 * @see ServerRequest#headers()
	 */
	interface Headers {

		/**
		 * Get the list of acceptable media types, as specified by the {@code Accept}
		 * header.
		 * <p>Returns an empty list if the acceptable media types are unspecified.
		 */
		List<MediaType> accept();

		/**
		 * Get the list of acceptable charsets, as specified by the
		 * {@code Accept-Charset} header.
		 */
		List<Charset> acceptCharset();

		/**
		 * Get the list of acceptable languages, as specified by the
		 * {@code Accept-Language} header.
		 */
		List<Locale.LanguageRange> acceptLanguage();

		/**
		 * Get the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 */
		OptionalLong contentLength();

		/**
		 * Get the media type of the body, as specified by the
		 * {@code Content-Type} header.
		 */
		Optional<MediaType> contentType();

		/**
		 * Get the value of the {@code Host} header, if available.
		 * <p>If the header value does not contain a port, the
		 * {@linkplain InetSocketAddress#getPort() port} in the returned address will
		 * be {@code 0}.
		 */
		@Nullable
		InetSocketAddress host();

		/**
		 * Get the value of the {@code Range} header.
		 * <p>Returns an empty list when the range is unknown.
		 */
		List<HttpRange> range();

		/**
		 * Get the header value(s), if any, for the header of the given name.
		 * <p>Returns an empty list if no header values are found.
		 * @param headerName the header name
		 */
		List<String> header(String headerName);

		/**
		 * Get the headers as an instance of {@link HttpHeaders}.
		 */
		HttpHeaders asHttpHeaders();
	}

}
