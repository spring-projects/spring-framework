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

package org.springframework.web.client.reactive;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Implementations of {@link ExchangeFilterFunction} that provide various useful request filter
 * operations, such as basic authentication, error handling, etc.
 *
 * @author Rob Winch
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class ExchangeFilterFunctions {

	/**
	 * Return a filter that will publish a {@link WebClientException} when the
	 * {@code ClientResponse} has a 4xx status code.
	 * @return the {@code ExchangeFilterFunction} that publishes a {@code WebClientException} when
	 * the response has a client error
	 */
	public static ExchangeFilterFunction clientError() {
		return statusError(HttpStatus::is4xxClientError);
	}

	/**
	 * Return a filter that will publish a {@link WebClientException} if the
	 * {@code ClientResponse} has a 5xx status code.
	 * @return the {@code ExchangeFilterFunction} that publishes a {@code WebClientException} when
	 * the response has a server error
	 */
	public static ExchangeFilterFunction serverError() {
		return statusError(HttpStatus::is5xxServerError);
	}

	/**
	 * Return a filter that will publish a {@link WebClientException} if the
	 * {@code ClientResponse} has a 4xx or 5xx status code.
	 * @return the {@code ExchangeFilterFunction} that publishes a {@code WebClientException} when
	 * the response has a client or server error
	 */
	public static ExchangeFilterFunction clientOrServerError() {
		return clientError().andThen(serverError());
	}

	private static ExchangeFilterFunction statusError(Predicate<HttpStatus> predicate) {
		Function<ClientResponse, Optional<? extends Throwable>> mapper =
				clientResponse -> {
					HttpStatus status = clientResponse.statusCode();
					if (predicate.test(status)) {
						return Optional.of(new WebClientException(
								"ClientResponse has invalid status code: " + status.value() +
										" " + status.getReasonPhrase()));
					}
					else {
						return Optional.empty();
					}
				};

		return errorMapper(mapper);
	}

	/**
	 * Return a filter that will publish a {@link WebClientException} if the response satisfies
	 * the given {@code predicate} function.
	 * @param predicate the predicate to test the response with
	 * @return the {@code ExchangeFilterFunction} that publishes a {@code WebClientException} when
	 * {@code predicate} returns {@code true}
	 */
	public static ExchangeFilterFunction errorPredicate(Predicate<ClientResponse> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");

		Function<ClientResponse, Optional<? extends Throwable>> mapper =
				clientResponse -> {
					if (predicate.test(clientResponse)) {
						return Optional.of(new WebClientException(
								"ClientResponse does not satisfy predicate : " + predicate));
					}
					else {
						return Optional.empty();
					}
				};

		return errorMapper(mapper);
	}

	/**
	 * Return a filter that maps the response to a potential error. Exceptions returned by
	 * {@code mapper} will be published as signal in the {@code Mono<ClientResponse>} return value.
	 * @param mapper the function that maps from response to optional error
	 * @return the {@code ExchangeFilterFunction} that propagates the errors provided by
	 * {@code mapper}
	 */
	public static ExchangeFilterFunction errorMapper(Function<ClientResponse,
			Optional<? extends Throwable>> mapper) {

		Assert.notNull(mapper, "'mapper' must not be null");
		return ExchangeFilterFunction.ofResponseProcessor(
				clientResponse -> {
					Optional<? extends Throwable> error = mapper.apply(clientResponse);
					return error.isPresent() ? Mono.error(error.get()) : Mono.just(clientResponse);
				});
	}

	/**
	 * Return a filter that adds an Authorization header for HTTP Basic Authentication.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the {@link ExchangeFilterFunction} that adds the Authorization header
	 */
	public static ExchangeFilterFunction basicAuthentication(String username, String password) {
		Assert.notNull(username, "'username' must not be null");
		Assert.notNull(password, "'password' must not be null");

		return ExchangeFilterFunction.ofRequestProcessor(
				clientRequest -> {
					String authorization = authorization(username, password);
					ClientRequest<?> authorizedRequest = ClientRequest.from(clientRequest)
							.header(HttpHeaders.AUTHORIZATION, authorization)
							.body(clientRequest.inserter());
					return Mono.just(authorizedRequest);
				});
	}

	private static String authorization(String username, String password) {
		String credentials = username + ":" + password;
		byte[] credentialBytes = credentials.getBytes(StandardCharsets.ISO_8859_1);
		byte[] encodedBytes = Base64.getEncoder().encode(credentialBytes);
		String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
		return "Basic " + encodedCredentials;
	}


}
