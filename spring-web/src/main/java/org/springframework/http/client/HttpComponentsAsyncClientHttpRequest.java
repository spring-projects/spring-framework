/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.FutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;


/**
 * {@link ClientHttpRequest} implementation based on
 * Apache HttpComponents HttpAsyncClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 4.0
 * @see HttpComponentsClientHttpRequestFactory#createRequest
 * @deprecated as of Spring 5.0, in favor of
 * {@link org.springframework.http.client.reactive.HttpComponentsClientHttpConnector}
 */
@Deprecated
final class HttpComponentsAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private final HttpAsyncClient httpClient;

	private final HttpUriRequest httpRequest;

	private final HttpContext httpContext;


	HttpComponentsAsyncClientHttpRequest(HttpAsyncClient client, HttpUriRequest request, HttpContext context) {
		this.httpClient = client;
		this.httpRequest = request;
		this.httpContext = context;
	}


	@Override
	public String getMethodValue() {
		return this.httpRequest.getMethod();
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	HttpContext getHttpContext() {
		return this.httpContext;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] bufferedOutput)
			throws IOException {

		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new NByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}

		HttpResponseFutureCallback callback = new HttpResponseFutureCallback(this.httpRequest);
		Future<HttpResponse> futureResponse = this.httpClient.execute(this.httpRequest, this.httpContext, callback);
		return new ClientHttpResponseFuture(futureResponse, callback);
	}


	private static class HttpResponseFutureCallback implements FutureCallback<HttpResponse> {

		private final HttpUriRequest request;

		private final ListenableFutureCallbackRegistry<ClientHttpResponse> callbacks =
				new ListenableFutureCallbackRegistry<>();

		public HttpResponseFutureCallback(HttpUriRequest request) {
			this.request = request;
		}

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
			this.request.abort();
		}
	}


	private static class ClientHttpResponseFuture extends FutureAdapter<ClientHttpResponse, HttpResponse>
			implements ListenableFuture<ClientHttpResponse> {

		private final HttpResponseFutureCallback callback;

		public ClientHttpResponseFuture(Future<HttpResponse> response, HttpResponseFutureCallback callback) {
			super(response);
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
		public void addCallback(SuccessCallback<? super ClientHttpResponse> successCallback,
				FailureCallback failureCallback) {

			this.callback.addSuccessCallback(successCallback);
			this.callback.addFailureCallback(failureCallback);
		}
	}

}
