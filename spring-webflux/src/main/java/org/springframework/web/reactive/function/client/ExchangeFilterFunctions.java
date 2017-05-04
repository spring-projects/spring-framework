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
					ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
							.header(HttpHeaders.AUTHORIZATION, authorization)
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
