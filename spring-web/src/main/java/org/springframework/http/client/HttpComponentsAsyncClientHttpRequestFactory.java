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
import java.net.URI;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.protocol.HttpContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.FutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * Asynchronous extension of the {@link HttpComponentsClientHttpRequestFactory}. Uses
 * <a href="http://hc.apache.org/httpcomponents-asyncclient-dev/">Apache HttpComponents
 * HttpAsyncClient 4.0</a> to create requests.
 *
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @since 4.0
 * @see HttpAsyncClient
 */
public class HttpComponentsAsyncClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory
		implements AsyncClientHttpRequestFactory, InitializingBean {

	private CloseableHttpAsyncClient httpAsyncClient;


	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with a default {@link HttpAsyncClient} and {@link HttpClient}.
	 */
	public HttpComponentsAsyncClientHttpRequestFactory() {
		this(HttpAsyncClients.createSystem());
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link HttpAsyncClient} instance and a default {@link HttpClient}.
	 * @param httpAsyncClient the HttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(CloseableHttpAsyncClient httpAsyncClient) {
		super();
		Assert.notNull(httpAsyncClient, "HttpAsyncClient must not be null");
		this.httpAsyncClient = httpAsyncClient;
	}

	/**
	 * Create a new instance of the {@code HttpComponentsAsyncClientHttpRequestFactory}
	 * with the given {@link HttpClient} and {@link HttpAsyncClient} instances.
	 * @param httpClient the HttpClient instance to use for this request factory
	 * @param httpAsyncClient the HttpAsyncClient instance to use for this request factory
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(
			CloseableHttpClient httpClient, CloseableHttpAsyncClient httpAsyncClient) {

		super(httpClient);
		Assert.notNull(httpAsyncClient, "HttpAsyncClient must not be null");
		this.httpAsyncClient = httpAsyncClient;
	}


	/**
	 * Set the {@code HttpClient} used for
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) asynchronous execution}.
	 */
	public void setHttpAsyncClient(CloseableHttpAsyncClient httpAsyncClient) {
		this.httpAsyncClient = httpAsyncClient;
	}

	/**
	 * Return the {@code HttpClient} used for
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) asynchronous execution}.
	 */
	public CloseableHttpAsyncClient getHttpAsyncClient() {
		return this.httpAsyncClient;
	}


	@Override
	public void afterPropertiesSet() {
		startAsyncClient();
	}

	private void startAsyncClient() {
        CloseableHttpAsyncClient asyncClient = getHttpAsyncClient();
		if (!asyncClient.isRunning()) {
			asyncClient.start();
		}
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpAsyncClient asyncClient = getHttpAsyncClient();
		startAsyncClient();
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
        HttpContext context = createHttpContext(httpMethod, uri);
        if (context == null) {
            context = HttpClientContext.create();
        }
		// Request configuration not set in the context
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// Use request configuration given by the user, when available
			RequestConfig config = null;
			if (httpRequest instanceof Configurable) {
				config = ((Configurable) httpRequest).getConfig();
			}
			if (config == null) {
				config = createRequestConfig(asyncClient);
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}
		return new HttpComponentsAsyncClientHttpRequest(asyncClient, httpRequest, context);
	}

	@Override
	public void destroy() throws Exception {
		try {
			super.destroy();
		}
		finally {
			getHttpAsyncClient().close();
		}
	}

	static final class HttpComponentsAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

		final HttpAsyncClient httpClient;

		final HttpUriRequest httpRequest;

		final HttpContext httpContext;


		HttpComponentsAsyncClientHttpRequest(HttpAsyncClient httpClient, HttpUriRequest httpRequest, HttpContext httpContext) {
			this.httpClient = httpClient;
			this.httpRequest = httpRequest;
			this.httpContext = httpContext;
		}


		@Override
		public HttpMethod getMethod() {
			return HttpMethod.resolve(this.httpRequest.getMethod());
		}

		@Override
		public URI getURI() {
			return this.httpRequest.getURI();
		}

		@Override
		protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] bufferedOutput)
				throws IOException {

			addHeaders(this.httpRequest, headers);

			if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
				HttpEntity requestEntity = new NByteArrayEntity(bufferedOutput);
				entityEnclosingRequest.setEntity(requestEntity);
			}

			final HttpResponseFutureCallback callback = new HttpResponseFutureCallback();
			final Future<HttpResponse> futureResponse =
					this.httpClient.execute(this.httpRequest, this.httpContext, callback);
			return new ClientHttpResponseFuture(futureResponse, callback);
		}


		private static class HttpResponseFutureCallback implements
				FutureCallback<HttpResponse> {

			private final ListenableFutureCallbackRegistry<ClientHttpResponse> callbacks =
					new ListenableFutureCallbackRegistry<ClientHttpResponse>();

			public void addCallback(ListenableFutureCallback<? super ClientHttpResponse> callback) {
				this.callbacks.addCallback(callback);
			}

			public void addSuccessCallback(SuccessCallback<? super ClientHttpResponse> callback) {
				this.callbacks.addSuccessCallback(callback);
			}

			public void addFailureCallback(FailureCallback callback) {
				this.callbacks.addFailureCallback(callback);
			}

			@Override
			public void completed(HttpResponse result) {
				this.callbacks.success(new HttpComponentsAsyncClientHttpResponse(result));
			}

			@Override
			public void failed(Exception ex) {
				this.callbacks.failure(ex);
			}

			@Override
			public void cancelled() {
			}
		}


		private static class ClientHttpResponseFuture extends FutureAdapter<ClientHttpResponse, HttpResponse>
				implements ListenableFuture<ClientHttpResponse> {

			private final HttpResponseFutureCallback callback;

			public ClientHttpResponseFuture(Future<HttpResponse> futureResponse, HttpResponseFutureCallback callback) {
				super(futureResponse);
				this.callback = callback;
			}

			@Override
			protected ClientHttpResponse adapt(HttpResponse response) {
				return new HttpComponentsAsyncClientHttpResponse(response);
			}

			@Override
			public void addCallback(ListenableFutureCallback<? super ClientHttpResponse> callback) {
				this.callback.addCallback(callback);
			}

			@Override
			public void addCallback(SuccessCallback<? super ClientHttpResponse> successCallback, FailureCallback failureCallback) {
				this.callback.addSuccessCallback(successCallback);
				this.callback.addFailureCallback(failureCallback);
			}
		}


	}

	private static final class HttpComponentsAsyncClientHttpResponse extends
			HttpComponentsClientHttpResponse {


		HttpComponentsAsyncClientHttpResponse(HttpResponse httpResponse) {
			super(httpResponse);
		}


		@Override
		public void close() {
	        // HTTP responses returned by async HTTP client are not bound to an
	        // active connection and do not have to deallocate any resources...
		}

	}
}
