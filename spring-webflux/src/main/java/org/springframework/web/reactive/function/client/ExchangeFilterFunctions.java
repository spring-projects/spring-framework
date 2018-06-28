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
 * Static factory methods providing access to built-in implementations of
 * {@link ExchangeFilterFunction} for basic authentication, error handling, etc.
 *
 * @author Rob Winch
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class ExchangeFilterFunctions {

	/**
	 * Name of the {@linkplain ClientRequest#attributes() request attribute} that
	 * contains the {@link Credentials} used by {@link #basicAuthentication()}.
	 */
	public static final String BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE =
			ExchangeFilterFunctions.class.getName() + ".basicAuthenticationCredentials";


	/**
	 * Return a filter for HTTP Basic Authentication that adds an authorization
	 * header, based on the given user and password.
	 * <p>Note that Basic Authentication only supports characters in the
	 * {@link StandardCharsets#ISO_8859_1 ISO-8859-1} character set.
	 * @param user the user
	 * @param password the password
	 * @return the filter for basic authentication
	 * @throws IllegalArgumentException if either {@code user} or
	 * {@code password} contain characters that cannot be encoded to ISO-8859-1.
	 */
	public static ExchangeFilterFunction basicAuthentication(String user, String password) {
		Assert.notNull(user, "'user' must not be null");
		Assert.notNull(password, "'password' must not be null");
		checkIllegalCharacters(user, password);
		return basicAuthenticationInternal(request -> Optional.of(new Credentials(user, password)));
	}

	/**
	 * Variant of {@link #basicAuthentication(String, String)} that looks up
	 * the {@link Credentials Credentials} provided in a
	 * {@linkplain ClientRequest#attributes() request attribute}, or if the
	 * attribute is not found, the authorization header is not added.
	 * @return the filter for basic authentication
	 * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
	 * @see Credentials#basicAuthenticationCredentials(String, String)
	 */
	public static ExchangeFilterFunction basicAuthentication() {
		return basicAuthenticationInternal(request ->
				request.attribute(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE)
						.map(credentials -> (Credentials) credentials));
	}

	private static ExchangeFilterFunction basicAuthenticationInternal(
			Function<ClientRequest, Optional<Credentials>> credentialsFunction) {

		return ExchangeFilterFunction.ofRequestProcessor(request ->
				credentialsFunction.apply(request)
						.map(credentials -> Mono.just(insertAuthorizationHeader(request, credentials)))
						.orElse(Mono.just(request)));
	}

	private static void checkIllegalCharacters(String username, String password) {
		// Basic authentication only supports ISO 8859-1, see
		// https://stackoverflow.com/questions/702629/utf-8-characters-mangled-in-http-basic-auth-username#703341
		CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();
		if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to ISO-8859-1");
		}
	}

	private static ClientRequest insertAuthorizationHeader(ClientRequest request, Credentials credentials) {
		return ClientRequest.from(request).headers(headers -> {
			String credentialsString = credentials.username + ":" + credentials.password;
			byte[] credentialBytes = credentialsString.getBytes(StandardCharsets.ISO_8859_1);
			byte[] encodedBytes = Base64.getEncoder().encode(credentialBytes);
			String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
			headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
		}).build();
	}

	/**
	 * Return a filter that generates an error signal when the given
	 * {@link HttpStatus} predicate matches.
	 * @param statusPredicate the predicate to check the HTTP status with
	 * @param exceptionFunction the function that to create the exception
	 * @return the filter to generate an error signal
	 */
	public static ExchangeFilterFunction statusError(Predicate<HttpStatus> statusPredicate,
			Function<ClientResponse, ? extends Throwable> exceptionFunction) {

		Assert.notNull(statusPredicate, "Predicate must not be null");
		Assert.notNull(exceptionFunction, "Function must not be null");

		return ExchangeFilterFunction.ofResponseProcessor(
				response -> (statusPredicate.test(response.statusCode()) ?
						Mono.error(exceptionFunction.apply(response)) : Mono.just(response)));
	}


	/**
	 * Stores user and password for HTTP basic authentication.
	 * @see #basicAuthentication()
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
		 * Return a {@literal Consumer} that stores the given user and password
		 * as a request attribute of type {@code Credentials} that is in turn
		 * used by {@link ExchangeFilterFunctions#basicAuthentication()}.
		 * @param user the user
		 * @param password the password
		 * @return a consumer that can be passed into
		 * {@linkplain ClientRequest.Builder#attributes(java.util.function.Consumer)}
		 * @see ClientRequest.Builder#attributes(java.util.function.Consumer)
		 * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
		 */
		public static Consumer<Map<String, Object>> basicAuthenticationCredentials(String user, String password) {
			Credentials credentials = new Credentials(user, password);
			checkIllegalCharacters(user, password);
			return (map -> map.put(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE, credentials));
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Credentials)) {
				return false;
			}
			Credentials otherCred = (Credentials) other;
			return (this.username.equals(otherCred.username) && this.password.equals(otherCred.password));
		}

		@Override
		public int hashCode() {
			return 31 * this.username.hashCode() + this.password.hashCode();
		}
	}

}
