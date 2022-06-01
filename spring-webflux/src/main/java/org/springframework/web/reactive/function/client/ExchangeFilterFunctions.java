/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
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
	 * Name of the request attribute with {@link Credentials} for {@link #basicAuthentication()}.
	 * @deprecated as of Spring 5.1 in favor of using
	 * {@link HttpHeaders#setBasicAuth(String, String)} while building the request.
	 */
	@Deprecated
	public static final String BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE =
			ExchangeFilterFunctions.class.getName() + ".basicAuthenticationCredentials";


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
	 * {@link HttpStatus} predicate matches.
	 * @param statusPredicate the predicate to check the HTTP status with
	 * @param exceptionFunction the function to create the exception
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

	/**
	 * Variant of {@link #basicAuthentication(String, String)} that looks up
	 * the {@link Credentials Credentials} in a
	 * {@link #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE request attribute}.
	 * @return the filter to use
	 * @see Credentials
	 * @deprecated as of Spring 5.1 in favor of using
	 * {@link HttpHeaders#setBasicAuth(String, String)} while building the request.
	 */
	@Deprecated
	public static ExchangeFilterFunction basicAuthentication() {
		return (request, next) -> {
			Object attr = request.attributes().get(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE);
			if (attr instanceof Credentials) {
				Credentials cred = (Credentials) attr;
				return next.exchange(ClientRequest.from(request)
						.headers(headers -> headers.setBasicAuth(cred.username, cred.password))
						.build());
			}
			else {
				return next.exchange(request);
			}
		};
	}


	/**
	 * Stores username and password for HTTP basic authentication.
	 * @deprecated as of Spring 5.1 in favor of using
	 * {@link HttpHeaders#setBasicAuth(String, String)} while building the request.
	 */
	@Deprecated
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
		 * Return a {@literal Consumer} that stores the given username and password
		 * as a request attribute of type {@code Credentials} that is in turn
		 * used by {@link ExchangeFilterFunctions#basicAuthentication()}.
		 * @param username the username
		 * @param password the password
		 * @return a consumer that can be passed into
		 * {@linkplain ClientRequest.Builder#attributes(java.util.function.Consumer)}
		 * @see ClientRequest.Builder#attributes(java.util.function.Consumer)
		 * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
		 */
		public static Consumer<Map<String, Object>> basicAuthenticationCredentials(String username, String password) {
			Credentials credentials = new Credentials(username, password);
			return (map -> map.put(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE, credentials));
		}

		@Override
		public boolean equals(@Nullable Object other) {
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
