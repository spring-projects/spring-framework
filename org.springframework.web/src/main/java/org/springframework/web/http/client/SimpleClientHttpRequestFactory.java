/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.util.Assert;
import org.springframework.web.http.HttpMethod;

/**
 * {@link ClientHttpRequestFactory} implementation that uses standard J2SE facilities.
 *
 * @author Arjen Poutsma
 * @see java.net.HttpURLConnection
 * @see org.springframework.web.http.client.commons.CommonsClientHttpRequestFactory
 * @since 3.0
 */
public class SimpleClientHttpRequestFactory implements ClientHttpRequestFactory {

	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		URL url = uri.toURL();
		URLConnection urlConnection = url.openConnection();
		Assert.isInstanceOf(HttpURLConnection.class, urlConnection);
		HttpURLConnection connection = (HttpURLConnection) urlConnection;
		prepareConnection(connection, httpMethod.name());
		return new SimpleClientHttpRequest(connection);
	}

	/**
	 * Template method for preparing the given {@link HttpURLConnection}.
	 *
	 * <p>Default implementation prepares the connection for input and output, and sets the HTTP method.
	 *
	 * @param connection the connection to prepare
	 * @param httpMethod the HTTP request method ({@code GET}, {@code POST}, etc.)
	 * @throws IOException in case of I/O errors
	 */
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestMethod(httpMethod);
	}

}