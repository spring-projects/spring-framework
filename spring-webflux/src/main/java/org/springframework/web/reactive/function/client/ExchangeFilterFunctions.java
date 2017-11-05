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

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
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
	 * Name of the {@link ClientRequest} attribute that contains the
	 * {@link Credentials}, as used by {@link #basicAuthentication()}.
	 */
	public static final String BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE =
			ExchangeFilterFunctions.class.getName() + ".basicAuthenticationCredentials";


	/**
	 * Return a filter that adds an Authorization header for HTTP Basic Authentication, based on
	 * the given username and password.
	 * <p>Note that Basic Authentication only supports characters in the
	 * {@link StandardCharsets#ISO_8859_1 ISO-8859-1} character set.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the {@link ExchangeFilterFunction} that adds the Authorization header
	 * @throws IllegalArgumentException if either {@code username} or {@code password} contain
	 * characters that cannot be encoded to ISO-8859-1
	 */
	public static ExchangeFilterFunction basicAuthentication(String username, String password) {
		Assert.notNull(username, "'username' must not be null");
		Assert.notNull(password, "'password' must not be null");

		checkIllegalCharacters(username, password);
		return basicAuthenticationInternal(r -> Optional.of(new Credentials(username, password)));
	}

	/**
	 * Return a filter that adds an Authorization header for HTTP Basic Authentication, based on
	 * the {@link Credentials} provided in the
	 * {@linkplain ClientRequest#attributes() request attributes}. If the attribute is not found,
	 * no authorization header is added.
	 * <p>Note that Basic Authentication only supports characters in the
	 * {@link StandardCharsets#ISO_8859_1 ISO-8859-1} character set.
	 * @return the {@link ExchangeFilterFunction} that adds the Authorization header
	 * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
	 * @see Credentials#basicAuthenticationCredentials(String, String)
	 */
	public static ExchangeFilterFunction basicAuthentication() {
		return basicAuthenticationInternal(
				request -> request.attribute(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE).map(o -> (Credentials)o));
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
		String credentialsString = credentials.username + ":" + credentials.password;
		byte[] credentialBytes = credentialsString.getBytes(StandardCharsets.ISO_8859_1);
		byte[] encodedBytes = Base64.getEncoder().encode(credentialBytes);
		String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
		return "Basic " + encodedCredentials;
	}

	/*
	 * Basic authentication only supports ISO 8859-1, see
	 * https://stackoverflow.com/questions/702629/utf-8-characters-mangled-in-http-basic-auth-username#703341
	 */
	private static void checkIllegalCharacters(String username, String password) {
		CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();
		if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to ISO-8859-1");
		}

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


	/**
	 * Represents a combination of username and password, as used by {@link #basicAuthentication()}.
	 * @see #basicAuthenticationCredentials(String, String)
	 */
	public static final class Credentials {

		private final String username;

		private final String password;

		/**
		 * Create a new {@code Credentials} instance with the given username and password.
		 * @param username the username
		 * @param password the password
		 */
		public Credentials(String username, String password) {
			Assert.notNull(username, "'username' must not be null");
			Assert.notNull(password, "'password' must not be null");

			this.username = username;
			this.password = password;
		}

		/**
		 * Return a consumer that stores the given username and password in the
		 * {@linkplain ClientRequest.Builder#attributes(java.util.function.Consumer) request
		 * attributes} as a {@code Credentials} object.
		 * @param username the username
		 * @param password the password
		 * @return a consumer that adds the given credentials to the attribute map
		 * @see ClientRequest.Builder#attributes(java.util.function.Consumer)
		 * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
		 */
		public static Consumer<Map<String, Object>> basicAuthenticationCredentials(String username, String password) {
			Credentials credentials = new Credentials(username, password);
			checkIllegalCharacters(username, password);

			return attributes -> attributes.put(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE, credentials);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof Credentials) {
				Credentials other = (Credentials) o;
				return this.username.equals(other.username) &&
						this.password.equals(other.password);

			}
			return false;
		}

		@Override
		public int hashCode() {
			return 31 * this.username.hashCode() + this.password.hashCode();
		}

	}

}
