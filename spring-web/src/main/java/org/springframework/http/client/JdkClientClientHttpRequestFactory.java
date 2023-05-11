/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;

import org.springframework.http.HttpMethod;


/**
 * {@link ClientHttpRequestFactory} implementation that uses a
 * <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html">HttpClient</a> to create requests.
 *
 * @author Marten Deinum
 * @since 6.1
 */
public class JdkClientClientHttpRequestFactory implements ClientHttpRequestFactory {

	private HttpClient client;

	private final boolean defaultClient;


	public JdkClientClientHttpRequestFactory() {
		this.client = HttpClient.newHttpClient();
		this.defaultClient = true;
	}

	public JdkClientClientHttpRequestFactory(HttpClient client) {
		this.client = client;
		this.defaultClient = false;
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return new JdkClientClientHttpRequest(this.client, uri, httpMethod);
	}

}
