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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

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
	protected Future<ClientHttpResponse> executeInternal(HttpHeaders headers,
			byte[] bufferedOutput) throws IOException {
		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest entityEnclosingRequest =
					(HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new ByteArrayEntity(bufferedOutput);
			entityEnclosingRequest.setEntity(requestEntity);
		}

		final Future<HttpResponse> futureResponse =
				this.httpClient.execute(this.httpRequest, this.httpContext, null);
		return new ClientHttpResponseFuture(futureResponse);
	}


	private static class ClientHttpResponseFuture implements Future<ClientHttpResponse> {

		private final Future<HttpResponse> futureResponse;


		public ClientHttpResponseFuture(Future<HttpResponse> futureResponse) {
			this.futureResponse = futureResponse;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return futureResponse.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return futureResponse.isCancelled();
		}

		@Override
		public boolean isDone() {
			return futureResponse.isDone();
		}

		@Override
		public ClientHttpResponse get()
				throws InterruptedException, ExecutionException {
			HttpResponse response = futureResponse.get();
			return new HttpComponentsClientHttpResponse(response);
		}

		@Override
		public ClientHttpResponse get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			HttpResponse response = futureResponse.get(timeout, unit);
			return new HttpComponentsClientHttpResponse(response);
		}

	}


}
