/*
 * Copyright 2002-2018 the original author or authors.
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
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

/**
 * {@link ClientHttpRequestInterceptor} to apply a BASIC authorization header.
 *
 * @author Phillip Webb
 * @since 4.3.1
 * @deprecated as of 5.1.1, in favor of {@link BasicAuthenticationInterceptor}
 * which reuses {@link org.springframework.http.HttpHeaders#setBasicAuth},
 * sharing its default charset ISO-8859-1 instead of UTF-8 as used here
 */
@Deprecated
public class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {

	private final String username;

	private final String password;


	/**
	 * Create a new interceptor which adds a BASIC authorization header
	 * for the given username and password.
	 * @param username the username to use
	 * @param password the password to use
	 */
	public BasicAuthorizationInterceptor(@Nullable String username, @Nullable String password) {
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		this.username = (username != null ? username : "");
		this.password = (password != null ? password : "");
	}


	@Override
	public ClientHttpResponse intercept(
			HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

		String token = Base64Utils.encodeToString(
				(this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));
		request.getHeaders().add("Authorization", "Basic " + token);
		return execution.execute(request, body);
	}

}
