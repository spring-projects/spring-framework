/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequestFactory} implementation that uses
 * <a href="http://square.github.io/okhttp/">OkHttp</a> 3.x to create requests.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.3
 */
@SuppressWarnings("deprecation")
public class OkHttp3ClientHttpRequestFactory
		implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory, DisposableBean {

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
	 * Sets the underlying read timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @see OkHttpClient.Builder#readTimeout(long, TimeUnit)
	 */
	public void setReadTimeout(int readTimeout) {
		this.client = this.client.newBuilder()
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * Sets the underlying write timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @see OkHttpClient.Builder#writeTimeout(long, TimeUnit)
	 */
	public void setWriteTimeout(int writeTimeout) {
		this.client = this.client.newBuilder()
				.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * Sets the underlying connect timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @see OkHttpClient.Builder#connectTimeout(long, TimeUnit)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.client = this.client.newBuilder()
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
				.build();
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttp3ClientHttpRequest(this.client, uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttp3AsyncClientHttpRequest(this.client, uri, httpMethod);
	}


	@Override
	public void destroy() throws IOException {
		if (this.defaultClient) {
			// Clean up the client if we created it in the constructor
			if (this.client.cache() != null) {
				this.client.cache().close();
			}
			this.client.dispatcher().executorService().shutdown();
		}
	}


	static Request buildRequest(HttpHeaders headers, byte[] content, URI uri, HttpMethod method)
			throws MalformedURLException {

		okhttp3.MediaType contentType = getContentType(headers);
		RequestBody body = (content.length > 0 ||
				okhttp3.internal.http.HttpMethod.requiresRequestBody(method.name()) ?
				RequestBody.create(contentType, content) : null);

		Request.Builder builder = new Request.Builder().url(uri.toURL()).method(method.name(), body);
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				builder.addHeader(headerName, headerValue);
			}
		}
		return builder.build();
	}

	private static okhttp3.MediaType getContentType(HttpHeaders headers) {
		String rawContentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
		return (StringUtils.hasText(rawContentType) ? okhttp3.MediaType.parse(rawContentType) : null);
	}

}
