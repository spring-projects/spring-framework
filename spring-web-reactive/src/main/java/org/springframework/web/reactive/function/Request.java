/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;

/**
 * Represents an HTTP request, as handled by a {@linkplain HandlerFunction handler function}.
 * Access to headers and body is offered by {@link Headers} and {@link Body} respectively.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface Request {

	/**
	 * Return the HTTP method.
	 */
	HttpMethod method();

	/**
	 * Return the request URI.
	 */
	URI uri();

	/**
	 * Return the request path.
	 */
	default String path() {
		return uri().getPath();
	}

	/**
	 * Return the headers of this request.
	 */
	Headers headers();

	/**
	 * Return the body of this request.
	 */
	Body body();

	/**
	 * Return the request attribute value if present.
	 * @param name the attribute name
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	<T> Optional<T> attribute(String name);

	/**
	 * Return a mutable map of attributes for this request.
	 */
	Map<String, Object> attributes();

	/**
	 * Return the first query parameter with the given name, if present.
	 * @param name the parameter name
	 * @return the parameter value
	 */
	default Optional<String> queryParam(String name) {
		List<String> queryParams = this.queryParams(name);
		return !queryParams.isEmpty() ? Optional.of(queryParams.get(0)) : Optional.empty();
	}

	/**
	 * Return all query parameter with the given name. Returns an empty list if no values could
	 * be found.
	 * @param name the parameter name
	 * @return the parameter values
	 */
	List<String> queryParams(String name);

	/**
	 * Return the path variable with the given name, if present.
	 * @param name the variable name
	 * @return the variable value
	 */
	default Optional<String> pathVariable(String name) {
		return Optional.ofNullable(this.pathVariables().get(name));
	}

	/**
	 * Return all path variables.
	 * @return the path variables
	 */
	Map<String, String> pathVariables();

	/**
	 * Represents the headers of the HTTP request.
	 * @see Request#headers()
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
		InetSocketAddress host();

		/**
		 * Return the value of the {@code Range} header.
		 * <p>Returns an empty list when the range is unknown.
		 */
		List<HttpRange> range();

		/**
		 * Return the header value(s), if any, for the header of the given name.
		 * <p>Return an empty list if no header values are found.
		 *
		 * @param headerName the header name
		 */
		List<String> header(String headerName);

		/**
		 * Return the headers as a "live" {@link HttpHeaders} instance.
		 */
		HttpHeaders asHttpHeaders();

	}

	/**
	 * Represents the body of the HTTP request.
	 * @see Request#body()
	 */
	interface Body {

		/**
		 * Return the request body as a stream of {@linkplain DataBuffer data buffers}.
		 * @return the request body byte stream
		 */
		Flux<DataBuffer> stream();

		/**
		 * Converts the body into a multiple-element stream of the given type.
		 * @param aClass the type
		 * @param <T> the type of the element contained in the flux
		 * @return a flux that streams element of the given type
		 */
		<T> Flux<T> convertTo(Class<? extends T> aClass);

		/**
		 * Converts the body into a single-element stream of the given type.
		 * @param aClass the type
		 * @param <T> the type of the element contained in the mono
		 * @return a flux that streams element of the given type
		 */
		<T> Mono<T> convertToMono(Class<? extends T> aClass);

	}
}
