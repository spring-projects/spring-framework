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

package org.springframework.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequestFactory} implementation that uses standard JDK facilities.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.net.HttpURLConnection
 * @see HttpComponentsClientHttpRequestFactory
 */
public class SimpleClientHttpRequestFactory implements ClientHttpRequestFactory {

	private static final int DEFAULT_CHUNK_SIZE = 4096;


	private @Nullable Proxy proxy;

	private int chunkSize = DEFAULT_CHUNK_SIZE;

	private int connectTimeout = -1;

	private int readTimeout = -1;


	/**
	 * Set the {@link Proxy} to use for this request factory.
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Set the number of bytes to write in each chunk when not buffering request
	 * bodies locally.
	 */
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * Set the underlying URLConnection's connect timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Default is the system's default timeout.
	 * @see URLConnection#setConnectTimeout(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Set the underlying URLConnection's connect timeout as {@code Duration}.
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Default is the system's default timeout.
	 * @since 6.1
	 * @see URLConnection#setConnectTimeout(int)
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		Assert.notNull(connectTimeout, "ConnectTimeout must not be null");
		this.connectTimeout = (int) connectTimeout.toMillis();
	}

	/**
	 * Set the underlying URLConnection's read timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Default is the system's default timeout.
	 * @see URLConnection#setReadTimeout(int)
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Set the underlying URLConnection's read timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * <p>Default is the system's default timeout.
	 * @since 6.1
	 * @see URLConnection#setReadTimeout(int)
	 */
	public void setReadTimeout(Duration readTimeout) {
		Assert.notNull(readTimeout, "ReadTimeout must not be null");
		this.readTimeout = (int) readTimeout.toMillis();
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		return new SimpleClientHttpRequest(connection, this.chunkSize);
	}

	/**
	 * Opens and returns a connection to the given URL.
	 * <p>The default implementation uses the given {@linkplain #setProxy(java.net.Proxy) proxy} -
	 * if any - to open a connection.
	 * @param url the URL to open a connection to
	 * @param proxy the proxy to use, may be {@code null}
	 * @return the opened connection
	 * @throws IOException in case of I/O errors
	 */
	protected HttpURLConnection openConnection(URL url, @Nullable Proxy proxy) throws IOException {
		URLConnection urlConnection = (proxy != null ? url.openConnection(proxy) : url.openConnection());
		if (!(urlConnection instanceof HttpURLConnection httpUrlConnection)) {
			throw new IllegalStateException(
					"HttpURLConnection required for [" + url + "] but got: " + urlConnection);
		}
		return httpUrlConnection;
	}

	/**
	 * Template method for preparing the given {@link HttpURLConnection}.
	 * <p>The default implementation prepares the connection for input and output, and sets the HTTP method.
	 * @param connection the connection to prepare
	 * @param httpMethod the HTTP request method ({@code GET}, {@code POST}, etc.)
	 * @throws IOException in case of I/O errors
	 */
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		boolean mayWrite =
				("POST".equals(httpMethod) || "PUT".equals(httpMethod) ||
						"PATCH".equals(httpMethod) || "DELETE".equals(httpMethod));

		connection.setDoInput(true);
		connection.setInstanceFollowRedirects("GET".equals(httpMethod));
		connection.setDoOutput(mayWrite);
		connection.setRequestMethod(httpMethod);
	}

}
