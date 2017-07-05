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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
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

		return basicAuthentication(r -> username, r -> password);
	}

	/**
	 * Return a filter that adds an Authorization header for HTTP Basic Authentication, based on
	 * the username and password provided in the
	 * {@linkplain ClientRequest#attributes() request attributes}.
	 * @return the {@link ExchangeFilterFunction} that adds the Authorization header
	 * @see #USERNAME_ATTRIBUTE
	 * @see #PASSWORD_ATTRIBUTE
	 */
	public static ExchangeFilterFunction basicAuthentication() {
		return basicAuthentication(
				request -> getRequiredAttribute(request, USERNAME_ATTRIBUTE),
				request -> getRequiredAttribute(request, PASSWORD_ATTRIBUTE)
				);
	}

	private static String getRequiredAttribute(ClientRequest request, String key) {
		Map<String, Object> attributes = request.attributes();
		if (attributes.containsKey(key)) {
			return (String) attributes.get(key);
		} else {
			throw new IllegalStateException(
					"Could not find request attribute with key \"" + key + "\"");
		}
	}

	private static ExchangeFilterFunction basicAuthentication(Function<ClientRequest, String> usernameFunction,
			Function<ClientRequest, String> passwordFunction) {

		return ExchangeFilterFunction.ofRequestProcessor(
				clientRequest -> {
					String authorization = authorization(usernameFunction.apply(clientRequest),
							passwordFunction.apply(clientRequest));
					ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
							.headers(headers -> {
								headers.set(HttpHeaders.AUTHORIZATION, authorization);
							})
							.build();
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
