/*
 * Copyright 2002-2024 the original author or authors.
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
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequestFactory} implementation that uses
 * <a href="https://square.github.io/okhttp/">OkHttp</a> 3.x to create requests.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.3
 * @deprecated since 6.1, in favor of other {@link ClientHttpRequestFactory} implementations;
 * scheduled for removal in 7.0
 */
@Deprecated(since = "6.1", forRemoval = true)
public class OkHttp3ClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

	private OkHttpClient client;

	private final boolean defaultClient;


	/**
	 * Create a factory with a default {@link OkHttpClient} instance.
	 */
	public OkHttp3ClientHttpRequestFactory() {
		this.client = new OkHttpClient();
		this.defaultClient = true;
	}

	/**
	 * Create a factory with the given {@link OkHttpClient} instance.
	 * @param client the client to use
	 */
	public OkHttp3ClientHttpRequestFactory(OkHttpClient client) {
		Assert.notNull(client, "OkHttpClient must not be null");
		this.client = client;
		this.defaultClient = false;
	}


	/**
	 * Set the underlying read timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 */
	public void setReadTimeout(int readTimeout) {
		this.client = this.client.newBuilder()
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * Set the underlying read timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @since 6.1
	 */
	public void setReadTimeout(Duration readTimeout) {
		this.client = this.client.newBuilder()
				.readTimeout(readTimeout)
				.build();
	}

	/**
	 * Set the underlying write timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 */
	public void setWriteTimeout(int writeTimeout) {
		this.client = this.client.newBuilder()
				.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * Set the underlying write timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @since 6.1
	 */
	public void setWriteTimeout(Duration writeTimeout) {
		this.client = this.client.newBuilder()
				.writeTimeout(writeTimeout)
				.build();
	}

	/**
	 * Set the underlying connect timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.client = this.client.newBuilder()
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * Set the underlying connect timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @since 6.1
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		this.client = this.client.newBuilder()
				.connectTimeout(connectTimeout)
				.build();
	}


	@Override
	@SuppressWarnings("removal")
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttp3ClientHttpRequest(this.client, uri, httpMethod);
	}


	@Override
	public void destroy() throws IOException {
		if (this.defaultClient) {
			// Clean up the client if we created it in the constructor
			Cache cache = this.client.cache();
			if (cache != null) {
				cache.close();
			}
			this.client.dispatcher().executorService().shutdown();
			this.client.connectionPool().evictAll();
		}
	}

}
