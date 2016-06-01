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

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * {@link AsyncClientHttpRequest} implementation that uses OkHttp 2.x to execute requests.
 *
 * <p>Created via the {@link OkHttpClientHttpRequestFactory}.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @since 4.3
 * @see org.springframework.http.client.OkHttp3AsyncClientHttpRequest
 */
class OkHttpAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

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

		Request request = OkHttpClientHttpRequestFactory.buildRequest(headers, content, this.uri, this.method);
		return new OkHttpListenableFuture(this.client.newCall(request));
	}


	private static class OkHttpListenableFuture extends SettableListenableFuture<ClientHttpResponse> {

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
