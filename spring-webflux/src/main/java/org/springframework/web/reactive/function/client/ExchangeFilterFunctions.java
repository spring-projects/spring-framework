/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.Assert;

/**
 * Static factory methods providing access to built-in implementations of
 * {@link ExchangeFilterFunction} for basic authentication, error handling, etc.
 *
 * @author Rob Winch
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class ExchangeFilterFunctions {

	/**
	 * Consume up to the specified number of bytes from the response body and
	 * cancel if any more data arrives.
	 * <p>Internally delegates to {@link DataBufferUtils#takeUntilByteCount}.
	 * @param maxByteCount the limit as number of bytes
	 * @return the filter to limit the response size with
	 * @since 5.1
	 */
	public static ExchangeFilterFunction limitResponseSize(long maxByteCount) {
		return (request, next) ->
				next.exchange(request).map(response ->
						response.mutate()
								.body(body -> DataBufferUtils.takeUntilByteCount(body, maxByteCount))
								.build());
	}

	/**
	 * Return a filter that generates an error signal when the given
	 * {@link HttpStatusCode} predicate matches.
	 * @param statusPredicate the predicate to check the HTTP status with
	 * @param exceptionFunction the function to create the exception
	 * @return the filter to generate an error signal
	 */
	public static ExchangeFilterFunction statusError(Predicate<HttpStatusCode> statusPredicate,
			Function<ClientResponse, ? extends Throwable> exceptionFunction) {

		Assert.notNull(statusPredicate, "Predicate must not be null");
		Assert.notNull(exceptionFunction, "Function must not be null");

		return ExchangeFilterFunction.ofResponseProcessor(
				response -> (statusPredicate.test(response.statusCode()) ?
						Mono.error(exceptionFunction.apply(response)) : Mono.just(response)));
	}

	/**
	 * Return a filter that applies HTTP Basic Authentication to the request
	 * headers via {@link HttpHeaders#setBasicAuth(String)} and
	 * {@link HttpHeaders#encodeBasicAuth(String, String, Charset)}.
	 * @param username the username
	 * @param password the password
	 * @return the filter to add authentication headers with
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 * @see HttpHeaders#setBasicAuth(String)
	 */
	public static ExchangeFilterFunction basicAuthentication(String username, String password) {
		String encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, null);
		return (request, next) ->
				next.exchange(ClientRequest.from(request)
						.headers(headers -> headers.setBasicAuth(encodedCredentials))
						.build());
	}

}
