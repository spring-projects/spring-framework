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

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Implementations of {@link ExchangeFilterFunction} that provide various useful request filter
 * operations, such as basic authentication.
 *
 * @author Rob Winch
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class ExchangeFilterFunctions {

	private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();


	/**
	 * Return a filter that adds an Authorization header for HTTP Basic.
	 * @param username the username to use
	 * @param password the password to use
	 * @return the {@link ExchangeFilterFunction} that adds the Authorization header
	 */
	public static ExchangeFilterFunction basicAuthentication(String username, String password) {
		Assert.notNull(username, "'username' must not be null");
		Assert.notNull(password, "'password' must not be null");

		return new ExchangeFilterFunction() {

			@Override
			public Mono<ClientResponse> filter(ClientRequest<?> request, ExchangeFunction next) {
				String authorization = authorization(username, password);
				ClientRequest<?> authorizedRequest = ClientRequest.from(request)
						.header(HttpHeaders.AUTHORIZATION, authorization)
						.body(request.inserter());

				return next.exchange(authorizedRequest);
			}

			private String authorization(String username, String password) {
				String credentials = username + ":" + password;
				return authorization(credentials);
			}

			private String authorization(String credentials) {
				byte[] credentialBytes = credentials.getBytes(StandardCharsets.ISO_8859_1);
				byte[] encodedBytes = BASE_64_ENCODER.encode(credentialBytes);
				String encodedCredentials = new String(encodedBytes, StandardCharsets.ISO_8859_1);
				return "Basic " + encodedCredentials;
			}
		};

	}




}
