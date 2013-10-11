/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.client;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import com.squareup.okhttp.OkHttpClient;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequestFactory} implementation that uses
 * <a href="http://square.github.io/okhttp/">OkHttp</a> to create requests.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @since 4.2
 */
public class OkHttpClientHttpRequestFactory
		implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory,
		DisposableBean {

	private final OkHttpClient client;

	private final boolean defaultClient;


	/**
	 * Create a new {@code OkHttpClientHttpRequestFactory} with a default
	 * {@link OkHttpClient}.
	 */
	public OkHttpClientHttpRequestFactory() {
		client = new OkHttpClient();
		defaultClient = true;
	}

	/**
	 * Create a new {@code OkHttpClientHttpRequestFactory} with the given
	 * {@link OkHttpClient}.
	 * @param okHttpClient the client to use
	 */
	public OkHttpClientHttpRequestFactory(OkHttpClient okHttpClient) {
		Assert.notNull(okHttpClient, "'okHttpClient' must not be null");
		client = okHttpClient;
		defaultClient = false;
	}


	/**
	 * Sets the underlying read timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * @see OkHttpClient#setReadTimeout(long, TimeUnit)
	 */
	public void setReadTimeout(int readTimeout) {
		this.client.setReadTimeout(readTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Sets the underlying write timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * @see OkHttpClient#setWriteTimeout(long, TimeUnit)
	 */
	public void setWriteTimeout(int writeTimeout) {
		this.client.setWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Sets the underlying connect timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * @see OkHttpClient#setConnectTimeout(long, TimeUnit)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.client.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
		return createRequestInternal(uri, httpMethod);
	}

	private OkHttpClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
		return new OkHttpClientHttpRequest(this.client, uri, httpMethod);
	}

	@Override
	public void destroy() throws Exception {
		if (defaultClient) {
			// Clean up the client if we created it in the constructor
			if (this.client.getCache() != null) {
				this.client.getCache().close();
			}
			this.client.getDispatcher().getExecutorService().shutdown();
		}
	}
}
