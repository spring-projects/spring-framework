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

package org.springframework.web.reactive.function.client;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.web.reactive.function.BodyExtractor;

/**
 * Represents an HTTP response, as returned by the {@link ExchangeFunction}.
 * Access to headers and body is offered by {@link Headers} and
 * {@link #body(BodyExtractor)}, {@link #bodyToMono(Class)}, {@link #bodyToFlux(Class)}
 * respectively.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientResponse {

	/**
	 * Return the status code of this response.
	 */
	HttpStatus statusCode();

	/**
	 * Return the headers of this response.
	 */
	Headers headers();

	/**
	 * Extract the body with the given {@code BodyExtractor}. Unlike {@link #bodyToMono(Class)} and
	 * {@link #bodyToFlux(Class)}; this method does not check for a 4xx or 5xx  status code before
	 * extracting the body.
	 * @param extractor the {@code BodyExtractor} that reads from the response
	 * @param <T> the type of the body returned
	 * @return the extracted body
	 */
	<T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor);

	/**
	 * Extract the body to a {@code Mono}. If the response has status code 4xx or 5xx, the
	 * {@code Mono} will contain a {@link WebClientException}.
	 * @param elementClass the class of element in the {@code Mono}
	 * @param <T> the element type
	 * @return a mono containing the body, or a {@link WebClientException} if the status code is
	 * 4xx or 5xx
	 */
	<T> Mono<T> bodyToMono(Class<? extends T> elementClass);

	/**
	 * Extract the body to a {@code Flux}. If the response has status code 4xx or 5xx, the
	 * {@code Flux} will contain a {@link WebClientException}.
	 * @param elementClass the class of element in the {@code Flux}
	 * @param <T> the element type
	 * @return a flux containing the body, or a {@link WebClientException} if the status code is
	 * 4xx or 5xx
	 */
	<T> Flux<T> bodyToFlux(Class<? extends T> elementClass);


	/**
	 * Represents the headers of the HTTP response.
	 * @see ClientResponse#headers()
	 */
	interface Headers {

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
		 * Return the header value(s), if any, for the header of the given name.
		 * <p>Return an empty list if no header values are found.
		 *
		 * @param headerName the header name
		 */
		List<String> header(String headerName);

		/**
		 * Return the headers as a {@link HttpHeaders} instance.
		 */
		HttpHeaders asHttpHeaders();

	}
}
