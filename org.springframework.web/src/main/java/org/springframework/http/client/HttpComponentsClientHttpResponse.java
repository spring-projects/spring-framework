/*
 * Copyright 2002-2011 the original author or authors.
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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * {@link org.springframework.http.client.ClientHttpResponse} implementation that uses
 * Apache Http Components HttpClient to execute requests.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequest}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 3.0
 * @see HttpComponentsClientHttpRequest#execute()
 */
final class HttpComponentsClientHttpResponse implements ClientHttpResponse {

	private final HttpResponse httpResponse;

	private HttpHeaders headers;

	public HttpComponentsClientHttpResponse(HttpResponse httpResponse) {
		this.httpResponse = httpResponse;
	}

	public HttpStatus getStatusCode() throws IOException {
		return HttpStatus.valueOf(httpResponse.getStatusLine().getStatusCode());
	}

	public String getStatusText() throws IOException {
		return httpResponse.getStatusLine().getReasonPhrase();
	}

	public HttpHeaders getHeaders() {
		if (headers == null) {
			headers = new HttpHeaders();
			for (Header header : httpResponse.getAllHeaders()) {
				headers.add(header.getName(), header.getValue());
			}
		}
		return headers;
	}

	public InputStream getBody() throws IOException {
		HttpEntity entity = httpResponse.getEntity();
		return entity != null ? entity.getContent() : null;
	}

	public void close() {
		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			try {
				// Release underlying connection back to the connection manager
				EntityUtils.consume(entity);
			}
			catch (IOException e) {
				// ignore
			}
		}
	}
}
