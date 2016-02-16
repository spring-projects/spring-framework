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

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * {@link ClientHttpRequestFactory} implementation that uses
 * <a href="http://square.github.io/okhttp/">OkHttp</a> to create requests.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @since 4.2
 */
public class OkHttpClientHttpRequestFactory
		implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory, DisposableBean {

	private final OkHttpClient client;

	private final boolean defaultClient;


	/**
	 * Create a factory with a default {@link OkHttpClient} instance.
	 */
	public OkHttpClientHttpRequestFactory() {
		this.client = new OkHttpClient();
		this.defaultClient = true;
	}

	/**
	 * Create a factory with the given {@link OkHttpClient} instance.
	 * @param client the client to use
	 */
	public OkHttpClientHttpRequestFactory(OkHttpClient client) {
		Assert.notNull(client, "'client' must not be null");
		this.client = client;
		this.defaultClient = false;
	}


	/**
	 * Sets the underlying read timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @see OkHttpClient#setReadTimeout(long, TimeUnit)
	 */
	public void setReadTimeout(int readTimeout) {
		this.client.setReadTimeout(readTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Sets the underlying write timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @see OkHttpClient#setWriteTimeout(long, TimeUnit)
	 */
	public void setWriteTimeout(int writeTimeout) {
		this.client.setWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * Sets the underlying connect timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * @see OkHttpClient#setConnectTimeout(long, TimeUnit)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.client.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttpClientHttpRequest(this.client, uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttpAsyncClientHttpRequest(this.client, uri, httpMethod);
	}


	@Override
	public void destroy() throws IOException {
		if (this.defaultClient) {
			// Clean up the client if we created it in the constructor
			if (this.client.getCache() != null) {
				this.client.getCache().close();
			}
			this.client.getDispatcher().getExecutorService().shutdown();
		}
	}


	private static Request buildRequest(HttpHeaders headers, byte[] content, URI uri,
			HttpMethod method) throws MalformedURLException {

		com.squareup.okhttp.MediaType contentType = getContentType(headers);
		RequestBody body = (content.length > 0 ? RequestBody.create(contentType, content) : null);

		URL url = uri.toURL();
		String methodName = method.name();
		Request.Builder builder = new Request.Builder().url(url).method(methodName, body);

		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				builder.addHeader(headerName, headerValue);
			}
		}

		return builder.build();
	}

	private static com.squareup.okhttp.MediaType getContentType(HttpHeaders headers) {
		String rawContentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
		return (StringUtils.hasText(rawContentType) ?
				com.squareup.okhttp.MediaType.parse(rawContentType) : null);
	}

	private static class OkHttpClientHttpRequest extends AbstractBufferingClientHttpRequest {

		private final OkHttpClient client;

		private final URI uri;

		private final HttpMethod method;


		public OkHttpClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
			this.client = client;
			this.uri = uri;
			this.method = method;
		}


		@Override
		public HttpMethod getMethod() {
			return this.method;
		}

		@Override
		public URI getURI() {
			return this.uri;
		}


		@Override
		protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] content) throws IOException {
			Request request = buildRequest(headers, content, this.uri, this.method);
			return new OkHttpClientHttpResponse(this.client.newCall(request).execute());
		}

	}

	private static class OkHttpAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

		private final OkHttpClient client;

		private final URI uri;

		private final HttpMethod method;


		public OkHttpAsyncClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
			this.client = client;
			this.uri = uri;
			this.method = method;
		}


		@Override
		public HttpMethod getMethod() {
			return this.method;
		}

		@Override
		public URI getURI() {
			return this.uri;
		}

		@Override
		protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] content)
				throws IOException {

			Request request = buildRequest(headers, content, this.uri, this.method);
			return new OkHttpListenableFuture(this.client.newCall(request));
		}


		private static class OkHttpListenableFuture extends
				SettableListenableFuture<ClientHttpResponse> {

			private final Call call;

			public OkHttpListenableFuture(Call call) {
				this.call = call;
				this.call.enqueue(new Callback() {
					@Override
					public void onResponse(Response response) {
						set(new OkHttpClientHttpResponse(response));
					}
					@Override
					public void onFailure(Request request, IOException ex) {
						setException(ex);
					}
				});
			}

			@Override
			protected void interruptTask() {
				this.call.cancel();
			}
		}

	}

	private static class OkHttpClientHttpResponse extends AbstractClientHttpResponse {

		private final Response response;

		private HttpHeaders headers;


		public OkHttpClientHttpResponse(Response response) {
			Assert.notNull(response, "'response' must not be null");
			this.response = response;
		}


		@Override
		public int getRawStatusCode() {
			return this.response.code();
		}

		@Override
		public String getStatusText() {
			return this.response.message();
		}

		@Override
		public InputStream getBody() throws IOException {
			return this.response.body().byteStream();
		}

		@Override
		public HttpHeaders getHeaders() {
			if (this.headers == null) {
				HttpHeaders headers = new HttpHeaders();
				for (String headerName : this.response.headers().names()) {
					for (String headerValue : this.response.headers(headerName)) {
						headers.add(headerName, headerValue);
					}
				}
				this.headers = headers;
			}
			return this.headers;
		}

		@Override
		public void close() {
			try {
				this.response.body().close();
			}
			catch (IOException ex) {
				// ignore
			}
		}

	}

}
