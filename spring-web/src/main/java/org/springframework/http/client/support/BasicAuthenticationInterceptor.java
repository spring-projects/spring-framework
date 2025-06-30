/*
 * Copyright 2002-present the original author or authors.
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

import java.io.IOException;
import java.nio.charset.Charset;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link ClientHttpRequestInterceptor} to apply a given HTTP Basic Authentication
 * username/password pair, unless a custom {@code Authorization} header has
 * already been set.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.1.1
 * @see HttpHeaders#setBasicAuth
 * @see HttpHeaders#AUTHORIZATION
 */
public class BasicAuthenticationInterceptor implements ClientHttpRequestInterceptor {

	private final String encodedCredentials;


	/**
	 * Create a new interceptor which adds Basic Authentication for the
	 * given username and password.
	 * @param username the username to use
	 * @param password the password to use
	 * @see HttpHeaders#setBasicAuth(String, String)
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 */
	public BasicAuthenticationInterceptor(String username, String password) {
		this(username, password, null);
	}

	/**
	 * Create a new interceptor which adds Basic Authentication for the
	 * given username and password, encoded using the specified charset.
	 * @param username the username to use
	 * @param password the password to use
	 * @param charset the charset to use
	 * @see HttpHeaders#setBasicAuth(String, String, Charset)
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 */
	public BasicAuthenticationInterceptor(String username, String password, @Nullable Charset charset) {
		this.encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, charset);
	}


	@Override
	public ClientHttpResponse intercept(
			HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

		HttpHeaders headers = request.getHeaders();
		if (!headers.containsHeader(HttpHeaders.AUTHORIZATION)) {
			headers.setBasicAuth(this.encodedCredentials);
		}
		return execution.execute(request, body);
	}

}
