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

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Static factory methods for creating {@link ClientWebRequestPostProcesor} instances.
 *
 * @author Rob Winch
 * @since 5.0
 * @see DefaultClientWebRequestBuilder#apply(ClientWebRequestPostProcessors)
 */
public abstract class ClientWebRequestPostProcessors {

	/**
	 * Adds an Authorization header for HTTP Basic
	 * @param username the username to add
	 * @param password the password to add
	 * @return the {@link ClientWebRequestPostProcessor} that adds the Authorization header
	 */
	public static ClientWebRequestPostProcessor httpBasic(String username, String password) {
		Assert.notNull(username, "username cannot be null");
		Assert.notNull(password, "password cannot be null");

		return new ClientWebRequestPostProcessor() {

			@Override
			public ClientWebRequest postProcess(ClientWebRequest toPostProcess) {
				String authorization = authorization(username, password);
				toPostProcess.getHttpHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
				return toPostProcess;
			}

			private String authorization(String username, String password) {
				String credentials = username + ":" + password;
				return authorization(credentials);
			}

			private String authorization(String credentials) {
				byte[] credentialBytes = credentials.getBytes(Charset.defaultCharset());
				Encoder encoder = Base64.getEncoder();
				String encodedCredentials = encoder.encodeToString(credentialBytes);
				return "Basic " + encodedCredentials;
			}
		};
	}

}
