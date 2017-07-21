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

import java.nio.charset.Charset;
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
	 * Name of the {@link ClientRequest} attribute that contains the username, as used by
	 * {@link #basicAuthentication()}
	 */
	public static final String USERNAME_ATTRIBUTE = ExchangeFilterFunctions.class.getName() + ".username";

	/**
	 * Name of the {@link ClientRequest} attribute that contains the password, as used by
	 * {@link #basicAuthentication()}
	 */
	public static final String PASSWORD_ATTRIBUTE = ExchangeFilterFunctions.class.getName() + ".password";


	/**
	 * Return a filter that adds an Authorization header for HTTP Basic Authentication, based on
	 * the given username and password.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the {@link ExchangeFilterFunction} that adds the Authorization header
	 */
	public static ExchangeFilterFunction basicAuthentication(String username, String password) {
		Assert.notNull(username, "'username' must not be null");
		Assert.notNull(password, "'password' must not be null");

		return basicAuthenticationInternal(r -> Optional.of(new Credentials(username, password)));
	}

	/**
	 * Return a filter that adds an Authorization header for HTTP Basic Authentication, based on
	 * the username and password provided in the
	 * {@linkplain ClientRequest#attributes() request attributes}. If the attributes are not found,
	 * no authorization header
	 * @return the {@link ExchangeFilterFunction} that adds the Authorization header
	 * @see #USERNAME_ATTRIBUTE
	 * @see #PASSWORD_ATTRIBUTE
	 */
	public static ExchangeFilterFunction basicAuthentication() {
		return basicAuthenticationInternal(
				request -> {
					Optional<String> username = request.attribute(USERNAME_ATTRIBUTE).map(o -> (String)o);
					Optional<String> password = request.attribute(PASSWORD_ATTRIBUTE).map(o -> (String)o);
					if (username.isPresent() && password.isPresent()) {
						return Optional.of(new Credentials(username.get(), password.get()));
					} else {
						return Optional.empty();
					}
				});
	}

	private static ExchangeFilterFunction basicAuthenticationInternal(
			Function<ClientRequest, Optional<Credentials>> credentialsFunction) {

		return ExchangeFilterFunction.ofRequestProcessor(
				clientRequest -> credentialsFunction.apply(clientRequest).map(
						credentials -> {
							ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
									.headers(headers -> {
										headers.set(HttpHeaders.AUTHORIZATION,
												authorization(credentials));
									})
									.build();
							return Mono.just(authorizedRequest);
						})
						.orElse(Mono.just(clientRequest)));
	}

	private static String authorization(Credentials credentials) {
		byte[] credentialBytes = credentials.toByteArray(StandardCharsets.ISO_8859_1);
		byte[] encodedBytes = Base64.getEncoder().encode(credentialBytes);
		String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
		return "Basic " + encodedCredentials;
	}

	/**
	 * Return a filter that returns a given {@link Throwable} as response if the given
	 * {@link HttpStatus} predicate matches.
	 * @param statusPredicate the predicate that should match the
	 * {@linkplain ClientResponse#statusCode() response status}
	 * @param exceptionFunction the function that returns the exception
	 * @return the {@link ExchangeFilterFunction} that returns the given exception if the predicate
	 * matches
	 */
	public static ExchangeFilterFunction statusError(Predicate<HttpStatus> statusPredicate,
			Function<ClientResponse, ? extends Throwable> exceptionFunction) {

		Assert.notNull(statusPredicate, "'statusPredicate' must not be null");
		Assert.notNull(exceptionFunction, "'exceptionFunction' must not be null");

		return ExchangeFilterFunction.ofResponseProcessor(
				clientResponse -> {
					if (statusPredicate.test(clientResponse.statusCode())) {
						return Mono.error(exceptionFunction.apply(clientResponse));
					}
					else {
						return Mono.just(clientResponse);
					}
				}
		);
	}


	private static final class Credentials {

		private String username;

		private String password;

		public Credentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public byte[] toByteArray(Charset charset) {
			String credentials = this.username + ":" + this.password;
			return credentials.getBytes(charset);
		}

	}

}
