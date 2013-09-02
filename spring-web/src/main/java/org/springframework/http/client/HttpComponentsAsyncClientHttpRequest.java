/*
 * Copyright 2002-2013 the original author or authors.
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
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.FutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;

/**
 * {@link ClientHttpRequest} implementation that uses Apache HttpComponents HttpClient to
 * execute requests.
 *
 * <p>Created via the {@link org.springframework.http.client.HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @see org.springframework.http.client.HttpComponentsClientHttpRequestFactory#createRequest(java.net.URI,
 *      org.springframework.http.HttpMethod)
 * @since 3.1
 */
final class HttpComponentsAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private final HttpAsyncClient httpClient;

	private final HttpUriRequest httpRequest;

	private final HttpContext httpContext;

	public HttpComponentsAsyncClientHttpRequest(HttpAsyncClient httpClient,
			HttpUriRequest httpRequest, HttpContext httpContext) {
		this.httpClient = httpClient;
		this.httpRequest = httpRequest;
		this.httpContext = httpContext;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.httpRequest.getMethod());
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers,
			byte[] bufferedOutput) throws IOException {
		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest =
					(HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new NByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}

		final HttpResponseFutureCallback callback = new HttpResponseFutureCallback();

		final Future<HttpResponse> futureResponse =
				this.httpClient.execute(this.httpRequest, this.httpContext, callback);
		return new ClientHttpResponseFuture(futureResponse, callback);
	}

	private static class HttpResponseFutureCallback implements FutureCallback<HttpResponse> {

		private final ListenableFutureCallbackRegistry<ClientHttpResponse> callbacks =
				new ListenableFutureCallbackRegistry<ClientHttpResponse>();

		public void addCallback(
				ListenableFutureCallback<? super ClientHttpResponse> callback) {
			callbacks.addCallback(callback);
		}

		@Override
		public void completed(HttpResponse result) {
			callbacks.success(new HttpComponentsAsyncClientHttpResponse(result));
		}

		@Override
		public void failed(Exception ex) {
			callbacks.failure(ex);
		}

		@Override
		public void cancelled() {
		}

	}


	private static class ClientHttpResponseFuture extends FutureAdapter<ClientHttpResponse, HttpResponse>
			implements ListenableFuture<ClientHttpResponse> {

		private final HttpResponseFutureCallback callback;

		private ClientHttpResponseFuture(Future<HttpResponse> futureResponse,
				HttpResponseFutureCallback callback) {
			super(futureResponse);
			this.callback = callback;
		}

		@Override
		protected ClientHttpResponse adapt(HttpResponse response) {
			return new HttpComponentsAsyncClientHttpResponse(response);
		}

		@Override
		public void addCallback(
				ListenableFutureCallback<? super ClientHttpResponse> callback) {
			this.callback.addCallback(callback);
		}
	}


}
