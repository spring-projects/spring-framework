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

import com.squareup.okhttp.OkHttpClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import static org.springframework.http.client.OkHttpRequestHelper.buildRequest;
import static org.springframework.http.client.OkHttpRequestHelper.getRequestBody;

/**
 * @author Luciano Leggieri
 */
public final class OkHttpAsyncClientHttpRequest extends AbstractAsyncClientHttpRequest
		implements StreamingHttpOutputMessage {

	private final OkHttpClient client;
	private final URI uri;
	private final HttpMethod httpMethod;
	private final OkHttpRequestBody body;

	public OkHttpAsyncClientHttpRequest(OkHttpClient okHttpClient, URI u, HttpMethod method) {
		this.client = okHttpClient;
		uri = u;
		httpMethod = method;
		body = new OkHttpRequestBody();
	}

	@Override
	public HttpMethod getMethod() {
		return httpMethod;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public void setBody(Body b) {
		assertNotExecuted();
		body.setBody(b);
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders ignored) {
		return body.getAsBuffer();
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers)
			throws IOException {
		return new ListenableFutureCall(
				client.newCall(buildRequest(httpMethod, uri, headers, getRequestBody(body))));
	}
}
