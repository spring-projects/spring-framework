/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.client.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * {@link ClientHttpRequestInterceptor} to apply a given HTTP Bearer Authentication
 * token, unless a custom {@code Authorization} header has already been set.
 *
 * @author Dmitry Ivanov
 * @see HttpHeaders#setBearerAuth
 * @see HttpHeaders#AUTHORIZATION
 * @since 6.1
 */
public class BearerAuthenticationInterceptor implements ClientHttpRequestInterceptor {

	private final Supplier<String> token;

	/**
	 * Create a new interceptor which adds Bearer Authentication for the
	 * given token.
	 *
	 * @param token supplier for the token to use
	 * @see HttpHeaders#setBearerAuth
	 */
	public BearerAuthenticationInterceptor(Supplier<String> token) {
		this.token = token;
	}

	@Override
	public ClientHttpResponse intercept(
			HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		HttpHeaders headers = request.getHeaders();
		if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
			headers.setBearerAuth(token.get());
		}
		return execution.execute(request, body);
	}

}
