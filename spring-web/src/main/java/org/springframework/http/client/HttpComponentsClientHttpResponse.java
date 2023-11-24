/*
 * Copyright 2002-2023 the original author or authors.
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
import java.io.InputStream;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.support.HttpComponentsHeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation based on
 * Apache HttpComponents HttpClient.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequest}.
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 3.1
 * @see HttpComponentsClientHttpRequest#execute()
 */
final class HttpComponentsClientHttpResponse implements ClientHttpResponse {

	private final ClassicHttpResponse httpResponse;

	@Nullable
	private HttpHeaders headers;


	HttpComponentsClientHttpResponse(ClassicHttpResponse httpResponse) {
		this.httpResponse = httpResponse;
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatusCode.valueOf(this.httpResponse.getCode());
	}

	@Override
	public String getStatusText() {
		return this.httpResponse.getReasonPhrase();
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			MultiValueMap<String, String> adapter = new HttpComponentsHeadersAdapter(this.httpResponse);
			this.headers = HttpHeaders.readOnlyHttpHeaders(adapter);
		}
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		HttpEntity entity = this.httpResponse.getEntity();
		return (entity != null ? entity.getContent() : InputStream.nullInputStream());
	}

	@Override
	public void close() {
		// Release underlying connection back to the connection manager
		try {
			try {
				// Attempt to keep connection alive by consuming its remaining content
				EntityUtils.consume(this.httpResponse.getEntity());
			}
			finally {
				this.httpResponse.close();
			}
		}
		catch (IOException ex) {
			// Ignore exception on close...
		}
	}

}
